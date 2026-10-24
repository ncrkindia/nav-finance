package com.navfinance.service;

import com.navfinance.dao.TransactionDao;
import com.navfinance.model.DashboardSummary;
import com.navfinance.model.FilterCriteria;
import com.navfinance.model.Transaction;
import com.navfinance.model.TransactionType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for computing analytical summaries, trend curves,
 * and KPI metrics across configurable time horizons (Weekly, Monthly, Quarterly, Annually).
 * <p>
 * Aggregates net cash balances, category distribution maps for pie/donut charts,
 * historical income/expense bar comparisons, and Udhar debt positions.
 * </p>
 */
public class AnalyticsService {

    private static AnalyticsService instance;
    private final TransactionDao transactionDao = new TransactionDao();

    /**
     * Private constructor for singleton pattern.
     */
    private AnalyticsService() {}

    /**
     * Returns the singleton instance of {@code AnalyticsService}.
     *
     * @return singleton service instance
     */
    public static synchronized AnalyticsService getInstance() {
        if (instance == null) {
            instance = new AnalyticsService();
        }
        return instance;
    }


    /**
     * Computes the complete dashboard analytical summary for a designated timeframe and offset.
     *
     * @param periodType Time aggregate bucket: "Weekly", "Monthly", "Quarterly", or "Annually"
     * @param offset Relative shift from current period (0 = current, -1 = previous, +1 = next)
     * @return Fully populated {@link DashboardSummary} model
     */
    public DashboardSummary getSummary(String periodType, int offset) {
        DashboardSummary summary = new DashboardSummary();
        summary.setPeriodType(periodType);

        LocalDate now = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate;
        String periodLabel;

        List<DashboardSummary.TrendPoint> trendPoints = new ArrayList<>();

        if ("Weekly".equalsIgnoreCase(periodType)) {
            LocalDate base = now.plusWeeks(offset);
            startDate = base.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            endDate = base.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            periodLabel = "Week of " + startDate.format(DateTimeFormatter.ofPattern("dd MMM")) + " - " + endDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));

