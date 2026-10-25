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
 * historical income/expense bar comparisons, and loan debt positions.
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

        // Loans & Dues
        double totalLent = transactionDao.getTotalLentAmount();
        double totalBorrowed = transactionDao.getTotalBorrowedAmount();
        summary.setTotalLent(totalLent);
        summary.setTotalBorrowed(totalBorrowed);
        summary.setNetLoanBalance(totalLent - totalBorrowed);


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

    /**
     * Computes multi-horizon entity tracking analytics and temporal trend curves for a specified
     * Person, Bill, EMI, Credit Card, or Category across Days, Week, Months, Quarter, or Years.
     *
     * @param entityType "PERSON", "BILL", "EMI", "CREDIT_CARD", or "CATEGORY"
     * @param entityId Primary key ID of the master entity (or null if category)
     * @param categoryName Category name string (used when entityType is "CATEGORY")
     * @param timeGranularity "DAYS", "WEEK", "MONTHS", "QUARTER", or "YEARS"
     * @param startDate Optional inclusive start date limit
     * @param endDate Optional inclusive end date limit
     * @return Fully populated {@link com.navfinance.model.EntityTrackerSummary}
     */
    public com.navfinance.model.EntityTrackerSummary getEntityTrackerSummary(String entityType, Long entityId,
                                                                             String categoryName, String timeGranularity,
                                                                             LocalDate startDate, LocalDate endDate) {
        com.navfinance.model.EntityTrackerSummary summary = new com.navfinance.model.EntityTrackerSummary();
        summary.setEntityType(entityType != null ? entityType.toUpperCase() : "CATEGORY");
        summary.setEntityId(entityId);
        summary.setTimeGranularity(timeGranularity != null ? timeGranularity.toUpperCase() : "MONTHS");

        FilterCriteria criteria = new FilterCriteria();
        criteria.setSortBy("tx_date");
        criteria.setSortAscending(true);

        if (startDate != null) criteria.setStartDate(startDate);
        if (endDate != null) criteria.setEndDate(endDate);

        String entityName = null;
        if ("PERSON".equalsIgnoreCase(entityType)) {
            criteria.setPersonId(entityId);
            com.navfinance.model.Person p = new com.navfinance.dao.PersonDao().findById(entityId);
            if (p != null) entityName = p.getName();
        } else if ("BILL".equalsIgnoreCase(entityType)) {
            criteria.setBillId(entityId);
            com.navfinance.model.Bill b = new com.navfinance.dao.BillDao().findById(entityId);
            if (b != null) entityName = b.getBillName();
        } else if ("EMI".equalsIgnoreCase(entityType)) {
            criteria.setEmiId(entityId);
            com.navfinance.model.Emi e = new com.navfinance.dao.EmiDao().findById(entityId);
            if (e != null) entityName = e.getEmiName();
        } else if ("CREDIT_CARD".equalsIgnoreCase(entityType)) {
            criteria.setCreditCardId(entityId);
            com.navfinance.model.CreditCard c = new com.navfinance.dao.CreditCardDao().findById(entityId);
            if (c != null) entityName = c.getCardName();
        } else if ("CATEGORY".equalsIgnoreCase(entityType)) {
            criteria.setCategory(categoryName);
            entityName = (categoryName != null && !categoryName.isBlank()) ? categoryName : "All Categories";
        }

        List<Transaction> txList = transactionDao.findByCriteria(criteria);
        summary.setTransactions(txList);
        summary.setTotalCount(txList.size());

        if (entityName == null && !txList.isEmpty()) {
            Transaction first = txList.get(0);
            if ("PERSON".equalsIgnoreCase(entityType)) entityName = first.getPersonName();
            else if ("BILL".equalsIgnoreCase(entityType)) entityName = first.getBillName();
            else if ("EMI".equalsIgnoreCase(entityType)) entityName = first.getEmiName();
            else if ("CREDIT_CARD".equalsIgnoreCase(entityType)) entityName = first.getCardName();
        }
        if (entityName == null) {
            entityName = entityType + (entityId != null ? " #" + entityId : "");
        }
        summary.setEntityName(entityName);

        if (txList.isEmpty()) {
            return summary;
        }

        double totalIn = 0.0;
        double totalOut = 0.0;
        LocalDate earliest = null;
        LocalDate latest = null;

        for (Transaction tx : txList) {
            if (tx.getType() == TransactionType.INCOME) {
                totalIn += tx.getAmount();
            } else if (tx.getType() == TransactionType.EXPENSE) {
                totalOut += tx.getAmount();
            }
            LocalDate d = tx.getTxDate() != null ? tx.getTxDate() : LocalDate.now();
            if (earliest == null || d.isBefore(earliest)) earliest = d;
            if (latest == null || d.isAfter(latest)) latest = d;
        }

        summary.setTotalInflow(totalIn);
        summary.setTotalOutflow(totalOut);
        summary.setNetBalance(totalIn - totalOut);
        summary.setEarliestDate(earliest);
        summary.setLatestDate(latest);

        // Build time-bucketed trend points
        List<DashboardSummary.TrendPoint> points = buildTrendPoints(txList, summary.getTimeGranularity(), earliest, latest, startDate, endDate);
        summary.setTrendPoints(points);

        if (!points.isEmpty()) {
            double totalVolume = totalIn + totalOut;
            summary.setAveragePerBucket(totalVolume / points.size());
        }

        return summary;
    }

    /**
     * Overloaded helper to aggregate transactions into trend points with auto-discovered date bounds.
     */
    public List<DashboardSummary.TrendPoint> buildTrendPoints(List<Transaction> txList, String granularity) {
        LocalDate earliest = null;
        LocalDate latest = null;
        for (Transaction tx : txList) {
            LocalDate d = tx.getTxDate() != null ? tx.getTxDate() : LocalDate.now();
            if (earliest == null || d.isBefore(earliest)) earliest = d;
            if (latest == null || d.isAfter(latest)) latest = d;
        }
        return buildTrendPoints(txList, granularity, earliest, latest, null, null);
    }

    /**
     * Aggregates transactions into discrete chronological time buckets based on requested granularity.
     */
    public List<DashboardSummary.TrendPoint> buildTrendPoints(List<Transaction> txList, String granularity,
                                                              LocalDate earliest, LocalDate latest,
                                                              LocalDate filterStart, LocalDate filterEnd) {
        List<DashboardSummary.TrendPoint> points = new ArrayList<>();
        LocalDate start = filterStart != null ? filterStart : earliest;
        LocalDate end = filterEnd != null ? filterEnd : latest;
        if (start == null) start = LocalDate.now().minusMonths(6);
        if (end == null) end = LocalDate.now();
        if (start.isAfter(end)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }

        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("dd MMM");
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMM yy");

        if ("DAYS".equalsIgnoreCase(granularity)) {
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(start, end);
            if (daysBetween <= 45) {
                // Continuous day-by-day sequence
                for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                    double in = 0.0;
                    double out = 0.0;
                    for (Transaction tx : txList) {
                        if (d.equals(tx.getTxDate())) {
                            if (tx.getType() == TransactionType.INCOME) in += tx.getAmount();
                            else if (tx.getType() == TransactionType.EXPENSE) out += tx.getAmount();
                        }
                    }
                    points.add(new DashboardSummary.TrendPoint(d.format(dayFmt), in, out));
                }
            } else {
                // Group by active transaction dates to prevent overcrowding
                java.util.Map<LocalDate, double[]> map = new java.util.TreeMap<>();
                for (Transaction tx : txList) {
                    LocalDate d = tx.getTxDate() != null ? tx.getTxDate() : LocalDate.now();
                    double[] vals = map.computeIfAbsent(d, k -> new double[2]);
                    if (tx.getType() == TransactionType.INCOME) vals[0] += tx.getAmount();
                    else if (tx.getType() == TransactionType.EXPENSE) vals[1] += tx.getAmount();
                }
                for (java.util.Map.Entry<LocalDate, double[]> entry : map.entrySet()) {
                    points.add(new DashboardSummary.TrendPoint(entry.getKey().format(dayFmt), entry.getValue()[0], entry.getValue()[1]));
                }
            }

        } else if ("WEEK".equalsIgnoreCase(granularity)) {
            LocalDate wStart = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate wLimit = end.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            while (!wStart.isAfter(wLimit)) {
                LocalDate wEnd = wStart.plusDays(6);
                double in = 0.0;
                double out = 0.0;
                for (Transaction tx : txList) {
                    LocalDate d = tx.getTxDate() != null ? tx.getTxDate() : LocalDate.now();
                    if (!d.isBefore(wStart) && !d.isAfter(wEnd)) {
                        if (tx.getType() == TransactionType.INCOME) in += tx.getAmount();
                        else if (tx.getType() == TransactionType.EXPENSE) out += tx.getAmount();
                    }
                }
                points.add(new DashboardSummary.TrendPoint("Wk " + wStart.format(dayFmt), in, out));
                wStart = wStart.plusWeeks(1);
            }

        } else if ("QUARTER".equalsIgnoreCase(granularity)) {
            int startQ = (start.getMonthValue() - 1) / 3 + 1;
            int startYear = start.getYear();
            int endQ = (end.getMonthValue() - 1) / 3 + 1;
            int endYear = end.getYear();

            int currQ = startQ;
            int currYear = startYear;
            while (currYear < endYear || (currYear == endYear && currQ <= endQ)) {
                LocalDate qStart = LocalDate.of(currYear, (currQ - 1) * 3 + 1, 1);
                LocalDate qEnd = qStart.plusMonths(3).minusDays(1);

                double in = 0.0;
                double out = 0.0;
                for (Transaction tx : txList) {
                    LocalDate d = tx.getTxDate() != null ? tx.getTxDate() : LocalDate.now();
                    if (!d.isBefore(qStart) && !d.isAfter(qEnd)) {
                        if (tx.getType() == TransactionType.INCOME) in += tx.getAmount();
                        else if (tx.getType() == TransactionType.EXPENSE) out += tx.getAmount();
                    }
                }
                points.add(new DashboardSummary.TrendPoint("Q" + currQ + "'" + String.valueOf(currYear).substring(2), in, out));

                currQ++;
                if (currQ > 4) {
                    currQ = 1;
                    currYear++;
                }
            }

        } else if ("YEARS".equalsIgnoreCase(granularity)) {
            for (int y = start.getYear(); y <= end.getYear(); y++) {
                LocalDate yStart = LocalDate.of(y, 1, 1);
                LocalDate yEnd = LocalDate.of(y, 12, 31);
                double in = 0.0;
                double out = 0.0;
                for (Transaction tx : txList) {
                    LocalDate d = tx.getTxDate() != null ? tx.getTxDate() : LocalDate.now();
                    if (!d.isBefore(yStart) && !d.isAfter(yEnd)) {
                        if (tx.getType() == TransactionType.INCOME) in += tx.getAmount();
                        else if (tx.getType() == TransactionType.EXPENSE) out += tx.getAmount();
                    }
                }
                points.add(new DashboardSummary.TrendPoint(String.valueOf(y), in, out));
            }

        } else {
            // Default: MONTHS
            java.time.YearMonth ymStart = java.time.YearMonth.from(start);
            java.time.YearMonth ymEnd = java.time.YearMonth.from(end);
            java.time.YearMonth curr = ymStart;
            while (!curr.isAfter(ymEnd)) {
                LocalDate mStart = curr.atDay(1);
                LocalDate mEnd = curr.atEndOfMonth();
                double in = 0.0;
                double out = 0.0;
                for (Transaction tx : txList) {
                    LocalDate d = tx.getTxDate() != null ? tx.getTxDate() : LocalDate.now();
                    if (!d.isBefore(mStart) && !d.isAfter(mEnd)) {
                        if (tx.getType() == TransactionType.INCOME) in += tx.getAmount();
                        else if (tx.getType() == TransactionType.EXPENSE) out += tx.getAmount();
                    }
                }
                points.add(new DashboardSummary.TrendPoint(curr.format(monthFmt), in, out));
                curr = curr.plusMonths(1);
            }
        }

        return points;
    }
}
