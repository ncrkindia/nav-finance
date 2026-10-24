package com.navfinance.model;

/**
 * Enumeration representing the mutual settlement status of a transaction
 * (specifically for Give / Loan transactions and Return received transactions).
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public enum SettlementStatus {

    /**
     * Regular transaction not categorized as loan or counterpart settlement.
     */
    NONE("Standard"),

    /**
     * Eligible Give or Return transaction with no linked settlement records yet.
     */
    UNLINKED("Unlinked"),

    /**
     * Partially settled transaction where a portion has been paid or received.
     */
    PARTIAL("Partially Settled"),

    /**
     * Completely paid or received transaction where 100% of the principal is settled.
     */
    NULLIFIED("Nullified");

    private final String defaultLabel;

    SettlementStatus(String defaultLabel) {
        this.defaultLabel = defaultLabel;
    }

    public String getDefaultLabel() {
        return defaultLabel;
    }

    public boolean isFullySettled() {
        return this == NULLIFIED;
    }

    public boolean hasLinks() {
        return this == PARTIAL || this == NULLIFIED;
    }
}
