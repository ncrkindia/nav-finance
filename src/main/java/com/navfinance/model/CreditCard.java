package com.navfinance.model;

import java.time.LocalDateTime;

/**
 * Model representing a credit card master entity for tracking card-based expenditures.
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public class CreditCard {

    private Long id;
    private String cardName;
    private String bank;
    private String lastDigits;
    private Double totalLimit = 0.0;
    private Double outstandingAmount = 0.0;
    private Double dueAmount = 0.0;
    private Integer dueDay = 0;
    private Integer billingDay = 0;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Default constructor. */
    public CreditCard() {}

    /**
     * Parameterized constructor.
     *
     * @param id Database identifier.
     * @param cardName Card title (e.g., HDFC Millennia).
     * @param bank Issuing bank name.
     * @param lastDigits Last 4 digits of the card number.
     */
    public CreditCard(Long id, String cardName, String bank, String lastDigits) {
        this.id = id;
        this.cardName = cardName;
        this.bank = bank;
        this.lastDigits = lastDigits;
    }

    /**
     * Comprehensive constructor with limits and billing dates.
     *
     * @param id Database identifier.
     * @param cardName Card title.
     * @param bank Issuing bank name.
     * @param lastDigits Last 4 digits.
     * @param totalLimit Sanctioned total credit limit.
     * @param outstandingAmount Current utilized balance.
     * @param dueAmount Current bill due amount to pay.
     * @param dueDay Day of month for payment due date (1-31).
     * @param billingDay Day of month for statement generation (1-31).
     */
    public CreditCard(Long id, String cardName, String bank, String lastDigits,
                      Double totalLimit, Double outstandingAmount, Double dueAmount,
                      Integer dueDay, Integer billingDay) {
        this.id = id;
        this.cardName = cardName;
        this.bank = bank;
        this.lastDigits = lastDigits;
        this.totalLimit = totalLimit;
        this.outstandingAmount = outstandingAmount;
        this.dueAmount = dueAmount;
        this.dueDay = dueDay;
        this.billingDay = billingDay;
    }

    /**
     * Convenience constructor without ID.
     *
     * @param cardName Card title.
     * @param bank Issuing bank name.
     * @param lastDigits Last 4 digits of the card number.
     */
    public CreditCard(String cardName, String bank, String lastDigits) {
        this(null, cardName, bank, lastDigits);
    }

    /** @return Database ID */
    public Long getId() { return id; }
    /** @param id Database ID */
    public void setId(Long id) { this.id = id; }

    /** @return Card name */
    public String getCardName() { return cardName; }
    /** @param cardName Card name */
    public void setCardName(String cardName) { this.cardName = cardName; }

    /** @return Issuing bank */
    public String getBank() { return bank; }
    /** @param bank Issuing bank */
    public void setBank(String bank) { this.bank = bank; }

    /** @return Last digits */
    public String getLastDigits() { return lastDigits; }
    /** @param lastDigits Last digits */
    public void setLastDigits(String lastDigits) { this.lastDigits = lastDigits; }

    /** @return Total sanctioned credit limit */
    public Double getTotalLimit() { return totalLimit; }
    /** @param totalLimit Total credit limit */
    public void setTotalLimit(Double totalLimit) { this.totalLimit = totalLimit; }

    /** @return Current utilized / outstanding balance */
    public Double getOutstandingAmount() { return outstandingAmount; }
    /** @param outstandingAmount Outstanding balance */
    public void setOutstandingAmount(Double outstandingAmount) { this.outstandingAmount = outstandingAmount; }

    /** @return Current bill statement due amount to pay */
    public Double getDueAmount() { return dueAmount; }
    /** @param dueAmount Bill due amount */
    public void setDueAmount(Double dueAmount) { this.dueAmount = dueAmount; }

    /** @return Monthly due payment day (1 to 31) */
    public Integer getDueDay() { return dueDay; }
    /** @param dueDay Monthly due day */
    public void setDueDay(Integer dueDay) { this.dueDay = dueDay; }

    /** @return Monthly billing cycle statement day (1 to 31) */
    public Integer getBillingDay() { return billingDay; }
    /** @param billingDay Billing statement day */
    public void setBillingDay(Integer billingDay) { this.billingDay = billingDay; }

    /**
     * Calculates the remaining available credit balance.
     *
     * @return Available credit (Total Limit - Outstanding)
     */
    public Double getAvailableLimit() {
        double limit = totalLimit != null ? totalLimit.doubleValue() : 0.0;
        double out = outstandingAmount != null ? outstandingAmount.doubleValue() : 0.0;
        return Math.max(0.0, limit - out);
    }

    /**
     * Calculates the card utilization percentage.
     *
     * @return Utilization % between 0.0 and 100.0 (or higher if over limit)
     */
    public Double getUtilizationPercentage() {
        if (totalLimit == null || totalLimit.doubleValue() <= 0.0) return 0.0;
        double out = outstandingAmount != null ? outstandingAmount.doubleValue() : 0.0;
        return (out / totalLimit.doubleValue()) * 100.0;
    }

    /** @return Creation timestamp */
    public LocalDateTime getCreatedAt() { return createdAt; }
    /** @param createdAt Creation timestamp */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** @return Last updated timestamp */
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    /** @param updatedAt Last updated timestamp */
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return cardName + (lastDigits != null && !lastDigits.isEmpty() ? " (.." + lastDigits + ")" : "");
    }
}
