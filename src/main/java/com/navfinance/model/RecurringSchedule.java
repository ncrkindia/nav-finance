package com.navfinance.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Domain entity representing a recurring payment schedule rule.
 * <p>
 * Supports both fixed term schedules (e.g., Home Loan EMI of ₹28,500 on 5th of every month
 * from Jan 2026 to Jan 2027) and ongoing recurring utility bills or credit card cycles
 * (e.g. Electricity bill on 15th with variable amount where the user is prompted for the actual bill amount).
 * Also flags whether payments are executed via automated mandate (⚡ Auto-Debit/NACH) or 🖐 Manual Payment.
 * </p>
 */
public class RecurringSchedule {

    private Long id;
    private String name;
    private TransactionType type = TransactionType.EXPENSE;
    private String category;
    private String subCategory;
    private int dayOfMonth = 1; // 1 to 31
    private AmountType amountType = AmountType.FIXED;
    private double defaultAmount = 0.0;
    private LocalDate startDate;
    private LocalDate endDate;
    private String paymentVia = "Net Banking";
    private boolean autoPay = false; // true = Auto-Payment (Auto-Debit/NACH), false = Manual Payment
    private String notes;
    private boolean active = true;
    private LocalDateTime createdAt;

    /**
     * Default constructor initializing start date to the beginning of the current month.
     */
    public RecurringSchedule() {
        this.startDate = LocalDate.now().withDayOfMonth(1);
    }

    /**
     * Gets the unique schedule ID.
     *
     * @return database primary key
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique schedule ID.
     *
     * @param id database primary key
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the schedule display name or rule title.
     *
     * @return rule name (e.g. "Home Loan EMI - SBI")
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the schedule display name.
     *
     * @param name rule title
     */
    public void setName(String name) {
        this.name = name;
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
     * Gets the recurring calendar day of month (1-31).
     *
     * @return day of month
     */
    public int getDayOfMonth() {
        return dayOfMonth;
    }

    /**
     * Sets the recurring calendar day of month clamped between 1 and 31.
     *
     * @param dayOfMonth day (1 to 31)
     */
    public void setDayOfMonth(int dayOfMonth) {
        this.dayOfMonth = Math.max(1, Math.min(31, dayOfMonth));
    }

    /**
     * Gets whether amount is fixed or variable.
     *
     * @return {@link AmountType#FIXED} or {@link AmountType#VARIABLE}
     */
    public AmountType getAmountType() {
        return amountType;
    }

    /**
     * Sets the amount specification type.
     *
     * @param amountType amount type enum
     */
    public void setAmountType(AmountType amountType) {
        this.amountType = amountType;
    }

    /**
     * Gets the default / expected monetary amount in INR.
     *
     * @return default amount
     */
    public double getDefaultAmount() {
        return defaultAmount;
    }

    /**
     * Sets the default / expected monetary amount.
     *
     * @param defaultAmount default amount
     */
    public void setDefaultAmount(double defaultAmount) {
        this.defaultAmount = defaultAmount;
    }

    /**
     * Gets the schedule inception / start date.
     *
     * @return start date (inclusive)
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Sets the schedule start date.
     *
     * @param startDate start date
     */
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    /**
     * Gets the schedule expiration date, or {@code null} if ongoing indefinitely.
     *
     * @return end date or {@code null}
     */
    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * Sets the schedule expiration date.
     *
     * @param endDate end date or {@code null}
     */
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    /**
     * Gets default payment mode (e.g., "Net Banking", "UPI", "Auto-Debit").
     *
     * @return payment mode string
     */
    public String getPaymentVia() {
        return paymentVia;
    }

    /**
     * Sets default payment mode.
     *
     * @param paymentVia payment mode string
     */
    public void setPaymentVia(String paymentVia) {
        this.paymentVia = paymentVia;
    }

    /**
     * Indicates whether this recurring schedule is configured for automatic debit.
     *
     * @return {@code true} for Auto-Debit/NACH mandate, {@code false} for Manual
     */
    public boolean isAutoPay() {
        return autoPay;
    }

    /**
     * Sets whether this schedule is automated or manual.
     *
     * @param autoPay {@code true} for automated
     */
    public void setAutoPay(boolean autoPay) {
        this.autoPay = autoPay;
    }

    /**
     * Gets explanatory notes or remarks.
     *
     * @return notes string
     */
    public String getNotes() {
        return notes;
    }

    /**
     * Sets explanatory notes.
     *
     * @param notes notes string
     */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Checks if the schedule is currently enabled and active.
     *
     * @return {@code true} if active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Enables or disables this recurring schedule.
     *
     * @param active {@code true} to enable
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Gets the timestamp when this schedule rule was registered.
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
     * Gets a formatted badge label indicating automated vs manual payment.
     *
     * @return "Auto-Payment" or "Manual Payment"
     */
    public String getAutoPayDisplay() {
        return autoPay ? "Auto-Payment" : "Manual Payment";
    }

    @Override
    public String toString() {
        return name != null ? name : "Recurring Schedule #" + id;
    }
}

