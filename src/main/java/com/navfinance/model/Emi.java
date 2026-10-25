package com.navfinance.model;

import java.time.LocalDateTime;

/**
 * Model representing an Equated Monthly Installment (EMI) loan master entry (e.g. Home Loan EMI, Car Loan EMI).
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public class Emi {

    private Long id;
    private String emiName;
    private String bankLender;
    private Double monthlyAmount;
    private String notes;
    private Double totalAmount = 0.0;
    private Double outstandingAmount = 0.0;
    private Double dueAmount = 0.0;
    private Integer dueDay = 0;
    private boolean isCreditCardEmi = false;
    private Long creditCardId;
    private String creditCardName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Default constructor. */
    public Emi() {}

    /**
     * Parameterized constructor.
     *
     * @param id Database identifier.
     * @param emiName Name or title of the loan installment.
     * @param bankLender Financial institution or bank.
     * @param monthlyAmount Fixed monthly payment amount.
     * @param notes Additional remarks or tenure details.
     */
    public Emi(Long id, String emiName, String bankLender, Double monthlyAmount, String notes) {
        this.id = id;
        this.emiName = emiName;
        this.bankLender = bankLender;
        this.monthlyAmount = monthlyAmount;
        this.notes = notes;
    }

    /**
     * Comprehensive constructor with loan limits, outstanding balances, and credit card association.
     *
     * @param id Database identifier.
     * @param emiName Name or title of the loan installment.
     * @param bankLender Financial institution or bank.
     * @param monthlyAmount Fixed monthly payment amount.
     * @param totalAmount Sanctioned loan limit / principal or purchase price.
     * @param outstandingAmount Remaining principal balance.
     * @param dueAmount Current due amount.
     * @param dueDay Monthly payment due day (1-31).
     * @param isCreditCardEmi Whether this EMI is billed on a Credit Card.
     * @param creditCardId Associated credit card ID if on CC.
     * @param notes Additional remarks or tenure details.
     */
    public Emi(Long id, String emiName, String bankLender, Double monthlyAmount,
               Double totalAmount, Double outstandingAmount, Double dueAmount,
               Integer dueDay, boolean isCreditCardEmi, Long creditCardId, String notes) {
        this.id = id;
        this.emiName = emiName;
        this.bankLender = bankLender;
        this.monthlyAmount = monthlyAmount;
        this.totalAmount = totalAmount;
        this.outstandingAmount = outstandingAmount;
        this.dueAmount = dueAmount;
        this.dueDay = dueDay;
        this.isCreditCardEmi = isCreditCardEmi;
        this.creditCardId = creditCardId;
        this.notes = notes;
    }

    /**
     * Convenience constructor without ID.
     *
     * @param emiName Name or title of the loan installment.
     * @param bankLender Financial institution or bank.
     * @param monthlyAmount Fixed monthly payment amount.
     * @param notes Additional remarks.
     */
    public Emi(String emiName, String bankLender, Double monthlyAmount, String notes) {
        this(null, emiName, bankLender, monthlyAmount, notes);
    }

    /** @return Database ID */
    public Long getId() { return id; }
    /** @param id Database ID */
    public void setId(Long id) { this.id = id; }

    /** @return EMI name */
    public String getEmiName() { return emiName; }
    /** @param emiName EMI name */
    public void setEmiName(String emiName) { this.emiName = emiName; }

    /** @return Lending bank */
    public String getBankLender() { return bankLender; }
    /** @param bankLender Lending bank */
    public void setBankLender(String bankLender) { this.bankLender = bankLender; }

    /** @return Monthly amount */
    public Double getMonthlyAmount() { return monthlyAmount; }
    /** @param monthlyAmount Monthly amount */
    public void setMonthlyAmount(Double monthlyAmount) { this.monthlyAmount = monthlyAmount; }

    /** @return Total sanctioned loan amount or limit */
    public Double getTotalAmount() { return totalAmount; }
    /** @param totalAmount Total sanctioned loan amount */
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    /** @return Current principal outstanding balance */
    public Double getOutstandingAmount() { return outstandingAmount; }
    /** @param outstandingAmount Outstanding balance */
    public void setOutstandingAmount(Double outstandingAmount) { this.outstandingAmount = outstandingAmount; }

    /** @return Current overdue / due amount to pay */
    public Double getDueAmount() { return dueAmount; }
    /** @param dueAmount Due amount */
    public void setDueAmount(Double dueAmount) { this.dueAmount = dueAmount; }

    /** @return Monthly due payment day (1-31) */
    public Integer getDueDay() { return dueDay; }
    /** @param dueDay Monthly due day */
    public void setDueDay(Integer dueDay) { this.dueDay = dueDay; }

    /** @return {@code true} if this EMI is running on a Credit Card */
    public boolean isCreditCardEmi() { return isCreditCardEmi; }
    /** @param creditCardEmi {@code true} if on CC */
    public void setCreditCardEmi(boolean creditCardEmi) { this.isCreditCardEmi = creditCardEmi; }

    /** @return Associated Credit Card ID if on CC, or {@code null} */
    public Long getCreditCardId() { return creditCardId; }
    /** @param creditCardId Associated Credit Card ID */
    public void setCreditCardId(Long creditCardId) { this.creditCardId = creditCardId; }

    /** @return Associated Credit Card name (transient display helper) */
    public String getCreditCardName() { return creditCardName; }
    /** @param creditCardName Associated Credit Card name */
    public void setCreditCardName(String creditCardName) { this.creditCardName = creditCardName; }

    /** @return Additional notes */
    public String getNotes() { return notes; }
    /** @param notes Additional notes */
    public void setNotes(String notes) { this.notes = notes; }

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
        return emiName != null ? emiName : "";
    }
}
