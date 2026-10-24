package com.navfinance.dao;

import com.navfinance.database.DatabaseManager;
import com.navfinance.model.AmountType;
import com.navfinance.model.RecurringSchedule;
import com.navfinance.model.TransactionType;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for managing recurring payment schedule rules
 * (e.g. loan EMIs, rent, monthly electricity bills, subscription plans).
 * <p>
 * Handles querying, creating, updating, and deactivating recurring schedule definitions.
 * </p>
 */
public class RecurringScheduleDao {

    /**
     * Retrieves all recurring schedule rules sorted by calendar day of month.
     *
     * @return List of all {@link RecurringSchedule} rules
     */
    public List<RecurringSchedule> findAll() {
        List<RecurringSchedule> list = new ArrayList<>();
        String sql = "SELECT * FROM recurring_schedules ORDER BY day_of_month ASC, name ASC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch recurring schedules: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves all enabled/active recurring schedules for due occurrence generation.
     *
     * @return List of active {@link RecurringSchedule} rules
     */
    public List<RecurringSchedule> findActive() {
        List<RecurringSchedule> list = new ArrayList<>();
        String sql = "SELECT * FROM recurring_schedules WHERE active = TRUE ORDER BY day_of_month ASC, name ASC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch active recurring schedules: " + e.getMessage());
        }
        return list;
    }

    /**
     * Finds a specific recurring schedule rule by ID.
     *
     * @param id Rule primary key ID
     * @return {@link RecurringSchedule} or {@code null} if not found
     */
    public RecurringSchedule findById(Long id) {
        if (id == null) return null;
        String sql = "SELECT * FROM recurring_schedules WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to find recurring schedule by id: " + e.getMessage());
        }
        return null;
    }

    /**
     * Inserts a new recurring schedule rule into the database.
     *
     * @param s Recurring schedule entity to persist
     * @return Persisted entity with generated ID and timestamps, or {@code null} on failure
     */
    public RecurringSchedule insert(RecurringSchedule s) {
        String sql = "INSERT INTO recurring_schedules (name, type, category, sub_category, day_of_month, amount_type, default_amount, start_date, end_date, payment_via, is_auto_pay, notes, active, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            LocalDateTime now = LocalDateTime.now();
            ps.setString(1, s.getName());
            ps.setString(2, s.getType() != null ? s.getType().name() : "EXPENSE");
            ps.setString(3, s.getCategory());
            ps.setString(4, s.getSubCategory());
            ps.setInt(5, s.getDayOfMonth());
            ps.setString(6, s.getAmountType() != null ? s.getAmountType().name() : "FIXED");
            ps.setDouble(7, s.getDefaultAmount());
            ps.setDate(8, s.getStartDate() != null ? Date.valueOf(s.getStartDate()) : Date.valueOf(now.toLocalDate()));
            if (s.getEndDate() != null) ps.setDate(9, Date.valueOf(s.getEndDate()));
            else ps.setNull(9, java.sql.Types.DATE);
            ps.setString(10, s.getPaymentVia());
            ps.setBoolean(11, s.isAutoPay());
            ps.setString(12, s.getNotes());
            ps.setBoolean(13, s.isActive());
            ps.setTimestamp(14, Timestamp.valueOf(now));
            ps.setTimestamp(15, Timestamp.valueOf(now));

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    s.setId(keys.getLong(1));
                    s.setCreatedAt(now);
                }
            }
            return s;
        } catch (SQLException e) {
            System.err.println("Failed to insert recurring schedule: " + e.getMessage());
            return null;
        }
    }

    /**
     * Updates an existing recurring schedule rule.
     *
     * @param s Recurring schedule entity containing updated attributes and valid ID
     * @return {@code true} if updated successfully, {@code false} otherwise
     */
    public boolean update(RecurringSchedule s) {
        String sql = "UPDATE recurring_schedules SET name = ?, type = ?, category = ?, sub_category = ?, day_of_month = ?, " +
                "amount_type = ?, default_amount = ?, start_date = ?, end_date = ?, payment_via = ?, is_auto_pay = ?, " +
                "notes = ?, active = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getType() != null ? s.getType().name() : "EXPENSE");
            ps.setString(3, s.getCategory());
            ps.setString(4, s.getSubCategory());
            ps.setInt(5, s.getDayOfMonth());
            ps.setString(6, s.getAmountType() != null ? s.getAmountType().name() : "FIXED");
            ps.setDouble(7, s.getDefaultAmount());
            ps.setDate(8, s.getStartDate() != null ? Date.valueOf(s.getStartDate()) : Date.valueOf(java.time.LocalDate.now()));
            if (s.getEndDate() != null) ps.setDate(9, Date.valueOf(s.getEndDate()));
            else ps.setNull(9, java.sql.Types.DATE);
            ps.setString(10, s.getPaymentVia());
            ps.setBoolean(11, s.isAutoPay());
            ps.setString(12, s.getNotes());
            ps.setBoolean(13, s.isActive());
            ps.setTimestamp(14, Timestamp.valueOf(LocalDateTime.now()));
            ps.setLong(15, s.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to update recurring schedule: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a recurring schedule rule by its ID.
     *
     * @param id Primary key ID to delete
     * @return {@code true} if deleted successfully, {@code false} otherwise
     */
    public boolean delete(Long id) {
        if (id == null) return false;
        String sql = "DELETE FROM recurring_schedules WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to delete recurring schedule: " + e.getMessage());
            return false;
        }
    }

    /**
     * Maps an active result set row to a {@link RecurringSchedule} instance.
     *
     * @param rs Active result set
     * @return Mapped RecurringSchedule entity
     * @throws SQLException on column reading error
     */
    private RecurringSchedule mapRow(ResultSet rs) throws SQLException {
        RecurringSchedule s = new RecurringSchedule();
        s.setId(rs.getLong("id"));
        s.setName(rs.getString("name"));
        String typeStr = rs.getString("type");
        if (typeStr != null) {
            try { s.setType(TransactionType.valueOf(typeStr)); } catch (Exception ignored) {}
        }
        s.setCategory(rs.getString("category"));
        s.setSubCategory(rs.getString("sub_category"));
        s.setDayOfMonth(rs.getInt("day_of_month"));
        String atStr = rs.getString("amount_type");
        if (atStr != null) {
            try { s.setAmountType(AmountType.valueOf(atStr)); } catch (Exception ignored) {}
        }
        s.setDefaultAmount(rs.getDouble("default_amount"));
        Date sd = rs.getDate("start_date");
        if (sd != null) s.setStartDate(sd.toLocalDate());
        Date ed = rs.getDate("end_date");
        if (ed != null) s.setEndDate(ed.toLocalDate());
        s.setPaymentVia(rs.getString("payment_via"));
        s.setAutoPay(rs.getBoolean("is_auto_pay"));
        s.setNotes(rs.getString("notes"));
        s.setActive(rs.getBoolean("active"));
        Timestamp ct = rs.getTimestamp("created_at");
        if (ct != null) s.setCreatedAt(ct.toLocalDateTime());
        return s;
    }
}

