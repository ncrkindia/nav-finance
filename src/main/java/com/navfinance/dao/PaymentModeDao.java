package com.navfinance.dao;

import com.navfinance.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) for managing payment channel modes
 * (e.g., Cash, UPI, Paytm, PhonePe, Net Banking, Credit Card, Debit Card).
 * <p>
 * Supports dynamic registration of user-defined custom payment channels.
 * </p>
 */
public class PaymentModeDao {

    /**
     * Retrieves all available payment modes configured in the database in insertion order.
     *
     * @return List of payment mode names
     */
    public List<String> findAllModes() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT mode_name FROM payment_modes ORDER BY id ASC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(rs.getString("mode_name"));
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch payment modes: " + e.getMessage());
        }
        return list;
    }

    /**
     * Adds a new custom payment mode if it does not already exist.
     *
     * @param modeName Unique name of the payment mode (e.g., "Google Pay", "Cheque")
     * @return {@code true} if added or already present, {@code false} on failure
     */
    public boolean addMode(String modeName) {
        if (modeName == null || modeName.trim().isEmpty()) return false;
        String sql = "MERGE INTO payment_modes (mode_name, is_system) KEY(mode_name) VALUES (?, FALSE)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, modeName.trim());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to add payment mode: " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates/renames an existing payment mode and cascades the rename to all transactions using it.
     *
     * @param oldName Current name of the payment mode
     * @param newName New name for the payment mode
     * @return {@code true} if updated successfully
     */
    public boolean updateMode(String oldName, String newName) {
        if (oldName == null || newName == null || newName.trim().isEmpty()) return false;
        String trimmedOld = oldName.trim();
        String trimmedNew = newName.trim();
        String sql = "UPDATE payment_modes SET mode_name = ? WHERE mode_name = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psMode = conn.prepareStatement(sql);
                 PreparedStatement psTx = conn.prepareStatement("UPDATE transactions SET payment_via = ? WHERE payment_via = ?")) {
                psMode.setString(1, trimmedNew);
                psMode.setString(2, trimmedOld);
                int updated = psMode.executeUpdate();

                psTx.setString(1, trimmedNew);
                psTx.setString(2, trimmedOld);
                psTx.executeUpdate();

                conn.commit();
                return updated > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Failed to update payment mode: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a payment mode from the database.
     *
     * @param modeName Name of the payment mode to remove
     * @return {@code true} if deleted successfully
     */
    public boolean deleteMode(String modeName) {
        if (modeName == null || modeName.trim().isEmpty()) return false;
        String sql = "DELETE FROM payment_modes WHERE mode_name = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, modeName.trim());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to delete payment mode: " + e.getMessage());
            return false;
        }
    }
}