            // Trend for past 6 weeks
            for (int i = 5; i >= 0; i--) {
                LocalDate wBase = base.minusWeeks(i);
                LocalDate wStart = wBase.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate wEnd = wBase.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                double in = transactionDao.sumAmountByTypeAndDateRange(TransactionType.INCOME, wStart, wEnd);
                double out = transactionDao.sumAmountByTypeAndDateRange(TransactionType.EXPENSE, wStart, wEnd);
                trendPoints.add(new DashboardSummary.TrendPoint(wStart.format(DateTimeFormatter.ofPattern("dd/MM")), in, out));
            }

        } else if ("Quarterly".equalsIgnoreCase(periodType)) {
            LocalDate base = now.plusMonths(offset * 3L);
            int currentQuarter = (base.getMonthValue() - 1) / 3 + 1;
            int startMonth = (currentQuarter - 1) * 3 + 1;
            startDate = LocalDate.of(base.getYear(), startMonth, 1);
            endDate = startDate.plusMonths(3).minusDays(1);
            periodLabel = "Q" + currentQuarter + " " + base.getYear() + " (" + startDate.format(DateTimeFormatter.ofPattern("MMM")) + " - " + endDate.format(DateTimeFormatter.ofPattern("MMM yyyy")) + ")";

            // Trend for past 4 quarters
            for (int i = 3; i >= 0; i--) {
                LocalDate qBase = base.minusMonths(i * 3L);
                int q = (qBase.getMonthValue() - 1) / 3 + 1;
                LocalDate qStart = LocalDate.of(qBase.getYear(), (q - 1) * 3 + 1, 1);
                LocalDate qEnd = qStart.plusMonths(3).minusDays(1);
                double in = transactionDao.sumAmountByTypeAndDateRange(TransactionType.INCOME, qStart, qEnd);
                double out = transactionDao.sumAmountByTypeAndDateRange(TransactionType.EXPENSE, qStart, qEnd);
                trendPoints.add(new DashboardSummary.TrendPoint("Q" + q + "'" + String.valueOf(qBase.getYear()).substring(2), in, out));
            }

        } else if ("Annually".equalsIgnoreCase(periodType)) {
            LocalDate base = now.plusYears(offset);
            startDate = LocalDate.of(base.getYear(), 1, 1);
            endDate = LocalDate.of(base.getYear(), 12, 31);
            periodLabel = "Year " + base.getYear();

            // Trend for 12 months in this year
            for (int m = 1; m <= 12; m++) {
                LocalDate mStart = LocalDate.of(base.getYear(), m, 1);
                LocalDate mEnd = mStart.with(TemporalAdjusters.lastDayOfMonth());
                double in = transactionDao.sumAmountByTypeAndDateRange(TransactionType.INCOME, mStart, mEnd);
                double out = transactionDao.sumAmountByTypeAndDateRange(TransactionType.EXPENSE, mStart, mEnd);
                trendPoints.add(new DashboardSummary.TrendPoint(mStart.format(DateTimeFormatter.ofPattern("MMM")), in, out));
            }

        } else {
            // Default: Monthly
            summary.setPeriodType("Monthly");
            LocalDate base = now.plusMonths(offset);
            startDate = base.withDayOfMonth(1);
            endDate = base.with(TemporalAdjusters.lastDayOfMonth());
            periodLabel = base.format(DateTimeFormatter.ofPattern("MMMM yyyy"));

            // Trend for past 6 months
            for (int i = 5; i >= 0; i--) {
                LocalDate mBase = base.minusMonths(i);
                LocalDate mStart = mBase.withDayOfMonth(1);
                LocalDate mEnd = mBase.with(TemporalAdjusters.lastDayOfMonth());
                double in = transactionDao.sumAmountByTypeAndDateRange(TransactionType.INCOME, mStart, mEnd);
                double out = transactionDao.sumAmountByTypeAndDateRange(TransactionType.EXPENSE, mStart, mEnd);
                trendPoints.add(new DashboardSummary.TrendPoint(mStart.format(DateTimeFormatter.ofPattern("MMM yy")), in, out));
            }
        }

        summary.setStartDate(startDate);
        summary.setEndDate(endDate);
        summary.setPeriodLabel(periodLabel);

        double totalIn = transactionDao.sumAmountByTypeAndDateRange(TransactionType.INCOME, startDate, endDate);
        double totalOut = transactionDao.sumAmountByTypeAndDateRange(TransactionType.EXPENSE, startDate, endDate);
        summary.setTotalIncome(totalIn);
        summary.setTotalExpense(totalOut);
        summary.setNetBalance(totalIn - totalOut);

        // Category breakdown
        summary.setIncomeByCategory(transactionDao.getCategoryBreakdown(TransactionType.INCOME, startDate, endDate));
        summary.setExpenseByCategory(transactionDao.getCategoryBreakdown(TransactionType.EXPENSE, startDate, endDate));

        // Trend
        summary.setTrendPoints(trendPoints);

        // Udhar
        double udharGiven = transactionDao.getTotalUdharGiven();
        double udharTaken = transactionDao.getTotalUdharTaken();
        summary.setTotalUdharGiven(udharGiven);
        summary.setTotalUdharTaken(udharTaken);
        summary.setNetUdharBalance(udharGiven - udharTaken);

        // Recent 10 transactions
        FilterCriteria recentCriteria = new FilterCriteria();
        recentCriteria.setSortBy("tx_date");
        recentCriteria.setSortAscending(false);
        List<Transaction> all = transactionDao.findByCriteria(recentCriteria);
        if (all.size() > 10) {
            summary.setRecentTransactions(all.subList(0, 10));
        } else {
            summary.setRecentTransactions(all);
        }

        return summary;
    }
}
