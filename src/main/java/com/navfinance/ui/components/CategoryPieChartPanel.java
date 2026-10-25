package com.navfinance.ui.components;

import com.navfinance.config.AppConfig;
import com.navfinance.ui.util.UiUtils;
import com.navfinance.ui.util.ZoomManager;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Custom graphical Donut/Pie Chart component illustrating expense distributions across categories.
 * <p>
 * Features anti-aliased 2D rendering, modern color-coded sectors, a center summary hole cutout,
 * and an aligned legend with formatted percentages that automatically scales during UI zoom.
 * </p>
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public class CategoryPieChartPanel extends JPanel {

    /** Chart headline title displayed in the upper-left corner. */
    private final String title;

    /** Map of category names to their respective aggregate expense totals. */
    private Map<String, Double> data = new LinkedHashMap<>();

    /** Palette of vibrant modern colors cycled through chart slices. */
    private static final Color[] SLICE_COLORS = {
            new Color(59, 130, 246),  // Blue
            new Color(16, 185, 129),  // Emerald
            new Color(245, 158, 11),  // Amber
            new Color(239, 68, 68),   // Red
            new Color(139, 92, 246),  // Purple
            new Color(236, 72, 153),  // Pink
            new Color(20, 184, 166),  // Teal
            new Color(249, 115, 22),  // Orange
            new Color(107, 114, 128)  // Gray
    };

    /**
     * Constructs a new {@link CategoryPieChartPanel} with the designated header title.
     *
     * @param title headline label
     */
    public CategoryPieChartPanel(String title) {
        this.title = title;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(380, 300));
        ZoomManager.addZoomListener(this::repaint);
    }

    /**
     * Updates the chart dataset and requests an immediate component repaint.
     *
     * @param data map of category names to aggregated spending values
     */
    public void setData(Map<String, Double> data) {
        this.data = data != null ? data : new LinkedHashMap<>();
        repaint();
    }

    /**
     * Custom painting routine rendering the rounded container, donut arcs, center cutout,
     * central cumulative total, and side legend entries.
     *
     * @param g graphic context
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth();
        int h = getHeight();
        float scale = ZoomManager.getCurrentScale();

        // 1. Background container with modern rounded border
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, w - 1, h - 1, 12, 12);
        g2.setColor(UiUtils.COLOR_BORDER);
        g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);

        // 2. Title header
        Font titleFont = UiUtils.boldFont();
        g2.setFont(titleFont);
        g2.setColor(new Color(30, 41, 59));
        int titleY = Math.max(22, Math.round(24 * scale));
        g2.drawString(title, 18, titleY);

        // Empty state check
        if (data.isEmpty()) {
            g2.setFont(UiUtils.regularFont());
            g2.setColor(UiUtils.COLOR_TEXT_MUTED);
            g2.drawString("No category expenses in this period.", 20, h / 2);
            g2.dispose();
            return;
        }

        // Calculate sum of all slice values
        double total = 0;
        for (double val : data.values()) total += val;

        if (total <= 0) {
            g2.setFont(UiUtils.regularFont());
            g2.setColor(UiUtils.COLOR_TEXT_MUTED);
            g2.drawString("No expense data recorded.", 20, h / 2);
            g2.dispose();
            return;
        }

        // 3. Donut Chart Positioning and Geometric bounds
        int topMargin = titleY + 12;
        int donutDiameter = Math.min(160, Math.min(w / 2 - 20, h - topMargin - 20));
        donutDiameter = Math.max(80, donutDiameter);
        int donutX = 20;
        int donutY = topMargin + (h - topMargin - donutDiameter) / 2;

        // Draw individual arc wedges starting from top (90 degrees)
        double currentAngle = 90.0;
        int colorIdx = 0;

        for (Map.Entry<String, Double> entry : data.entrySet()) {
            double angle = (entry.getValue() / total) * 360.0;
            Color sliceColor = SLICE_COLORS[colorIdx % SLICE_COLORS.length];
            g2.setColor(sliceColor);

            Arc2D.Double arc = new Arc2D.Double(donutX, donutY, donutDiameter, donutDiameter, currentAngle, -angle, Arc2D.PIE);
            g2.fill(arc);

            currentAngle -= angle;
            colorIdx++;
        }

        // 4. Inner circle cutout for the Donut visual effect
        int holeDiameter = (int) (donutDiameter * 0.58);
        int holeX = donutX + (donutDiameter - holeDiameter) / 2;
        int holeY = donutY + (donutDiameter - holeDiameter) / 2;
        g2.setColor(getBackground());
        g2.fill(new Ellipse2D.Double(holeX, holeY, holeDiameter, holeDiameter));

        // 5. Center metric label inside donut hole
        Font holeFont = UiUtils.boldFont().deriveFont(Math.max(9f, Math.min(13f, 10f * scale)));
        g2.setFont(holeFont);
        g2.setColor(new Color(30, 41, 59));
        FontMetrics fmHole = g2.getFontMetrics(holeFont);
        String totalStr = AppConfig.formatCurrencyPlain(total);
        if (totalStr.length() > 9) totalStr = String.format("%.1fk", total / 1000);
        int tw = fmHole.stringWidth(totalStr);
        g2.drawString(totalStr, holeX + (holeDiameter - tw) / 2, holeY + (holeDiameter / 2) + (fmHole.getAscent() / 2) - 1);

        // 6. Right-hand Legend listing with percentage shares
        int legendX = donutX + donutDiameter + 16;
        int legendY = topMargin + 10;
        colorIdx = 0;

        Font legFont = UiUtils.regularFont().deriveFont(Math.max(9f, 10f * scale));
        g2.setFont(legFont);
        FontMetrics fmLeg = g2.getFontMetrics(legFont);
        int stepY = Math.max(16, fmLeg.getHeight() + 4);
        int boxSize = Math.max(8, Math.round(9 * scale));

        for (Map.Entry<String, Double> entry : data.entrySet()) {
            if (legendY > h - 12) break; // Restrict legend within vertical boundary

            Color sliceColor = SLICE_COLORS[colorIdx % SLICE_COLORS.length];
            g2.setColor(sliceColor);
            g2.fillRoundRect(legendX, legendY - boxSize + 1, boxSize, boxSize, 3, 3);

            g2.setColor(new Color(51, 65, 85));
            double pct = (entry.getValue() / total) * 100.0;
            String text = String.format("%s (%.0f%%)", entry.getKey(), pct);
            if (text.length() > 20) text = text.substring(0, 17) + "...";
            g2.drawString(text, legendX + boxSize + 6, legendY);

            legendY += stepY;
            colorIdx++;
        }

        g2.dispose();
    }
}
