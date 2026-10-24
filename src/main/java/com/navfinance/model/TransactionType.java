package com.navfinance.model;

/**
 * Enumeration representing the fundamental direction of money flow.
 * <ul>
 *   <li>{@link #INCOME}: Cash inflow (e.g. Salary, Loan received, Udhar returned, Investment return).</li>
 *   <li>{@link #EXPENSE}: Cash outflow (e.g. EMIs, Credit Cards, Bills, Udhar given, Groceries).</li>
 * </ul>
 */
public enum TransactionType {
    /** Inflow of money into personal accounts. */
    INCOME("Income", "In"),

    /** Outflow of money from personal accounts. */
    EXPENSE("Expense", "Out");

    private final String displayName;
    private final String shortName;

    /**
     * Constructs a TransactionType.
     *
     * @param displayName Full human-readable name
     * @param shortName Abbreviated indicator
     */
    TransactionType(String displayName, String shortName) {
        this.displayName = displayName;
        this.shortName = shortName;
    }

    /**
     * Gets the full display name (e.g., "Income", "Expense").
     *
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the abbreviated short identifier (e.g., "In", "Out").
     *
     * @return short identifier
     */
    public String getShortName() {
        return shortName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

