package com.navfinance.dao;

import com.navfinance.database.DatabaseManager;
import com.navfinance.model.AuditLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) responsible for persisting and querying system audit logs.
 * <p>
 * Ensures every financial mutation, master data update, CSV import, and settlement
 * is permanently recorded with timestamps, entity context, action types, and diff details.
 * </p>
 */
public class AuditDao {

    /**
     * Records a new audit log event in the database.
     *
     * @param entityType The entity classification (e.g., "TRANSACTION", "DUE_PAYMENT", "PERSON")
     * @param entityId The primary key ID of the affected entity, or {@code null} if batch/system-wide
     * @param action The operation performed (e.g., "CREATE", "UPDATE", "DELETE", "SETTLE")
     * @param details Detailed narrative or diff of the changes
     */
    public void log(String entityType, Long entityId, String action, String details) {
        String sql = "INSERT INTO audit_logs (entity_type, entity_id, action, details, log_timestamp) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entityType);
            if (entityId != null) ps.setLong(2, entityId);
            else ps.setNull(2, java.sql.Types.BIGINT);
            ps.setString(3, action);
            ps.setString(4, details);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to insert audit log: " + e.getMessage());
        }
    }

    /**
     * Convenience method to persist an {@link AuditLog} instance.
     *
     * @param log Audit log model to persist
     */
    public void log(AuditLog log) {
        if (log != null) {
            log(log.getEntityType(), log.getEntityId(), log.getAction(), log.getDetails());
        }
    }

    /**
     * Retrieves the most recent audit records ordered newest first.
     *
     * @param limit Maximum number of records to return (defaults to 200 if &lt;= 0)
     * @return List of {@link AuditLog} instances
     */
    public List<AuditLog> findAll(int limit) {
        List<AuditLog> list = new ArrayList<>();
        String sql = "SELECT * FROM audit_logs ORDER BY log_timestamp DESC, id DESC LIMIT ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit > 0 ? limit : 200);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to query audit logs: " + e.getMessage());
        }
        return list;
    }

    /**
     * Queries audit logs filtered by target entity type.
     *
     * @param entityType Target entity classification (e.g. "TRANSACTION", "DUE_PAYMENT")
     * @param limit Maximum records to return
     * @return Filtered list of audit logs
     */
    public List<AuditLog> findByEntityType(String entityType, int limit) {
        List<AuditLog> list = new ArrayList<>();
        String sql = "SELECT * FROM audit_logs WHERE entity_type = ? ORDER BY log_timestamp DESC, id DESC LIMIT ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entityType);
            ps.setInt(2, limit > 0 ? limit : 200);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to query audit logs by entity type: " + e.getMessage());
        }
        return list;
    }

    /**
     * Maps a JDBC {@link ResultSet} row to an {@link AuditLog} instance.
     *
     * @param rs Active result set positioned at a valid row
     * @return Mapped {@link AuditLog} object
     * @throws SQLException on database column access error
     */
    private AuditLog mapRow(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setId(rs.getLong("id"));
        log.setEntityType(rs.getString("entity_type"));
        long eid = rs.getLong("entity_id");
        if (!rs.wasNull()) log.setEntityId(eid);
        log.setAction(rs.getString("action"));
        log.setDetails(rs.getString("details"));
        Timestamp ts = rs.getTimestamp("log_timestamp");
        if (ts != null) log.setTimestamp(ts.toLocalDateTime());
        return log;
    }
}

