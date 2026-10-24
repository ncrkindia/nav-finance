package com.navfinance.dao;

import com.navfinance.database.DatabaseManager;
import com.navfinance.model.Person;

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
 * Data Access Object (DAO) for managing Person master records
 * used for Udhar Given / Udhar Taken tracking and personal loan accounting.
 */
public class PersonDao {

    /**
     * Retrieves all contacts ordered alphabetically by name.
     *
     * @return List of all {@link Person} entities
     */
    public List<Person> findAll() {
        List<Person> list = new ArrayList<>();
        String sql = "SELECT * FROM persons ORDER BY name ASC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch persons: " + e.getMessage());
        }
        return list;
    }

    /**
     * Finds a person record by primary key ID.
     *
     * @param id Primary key ID
     * @return {@link Person} entity, or {@code null} if not found
     */
    public Person findById(Long id) {
        if (id == null) return null;
        String sql = "SELECT * FROM persons WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to find person by id: " + e.getMessage());
        }
        return null;
    }

    /**
     * Inserts a new person record.
     *
     * @param person Person entity to persist
     * @return Persisted person with generated ID and timestamps
     */
    public Person insert(Person person) {
        String sql = "INSERT INTO persons (name, contact, relation, notes, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            LocalDateTime now = LocalDateTime.now();
            ps.setString(1, person.getName());
            ps.setString(2, person.getContact());
            ps.setString(3, person.getRelation());
            ps.setString(4, person.getNotes());
            ps.setTimestamp(5, Timestamp.valueOf(now));
            ps.setTimestamp(6, Timestamp.valueOf(now));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    person.setId(keys.getLong(1));
                    person.setCreatedAt(now);
                    person.setUpdatedAt(now);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to insert person: " + e.getMessage());
        }
        return person;
    }

    /**
     * Updates an existing person record.
     *
     * @param person Person entity containing updated details and valid ID
     * @return {@code true} if updated successfully, {@code false} otherwise
     */
    public boolean update(Person person) {
        String sql = "UPDATE persons SET name = ?, contact = ?, relation = ?, notes = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            LocalDateTime now = LocalDateTime.now();
            ps.setString(1, person.getName());
            ps.setString(2, person.getContact());
            ps.setString(3, person.getRelation());
            ps.setString(4, person.getNotes());
            ps.setTimestamp(5, Timestamp.valueOf(now));
            ps.setLong(6, person.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to update person: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a person record by ID.
     *
     * @param id Primary key ID to delete
     * @return {@code true} if deleted successfully, {@code false} otherwise
     */
    public boolean delete(Long id) {
        String sql = "DELETE FROM persons WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to delete person: " + e.getMessage());
            return false;
        }
    }

    /**
     * Maps an active result set row to a {@link Person} entity.
     *
     * @param rs Active result set
     * @return Mapped Person entity
     * @throws SQLException on column reading error
     */
    private Person mapRow(ResultSet rs) throws SQLException {
        Person p = new Person();
        p.setId(rs.getLong("id"));
        p.setName(rs.getString("name"));
        p.setContact(rs.getString("contact"));
        p.setRelation(rs.getString("relation"));
        p.setNotes(rs.getString("notes"));
        Timestamp cat = rs.getTimestamp("created_at");
        if (cat != null) p.setCreatedAt(cat.toLocalDateTime());
        Timestamp uat = rs.getTimestamp("updated_at");
        if (uat != null) p.setUpdatedAt(uat.toLocalDateTime());
        return p;
    }
}

