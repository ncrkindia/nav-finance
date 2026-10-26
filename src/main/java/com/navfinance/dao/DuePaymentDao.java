package com.navfinance.dao;

import com.navfinance.database.DatabaseManager;
import com.navfinance.model.DuePayment;
import com.navfinance.model.DueStatus;
import com.navfinance.model.TransactionType;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for managing individual scheduled bill and EMI due payment occurrences.
 * <p>
 * Supports querying by billing period or overdue status, marking occurrences settled/unsettled,
 * linking to ledger transactions, and preventing duplicate monthly occurrences.
 * </p>
 */
public class DuePaymentDao {

    /**
     * Retrieves all due payment occurrences ordered newest due date first.
     *
     * @return List of all {@link DuePayment} records
     */
    public List<DuePayment> findAll() {
        List<DuePayment> list = new ArrayList<>();
        String sql = "SELECT * FROM due_payments ORDER BY due_date DESC, id DESC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch due payments: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves all due payment occurrences within a specified date window.
     *
     * @param from Start date boundary (inclusive)
     * @param to End date boundary (inclusive)
     * @return List of {@link DuePayment} occurrences within range
     */
    public List<DuePayment> findByPeriod(LocalDate from, LocalDate to) {
        List<DuePayment> list = new ArrayList<>();
        String sql = "SELECT * FROM due_payments WHERE due_date BETWEEN ? AND ? ORDER BY due_date ASC, id ASC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch due payments by period: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves all unsettled payments whose due date has passed.
     *
     * @param today Current evaluation date (typically today)
     * @return List of overdue {@link DuePayment} occurrences
     */
    public List<DuePayment> findOverdue(LocalDate today) {
        List<DuePayment> list = new ArrayList<>();
        String sql = "SELECT * FROM due_payments WHERE status != 'SETTLED' AND due_date < ? ORDER BY due_date ASC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(today));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch overdue due payments: " + e.getMessage());
        }
        return list;
    }

