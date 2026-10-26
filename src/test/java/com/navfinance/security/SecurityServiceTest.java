package com.navfinance.security;

import com.navfinance.config.AppConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests verifying optional login security, cryptographic salted SHA-256 hashing,
 * credential management, and session authentication states.
 */
public class SecurityServiceTest {

    private SecurityService securityService;

    @BeforeAll
    static void setUpAll() {
        System.setProperty("java.awt.headless", "true");
        AppConfig.setTestMode(true);
    }

    @AfterAll
    static void tearDownAll() {
        AppConfig.setTestMode(false);
    }

    @BeforeEach
    void setUp() {
        securityService = SecurityService.getInstance();
        AppConfig.setLoginEnabled(false);
        AppConfig.setLoginUsername("admin");
        AppConfig.setLoginPasswordHash("");
        AppConfig.setLoginSalt("");
        securityService.logout();
    }

    @Test
    @DisplayName("Password hashing produces consistent 64-character hex digests with salt")
    void testPasswordHashing() {
        String salt1 = SecurityService.generateSalt();
        assertNotNull(salt1);
        assertEquals(32, salt1.length());

        String hash1 = SecurityService.hashPassword("SuperSecret123", salt1);
        assertNotNull(hash1);
        assertEquals(64, hash1.length());

        // Same password and salt produces exact same hash
        String hash2 = SecurityService.hashPassword("SuperSecret123", salt1);
        assertEquals(hash1, hash2);

        // Different salt produces completely different hash
        String salt2 = SecurityService.generateSalt();
        String hash3 = SecurityService.hashPassword("SuperSecret123", salt2);
        assertFalse(hash1.equals(hash3));
    }

    @Test
    @DisplayName("Initial default credentials accept admin/admin when hash is empty")
    void testInitialDefaultCredentials() {
        assertTrue(securityService.verifyCredentials("admin", "admin"));
        assertTrue(securityService.verifyCredentials("ADMIN", "admin"));
        assertFalse(securityService.verifyCredentials("wronguser", "admin"));
        assertFalse(securityService.verifyCredentials("admin", "wrongpassword"));
    }

    @Test
    @DisplayName("Updating credentials and password modification persists salted SHA-256 hash")
    void testUpdateCredentials() {
        // Initial setup
        boolean ok = securityService.updateCredentials("admin", "finance_manager", "Pass@2026");
        assertTrue(ok);
        assertEquals("finance_manager", securityService.getUsername());

        // Verification with new credentials
        assertTrue(securityService.verifyCredentials("finance_manager", "Pass@2026"));
        assertFalse(securityService.verifyCredentials("finance_manager", "WrongPass"));
        assertFalse(securityService.verifyCredentials("admin", "Pass@2026"));

        // Changing password with wrong current password should fail
        boolean failedUpdate = securityService.updateCredentials("WrongCurrent", "finance_manager", "NewPass");
        assertFalse(failedUpdate);

        // Changing password with correct current password should succeed
        boolean successUpdate = securityService.updateCredentials("Pass@2026", "finance_manager", "NewPass999");
        assertTrue(successUpdate);
        assertTrue(securityService.verifyCredentials("finance_manager", "NewPass999"));
    }

    @Test
    @DisplayName("Login enabled flag controls session authentication requirements")
    void testLoginEnabledSessionState() {
        // When disabled: isAuthenticated is always true
        securityService.setLoginEnabled(false);
        assertFalse(securityService.isLoginEnabled());
        assertTrue(securityService.isAuthenticated());

        // When enabled: requires explicit authentication
        securityService.setLoginEnabled(true);
        assertTrue(securityService.isLoginEnabled());
        assertFalse(securityService.isAuthenticated());

        // Authenticate with valid credentials
        boolean authResult = securityService.authenticate("admin", "admin");
        assertTrue(authResult);
        assertTrue(securityService.isAuthenticated());

        // Logout locks the session
        securityService.logout();
        assertFalse(securityService.isAuthenticated());
    }
}
