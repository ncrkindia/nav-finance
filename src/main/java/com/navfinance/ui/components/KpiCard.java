package com.navfinance.ui.components;

import com.navfinance.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;

/**
 * Visual Key Performance Indicator (KPI) metric card component for financial dashboards.
 * <p>
 * Displays a descriptive title, prominent metric value with colored emphasis, optional subtitle,
 * an optional vector metric icon, and a rounded container with a vertical colored accent indicator strip.
 * </p>
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
        setLayout(new BorderLayout(12, 10));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        setPreferredSize(new Dimension(220, 105));

        JPanel contentPanel = new JPanel(new GridLayout(3, 1, 3, 3));
        contentPanel.setOpaque(false);

        lblTitle = new JLabel(title);
        lblTitle.setFont(UiUtils.FONT_SUBTITLE);
        lblTitle.setForeground(UiUtils.COLOR_TEXT_MUTED);

        lblValue = new JLabel(initialValue);
        lblValue.setFont(UiUtils.FONT_KPI_VALUE);
        lblValue.setForeground(accentColor);

        lblSubtitle = new JLabel(subtitle != null ? subtitle : "");
        lblSubtitle.setFont(UiUtils.FONT_SUBTITLE);
        lblSubtitle.setForeground(UiUtils.COLOR_TEXT_MUTED);

        contentPanel.add(lblTitle);
        contentPanel.add(lblValue);
        contentPanel.add(lblSubtitle);

        add(contentPanel, BorderLayout.CENTER);

        if (icon != null) {
            JLabel lblIcon = new JLabel(icon);
            lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
            lblIcon.setVerticalAlignment(SwingConstants.CENTER);
            lblIcon.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
            add(lblIcon, BorderLayout.EAST);
        }
    }

    /**
     * Updates the primary displayed metric value string.
     *
     * @param value new metric value text
     */
    public void setValue(String value) {
        lblValue.setText(value);
    }

    /**
     * Updates the contextual subtitle description.
     *
     * @param subtitle new subtitle text
     */
    public void setSubtitle(String subtitle) {
        lblSubtitle.setText(subtitle != null ? subtitle : "");
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
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, w, h, 14, 14);

        // Thin neutral border
        g2.setColor(UiUtils.COLOR_BORDER);
        g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);

        // Vertical colored indicator stripe on the left edge
        g2.setColor(accentColor);
        g2.fillRoundRect(0, 10, 5, h - 20, 4, 4);

        g2.dispose();
        super.paintComponent(g);
    }
}
