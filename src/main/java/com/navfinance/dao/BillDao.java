package com.navfinance.dao;

import com.navfinance.database.DatabaseManager;
import com.navfinance.model.Bill;

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
 * Data Access Object (DAO) for managing Utility Bills master data records
 * (e.g., Electricity, Water, Mobile Postpaid, Broadband Internet).
 */
public class BillDao {

    /**
     * Retrieves all configured bill master templates ordered alphabetically by bill name.
     *
     * @return List of all {@link Bill} records
     */
    public List<Bill> findAll() {
        List<Bill> list = new ArrayList<>();
        String sql = "SELECT * FROM bills ORDER BY bill_name ASC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch bills: " + e.getMessage());
        }
        return list;
    }

    /**
     * Finds a bill record by primary key ID.
     *
     * @param id Primary key ID
     * @return {@link Bill} entity, or {@code null} if not found
     */
    public Bill findById(Long id) {
        if (id == null) return null;
        String sql = "SELECT * FROM bills WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to find bill by id: " + e.getMessage());
        }
        return null;
    }

    /**
     * Inserts a new bill record into the database.
     *
     * @param bill Bill domain object to persist
     * @return The persisted bill with generated primary key and timestamps
     */
    public Bill insert(Bill bill) {
        String sql = "INSERT INTO bills (bill_name, provider, default_amount, amount_type, due_day, due_amount, notes, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            LocalDateTime now = LocalDateTime.now();
            ps.setString(1, bill.getBillName());
            ps.setString(2, bill.getProvider());
            if (bill.getDefaultAmount() != null) ps.setDouble(3, bill.getDefaultAmount());
            else ps.setNull(3, java.sql.Types.DECIMAL);
            ps.setString(4, bill.getAmountType() != null ? bill.getAmountType().name() : "FIXED");
            ps.setInt(5, bill.getDueDay() != null ? bill.getDueDay().intValue() : 0);
            ps.setDouble(6, bill.getDueAmount() != null ? bill.getDueAmount().doubleValue() : 0.0);
            ps.setString(7, bill.getNotes());
            ps.setTimestamp(8, Timestamp.valueOf(now));
            ps.setTimestamp(9, Timestamp.valueOf(now));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    bill.setId(keys.getLong(1));
                    bill.setCreatedAt(now);
                    bill.setUpdatedAt(now);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to insert bill: " + e.getMessage());
        }
        return bill;
    }

    /**
     * Updates an existing bill record.
     *
     * @param bill Bill domain object with updated values and valid ID
     * @return {@code true} if updated successfully, {@code false} otherwise
     */
    public boolean update(Bill bill) {
        String sql = "UPDATE bills SET bill_name = ?, provider = ?, default_amount = ?, amount_type = ?, due_day = ?, due_amount = ?, notes = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            LocalDateTime now = LocalDateTime.now();
            ps.setString(1, bill.getBillName());
            ps.setString(2, bill.getProvider());
            if (bill.getDefaultAmount() != null) ps.setDouble(3, bill.getDefaultAmount());
            else ps.setNull(3, java.sql.Types.DECIMAL);
            ps.setString(4, bill.getAmountType() != null ? bill.getAmountType().name() : "FIXED");
            ps.setInt(5, bill.getDueDay() != null ? bill.getDueDay().intValue() : 0);
            ps.setDouble(6, bill.getDueAmount() != null ? bill.getDueAmount().doubleValue() : 0.0);
            ps.setString(7, bill.getNotes());
            ps.setTimestamp(8, Timestamp.valueOf(now));
            ps.setLong(9, bill.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to update bill: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a bill record by ID.
     *
     * @param id Primary key ID to delete
     * @return {@code true} if deleted successfully, {@code false} otherwise
     */
    public boolean delete(Long id) {
        String sql = "DELETE FROM bills WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to delete bill: " + e.getMessage());
            return false;
        }
    }

    /**
     * Maps a result set row into a {@link Bill} entity.
     *
     * @param rs Active result set
     * @return Mapped Bill entity
     * @throws SQLException on column extraction error
     */
    private Bill mapRow(ResultSet rs) throws SQLException {
        Bill b = new Bill();
        b.setId(rs.getLong("id"));
        b.setBillName(rs.getString("bill_name"));
        b.setProvider(rs.getString("provider"));
        double amt = rs.getDouble("default_amount");
        if (!rs.wasNull()) b.setDefaultAmount(amt);
        try {
            String at = rs.getString("amount_type");
            if (at != null && !at.isBlank()) {
                b.setAmountType(com.navfinance.model.AmountType.valueOf(at.toUpperCase()));
            }
        } catch (SQLException | IllegalArgumentException ignored) {}
        try {
            int dd = rs.getInt("due_day");
            if (!rs.wasNull()) b.setDueDay(dd);
        } catch (SQLException ignored) {}
        try {
            double da = rs.getDouble("due_amount");
            if (!rs.wasNull()) b.setDueAmount(da);
        } catch (SQLException ignored) {}
        b.setNotes(rs.getString("notes"));
        Timestamp cat = rs.getTimestamp("created_at");
        if (cat != null) b.setCreatedAt(cat.toLocalDateTime());
        Timestamp uat = rs.getTimestamp("updated_at");
        if (uat != null) b.setUpdatedAt(uat.toLocalDateTime());
        return b;
    }
}

