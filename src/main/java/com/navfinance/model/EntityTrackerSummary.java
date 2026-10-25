package com.navfinance.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates analytical aggregates, temporal trend curves, and itemized ledger transactions
 * when tracking a specific Person, Bill, EMI, Credit Card, or Category across customizable
 * time horizons (Days, Week, Months, Quarter, Years).
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public class EntityTrackerSummary {

    private String entityType; // "PERSON", "BILL", "EMI", "CREDIT_CARD", "CATEGORY"
    private Long entityId;
    private String entityName;
    private String timeGranularity; // "DAYS", "WEEK", "MONTHS", "QUARTER", "YEARS"
    private int totalCount = 0;
    private double totalInflow = 0.0;
    private double totalOutflow = 0.0;
    private double netBalance = 0.0;
    private double averagePerBucket = 0.0;
    private LocalDate earliestDate;
    private LocalDate latestDate;
    private List<DashboardSummary.TrendPoint> trendPoints = new ArrayList<>();
    private List<Transaction> transactions = new ArrayList<>();

    public EntityTrackerSummary() {}

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }

    public String getTimeGranularity() { return timeGranularity; }
    public void setTimeGranularity(String timeGranularity) { this.timeGranularity = timeGranularity; }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

    public double getTotalInflow() { return totalInflow; }
    public void setTotalInflow(double totalInflow) { this.totalInflow = totalInflow; }

    public double getTotalOutflow() { return totalOutflow; }
    public void setTotalOutflow(double totalOutflow) { this.totalOutflow = totalOutflow; }

    public double getNetBalance() { return netBalance; }
    public void setNetBalance(double netBalance) { this.netBalance = netBalance; }

    public double getAveragePerBucket() { return averagePerBucket; }
    public void setAveragePerBucket(double averagePerBucket) { this.averagePerBucket = averagePerBucket; }

    public LocalDate getEarliestDate() { return earliestDate; }
    public void setEarliestDate(LocalDate earliestDate) { this.earliestDate = earliestDate; }

    public LocalDate getLatestDate() { return latestDate; }
    public void setLatestDate(LocalDate latestDate) { this.latestDate = latestDate; }

    public List<DashboardSummary.TrendPoint> getTrendPoints() { return trendPoints; }
    public void setTrendPoints(List<DashboardSummary.TrendPoint> trendPoints) {
        this.trendPoints = trendPoints != null ? trendPoints : new ArrayList<>();
    }

    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions != null ? transactions : new ArrayList<>();
    }
}
