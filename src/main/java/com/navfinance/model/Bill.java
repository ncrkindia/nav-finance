package com.navfinance.model;

import java.time.LocalDateTime;

/**
 * Model representing a recurring utility bill (e.g. Airtel Fiber, Electricity Noida, Khardoni, Gas).
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public class Bill {

    private Long id;
    private String billName;
    private String provider;
    private Double defaultAmount;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Default constructor. */
    public Bill() {}

    /**
     * Parameterized constructor.
     *
     * @param id Database identifier.
     * @param billName Descriptive bill name (e.g., Airtel Bill).
     * @param provider Service provider name.
     * @param defaultAmount Default/estimated monthly billing amount.
     * @param notes Additional remarks.
     */
    public Bill(Long id, String billName, String provider, Double defaultAmount, String notes) {
        this.id = id;
        this.billName = billName;
        this.provider = provider;
        this.defaultAmount = defaultAmount;
        this.notes = notes;
    }

    /**
     * Convenience constructor without ID.
     *
     * @param billName Descriptive bill name.
     * @param provider Service provider name.
     * @param defaultAmount Default/estimated monthly billing amount.
     * @param notes Additional remarks.
     */
    public Bill(String billName, String provider, Double defaultAmount, String notes) {
        this(null, billName, provider, defaultAmount, notes);
    }

    /** @return Database ID */
    public Long getId() { return id; }
    /** @param id Database ID */
    public void setId(Long id) { this.id = id; }

    /** @return Bill name */
    public String getBillName() { return billName; }
    /** @param billName Bill name */
    public void setBillName(String billName) { this.billName = billName; }

    /** @return Provider name */
    public String getProvider() { return provider; }
    /** @param provider Provider name */
    public void setProvider(String provider) { this.provider = provider; }

    /** @return Default amount */
    public Double getDefaultAmount() { return defaultAmount; }
    /** @param defaultAmount Default amount */
    public void setDefaultAmount(Double defaultAmount) { this.defaultAmount = defaultAmount; }

    /** @return Remarks */
    public String getNotes() { return notes; }
    /** @param notes Remarks */
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
        return billName != null ? billName : "";
    }
}
