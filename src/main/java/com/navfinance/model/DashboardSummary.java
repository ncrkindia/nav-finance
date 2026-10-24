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

    private double totalUdharGiven;
    private double totalUdharTaken;
    private double netUdharBalance;

    private Map<String, Double> incomeByCategory = new LinkedHashMap<>();
    private Map<String, Double> expenseByCategory = new LinkedHashMap<>();
    private List<TrendPoint> trendPoints = new ArrayList<>();
    private List<Transaction> recentTransactions = new ArrayList<>();

    /**
     * Immutable value object holding dual income and expense data points for chart bars.
     */
    public static class TrendPoint {
        private final String label;
        private final double income;
        private final double expense;

        /**
         * Constructs a chart trend point.
         *
         * @param label Category or temporal step label (e.g., "May 2026").
         * @param income Total income amount.
         * @param expense Total expense amount.
         */
        public TrendPoint(String label, double income, double expense) {
            this.label = label;
            this.income = income;
            this.expense = expense;
        }

        /** @return Step label */
        public String getLabel() { return label; }
        /** @return Income figure */
        public double getIncome() { return income; }
        /** @return Expense figure */
        public double getExpense() { return expense; }
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

    /** @return Total outstanding Udhaar given to others */
    public double getTotalUdharGiven() { return totalUdharGiven; }
    /** @param totalUdharGiven Total Udhaar given */
    public void setTotalUdharGiven(double totalUdharGiven) { this.totalUdharGiven = totalUdharGiven; }

    /** @return Total outstanding Udhaar taken from others */
    public double getTotalUdharTaken() { return totalUdharTaken; }
    /** @param totalUdharTaken Total Udhaar taken */
    public void setTotalUdharTaken(double totalUdharTaken) { this.totalUdharTaken = totalUdharTaken; }

    /** @return Net Udhaar position (Given - Taken) */
    public double getNetUdharBalance() { return netUdharBalance; }
    /** @param netUdharBalance Net Udhaar position */
    public void setNetUdharBalance(double netUdharBalance) { this.netUdharBalance = netUdharBalance; }

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
