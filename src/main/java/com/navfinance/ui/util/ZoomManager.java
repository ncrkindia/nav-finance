package com.navfinance.ui.util;

import com.formdev.flatlaf.FlatLaf;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages UI Zoom In, Zoom Out, and Reset Zoom scaling across all Swing and FlatLaf components.
 * <p>
 * Controls application-wide typography scaling across tables, menus, dialogs, buttons, and input
 * fields, notifying registered UI listeners and refreshing top-level window component trees
 * upon every scale alteration.
 * </p>
 */
public final class ZoomManager {

    /** Base typography font size in points at 100% baseline zoom. */
    private static final float BASE_FONT_SIZE = 12.0f;

    /** Current pointer index in the {@link #ZOOM_SCALES} array (defaults to 2 for 100%). */
    private static int zoomIndex = 2; // Default 100%

    /** Array of supported zoom scale multipliers ranging from 80% to 175%. */
    private static final float[] ZOOM_SCALES = {0.80f, 0.90f, 1.00f, 1.15f, 1.30f, 1.50f, 1.75f};

    /** Display percentage labels corresponding to each supported zoom factor index. */
    private static final String[] ZOOM_LABELS = {"80%", "90%", "100%", "115%", "130%", "150%", "175%"};

    /** Registered listener callbacks invoked whenever the zoom factor changes. */
    private static final List<Runnable> zoomListeners = new ArrayList<>();

    /** Private constructor preventing instantiation of utility class. */
    private ZoomManager() {}

    /**
     * Retrieves the current decimal zoom scale factor (e.g., 1.00f for 100%, 1.15f for 115%).
     *
     * @return current zoom multiplier
     */
    public static float getCurrentScale() {
        return ZOOM_SCALES[zoomIndex];
    }

    /**
     * Retrieves the user-friendly percentage label for the active zoom level (e.g., "100%").
     *
     * @return percentage string representation
     */
    public static String getZoomLabel() {
        return ZOOM_LABELS[zoomIndex];
    }

    /**
     * Checks if further magnification is permitted.
     *
     * @return {@code true} if current zoom is below maximum scale (175%)
     */
    public static boolean canZoomIn() {
        return zoomIndex < ZOOM_SCALES.length - 1;
    }

    /**
     * Checks if further reduction is permitted.
     *
     * @return {@code true} if current zoom is above minimum scale (80%)
     */
    public static boolean canZoomOut() {
        return zoomIndex > 0;
    }

    /**
     * Steps up to the next magnification level and applies font scaling across all open frames.
     *
     * @param mainFrame the primary application frame
     */
    public static void zoomIn(JFrame mainFrame) {
        if (canZoomIn()) {
            zoomIndex++;
            applyZoom(mainFrame);
        }
    }

    /**
     * Steps down to the next smaller magnification level and refreshes component trees.
     *
     * @param mainFrame the primary application frame
     */
    public static void zoomOut(JFrame mainFrame) {
        if (canZoomOut()) {
            zoomIndex--;
            applyZoom(mainFrame);
        }
    }

    /**
     * Resets the application zoom level to the default 100% baseline.
     *
     * @param mainFrame the primary application frame
     */
    public static void resetZoom(JFrame mainFrame) {
        zoomIndex = 2; // 100%
        applyZoom(mainFrame);
    }

    /**
     * Registers a callback listener to be triggered whenever the zoom scale is adjusted.
     *
     * @param listener callback task to execute
     */
    public static void addZoomListener(Runnable listener) {
        zoomListeners.add(listener);
    }

    /**
     * Recalculates font dimensions, registers updated fonts in the global {@link UIManager},
     * updates all active window component trees, and notifies zoom listeners.
     *
     * @param mainFrame root application window frame
     */
    private static void applyZoom(JFrame mainFrame) {
        float scale = getCurrentScale();
        int newFontSize = Math.round(BASE_FONT_SIZE * scale);

        Font defaultFont = new Font("Segoe UI", Font.PLAIN, newFontSize);
        Font boldFont = new Font("Segoe UI", Font.BOLD, newFontSize);

        // Update global Swing UIManager font keys
        UIManager.put("defaultFont", defaultFont);
        UIManager.put("Button.font", boldFont);
        UIManager.put("Label.font", defaultFont);
        UIManager.put("Table.font", defaultFont);
        UIManager.put("TableHeader.font", boldFont);
        UIManager.put("TextField.font", defaultFont);
        UIManager.put("ComboBox.font", defaultFont);
        UIManager.put("TabbedPane.font", boldFont);

        // Propagate UI font updates to all currently realized windows and dialogs
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
        }

        // Notify FlatLaf rendering engine
        FlatLaf.updateUI();

        // Revalidate and repaint root frame layout
        if (mainFrame != null) {
            mainFrame.revalidate();
            mainFrame.repaint();
        }

        // Fire registered callback listeners
        for (Runnable r : zoomListeners) {
            r.run();
        }
    }
}
