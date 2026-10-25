package com.navfinance.ui.util;

import com.formdev.flatlaf.FlatLaf;
import com.navfinance.config.AppConfig;
import com.navfinance.i18n.I18n;

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
 * upon every scale alteration. Zoom level is persisted across application restarts.
 * </p>
 */
public final class ZoomManager {

    /** Base typography font size in points at 100% baseline zoom. */
    private static final float BASE_FONT_SIZE = 12.0f;

    /** Array of supported zoom scale multipliers ranging from 80% to 175%. */
    private static final float[] ZOOM_SCALES = {0.80f, 0.90f, 1.00f, 1.15f, 1.30f, 1.50f, 1.75f};

    /** Display percentage labels corresponding to each supported zoom factor index. */
    private static final String[] ZOOM_LABELS = {"80%", "90%", "100%", "115%", "130%", "150%", "175%"};

    /** Current pointer index in the {@link #ZOOM_SCALES} array (restored from AppConfig). */
    private static int zoomIndex = initZoomIndex();

    /** Registered listener callbacks invoked whenever the zoom factor changes. */
    private static final List<Runnable> zoomListeners = new ArrayList<>();

    /** Private constructor preventing instantiation of utility class. */
    private ZoomManager() {}

    /**
     * Initializes the zoom index from user configuration, clamping to valid range.
     *
     * @return valid zoom index
     */
    private static int initZoomIndex() {
        int idx = AppConfig.getZoomIndex();
        if (idx < 0 || idx >= ZOOM_SCALES.length) {
            idx = 2; // Default 100%
        }
        return idx;
    }

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
     * Returns the total count of supported zoom scale stops.
     *
     * @return number of zoom steps
     */
    public static int getScaleCount() {
        return ZOOM_SCALES.length;
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
     * Steps up to the next magnification level, persists setting, and applies font scaling.
     *
     * @param mainFrame the primary application frame
     */
    public static void zoomIn(JFrame mainFrame) {
        if (canZoomIn()) {
            zoomIndex++;
            AppConfig.setZoomIndex(zoomIndex);
            applyZoom(mainFrame);
        }
    }

    /**
     * Steps up to the next magnification level for the active application window.
     */
    public static void zoomIn() {
        zoomIn(null);
    }

    /**
     * Steps down to the next smaller magnification level, persists setting, and refreshes component trees.
     *
     * @param mainFrame the primary application frame
     */
    public static void zoomOut(JFrame mainFrame) {
        if (canZoomOut()) {
            zoomIndex--;
            AppConfig.setZoomIndex(zoomIndex);
            applyZoom(mainFrame);
        }
    }

    /**
     * Steps down to the next smaller magnification level for the active application window.
     */
    public static void zoomOut() {
        zoomOut(null);
    }

    /**
     * Resets the application zoom level to the default 100% baseline and persists setting.
     *
     * @param mainFrame the primary application frame
     */
    public static void resetZoom(JFrame mainFrame) {
        zoomIndex = 2; // 100%
        AppConfig.setZoomIndex(zoomIndex);
        applyZoom(mainFrame);
    }

    /**
     * Resets the application zoom level to the default 100% baseline.
     */
    public static void resetZoom() {
        resetZoom(null);
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
    public static void applyZoom(JFrame mainFrame) {
        float scale = getCurrentScale();
        int newFontSize = Math.round(BASE_FONT_SIZE * scale);
        String fontFamily = I18n.getFontFamily();

        javax.swing.plaf.FontUIResource defaultFont = new javax.swing.plaf.FontUIResource(fontFamily, Font.PLAIN, newFontSize);
        javax.swing.plaf.FontUIResource boldFont = new javax.swing.plaf.FontUIResource(fontFamily, Font.BOLD, newFontSize);
        javax.swing.plaf.FontUIResource titleFont = new javax.swing.plaf.FontUIResource(fontFamily, Font.BOLD, Math.round(18.0f * scale));

        // Update global Swing UIManager font keys with FontUIResource
        UIManager.put("defaultFont", defaultFont);
        UIManager.put("Button.font", boldFont);
        UIManager.put("Label.font", defaultFont);
        UIManager.put("Table.font", defaultFont);
        UIManager.put("TableHeader.font", boldFont);
        UIManager.put("TextField.font", defaultFont);
        UIManager.put("ComboBox.font", defaultFont);
        UIManager.put("TabbedPane.font", boldFont);
        UIManager.put("TitlePane.font", titleFont);
        UIManager.put("TitlePane.titleFont", titleFont);
        UIManager.put("RootPane.titlePane.font", titleFont);
        UIManager.put("InternalFrame.titleFont", titleFont);

        // Dynamically scale table row height globally
        int scaledRowHeight = Math.max(26, Math.round(28.0f * scale));
        UIManager.put("Table.rowHeight", scaledRowHeight);

        // Ensure ComboBox dropdown arrow visibility
        UIManager.put("Component.arrowType", "chevron");
        UIManager.put("ComboBox.buttonStyle", "chevron");
        UIManager.put("ComboBox.buttonArrowColor", new java.awt.Color(71, 85, 105));
        UIManager.put("ComboBox.buttonHoverArrowColor", new java.awt.Color(15, 23, 42));

        // Update centralized UiUtils static font caches
        UiUtils.refreshFonts();

        // Propagate UI updates and scale table row heights across all realized windows
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
            updateComponentTreeRecursively(window, scale, scaledRowHeight);
            window.revalidate();
            window.repaint();
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

    /**
     * Recursively scales custom component dimensions (such as JTable row heights) across window trees.
     */
    private static void updateComponentTreeRecursively(java.awt.Component comp, float scale, int rowHeight) {
        if (comp instanceof javax.swing.JTable table) {
            table.setRowHeight(rowHeight);
            table.setFont(UiUtils.regularFont());
            if (table.getTableHeader() != null) {
                table.getTableHeader().setFont(UiUtils.boldFont());
            }
        } else if (comp instanceof com.navfinance.ui.components.KpiCard kpi) {
            kpi.refreshZoom(scale);
        }

        if (comp instanceof java.awt.Container container) {
            for (java.awt.Component child : container.getComponents()) {
                updateComponentTreeRecursively(child, scale, rowHeight);
            }
        }
    }
}
