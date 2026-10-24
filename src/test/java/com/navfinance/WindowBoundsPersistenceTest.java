package com.navfinance;

import com.navfinance.config.AppConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying that window coordinates, dimensions, and maximized states
 * are persisted to settings.properties and restored on application restart.
 */
public class WindowBoundsPersistenceTest {

    private int origX;
    private int origY;
    private int origWidth;
    private int origHeight;
    private boolean origMaximized;

    @BeforeEach
    void setUp() {
        origX = AppConfig.getWindowX();
        origY = AppConfig.getWindowY();
        origWidth = AppConfig.getWindowWidth();
        origHeight = AppConfig.getWindowHeight();
        origMaximized = AppConfig.isWindowMaximized();
    }

    @AfterEach
    void tearDown() {
        AppConfig.saveWindowBounds(origX, origY, origWidth, origHeight, origMaximized);
    }

    @Test
    void testWindowBoundsPersistenceAndReload() {
        // 1. Verify default initial coordinates start at (0, 0)
        assertEquals(0, AppConfig.getWindowX());
        assertEquals(0, AppConfig.getWindowY());

        // 2. Persist moved and resized normal window
        AppConfig.saveWindowBounds(120, 80, 1400, 920, false);
        assertEquals(120, AppConfig.getWindowX());
        assertEquals(80, AppConfig.getWindowY());
        assertEquals(1400, AppConfig.getWindowWidth());
        assertEquals(920, AppConfig.getWindowHeight());
        assertFalse(AppConfig.isWindowMaximized());

        // 3. Reload from disk to simulate application restart
        AppConfig.reloadSettings();
        assertEquals(120, AppConfig.getWindowX(), "Restored X after restart must match saved X");
        assertEquals(80, AppConfig.getWindowY(), "Restored Y after restart must match saved Y");
        assertEquals(1400, AppConfig.getWindowWidth(), "Restored Width after restart must match saved Width");
        assertEquals(920, AppConfig.getWindowHeight(), "Restored Height after restart must match saved Height");
        assertFalse(AppConfig.isWindowMaximized(), "Restored maximized state must be false");

        // 4. Persist maximized window state
        AppConfig.saveWindowBounds(120, 80, 1400, 920, true);
        AppConfig.reloadSettings();
        assertTrue(AppConfig.isWindowMaximized(), "Restored maximized state must be true");
        assertEquals(120, AppConfig.getWindowX());
        assertEquals(80, AppConfig.getWindowY());
    }
}
