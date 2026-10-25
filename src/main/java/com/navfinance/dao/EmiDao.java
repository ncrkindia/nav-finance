package com.navfinance.dao;

import com.navfinance.database.DatabaseManager;
import com.navfinance.model.Emi;

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
 * Data Access Object (DAO) for managing Equated Monthly Installment (EMI) loan templates
 * (e.g., Home Loan, Car Loan, Personal Loan, Education Loan).
 */
public class EmiDao {

    /**
     * Retrieves all loan/EMI master records ordered alphabetically by loan name.
     *
     * @return List of all {@link Emi} records
     */
    public List<Emi> findAll() {
        List<Emi> list = new ArrayList<>();
        String sql = "SELECT * FROM emis ORDER BY emi_name ASC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch EMIs: " + e.getMessage());
        }
        return list;
    }

    /**
     * Finds an EMI record by primary key ID.
     *
     * @param id Primary key ID
     * @return {@link Emi} entity or {@code null} if not found
     */
    public Emi findById(Long id) {
        if (id == null) return null;
        String sql = "SELECT * FROM emis WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to find EMI by id: " + e.getMessage());
        }
        return null;
    }

    /**
     * Finds all running EMIs associated with a specific credit card.
     *
     * @param creditCardId Credit card primary key
     * @return List of EMIs on that card
     */
    public List<Emi> findByCreditCardId(Long creditCardId) {
        List<Emi> list = new ArrayList<>();
        if (creditCardId == null) return list;
        String sql = "SELECT * FROM emis WHERE is_credit_card_emi = ? AND credit_card_id = ? ORDER BY emi_name ASC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, true);
            ps.setLong(2, creditCardId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch EMIs for credit card #" + creditCardId + ": " + e.getMessage());
        }
        return list;
    }

    /**
     * Inserts a new EMI master record.
     *
     * @param emi EMI entity to persist
     * @return Persisted EMI with generated ID and timestamps
     */
    public Emi insert(Emi emi) {
        String sql = "INSERT INTO emis (emi_name, bank_lender, monthly_amount, total_amount, outstanding_amount, due_amount, due_day, is_credit_card_emi, credit_card_id, notes, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            LocalDateTime now = LocalDateTime.now();
            ps.setString(1, emi.getEmiName());
            ps.setString(2, emi.getBankLender());
            if (emi.getMonthlyAmount() != null) ps.setDouble(3, emi.getMonthlyAmount());
            else ps.setDouble(3, 0.0);
            ps.setDouble(4, emi.getTotalAmount() != null ? emi.getTotalAmount() : 0.0);
            ps.setDouble(5, emi.getOutstandingAmount() != null ? emi.getOutstandingAmount() : 0.0);
            ps.setDouble(6, emi.getDueAmount() != null ? emi.getDueAmount() : 0.0);
            ps.setInt(7, emi.getDueDay() != null ? emi.getDueDay() : 0);
            ps.setBoolean(8, emi.isCreditCardEmi());
            if (emi.getCreditCardId() != null) ps.setLong(9, emi.getCreditCardId());
            else ps.setNull(9, java.sql.Types.BIGINT);
            ps.setString(10, emi.getNotes());
            ps.setTimestamp(11, Timestamp.valueOf(now));
            ps.setTimestamp(12, Timestamp.valueOf(now));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    emi.setId(keys.getLong(1));
                    emi.setCreatedAt(now);
                    emi.setUpdatedAt(now);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to insert EMI: " + e.getMessage());
        }
        return emi;
    }

    /**
     * Updates an existing EMI record.
     *
     * @param emi EMI entity containing updated fields and valid ID
     * @return {@code true} if updated successfully, {@code false} otherwise
     */
    public boolean update(Emi emi) {
        String sql = "UPDATE emis SET emi_name = ?, bank_lender = ?, monthly_amount = ?, total_amount = ?, outstanding_amount = ?, due_amount = ?, due_day = ?, is_credit_card_emi = ?, credit_card_id = ?, notes = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            LocalDateTime now = LocalDateTime.now();
            ps.setString(1, emi.getEmiName());
            ps.setString(2, emi.getBankLender());
            if (emi.getMonthlyAmount() != null) ps.setDouble(3, emi.getMonthlyAmount());
            else ps.setDouble(3, 0.0);
            ps.setDouble(4, emi.getTotalAmount() != null ? emi.getTotalAmount() : 0.0);
            ps.setDouble(5, emi.getOutstandingAmount() != null ? emi.getOutstandingAmount() : 0.0);
            ps.setDouble(6, emi.getDueAmount() != null ? emi.getDueAmount() : 0.0);
            ps.setInt(7, emi.getDueDay() != null ? emi.getDueDay() : 0);
            ps.setBoolean(8, emi.isCreditCardEmi());
            if (emi.getCreditCardId() != null) ps.setLong(9, emi.getCreditCardId());
            else ps.setNull(9, java.sql.Types.BIGINT);
            ps.setString(10, emi.getNotes());
            ps.setTimestamp(11, Timestamp.valueOf(now));
            ps.setLong(12, emi.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to update EMI: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes an EMI record by ID.
     *
     * @param id Primary key ID to delete
     * @return {@code true} if deleted successfully, {@code false} otherwise
     */
    public boolean delete(Long id) {
        String sql = "DELETE FROM emis WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to delete EMI: " + e.getMessage());
            return false;
        }
    }

    /**
     * Maps an active result set row to an {@link Emi} instance.
     *
     * @param rs Active result set
     * @return Mapped Emi entity
     * @throws SQLException on column reading error
     */
    private Emi mapRow(ResultSet rs) throws SQLException {
        Emi e = new Emi();
        e.setId(rs.getLong("id"));
        e.setEmiName(rs.getString("emi_name"));
        e.setBankLender(rs.getString("bank_lender"));
        double amt = rs.getDouble("monthly_amount");
        if (!rs.wasNull()) e.setMonthlyAmount(amt);
        e.setTotalAmount(getDoubleFromRs(rs, "total_amount"));
        e.setOutstandingAmount(getDoubleFromRs(rs, "outstanding_amount"));
        e.setDueAmount(getDoubleFromRs(rs, "due_amount"));
        e.setDueDay(getIntFromRs(rs, "due_day"));
        try {
            e.setCreditCardEmi(rs.getBoolean("is_credit_card_emi"));
        } catch (SQLException ignored) {}
        try {
            long ccId = rs.getLong("credit_card_id");
            if (!rs.wasNull()) e.setCreditCardId(ccId);
        } catch (SQLException ignored) {}
        e.setNotes(rs.getString("notes"));
        Timestamp cat = rs.getTimestamp("created_at");
        if (cat != null) e.setCreatedAt(cat.toLocalDateTime());
        Timestamp uat = rs.getTimestamp("updated_at");
        if (uat != null) e.setUpdatedAt(uat.toLocalDateTime());
        return e;
    }

    private Double getDoubleFromRs(ResultSet rs, String col) {
        try {
            double v = rs.getDouble(col);
            return rs.wasNull() ? 0.0 : v;
        } catch (SQLException e) {
            return 0.0;
        }
    }

    private Integer getIntFromRs(ResultSet rs, String col) {
        try {
            int v = rs.getInt(col);
            return rs.wasNull() ? 0 : v;
        } catch (SQLException e) {
            return 0;
        }
    }
}

