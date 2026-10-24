package com.navfinance;

import com.navfinance.config.AppConfig;
import com.navfinance.ui.util.UiUtils;
import com.navfinance.ui.util.ZoomManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying that UI Zoom In, Zoom Out, and Reset Zoom states are persisted
 * to settings.properties and accurately restored on application restart.
 */
public class ZoomPersistenceTest {

    @AfterEach
    void resetToDefault() {
        ZoomManager.resetZoom(null);
    }

    @Test
    void testZoomPersistenceAndRestoration() {
        // Initial reset
        ZoomManager.resetZoom(null);
        assertEquals(2, AppConfig.getZoomIndex());
        assertEquals("100%", ZoomManager.getZoomLabel());
        assertEquals(1.00f, ZoomManager.getCurrentScale(), 0.001f);

        // Zoom In once -> 115% (index 3)
        ZoomManager.zoomIn(null);
        assertEquals(3, AppConfig.getZoomIndex(), "Zoom in should persist index 3");
        assertEquals("115%", ZoomManager.getZoomLabel());
        assertEquals(1.15f, ZoomManager.getCurrentScale(), 0.001f);

        // Zoom In again -> 130% (index 4)
        ZoomManager.zoomIn(null);
        assertEquals(4, AppConfig.getZoomIndex(), "Zoom in should persist index 4");
        assertEquals("130%", ZoomManager.getZoomLabel());
        assertEquals(1.30f, ZoomManager.getCurrentScale(), 0.001f);

        // Verify typography scales
        UiUtils.refreshFonts();
        assertTrue(UiUtils.regularFont().getSize() > 12, "Regular font at 130% should be larger than baseline 12pt");

        // Zoom Out twice -> 100% (index 2)
        ZoomManager.zoomOut(null);
        ZoomManager.zoomOut(null);
        assertEquals(2, AppConfig.getZoomIndex(), "Zoom out twice should return to index 2");
        assertEquals("100%", ZoomManager.getZoomLabel());

        // Zoom Out once more -> 90% (index 1)
        ZoomManager.zoomOut(null);
        assertEquals(1, AppConfig.getZoomIndex(), "Zoom out should persist index 1");
        assertEquals("90%", ZoomManager.getZoomLabel());
        assertEquals(0.90f, ZoomManager.getCurrentScale(), 0.001f);

        // Reset zoom -> 100% (index 2)
        ZoomManager.resetZoom(null);
        assertEquals(2, AppConfig.getZoomIndex());
        assertEquals("100%", ZoomManager.getZoomLabel());
    }
}
