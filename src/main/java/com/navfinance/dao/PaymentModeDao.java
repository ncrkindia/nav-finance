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
}

