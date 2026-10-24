package com.navfinance.ui.util;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;

/**
 * Common User Interface utility toolkit providing centralized visual styles,
 * color tokens, typography scales, button factories, and modal notification helpers.
 * <p>
 * Ensures aesthetic uniformity across dialogs, panels, and table renderers in accordance
 * with the modern FlatLaf design system.
 * </p>
 */
public final class UiUtils {

    /** Private constructor to prevent instantiation of utility class. */
    private UiUtils() {}

    // =========================================================================
    // Color Palette Tokens
    // =========================================================================

    /** Modern royal blue primary branding color (#2563EB). */
    public static final Color COLOR_PRIMARY = new Color(37, 99, 235);

    /** Darker shade of primary blue used for mouse hover states (#1D4ED8). */
    public static final Color COLOR_PRIMARY_HOVER = new Color(29, 78, 216);

    /** Vibrant emerald green representing positive financial inflows and income (#16A34A). */
    public static final Color COLOR_INCOME = new Color(22, 163, 74);

    /** Soft pastel green background tint for income badges and cards (#F0FDF4). */
    public static final Color COLOR_INCOME_BG = new Color(240, 253, 244);

    /** Vibrant crimson red representing expenses, deficits, and overdue liabilities (#DC2626). */
    public static final Color COLOR_EXPENSE = new Color(220, 38, 38);

    /** Soft pastel red background tint for expense badges and cards (#FEF2F2). */
    public static final Color COLOR_EXPENSE_BG = new Color(254, 242, 242);

    /** Warm amber gold signifying warnings, pending loans, and dues balances (#D97706). */
    public static final Color COLOR_AMBER = new Color(217, 119, 6);

    /** Soft pastel amber background tint for warning badges (#FEF3C7). */
    public static final Color COLOR_AMBER_BG = new Color(254, 243, 199);

    /** Pure white background for elevated card surfaces (#FFFFFF). */
    public static final Color COLOR_CARD_BG = new Color(255, 255, 255);

    /** Slate neutral subtle border color (#E2E8F0). */
    public static final Color COLOR_BORDER = new Color(226, 232, 240);

    /** Primary dark charcoal text color (#0F172A). */
    public static final Color COLOR_TEXT_PRIMARY = new Color(15, 23, 42);

    /** Secondary slate dark text color (#334155). */
    public static final Color COLOR_TEXT_SECONDARY = new Color(51, 65, 85);

