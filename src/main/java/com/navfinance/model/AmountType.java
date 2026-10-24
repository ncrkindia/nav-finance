package com.navfinance.model;

/**
 * Enumeration defining how the monetary amount is specified for a recurring bill or schedule.
 * <p>
 * Supports both fixed predictability (e.g. loan EMIs, rent, subscription fees)
 * and variable usage-based amounts (e.g. electricity bills, credit card statements)
 * where the user is prompted for the exact amount upon settlement.
 * </p>
 */
public enum AmountType {
    /** Fixed amount known in advance (e.g., Home loan EMI ₹28,500). */
    FIXED("Fixed Amount"),

    /** Variable amount determined each billing cycle (e.g., Electricity bill). */
    VARIABLE("Variable (Enter amount when due)");

    private final String displayName;

    /**
     * Constructs an AmountType with a human-readable display label.
     *
     * @param displayName User-friendly label for UI components
     */
    AmountType(String displayName) {
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

