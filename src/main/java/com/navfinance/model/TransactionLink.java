package com.navfinance.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing an explicit financial settlement link between a Give Transaction
 * (e.g. Loan Given / Lent) and a Return Transaction (e.g. Return received / Repayment).
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public class TransactionLink {

    private Long id;
    private Long giveTxId;
    private Long returnTxId;
    private double amount;
    private String notes;
    private LocalDateTime createdAt;

    // Denormalized/joined audit and presentation attributes
    private LocalDate giveTxDate;
    private double giveTxAmount;
    private String giveTxNotes;
    private String givePersonName;

    private LocalDate returnTxDate;
    private double returnTxAmount;
    private String returnTxNotes;
    private String returnPersonName;

    /**
     * Default constructor.
     */
    public TransactionLink() {}

    /**
     * Parameterized constructor.
     *
     * @param giveTxId   ID of the Give transaction (Expense / Lent)
     * @param returnTxId ID of the Return transaction (Income / Repayment)
     * @param amount     Settled / linked allocation amount
     * @param notes      Optional link remarks
     */
    public TransactionLink(Long giveTxId, Long returnTxId, double amount, String notes) {
        this.giveTxId = giveTxId;
        this.returnTxId = returnTxId;
        this.amount = amount;
        this.notes = notes;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGiveTxId() { return giveTxId; }
    public void setGiveTxId(Long giveTxId) { this.giveTxId = giveTxId; }

    public Long getReturnTxId() { return returnTxId; }
    public void setReturnTxId(Long returnTxId) { this.returnTxId = returnTxId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDate getGiveTxDate() { return giveTxDate; }
    public void setGiveTxDate(LocalDate giveTxDate) { this.giveTxDate = giveTxDate; }

    public double getGiveTxAmount() { return giveTxAmount; }
    public void setGiveTxAmount(double giveTxAmount) { this.giveTxAmount = giveTxAmount; }

    public String getGiveTxNotes() { return giveTxNotes; }
    public void setGiveTxNotes(String giveTxNotes) { this.giveTxNotes = giveTxNotes; }

    public String getGivePersonName() { return givePersonName; }
    public void setGivePersonName(String givePersonName) { this.givePersonName = givePersonName; }

    public LocalDate getReturnTxDate() { return returnTxDate; }
    public void setReturnTxDate(LocalDate returnTxDate) { this.returnTxDate = returnTxDate; }

    public double getReturnTxAmount() { return returnTxAmount; }
    public void setReturnTxAmount(double returnTxAmount) { this.returnTxAmount = returnTxAmount; }

    public String getReturnTxNotes() { return returnTxNotes; }
    public void setReturnTxNotes(String returnTxNotes) { this.returnTxNotes = returnTxNotes; }

    public String getReturnPersonName() { return returnPersonName; }
    public void setReturnPersonName(String returnPersonName) { this.returnPersonName = returnPersonName; }

    @Override
    public String toString() {
        return "TransactionLink{" +
                "id=" + id +
                ", giveTxId=" + giveTxId +
                ", returnTxId=" + returnTxId +
                ", amount=" + amount +
                '}';
    }
}
