package com.navfinance.model;

import java.time.LocalDate;

/**
 * Encapsulates search and filtering criteria for ledger transactions and audit logs.
 * <p>
 * Supports multi-dimensional filtering across transaction type, primary category,
 * sub-category, payment channel, linked person, date ranges, min/max monetary bounds,
 * planned status, and full-text search keywords, with customizable sort columns and orders.
 * </p>
 */
public class FilterCriteria {
    /** Target transaction type (Income/Expense), or {@code null} to include all types. */
    private TransactionType type;

    /** Primary category name filter (e.g., "Salary", "EMI Payment"), or {@code null}. */
    private String category;

    /** Sub-category name filter (e.g., "Home Loan", "Electricity"), or {@code null}. */
    private String subCategory;

    /** Payment mode or channel filter (e.g., "UPI", "Cash", "Paytm"), or {@code null}. */
    private String paymentVia;

    /** Unique person ID for Udhar tracking, or {@code null}. */
    private Long personId;

    /** Earliest transaction date boundary (inclusive). */
    private LocalDate startDate;

    /** Latest transaction date boundary (inclusive). */
    private LocalDate endDate;

    /** Minimum transaction amount boundary (inclusive). */
    private Double minAmount;

    /** Maximum transaction amount boundary (inclusive). */
    private Double maxAmount;

    /** Flag filtering planned/expected entries vs realized entries, or {@code null} for both. */
    private Boolean expected;

    /** Keyword string for substring matching against notes, payee, category, and sub-category. */
    private String searchKeyword;

    /** Database column or property to sort by (defaults to {@code "tx_date"}). */
    private String sortBy = "tx_date";

    /** Sort direction: {@code true} for ascending, {@code false} for descending (default). */
    private boolean sortAscending = false;

    /**
     * Default constructor initializing filter with default date descending sort.
     */
    public FilterCriteria() {
    }

    /**
     * Gets the transaction type filter.
     *
     * @return transaction type or {@code null}
     */
    public TransactionType getType() {
        return type;
    }

    /**
     * Sets the transaction type filter.
     *
     * @param type transaction type or {@code null} for all
     */
    public void setType(TransactionType type) {
        this.type = type;
    }

    /**
     * Gets the category filter.
     *
     * @return category string or {@code null}
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the category filter.
     *
     * @param category category string or {@code null}
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Gets the sub-category filter.
     *
     * @return sub-category string or {@code null}
     */
    public String getSubCategory() {
        return subCategory;
    }

    /**
     * Sets the sub-category filter.
     *
     * @param subCategory sub-category string or {@code null}
     */
    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }

    /**
     * Gets the payment channel filter.
     *
     * @return payment channel or {@code null}
     */
    public String getPaymentVia() {
        return paymentVia;
    }

    /**
     * Sets the payment channel filter.
     *
     * @param paymentVia payment mode string
     */
    public void setPaymentVia(String paymentVia) {
        this.paymentVia = paymentVia;
    }

    /**
     * Gets the linked person ID filter.
     *
     * @return person ID or {@code null}
     */
    public Long getPersonId() {
        return personId;
    }

    /**
     * Sets the linked person ID filter.
     *
     * @param personId person ID or {@code null}
     */
    public void setPersonId(Long personId) {
        this.personId = personId;
    }

    /**
     * Gets the start date filter.
     *
     * @return start date (inclusive)
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Sets the start date filter.
     *
     * @param startDate start date (inclusive)
     */
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    /**
     * Gets the end date filter.
     *
     * @return end date (inclusive)
     */
    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * Sets the end date filter.
     *
     * @param endDate end date (inclusive)
     */
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    /**
     * Gets the minimum monetary amount.
     *
     * @return minimum amount or {@code null}
     */
    public Double getMinAmount() {
        return minAmount;
    }

    /**
     * Sets the minimum monetary amount.
     *
     * @param minAmount minimum amount or {@code null}
     */
    public void setMinAmount(Double minAmount) {
        this.minAmount = minAmount;
    }

    /**
     * Gets the maximum monetary amount.
     *
     * @return maximum amount or {@code null}
     */
    public Double getMaxAmount() {
        return maxAmount;
    }

    /**
     * Sets the maximum monetary amount.
     *
     * @param maxAmount maximum amount or {@code null}
     */
    public void setMaxAmount(Double maxAmount) {
        this.maxAmount = maxAmount;
    }

    /**
     * Gets the expected/planned status filter.
     *
     * @return {@code Boolean.TRUE} for planned only, {@code Boolean.FALSE} for realized only, or {@code null}
     */
    public Boolean getExpected() {
        return expected;
    }

    /**
     * Sets the expected/planned status filter.
     *
     * @param expected boolean flag or {@code null}
     */
    public void setExpected(Boolean expected) {
        this.expected = expected;
    }

    /**
     * Gets the search keyword.
     *
     * @return search keyword string
     */
    public String getSearchKeyword() {
        return searchKeyword;
    }

    /**
     * Sets the search keyword.
     *
     * @param searchKeyword search string
     */
    public void setSearchKeyword(String searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    /**
     * Gets the sort column name.
     *
     * @return column name (e.g. "tx_date", "amount")
     */
    public String getSortBy() {
        return sortBy;
    }

    /**
     * Sets the sort column name.
     *
     * @param sortBy column name
     */
    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    /**
     * Checks if ascending sort order is active.
     *
     * @return {@code true} if ascending, {@code false} if descending
     */
    public boolean isSortAscending() {
        return sortAscending;
    }

    /**
     * Sets sort direction.
     *
     * @param sortAscending {@code true} for ascending, {@code false} for descending
     */
    public void setSortAscending(boolean sortAscending) {
        this.sortAscending = sortAscending;
    }
}

