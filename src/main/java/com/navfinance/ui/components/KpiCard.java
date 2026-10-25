package com.navfinance.ui.components;

import com.navfinance.ui.util.UiUtils;
import com.navfinance.ui.util.ZoomManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Visual Key Performance Indicator (KPI) metric card component for financial dashboards.
 * <p>
 * Displays a descriptive title, prominent metric value with colored emphasis, optional subtitle,
 * an optional vector metric icon, and a rounded container with a vertical colored accent indicator strip.
 * Automatically adapts dimensions and label positioning during UI zoom in/out to prevent label overlapping.
 * </p>
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public class KpiCard extends JPanel {

    /** Accent color applied to the primary metric text and vertical indicator bar. */
    private final Color accentColor;

    /** Title label describing the financial metric (e.g., "Total Income"). */
    private final JLabel lblTitle;

    /** Primary large metric display label. */
    private final JLabel lblValue;

    /** Secondary footnote label displaying context (e.g., "Across 14 entries"). */
    private final JLabel lblSubtitle;

    /** Optional right-side icon label. */
    private JLabel lblIcon;

    /**
     * Constructs a styled KPI card without an icon.
     *
     * @param title metric category title
     * @param initialValue formatted metric value string
     * @param subtitle supporting context or period detail
     * @param accentColor visual accent color
     */
    public KpiCard(String title, String initialValue, String subtitle, Color accentColor) {
        this(title, initialValue, subtitle, accentColor, null);
    }

    /**
     * Constructs a styled KPI card with an optional visual metric icon.
     *
     * @param title metric category title
     * @param initialValue formatted metric value string
     * @param subtitle supporting context or period detail
     * @param accentColor visual accent color
     * @param icon optional vector icon to display on the right card edge
     */
    public KpiCard(String title, String initialValue, String subtitle, Color accentColor, Icon icon) {
        this.accentColor = accentColor;
        setLayout(new BorderLayout(12, 6));
        setBackground(Color.WHITE);
        setOpaque(false);

        // Vertical box layout guarantees title, value, and subtitle NEVER overlap
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        lblTitle = new JLabel(title);
        lblTitle.setFont(UiUtils.subtitleFont());
        lblTitle.setForeground(UiUtils.COLOR_TEXT_MUTED);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblValue = new JLabel(initialValue);
        lblValue.setFont(UiUtils.kpiFont());
        lblValue.setForeground(accentColor);
        lblValue.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblSubtitle = new JLabel(subtitle != null ? subtitle : "");
        lblSubtitle.setFont(UiUtils.subtitleFont());
        lblSubtitle.setForeground(UiUtils.COLOR_TEXT_MUTED);
        lblSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        contentPanel.add(lblTitle);
        contentPanel.add(Box.createVerticalStrut(3));
        contentPanel.add(lblValue);
        contentPanel.add(Box.createVerticalStrut(3));
        contentPanel.add(lblSubtitle);

        add(contentPanel, BorderLayout.CENTER);

        if (icon != null) {
            lblIcon = new JLabel(icon);
            lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
            lblIcon.setVerticalAlignment(SwingConstants.CENTER);
            lblIcon.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
            add(lblIcon, BorderLayout.EAST);
        }

        updateDimensions();
        ZoomManager.addZoomListener(this::refreshFonts);
    }

    /**
     * Dynamically updates preferred and minimum bounds and internal insets based on zoom scale.
     */
    private void updateDimensions() {
        float scale = ZoomManager.getCurrentScale();
        int padY = Math.max(10, Math.round(12 * scale));
        int padX = Math.max(14, Math.round(16 * scale));
        setBorder(BorderFactory.createEmptyBorder(padY, padX, padY, padX));

        int cardW = Math.max(200, Math.round(220 * scale));
        int cardH = Math.max(95, Math.round(108 * scale));
        setPreferredSize(new Dimension(cardW, cardH));
        setMinimumSize(new Dimension(cardW, cardH));
    }

    /**
     * Updates the primary displayed metric value string.
     *
     * @param value new metric value text
     */
    public void setValue(String value) {
        lblValue.setText(value);
        lblValue.setFont(UiUtils.kpiFont());
    }

    /**
     * Updates the contextual subtitle description.
     *
     * @param subtitle new subtitle text
     */
    public void setSubtitle(String subtitle) {
        lblSubtitle.setText(subtitle != null ? subtitle : "");
        lblSubtitle.setFont(UiUtils.subtitleFont());
    }

    /**
     * Updates the card metric title label.
     *
     * @param title new title text
     */
    public void setTitle(String title) {
        lblTitle.setText(title != null ? title : "");
        lblTitle.setFont(UiUtils.subtitleFont());
    }

    /**
     * Refreshes label font references and scales card dimensions to prevent overlaps.
     */
    public void refreshFonts() {
        lblTitle.setFont(UiUtils.subtitleFont());
        lblValue.setFont(UiUtils.kpiFont());
        lblSubtitle.setFont(UiUtils.subtitleFont());
        updateDimensions();
        revalidate();
        repaint();
    }

    /**
     * Public hook allowing explicit zoom scale application from window walkers.
     */
    public void refreshZoom(float scale) {
        refreshFonts();
    }

    /**
     * Custom painting procedure rendering rounded borders and the vertical accent pill bar.
     *
     * @param g graphic context
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // White card background with smooth rounded corners
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(0, 0, w, h, 14, 14);

        // Thin neutral border
        g2.setColor(UiUtils.COLOR_BORDER);
        g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);

        // Vertical colored indicator stripe on the left edge
        g2.setColor(accentColor);
        g2.fillRoundRect(0, 10, 5, Math.max(10, h - 20), 4, 4);

        g2.dispose();
        super.paintComponent(g);
    }
}
