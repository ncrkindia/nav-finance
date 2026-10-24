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
