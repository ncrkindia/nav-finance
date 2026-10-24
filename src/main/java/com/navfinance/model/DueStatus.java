package com.navfinance.model;

/**
 * Enumeration representing the lifecycle state of a scheduled payment or bill occurrence.
 * <ul>
 *   <li>{@link #PENDING}: Payment is upcoming or currently due on the designated date.</li>
 *   <li>{@link #OVERDUE}: Due date has passed and the payment has not been marked as settled.</li>
 *   <li>{@link #SETTLED}: Payment has been paid and settled by the user or through auto-debit.</li>
 * </ul>
 */
public enum DueStatus {
    /** Payment is due soon or upcoming on its scheduled date. */
    PENDING("Due / Upcoming"),

    /** Payment due date has elapsed without being settled. */
    OVERDUE("Overdue"),

    /** Payment has been fulfilled and marked settled. */
    SETTLED("Settled");

    private final String displayName;

    /**
     * Constructs a DueStatus with its UI display string.
     *
     * @param displayName User-friendly label
     */
    DueStatus(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the user-friendly display name.
     *
     * @return display label
     */
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

