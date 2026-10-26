package com.navfinance.security;

import com.navfinance.config.AppConfig;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Service managing user authentication, cryptographic password hashing (salted SHA-256),
 * session login state, and optional login protection settings for Nav Finance.
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public class SecurityService {

    private static SecurityService instance;

    /** Session authentication state flag. */
    private boolean authenticated = false;

    private SecurityService() {}

    /**
     * Retrieves the singleton {@link SecurityService} instance.
     *
     * @return singleton service instance
     */
    public static synchronized SecurityService getInstance() {
        if (instance == null) {
            instance = new SecurityService();
        }
        return instance;
    }

    /**
     * Checks if optional login security is currently enabled.
     *
     * @return {@code true} if login protection is enabled
     */
    public boolean isLoginEnabled() {
        return AppConfig.isLoginEnabled();
    }

    /**
     * Enables or disables startup login protection.
     *
     * @param enabled {@code true} to require login on startup
     */
    public void setLoginEnabled(boolean enabled) {
        AppConfig.setLoginEnabled(enabled);
        AppConfig.saveSettings();
    }

    /**
     * Retrieves the current administrator/user login username.
     *
     * @return configured username
     */
    public String getUsername() {
        return AppConfig.getLoginUsername();
    }

    /**
     * Sets the configured username.
     *
     * @param username new username
     */
    public void setUsername(String username) {
        if (username != null && !username.trim().isEmpty()) {
            AppConfig.setLoginUsername(username.trim());
            AppConfig.saveSettings();
        }
    }

    /**
     * Indicates whether the user has successfully authenticated in the current runtime session.
     * Always returns {@code true} if login security is disabled.
     *
     * @return {@code true} if authenticated or login is disabled
     */
    public boolean isAuthenticated() {
        if (!isLoginEnabled()) {
            return true;
        }
        return authenticated;
    }

    /**
     * Sets the in-memory session authentication state.
     *
     * @param auth authenticated state
     */
    public void setAuthenticated(boolean auth) {
        this.authenticated = auth;
    }

    /**
     * Verifies provided login credentials against stored username and salted SHA-256 hash.
     *
     * @param username input username
     * @param password input plaintext password
     * @return {@code true} if credentials match
     */
    public boolean verifyCredentials(String username, String password) {
        String savedUser = AppConfig.getLoginUsername();
        if (savedUser == null || savedUser.isEmpty()) {
            savedUser = "admin";
        }

        String inputUser = username != null ? username.trim() : "";
        if (!savedUser.equalsIgnoreCase(inputUser)) {
            return false;
        }

        String savedHash = AppConfig.getLoginPasswordHash();
        String savedSalt = AppConfig.getLoginSalt();

        // If no password hash has been configured yet, accept default password "admin"
        if (savedHash == null || savedHash.isEmpty()) {
            return password != null && (password.equals("admin") || password.isEmpty());
        }

        String computedHash = hashPassword(password != null ? password : "", savedSalt != null ? savedSalt : "");
        return savedHash.equals(computedHash);
    }

    /**
     * Authenticates the user in the current session if credentials match.
     *
     * @param username input username
     * @param password input password
     * @return {@code true} if authentication succeeded
     */
    public boolean authenticate(String username, String password) {
        if (verifyCredentials(username, password)) {
            this.authenticated = true;
            return true;
        }
        return false;
    }

    /**
     * Invalidates the active authenticated session (locks application).
     */
    public void logout() {
        this.authenticated = false;
    }

    /**
     * Updates username and password credentials after validating the current password.
     *
     * @param currentPassword active password (must verify if previously configured)
     * @param newUsername new username (optional, retains existing if blank)
     * @param newPassword new password (if blank, retains existing password)
     * @return {@code true} if credentials were updated successfully
     */
    public boolean updateCredentials(String currentPassword, String newUsername, String newPassword) {
        // Validate current password if password was previously set
        String savedHash = AppConfig.getLoginPasswordHash();
        if (savedHash != null && !savedHash.isEmpty()) {
            if (!verifyCredentials(AppConfig.getLoginUsername(), currentPassword)) {
                return false;
            }
        }

        if (newUsername != null && !newUsername.trim().isEmpty()) {
            AppConfig.setLoginUsername(newUsername.trim());
        }

        if (newPassword != null && !newPassword.isEmpty()) {
            String newSalt = generateSalt();
            String newHash = hashPassword(newPassword, newSalt);
            AppConfig.setLoginSalt(newSalt);
            AppConfig.setLoginPasswordHash(newHash);
        }

        AppConfig.saveSettings();
        return true;
    }

    /**
     * Computes salted SHA-256 cryptographic digest string for a given plaintext password.
     *
     * @param password plaintext password
     * @param salt cryptographic salt string
     * @return 64-character lowercase hex digest string
     */
    public static String hashPassword(String password, String salt) {
        if (password == null) password = "";
        if (salt == null) salt = "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] bytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Generates a cryptographically secure random 16-byte salt as a 32-character hex string.
     *
     * @return random hex salt string
     */
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        StringBuilder sb = new StringBuilder(salt.length * 2);
        for (byte b : salt) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
