package com.navfinance.model;

import com.navfinance.config.AppConfig;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    /** Unique person ID for loan and dues tracking, or {@code null}. */
    private Long personId;

    /** Denormalized person name for display and report metadata. */
    private String personName;

    /** Unique bill ID filter, or {@code null}. */
    private Long billId;

    /** Denormalized bill name for display and report metadata. */
    private String billName;

    /** Unique EMI ID filter, or {@code null}. */
    private Long emiId;

    /** Denormalized EMI name for display and report metadata. */
    private String emiName;

    /** Unique credit card ID filter, or {@code null}. */
    private Long creditCardId;

    /** Denormalized credit card name for display and report metadata. */
    private String cardName;

    /** Display name of active date preset (e.g., "This Month", "Today", "All Time"). */
    private String datePresetName;

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
    /**
     * Gets the linked person name filter.
     *
     * @return person name or {@code null}
     */
    public String getPersonName() {
        return personName;
    }

    /**
     * Sets the linked person name filter.
     *
     * @param personName person name or {@code null}
     */
    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getBillName() {
        return billName;
    }

    public void setBillName(String billName) {
        this.billName = billName;
    }

    public Long getEmiId() {
        return emiId;
    }

    public void setEmiId(Long emiId) {
        this.emiId = emiId;
    }

    public String getEmiName() {
        return emiName;
    }

    public void setEmiName(String emiName) {
        this.emiName = emiName;
    }

    public Long getCreditCardId() {
        return creditCardId;
    }

    public void setCreditCardId(Long creditCardId) {
        this.creditCardId = creditCardId;
    }

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    /**
     * Gets the display name of the active date preset.
     *
     * @return preset name or {@code null}
     */
    public String getDatePresetName() {
        return datePresetName;
    }

    /**
     * Sets the display name of the active date preset.
     *
     * @param datePresetName preset name or {@code null}
     */
    public void setDatePresetName(String datePresetName) {
        this.datePresetName = datePresetName;
    }

    /**
     * Returns a human-friendly representation of the date filter.
     * E.g. "01-Oct-2026 to 25-Oct-2026 (This Month)" or "All Time".
     *
     * @return formatted date range string
     */
    public String getFormattedDateRange() {
        if (startDate == null && endDate == null) {
            return (datePresetName != null && !datePresetName.isBlank()) ? datePresetName : "All Time";
        }
        String startStr = startDate != null ? AppConfig.formatDate(startDate) : "Beginning";
        String endStr = endDate != null ? AppConfig.formatDate(endDate) : "Present";
        String range = startStr + " to " + endStr;
        if (datePresetName != null && !datePresetName.isBlank() && !datePresetName.equalsIgnoreCase("Custom Range")) {
            range += " (" + datePresetName + ")";
        }
        return range;
    }

    /**
     * Checks if any filtering criteria beyond default is applied.
     *
     * @return {@code true} if any filter condition is active
     */
    public boolean hasActiveFilters() {
        return type != null
                || (category != null && !category.isBlank())
                || (subCategory != null && !subCategory.isBlank())
                || (paymentVia != null && !paymentVia.isBlank())
                || personId != null
                || (personName != null && !personName.isBlank())
                || billId != null
                || (billName != null && !billName.isBlank())
                || emiId != null
                || (emiName != null && !emiName.isBlank())
                || creditCardId != null
                || (cardName != null && !cardName.isBlank())
                || startDate != null
                || endDate != null
                || minAmount != null
                || maxAmount != null
                || expected != null
                || (searchKeyword != null && !searchKeyword.isBlank());
    }

    /**
     * Returns a list of label-value pairs representing active filter conditions for report exports.
     *
     * @return list of [FilterLabel, FilterValue] arrays
     */
    public List<String[]> getFilterSummary() {
        List<String[]> filters = new ArrayList<>();
        filters.add(new String[]{"Date Range", getFormattedDateRange()});

        if (type != null) {
            filters.add(new String[]{"Type", type.name()});
        }
        if (category != null && !category.isBlank()) {
            filters.add(new String[]{"Category", category});
        }
        if (subCategory != null && !subCategory.isBlank()) {
            filters.add(new String[]{"Sub-Category", subCategory});
        }
        if (personName != null && !personName.isBlank()) {
            filters.add(new String[]{"Associated Person", personName});
        } else if (personId != null) {
            filters.add(new String[]{"Person ID", String.valueOf(personId)});
        }
        if (billName != null && !billName.isBlank()) {
            filters.add(new String[]{"Associated Bill", billName});
        } else if (billId != null) {
            filters.add(new String[]{"Bill ID", String.valueOf(billId)});
        }
        if (emiName != null && !emiName.isBlank()) {
            filters.add(new String[]{"Associated EMI", emiName});
        } else if (emiId != null) {
            filters.add(new String[]{"EMI ID", String.valueOf(emiId)});
        }
        if (cardName != null && !cardName.isBlank()) {
            filters.add(new String[]{"Associated Card", cardName});
        } else if (creditCardId != null) {
            filters.add(new String[]{"Card ID", String.valueOf(creditCardId)});
        }
        if (paymentVia != null && !paymentVia.isBlank()) {
            filters.add(new String[]{"Payment Via", paymentVia});
        }
        if (minAmount != null || maxAmount != null) {
            String min = minAmount != null ? AppConfig.formatCurrency(minAmount) : AppConfig.formatCurrency(0);
            String max = maxAmount != null ? AppConfig.formatCurrency(maxAmount) : "Any";
            filters.add(new String[]{"Amount Range", min + " - " + max});
        }
        if (expected != null) {
            filters.add(new String[]{"Status", expected ? "Planned / Expected" : "Realized"});
        }
        if (searchKeyword != null && !searchKeyword.isBlank()) {
            filters.add(new String[]{"Search Keyword", searchKeyword});
        }
        return filters;
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

