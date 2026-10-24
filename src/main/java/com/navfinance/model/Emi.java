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
