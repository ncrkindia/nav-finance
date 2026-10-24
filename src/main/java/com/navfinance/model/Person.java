package com.navfinance.model;

import java.time.LocalDateTime;

/**
 * Model representing an individual contact in the master directory (for Udhaar Given, Taken, or Returns).
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public class Person {

    private Long id;
    private String name;
    private String contact;
    private String relation;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Default constructor. */
    public Person() {}

    /**
     * Parameterized constructor with all business fields.
     *
     * @param id Database identifier.
     * @param name Full name of the person.
     * @param contact Phone number or email.
     * @param relation Relationship tag (e.g. Friend, Relative, Colleague, Landlord).
     * @param notes Additional remarks.
     */
    public Person(Long id, String name, String contact, String relation, String notes) {
        this.id = id;
        this.name = name;
        this.contact = contact;
        this.relation = relation;
        this.notes = notes;
    }

    /**
     * Convenience constructor without ID.
     *
     * @param name Full name of the person.
     * @param contact Phone number or email.
     * @param relation Relationship tag.
     * @param notes Additional remarks.
     */
    public Person(String name, String contact, String relation, String notes) {
        this(null, name, contact, relation, notes);
    }

    /** @return Database ID */
    public Long getId() { return id; }
    /** @param id Database ID */
    public void setId(Long id) { this.id = id; }

    /** @return Full name */
    public String getName() { return name; }
    /** @param name Full name */
    public void setName(String name) { this.name = name; }

    /** @return Contact information */
    public String getContact() { return contact; }
    /** @param contact Contact information */
    public void setContact(String contact) { this.contact = contact; }

    /** @return Relationship type */
    public String getRelation() { return relation; }
    /** @param relation Relationship type */
    public void setRelation(String relation) { this.relation = relation; }

    /** @return Additional notes */
    public String getNotes() { return notes; }
    /** @param notes Additional notes */
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
        return name + (relation != null && !relation.isEmpty() ? " (" + relation + ")" : "");
    }
}
