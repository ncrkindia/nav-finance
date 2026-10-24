package com.navfinance.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Domain entity representing an individual monthly occurrence of a scheduled payment,
 * recurring EMI, or utility bill.
 * <p>
 * Tracks dynamic real-time overdue metrics, settlement status, linked cashbook transaction ID,
 * and whether the payment executes automatically via mandate (Auto-Debit/NACH) or requires
 * manual action.
 * </p>
 */
public class DuePayment {

    private Long id;
    private Long scheduleId;
    private String title;
    private TransactionType type = TransactionType.EXPENSE;
    private String category;
    private String subCategory;
    private LocalDate dueDate;
    private double expectedAmount = 0.0;
    private boolean variable = false;
    private boolean autoPay = false; // true = Auto-Payment, false = Manual
    private DueStatus status = DueStatus.PENDING;
    private double settledAmount = 0.0;
    private LocalDate settledDate;
    private String settledVia;
    private Long transactionId;
    private String notes;
    private LocalDateTime createdAt;

    /**
     * Default no-argument constructor.
     */
    public DuePayment() {}

    /**
     * Gets the unique occurrence ID.
     *
     * @return primary key
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique occurrence ID.
     *
     * @param id primary key
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the linked recurring schedule rule ID.
     *
     * @return schedule ID or {@code null} if standalone
     */
    public Long getScheduleId() {
        return scheduleId;
    }

    /**
     * Sets the linked recurring schedule rule ID.
     *
     * @param scheduleId schedule rule ID
     */
    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    /**
     * Gets the payment or bill title.
     *
     * @return title string (e.g. "Home Loan EMI - SBI")
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the payment or bill title.
     *
     * @param title payment title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Gets the flow direction (typically EXPENSE).
     *
     * @return transaction type
     */
    public TransactionType getType() {
        return type;
    }

    /**
     * Sets the flow direction.
     *
     * @param type transaction type
     */
    public void setType(TransactionType type) {
        this.type = type;
    }

    /**
     * Gets the primary category (e.g. "EMI Payment", "Bill").
     *
     * @return category string
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the primary category.
     *
     * @param category category string
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Gets the sub-category (e.g. "Home Loan", "Electricity Bill").
     *
     * @return sub-category string
     */
    public String getSubCategory() {
        return subCategory;
    }

    /**
     * Sets the sub-category.
     *
     * @param subCategory sub-category string
     */
    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }

    /**
     * Gets the calendar date on which this payment is due.
     *
     * @return due date
     */
    public LocalDate getDueDate() {
        return dueDate;
    }

    /**
     * Sets the payment due date.
     *
     * @param dueDate due date
     */
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    /**
     * Gets the anticipated / budgeted monetary amount.
     *
     * @return expected amount in INR
     */
    public double getExpectedAmount() {
        return expectedAmount;
    }

    /**
     * Sets the expected monetary amount.
     *
     * @param expectedAmount expected amount
     */
    public void setExpectedAmount(double expectedAmount) {
        this.expectedAmount = expectedAmount;
    }

    /**
     * Checks if this payment is usage-based with variable amount requiring user prompt.
     *
     * @return {@code true} if variable
     */
    public boolean isVariable() {
        return variable;
    }

    /**
     * Sets whether this payment amount is variable.
     *
     * @param variable {@code true} if variable
     */
    public void setVariable(boolean variable) {
        this.variable = variable;
    }

    /**
     * Indicates whether this payment is executed automatically via bank mandate.
     *
     * @return {@code true} if auto-debit, {@code false} if manual
     */
    public boolean isAutoPay() {
        return autoPay;
    }

    /**
     * Sets whether this payment is automated.
     *
     * @param autoPay {@code true} if auto-debit
     */
    public void setAutoPay(boolean autoPay) {
        this.autoPay = autoPay;
    }

    /**
     * Gets the recorded status in the database (PENDING, OVERDUE, or SETTLED).
     *
     * @return due status
     */
    public DueStatus getStatus() {
        return status;
    }

    /**
     * Sets the due status.
     *
     * @param status status enum
     */
    public void setStatus(DueStatus status) {
        this.status = status;
    }

    /**
     * Gets the actual monetary amount paid upon settlement.
     *
     * @return settled amount in INR
     */
    public double getSettledAmount() {
        return settledAmount;
    }

    /**
     * Sets the settled monetary amount.
     *
     * @param settledAmount actual paid amount
     */
    public void setSettledAmount(double settledAmount) {
        this.settledAmount = settledAmount;
    }

    /**
     * Gets the calendar date on which this payment was settled.
     *
     * @return settlement date or {@code null}
     */
    public LocalDate getSettledDate() {
        return settledDate;
    }

    /**
     * Sets the settlement date.
     *
     * @param settledDate date settled
     */
    public void setSettledDate(LocalDate settledDate) {
        this.settledDate = settledDate;
    }

    /**
     * Gets the payment channel used during settlement (e.g. "UPI", "Net Banking").
     *
     * @return payment mode string
     */
    public String getSettledVia() {
        return settledVia;
    }

    /**
     * Sets the payment channel used during settlement.
     *
     * @param settledVia payment mode string
     */
    public void setSettledVia(String settledVia) {
        this.settledVia = settledVia;
    }

    /**
     * Gets the ID of the realized transaction created in the cashbook ledger upon settlement.
     *
     * @return transaction ID or {@code null}
     */
    public Long getTransactionId() {
        return transactionId;
    }

    /**
     * Links this payment occurrence to a ledger transaction.
     *
     * @param transactionId transaction ID
     */
    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Gets notes or payment remarks.
     *
     * @return notes string
     */
    public String getNotes() {
        return notes;
    }

    /**
     * Sets notes or remarks.
     *
     * @param notes notes string
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Gets creation timestamp.
     *
     * @return creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets creation timestamp.
     *
     * @param createdAt creation timestamp
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Checks if this payment is in {@link DueStatus#SETTLED} state.
     *
     * @return {@code true} if settled
     */
    public boolean isSettled() {
        return status == DueStatus.SETTLED;
    }

    /**
     * Computes whether this payment is currently overdue based on the system date.
     *
     * @return {@code true} if not settled and due date is strictly in the past
     */
    public boolean isOverdue() {
        return !isSettled() && dueDate != null && dueDate.isBefore(LocalDate.now());
    }

    /**
     * Computes the real-time effective status evaluating calendar boundaries.
     *
     * @return {@link DueStatus#SETTLED}, {@link DueStatus#OVERDUE}, or {@link DueStatus#PENDING}
     */
    public DueStatus getEffectiveStatus() {
        if (isSettled()) return DueStatus.SETTLED;
        if (isOverdue()) return DueStatus.OVERDUE;
        return DueStatus.PENDING;
    }

    /**
     * Computes how many calendar days this payment is past its due date.
     *
     * @return number of elapsed days overdue, or 0 if not overdue
     */
    public long getDaysOverdue() {
        if (isOverdue() && dueDate != null) {
            return ChronoUnit.DAYS.between(dueDate, LocalDate.now());
        }
        return 0;
    }

    /**
     * Gets a formatted label badge indicating automated vs manual payment.
     *
     * @return "Auto-Payment" or "Manual Payment"
     */
    public String getAutoPayDisplay() {
        return autoPay ? "Auto-Payment" : "Manual Payment";
    }
}

