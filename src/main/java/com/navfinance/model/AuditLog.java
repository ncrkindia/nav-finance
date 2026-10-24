package com.navfinance.model;

import java.time.LocalDateTime;

/**
 * Model representing an immutable audit trail entry tracking system-wide state changes,
 * user mutations, creation, update diffs, and deletions.
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public class AuditLog {

    private Long id;
    private String entityType;
    private Long entityId;
    private String action; // CREATE, UPDATE, DELETE, SETTLE, UNSETTLE
    private String details;
    private LocalDateTime timestamp;

    /** Default constructor. */
    public AuditLog() {}

    /**
     * Complete parameterized constructor.
     *
     * @param id Database record identifier.
     * @param entityType Name of the entity table modified.
     * @param entityId Primary key of the affected record.
     * @param action Mutation action performed.
     * @param details Textual description or field diff.
     * @param timestamp Date and time the action occurred.
     */
    public AuditLog(Long id, String entityType, Long entityId, String action, String details, LocalDateTime timestamp) {
        this.id = id;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.details = details;
        this.timestamp = timestamp;
    }

    /**
     * Convenience constructor with automatic current timestamp.
     *
     * @param entityType Name of the entity table modified.
     * @param entityId Primary key of the affected record.
     * @param action Mutation action performed.
     * @param details Textual description or field diff.
     */
    public AuditLog(String entityType, Long entityId, String action, String details) {
        this(null, entityType, entityId, action, details, LocalDateTime.now());
    }

    /** @return Database ID */
    public Long getId() { return id; }
    /** @param id Database ID */
    public void setId(Long id) { this.id = id; }

    /** @return Entity type (e.g. TRANSACTION, PERSON, BILL) */
    public String getEntityType() { return entityType; }
    /** @param entityType Entity type */
    public void setEntityType(String entityType) { this.entityType = entityType; }

    /** @return Target entity ID */
    public Long getEntityId() { return entityId; }
    /** @param entityId Target entity ID */
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    /** @return Action name (CREATE, UPDATE, DELETE) */
    public String getAction() { return action; }
    /** @param action Action name */
    public void setAction(String action) { this.action = action; }

    /** @return Audit diff or description */
    public String getDetails() { return details; }
    /** @param details Audit diff or description */
    public void setDetails(String details) { this.details = details; }

    /** @return Event timestamp */
    public LocalDateTime getTimestamp() { return timestamp; }
    /** @param timestamp Event timestamp */
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + action + " on " + entityType + "#" + entityId + ": " + details;
    }
}
