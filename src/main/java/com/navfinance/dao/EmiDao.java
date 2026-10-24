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
     * Inserts a new EMI master record.
     *
     * @param emi EMI entity to persist
     * @return Persisted EMI with generated ID and timestamps
     */
    public Emi insert(Emi emi) {
        String sql = "INSERT INTO emis (emi_name, bank_lender, monthly_amount, notes, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            LocalDateTime now = LocalDateTime.now();
            ps.setString(1, emi.getEmiName());
            ps.setString(2, emi.getBankLender());
            if (emi.getMonthlyAmount() != null) ps.setDouble(3, emi.getMonthlyAmount());
            else ps.setNull(3, java.sql.Types.DECIMAL);
            ps.setString(4, emi.getNotes());
            ps.setTimestamp(5, Timestamp.valueOf(now));
            ps.setTimestamp(6, Timestamp.valueOf(now));
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
        String sql = "UPDATE emis SET emi_name = ?, bank_lender = ?, monthly_amount = ?, notes = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            LocalDateTime now = LocalDateTime.now();
            ps.setString(1, emi.getEmiName());
            ps.setString(2, emi.getBankLender());
            if (emi.getMonthlyAmount() != null) ps.setDouble(3, emi.getMonthlyAmount());
            else ps.setNull(3, java.sql.Types.DECIMAL);
            ps.setString(4, emi.getNotes());
            ps.setTimestamp(5, Timestamp.valueOf(now));
            ps.setLong(6, emi.getId());
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
        e.setNotes(rs.getString("notes"));
        Timestamp cat = rs.getTimestamp("created_at");
        if (cat != null) e.setCreatedAt(cat.toLocalDateTime());
        Timestamp uat = rs.getTimestamp("updated_at");
        if (uat != null) e.setUpdatedAt(uat.toLocalDateTime());
        return e;
    }
}

