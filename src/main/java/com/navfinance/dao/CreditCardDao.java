package com.navfinance.dao;

import com.navfinance.database.DatabaseManager;
import com.navfinance.model.CreditCard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for managing Credit Card master records.
 * <p>
 * Stores card aliases (e.g., "HDFC Millennia", "SBI Cashback"), issuing banks,
 * and last four digits for statement and billing reconciliation.
 * </p>
 */
public class CreditCardDao {

    /**
     * Retrieves all configured credit cards ordered alphabetically by card name.
     *
     * @return List of all {@link CreditCard} entities
     */
    public List<CreditCard> findAll() {
        List<CreditCard> list = new ArrayList<>();
        String sql = "SELECT * FROM credit_cards ORDER BY card_name ASC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch credit cards: " + e.getMessage());
        }
        return list;
    }

    /**
     * Finds a credit card by its primary key ID.
     *
     * @param id Primary key ID
     * @return {@link CreditCard} or {@code null} if not found
     */
    public CreditCard findById(Long id) {
        if (id == null) return null;
        String sql = "SELECT * FROM credit_cards WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to find credit card by id: " + e.getMessage());
        }
        return null;
    }

    /**
     * Persists a new credit card record into the database.
     *
     * @param card Credit card entity to insert
     * @return Inserted credit card with generated ID and timestamps
     */
    public CreditCard insert(CreditCard card) {
        String sql = "INSERT INTO credit_cards (card_name, bank, last_digits, created_at, updated_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            LocalDateTime now = LocalDateTime.now();
            ps.setString(1, card.getCardName());
            ps.setString(2, card.getBank());
            ps.setString(3, card.getLastDigits());
            ps.setTimestamp(4, Timestamp.valueOf(now));
            ps.setTimestamp(5, Timestamp.valueOf(now));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    card.setId(keys.getLong(1));
                    card.setCreatedAt(now);
                    card.setUpdatedAt(now);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to insert credit card: " + e.getMessage());
        }
        return card;
    }

    /**
     * Updates an existing credit card record.
     *
     * @param card Credit card entity containing updated attributes and valid ID
     * @return {@code true} if updated successfully, {@code false} otherwise
     */
    public boolean update(CreditCard card) {
        String sql = "UPDATE credit_cards SET card_name = ?, bank = ?, last_digits = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            LocalDateTime now = LocalDateTime.now();
            ps.setString(1, card.getCardName());
            ps.setString(2, card.getBank());
            ps.setString(3, card.getLastDigits());
            ps.setTimestamp(4, Timestamp.valueOf(now));
            ps.setLong(5, card.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to update credit card: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a credit card record by its primary key ID.
     *
     * @param id Primary key ID to delete
     * @return {@code true} if deleted successfully, {@code false} otherwise
     */
    public boolean delete(Long id) {
        String sql = "DELETE FROM credit_cards WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to delete credit card: " + e.getMessage());
            return false;
        }
    }

    /**
     * Maps an active result set row to a {@link CreditCard} entity.
     *
     * @param rs Active result set
     * @return Mapped CreditCard entity
     * @throws SQLException on column reading error
     */
    private CreditCard mapRow(ResultSet rs) throws SQLException {
        CreditCard c = new CreditCard();
        c.setId(rs.getLong("id"));
        c.setCardName(rs.getString("card_name"));
        c.setBank(rs.getString("bank"));
        c.setLastDigits(rs.getString("last_digits"));
        Timestamp cat = rs.getTimestamp("created_at");
        if (cat != null) c.setCreatedAt(cat.toLocalDateTime());
        Timestamp uat = rs.getTimestamp("updated_at");
        if (uat != null) c.setUpdatedAt(uat.toLocalDateTime());
        return c;
    }
}