    /** Muted slate gray text color for secondary labels and subtitles (#64748B). */
    public static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139);

    // =========================================================================
    // Typography Scales (Seamless Devanagari & Latin Typography)
    // =========================================================================

    /** Prominent screen and dialog title font (18pt Bold). */
    public static Font FONT_TITLE = com.navfinance.i18n.I18n.getTitleFont();

    /** Section heading font (14pt Bold). */
    public static Font FONT_HEADER = com.navfinance.i18n.I18n.getHeaderFont();

    /** Subtitle and secondary descriptive font (12pt Plain). */
    public static Font FONT_SUBTITLE = com.navfinance.i18n.I18n.getSubFont();

    /** Emphasized bold text font for tables, buttons, and badges (12pt Bold). */
    public static Font FONT_BOLD = com.navfinance.i18n.I18n.getBoldFont();

    /** Standard body copy font (12pt Plain). */
    public static Font FONT_REGULAR = com.navfinance.i18n.I18n.getRegularFont();

    /** Large metric headline font for KPI dashboard cards (22pt Bold). */
    public static Font FONT_KPI_VALUE = com.navfinance.i18n.I18n.getKpiFont();

    /** Refreshes all cached static font scales when the system language or zoom changes. */
    public static void refreshFonts() {
        float scale = ZoomManager.getCurrentScale();
        String family = com.navfinance.i18n.I18n.getFontFamily();
        FONT_TITLE = new Font(family, Font.BOLD, Math.round(18.0f * scale));
        FONT_HEADER = new Font(family, Font.BOLD, Math.round(14.0f * scale));
        FONT_SUBTITLE = new Font(family, Font.PLAIN, Math.round(11.0f * scale));
        FONT_BOLD = new Font(family, Font.BOLD, Math.round(12.0f * scale));
        FONT_REGULAR = new Font(family, Font.PLAIN, Math.round(12.0f * scale));
        FONT_KPI_VALUE = new Font(family, Font.BOLD, Math.round(22.0f * scale));
    }

    /** @return active localized title font */
    public static Font titleFont() { return FONT_TITLE; }

    /** @return active localized header font */
    public static Font headerFont() { return FONT_HEADER; }

    /** @return active localized subtitle font */
    public static Font subtitleFont() { return FONT_SUBTITLE; }

    /** @return active localized bold font */
    public static Font boldFont() { return FONT_BOLD; }

    /** @return active localized regular body font */
    public static Font regularFont() { return FONT_REGULAR; }

    /** @return active localized large KPI metric font */
    public static Font kpiFont() { return FONT_KPI_VALUE; }

    // =========================================================================
    // Button Factories
    // =========================================================================

    /**
     * Constructs a styled primary call-to-action button in royal blue.
     *
     * @param text label text to display on button
     * @return styled {@link JButton} instance
     */
    public static JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setBackground(COLOR_PRIMARY);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        return btn;
    }

    /**
     * Constructs a styled success button in green for confirmations and positive actions.
     *
     * @param text label text to display on button
     * @return styled {@link JButton} instance
     */
    public static JButton createSuccessButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setBackground(COLOR_INCOME);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        return btn;
    }

    /**
     * Constructs a styled danger button in red for destructive actions such as deletion.
     *
     * @param text label text to display on button
     * @return styled {@link JButton} instance
     */
    public static JButton createDangerButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setBackground(COLOR_EXPENSE);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        return btn;
    }

    /**
     * Constructs a styled secondary outline button for cancellations and passive actions.
     *
     * @param text label text to display on button
     * @return styled {@link JButton} instance
     */
    public static JButton createSecondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_REGULAR);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));
        return btn;
    }

    /**
     * Constructs a styled primary call-to-action button in royal blue with an icon.
     *
     * @param text label text to display on button
     * @param icon icon to display alongside label
     * @return styled {@link JButton} instance
     */
    public static JButton createPrimaryButton(String text, Icon icon) {
        JButton btn = createPrimaryButton(text);
        if (icon != null) {
            btn.setIcon(icon);
            btn.setIconTextGap(8);
        }
        return btn;
    }

    /**
     * Constructs a styled success button in green with an icon.
     *
     * @param text label text to display on button
     * @param icon icon to display alongside label
     * @return styled {@link JButton} instance
     */
    public static JButton createSuccessButton(String text, Icon icon) {
        JButton btn = createSuccessButton(text);
        if (icon != null) {
            btn.setIcon(icon);
            btn.setIconTextGap(8);
        }
        return btn;
    }

    /**
     * Constructs a styled danger button in red with an icon.
     *
     * @param text label text to display on button
     * @param icon icon to display alongside label
     * @return styled {@link JButton} instance
     */
    public static JButton createDangerButton(String text, Icon icon) {
        JButton btn = createDangerButton(text);
        if (icon != null) {
            btn.setIcon(icon);
            btn.setIconTextGap(8);
        }
        return btn;
    }

    /**
     * Constructs a styled secondary outline button with an icon.
     *
     * @param text label text to display on button
     * @param icon icon to display alongside label
     * @return styled {@link JButton} instance
     */
    public static JButton createSecondaryButton(String text, Icon icon) {
        JButton btn = createSecondaryButton(text);
        if (icon != null) {
            btn.setIcon(icon);
            btn.setIconTextGap(8);
        }
        return btn;
    }

    /**
     * Creates a compound rounded border with inner padding suitable for dashboard cards.
     *
     * @return composite {@link Border} instance
     */
    public static Border createCardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1, true),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        );
    }

    // =========================================================================
    // Modal Notification Helpers
    // =========================================================================

    /**
     * Displays an informational success dialog to the user.
     *
     * @param parent parent component for modal anchoring
     * @param message notification message to display
     */
    public static void showSuccess(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Displays a warning alert dialog to the user.
     *
     * @param parent parent component for modal anchoring
     * @param message warning message to display
     */
    public static void showWarning(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * Displays a standard informational message dialog.
     *
     * @param parent parent component for modal anchoring
     * @param message info message to display
     */
    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Displays a critical error message dialog.
     *
     * @param parent parent component for modal anchoring
     * @param message error message to display
     */
    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Displays a binary Yes/No confirmation dialog.
     *
     * @param parent parent component for modal anchoring
     * @param message query or prompt text
     * @return {@code true} if the user confirmed by selecting YES, {@code false} otherwise
     */
    public static boolean confirm(Component parent, String message) {
        int result = JOptionPane.showConfirmDialog(parent, message, "Confirm Action", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return result == JOptionPane.YES_OPTION;
    }
}
