package com.navfinance.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Core domain entity representing a financial transaction (Income or Expense).
 * <p>
 * Supports both actual cash flow entries and planned/expected future entries,
 * categorized payment channels, optional master entity references (Person, Bill, EMI, Credit Card),
 * and audit timestamps.
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public class Transaction {

    private Long id;
    private TransactionType type;
    private LocalDate txDate;
    private boolean expected;
    private double amount;
    private String paymentVia;
    private String category;
    private String subCategory;
    private Long personId;
    private Long billId;
    private Long emiId;
    private Long creditCardId;
    private String personName;
    private String billName;
    private String emiName;
    private String cardName;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Default no-argument constructor.
     */
    public Transaction() {}

    /**
     * Parameterized constructor for creating a complete transaction entity.
     *
     * @param id Unique database identifier.
     * @param type Transaction type (INCOME or EXPENSE).
     * @param txDate Effective transaction date or planned date.
     * @param expected True if anticipated/planned, false if realized.
     * @param amount Transaction amount in currency.
     * @param paymentVia Payment method or channel.
     * @param category Main classification category.
     * @param subCategory Sub-category or source description.
     * @param personId Linked person ID (optional).
     * @param billId Linked bill ID (optional).
     * @param emiId Linked EMI ID (optional).
     * @param creditCardId Linked credit card ID (optional).
     * @param notes Additional remarks or audit details.
     */
    public Transaction(Long id, TransactionType type, LocalDate txDate, boolean expected, double amount,
                       String paymentVia, String category, String subCategory,
                       Long personId, Long billId, Long emiId, Long creditCardId,
                       String notes) {
        this.id = id;
        this.type = type;
        this.txDate = txDate;
        this.expected = expected;
        this.amount = amount;
        this.paymentVia = paymentVia;
        this.category = category;
        this.subCategory = subCategory;
        this.personId = personId;
        this.billId = billId;
        this.emiId = emiId;
        this.creditCardId = creditCardId;
        this.notes = notes;
    }

    /** @return Database ID */
    public Long getId() { return id; }
    /** @param id Database ID */
    public void setId(Long id) { this.id = id; }

    /** @return Transaction type (INCOME/EXPENSE) */
    public TransactionType getType() { return type; }
    /** @param type Transaction type */
    public void setType(TransactionType type) { this.type = type; }

    /** @return Transaction date */
    public LocalDate getTxDate() { return txDate; }
    /** @param txDate Transaction date */
    public void setTxDate(LocalDate txDate) { this.txDate = txDate; }

    /** @return True if expected/planned */
    public boolean isExpected() { return expected; }
    /** @param expected Expected flag */
    public void setExpected(boolean expected) { this.expected = expected; }

    /** @return Monetary amount */
    public double getAmount() { return amount; }
    /** @param amount Monetary amount */
    public void setAmount(double amount) { this.amount = amount; }

    /** @return Payment channel */
    public String getPaymentVia() { return paymentVia; }
    /** @param paymentVia Payment channel */
    public void setPaymentVia(String paymentVia) { this.paymentVia = paymentVia; }

    /** @return Main category */
    public String getCategory() { return category; }
    /** @param category Main category */
    public void setCategory(String category) { this.category = category; }

    /** @return Sub-category */
    public String getSubCategory() { return subCategory; }
    /** @param subCategory Sub-category */
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }

    /** @return Linked person master ID */
    public Long getPersonId() { return personId; }
    /** @param personId Linked person master ID */
    public void setPersonId(Long personId) { this.personId = personId; }

    /** @return Linked bill master ID */
    public Long getBillId() { return billId; }
    /** @param billId Linked bill master ID */
    public void setBillId(Long billId) { this.billId = billId; }

    /** @return Linked EMI master ID */
    public Long getEmiId() { return emiId; }
    /** @param emiId Linked EMI master ID */
    public void setEmiId(Long emiId) { this.emiId = emiId; }

    /** @return Linked credit card master ID */
    public Long getCreditCardId() { return creditCardId; }
    /** @param creditCardId Linked credit card master ID */
    public void setCreditCardId(Long creditCardId) { this.creditCardId = creditCardId; }

    /** @return Denormalized person name */
    public String getPersonName() { return personName; }
    /** @param personName Denormalized person name */
    public void setPersonName(String personName) { this.personName = personName; }

    /** @return Denormalized bill name */
    public String getBillName() { return billName; }
    /** @param billName Denormalized bill name */
    public void setBillName(String billName) { this.billName = billName; }

    /** @return Denormalized EMI name */
    public String getEmiName() { return emiName; }
    /** @param emiName Denormalized EMI name */
    public void setEmiName(String emiName) { this.emiName = emiName; }

    /** @return Denormalized credit card name */
    public String getCardName() { return cardName; }
    /** @param cardName Denormalized credit card name */
    public void setCardName(String cardName) { this.cardName = cardName; }

    /**
     * Returns the type of associated master entity ("Person", "Bill", "EMI", "Card", or "-").
     *
     * @return master category type
     */
    public String getAssociatedMasterType() {
        if (personName != null && !personName.isBlank()) return "Person";
        if (billName != null && !billName.isBlank()) return "Bill";
        if (emiName != null && !emiName.isBlank()) return "EMI";
        if (cardName != null && !cardName.isBlank()) return "Card";
        return "-";
    }

    /**
     * Returns the clean name of the associated master entity without prefix.
     *
     * @return clean master name
     */
    public String getAssociatedMasterName() {
        if (personName != null && !personName.isBlank()) return personName.trim();
        if (billName != null && !billName.isBlank()) return billName.trim();
        if (emiName != null && !emiName.isBlank()) return emiName.trim();
        if (cardName != null && !cardName.isBlank()) return cardName.trim();
        return "-";
    }

    /**
     * Returns a formatted descriptive label for display in tables and reports.
     * Examples: "Ramesh Sharma", "Bill: Electricity", "EMI: Car Loan", "Card: HDFC Regalia".
     *
     * @return formatted master entity string, or "-" if unlinked
     */
    public String getAssociatedMasterDisplay() {
        if (personName != null && !personName.isBlank()) {
            return personName.trim();
        }
        if (billName != null && !billName.isBlank()) {
            return "Bill: " + billName.trim();
        }
        if (emiName != null && !emiName.isBlank()) {
            return "EMI: " + emiName.trim();
        }
        if (cardName != null && !cardName.isBlank()) {
            return "Card: " + cardName.trim();
        }
        return "-";
    }

    /** @return Transaction notes */
    public String getNotes() { return notes; }
    /** @param notes Transaction notes */
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
        return "Transaction{" +
                "id=" + id +
                ", type=" + type +
                ", txDate=" + txDate +
                ", amount=" + amount +
                ", category='" + category + '\'' +
                ", subCategory='" + subCategory + '\'' +
                '}';
    }
}