    /**
     * Finds a specific due payment occurrence by its ID.
     *
     * @param id Primary key ID
     * @return {@link DuePayment} or {@code null} if not found
     */
    public DuePayment findById(Long id) {
        if (id == null) return null;
        String sql = "SELECT * FROM due_payments WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to find due payment by id: " + e.getMessage());
        }
        return null;
    }

    /**
     * Checks if an occurrence already exists for a given schedule rule on a target due date.
     *
     * @param scheduleId Recurring schedule rule ID
     * @param dueDate Due date to test
     * @return {@code true} if occurrence exists, {@code false} otherwise
     */
    public boolean existsForScheduleAndDate(Long scheduleId, LocalDate dueDate) {
        if (scheduleId == null || dueDate == null) return false;
        String sql = "SELECT COUNT(*) FROM due_payments WHERE schedule_id = ? AND due_date = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, scheduleId);
            ps.setDate(2, Date.valueOf(dueDate));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to test existence of due payment: " + e.getMessage());
        }
        return false;
    }

    /**
     * Inserts a new due payment occurrence record.
     *
     * @param p Due payment entity to persist
     * @return Persisted entity with generated ID and timestamps, or {@code null} on failure
     */
    public DuePayment insert(DuePayment p) {
        String sql = "INSERT INTO due_payments (schedule_id, title, type, category, sub_category, due_date, expected_amount, " +
                "is_variable, is_auto_pay, status, settled_amount, settled_date, settled_via, transaction_id, notes, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            LocalDateTime now = LocalDateTime.now();
            if (p.getScheduleId() != null) ps.setLong(1, p.getScheduleId());
            else ps.setNull(1, java.sql.Types.BIGINT);
            ps.setString(2, p.getTitle());
            ps.setString(3, p.getType() != null ? p.getType().name() : "EXPENSE");
            ps.setString(4, p.getCategory());
            ps.setString(5, p.getSubCategory());
            ps.setDate(6, Date.valueOf(p.getDueDate()));
            ps.setDouble(7, p.getExpectedAmount());
            ps.setBoolean(8, p.isVariable());
            ps.setBoolean(9, p.isAutoPay());
            ps.setString(10, p.getStatus() != null ? p.getStatus().name() : "PENDING");
            ps.setDouble(11, p.getSettledAmount());
            if (p.getSettledDate() != null) ps.setDate(12, Date.valueOf(p.getSettledDate()));
            else ps.setNull(12, java.sql.Types.DATE);
            ps.setString(13, p.getSettledVia());
            if (p.getTransactionId() != null) ps.setLong(14, p.getTransactionId());
            else ps.setNull(14, java.sql.Types.BIGINT);
            ps.setString(15, p.getNotes());
            ps.setTimestamp(16, Timestamp.valueOf(now));

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    p.setId(keys.getLong(1));
                    p.setCreatedAt(now);
                }
            }
            return p;
        } catch (SQLException e) {
            System.err.println("Failed to insert due payment: " + e.getMessage());
            return null;
        }
    }

    /**
     * Updates an existing due payment occurrence.
     *
     * @param p Due payment entity with updated values and valid ID
     * @return {@code true} if updated successfully, {@code false} otherwise
     */
    public boolean update(DuePayment p) {
        String sql = "UPDATE due_payments SET title = ?, type = ?, category = ?, sub_category = ?, due_date = ?, " +
                "expected_amount = ?, is_variable = ?, is_auto_pay = ?, status = ?, settled_amount = ?, settled_date = ?, " +
                "settled_via = ?, transaction_id = ?, notes = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getTitle());
            ps.setString(2, p.getType() != null ? p.getType().name() : "EXPENSE");
            ps.setString(3, p.getCategory());
            ps.setString(4, p.getSubCategory());
            ps.setDate(5, Date.valueOf(p.getDueDate()));
            ps.setDouble(6, p.getExpectedAmount());
            ps.setBoolean(7, p.isVariable());
            ps.setBoolean(8, p.isAutoPay());
            ps.setString(9, p.getStatus() != null ? p.getStatus().name() : "PENDING");
            ps.setDouble(10, p.getSettledAmount());
            if (p.getSettledDate() != null) ps.setDate(11, Date.valueOf(p.getSettledDate()));
            else ps.setNull(11, java.sql.Types.DATE);
            ps.setString(12, p.getSettledVia());
            if (p.getTransactionId() != null) ps.setLong(13, p.getTransactionId());
            else ps.setNull(13, java.sql.Types.BIGINT);
            ps.setString(14, p.getNotes());
            ps.setLong(15, p.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to update due payment: " + e.getMessage());
            return false;
        }
    }

    /**
     * Marks a due payment occurrence as settled, recording actual paid amount, settlement date,
     * payment channel, and linking to the generated cashbook transaction ID.
     *
     * @param id Due payment occurrence ID
     * @param settledAmount Actual monetary amount paid
     * @param settledDate Date when payment cleared
     * @param settledVia Payment channel mode (e.g. UPI, Net Banking)
     * @param transactionId ID of the transaction created in the ledger
     * @return {@code true} if settlement marked successfully
     */
    public boolean markSettled(Long id, double settledAmount, LocalDate settledDate, String settledVia, Long transactionId) {
        String sql = "UPDATE due_payments SET status = 'SETTLED', settled_amount = ?, settled_date = ?, settled_via = ?, " +
                "transaction_id = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, settledAmount);
            ps.setDate(2, Date.valueOf(settledDate != null ? settledDate : LocalDate.now()));
            ps.setString(3, settledVia);
            if (transactionId != null) ps.setLong(4, transactionId);
            else ps.setNull(4, java.sql.Types.BIGINT);
            ps.setLong(5, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to mark due payment settled: " + e.getMessage());
            return false;
        }
    }

    /**
     * Reverts a settled payment occurrence back to PENDING state and clears settlement fields.
     *
     * @param id Due payment occurrence ID
     * @return {@code true} if reverted successfully
     */
    public boolean markUnsettled(Long id) {
        String sql = "UPDATE due_payments SET status = 'PENDING', settled_amount = 0.0, settled_date = NULL, " +
                "settled_via = NULL, transaction_id = NULL WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to mark due payment unsettled: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a due payment occurrence by its ID.
     *
     * @param id Primary key ID to delete
     * @return {@code true} if deleted successfully, {@code false} otherwise
     */
    public boolean delete(Long id) {
        if (id == null) return false;
        String sql = "DELETE FROM due_payments WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to delete due payment: " + e.getMessage());
            return false;
        }
    }

    /**
     * Purges invalid due payment records with type = 'INCOME' (since due payments represent outgoing liabilities).
     *
     * @return Number of purged invalid records
     */
    public int deleteInvalidIncomeDuePayments() {
        String sql = "DELETE FROM due_payments WHERE type = 'INCOME'";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            return stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("Failed to delete income due payments: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Retrieves all unsettled payments within a specified date window.
     *
     * @param from Start date boundary (inclusive)
     * @param to End date boundary (inclusive)
     * @return List of unsettled {@link DuePayment} records
     */
    public List<DuePayment> findUnsettledByPeriod(LocalDate from, LocalDate to) {
        List<DuePayment> list = new ArrayList<>();
        String sql = "SELECT * FROM due_payments WHERE status != 'SETTLED' AND due_date BETWEEN ? AND ? ORDER BY due_date ASC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch unsettled due payments by period: " + e.getMessage());
        }
        return list;
    }

    /**
     * Maps an active result set row to a {@link DuePayment} entity.
     *
     * @param rs Active result set
     * @return Mapped DuePayment entity
     * @throws SQLException on column reading error
     */
    private DuePayment mapRow(ResultSet rs) throws SQLException {
        DuePayment p = new DuePayment();
        p.setId(rs.getLong("id"));
        long sId = rs.getLong("schedule_id");
        if (!rs.wasNull()) p.setScheduleId(sId);
        p.setTitle(rs.getString("title"));
        String typeStr = rs.getString("type");
        if (typeStr != null) {
            try { p.setType(TransactionType.valueOf(typeStr)); } catch (Exception ignored) {}
        }
        p.setCategory(rs.getString("category"));
        p.setSubCategory(rs.getString("sub_category"));
        Date dd = rs.getDate("due_date");
        if (dd != null) p.setDueDate(dd.toLocalDate());
        p.setExpectedAmount(rs.getDouble("expected_amount"));
        p.setVariable(rs.getBoolean("is_variable"));
        p.setAutoPay(rs.getBoolean("is_auto_pay"));
        String stStr = rs.getString("status");
        if (stStr != null) {
            try { p.setStatus(DueStatus.valueOf(stStr)); } catch (Exception ignored) {}
        }
        p.setSettledAmount(rs.getDouble("settled_amount"));
        Date sd = rs.getDate("settled_date");
        if (sd != null) p.setSettledDate(sd.toLocalDate());
        p.setSettledVia(rs.getString("settled_via"));
        long tId = rs.getLong("transaction_id");
        if (!rs.wasNull()) p.setTransactionId(tId);
        p.setNotes(rs.getString("notes"));
        Timestamp ct = rs.getTimestamp("created_at");
        if (ct != null) p.setCreatedAt(ct.toLocalDateTime());
        return p;
    }
}

