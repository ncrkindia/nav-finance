package com.navfinance.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data aggregation model encapsulating financial metrics, category breakdowns,
 * historical bar trends, and recent transaction history for the executive dashboard.
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public class DashboardSummary {

    private String periodType;
    private String periodLabel;
    private LocalDate startDate;
    private LocalDate endDate;

    private double totalIncome;
    private double totalExpense;
    private double netBalance;

    private double totalLent;
    private double totalBorrowed;
    private double netLoanBalance;

    private Map<String, Double> incomeByCategory = new LinkedHashMap<>();
    private Map<String, Double> expenseByCategory = new LinkedHashMap<>();
    private List<TrendPoint> trendPoints = new ArrayList<>();
    private List<Transaction> recentTransactions = new ArrayList<>();

    /**
     * Value object holding dual income and expense data points for chart bars with date bounds.
     */
    public static class TrendPoint {
        private final String label;
        private final double income;
        private final double expense;
        private LocalDate startDate;
        private LocalDate endDate;
        private String fullTitle;

        /**
         * Constructs a basic chart trend point.
         *
         * @param label Category or temporal step label (e.g., "May 2026").
         * @param income Total income amount.
         * @param expense Total expense amount.
         */
        public TrendPoint(String label, double income, double expense) {
            this(label, income, expense, null, null, label);
        }

        /**
         * Constructs a fully qualified trend point with temporal boundaries and detailed title.
         *
         * @param label Short step label for axis
         * @param income Total income amount
         * @param expense Total expense amount
         * @param startDate Inclusive start date
         * @param endDate Inclusive end date
         * @param fullTitle Detailed title for tooltips and headers
         */
        public TrendPoint(String label, double income, double expense, LocalDate startDate, LocalDate endDate, String fullTitle) {
            this.label = label;
            this.income = income;
            this.expense = expense;
            this.startDate = startDate;
            this.endDate = endDate;
            this.fullTitle = fullTitle != null ? fullTitle : label;
        }

        /** @return Step label */
        public String getLabel() { return label; }
        /** @return Income figure */
        public double getIncome() { return income; }
        /** @return Expense figure */
        public double getExpense() { return expense; }
        /** @return Inclusive start date or null */
        public LocalDate getStartDate() { return startDate; }
        /** @param startDate Inclusive start date */
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        /** @return Inclusive end date or null */
        public LocalDate getEndDate() { return endDate; }
        /** @param endDate Inclusive end date */
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        /** @return Formatted title for display in tooltips and drilldowns */
        public String getFullTitle() { return fullTitle != null ? fullTitle : label; }
        /** @param fullTitle Formatted title */
        public void setFullTitle(String fullTitle) { this.fullTitle = fullTitle; }
    }

    /** Default constructor. */
    public DashboardSummary() {}

    /** @return Period type (e.g. Weekly, Monthly, Quarterly, Annually) */
    public String getPeriodType() { return periodType; }
    /** @param periodType Period type */
    public void setPeriodType(String periodType) { this.periodType = periodType; }

    /** @return Display label for the period (e.g. "October 2026") */
    public String getPeriodLabel() { return periodLabel; }
    /** @param periodLabel Display label */
    public void setPeriodLabel(String periodLabel) { this.periodLabel = periodLabel; }

    /** @return Period start date */
    public LocalDate getStartDate() { return startDate; }
    /** @param startDate Period start date */
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    /** @return Period end date */
    public LocalDate getEndDate() { return endDate; }
    /** @param endDate Period end date */
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    /** @return Total income amount in period */
    public double getTotalIncome() { return totalIncome; }
    /** @param totalIncome Total income amount */
    public void setTotalIncome(double totalIncome) { this.totalIncome = totalIncome; }

    /** @return Total expense amount in period */
    public double getTotalExpense() { return totalExpense; }
    /** @param totalExpense Total expense amount */
    public void setTotalExpense(double totalExpense) { this.totalExpense = totalExpense; }

    /** @return Net balance (Income - Expense) */
    public double getNetBalance() { return netBalance; }
    /** @param netBalance Net balance */
    public void setNetBalance(double netBalance) { this.netBalance = netBalance; }

    /** @return Total outstanding money lent to others */
    public double getTotalLent() { return totalLent; }
    /** @param totalLent Total money lent */
    public void setTotalLent(double totalLent) { this.totalLent = totalLent; }

    /** @return Total outstanding money borrowed from others */
    public double getTotalBorrowed() { return totalBorrowed; }
    /** @param totalBorrowed Total money borrowed */
    public void setTotalBorrowed(double totalBorrowed) { this.totalBorrowed = totalBorrowed; }

    /** @return Net loan position (Lent - Borrowed) */
    public double getNetLoanBalance() { return netLoanBalance; }
    /** @param netLoanBalance Net loan position */
    public void setNetLoanBalance(double netLoanBalance) { this.netLoanBalance = netLoanBalance; }


    /** @return Map of income totals by category */
    public Map<String, Double> getIncomeByCategory() { return incomeByCategory; }
    /** @param incomeByCategory Map of income by category */
    public void setIncomeByCategory(Map<String, Double> incomeByCategory) { this.incomeByCategory = incomeByCategory; }

    /** @return Map of expense totals by category */
    public Map<String, Double> getExpenseByCategory() { return expenseByCategory; }
    /** @param expenseByCategory Map of expense by category */
    public void setExpenseByCategory(Map<String, Double> expenseByCategory) { this.expenseByCategory = expenseByCategory; }

    /** @return List of time-series trend points */
    public List<TrendPoint> getTrendPoints() { return trendPoints; }
    /** @param trendPoints List of trend points */
    public void setTrendPoints(List<TrendPoint> trendPoints) { this.trendPoints = trendPoints; }

    /** @return List of recent transactions */
    public List<Transaction> getRecentTransactions() { return recentTransactions; }
    /** @param recentTransactions List of recent transactions */
    public void setRecentTransactions(List<Transaction> recentTransactions) { this.recentTransactions = recentTransactions; }
}
