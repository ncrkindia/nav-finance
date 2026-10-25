package com.navfinance.ui.components;

import com.navfinance.model.DashboardSummary;
import com.navfinance.ui.util.UiUtils;
import com.navfinance.ui.util.ZoomManager;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom 2D graphical Bar Chart component plotting temporal cash flow comparisons
 * between cumulative Income inflows and Expense outflows.
 * <p>
 * Features responsive dynamic legend positioning, non-overlapping axis labels,
 * zero-suppression, and smooth anti-aliased rendering.
 * </p>
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public class TrendChartPanel extends JPanel {

    /** Sequential list of data points representing time increments (e.g. days or months). */
    private List<DashboardSummary.TrendPoint> points = new ArrayList<>();

    /** Headline title displayed at the top-left of the chart. */
    private String title;

    /**
     * Constructs a {@link TrendChartPanel} with the specified headline label.
     *
     * @param title headline label
     */
    public TrendChartPanel(String title) {
        this.title = title != null ? title : "";
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(540, 300));
        ZoomManager.addZoomListener(this::repaint);
    }

    /**
     * Updates the headline title of the chart and triggers a repaint.
     *
     * @param title new headline title
     */
    public void setTitle(String title) {
        this.title = title != null ? title : "";
        repaint();
    }

    /**
     * Gets the headline title.
     *
     * @return current title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Updates the sequential time-series trend data points and triggers a repaint.
     *
     * @param points list of {@link DashboardSummary.TrendPoint} records
     */
    public void setTrendPoints(List<DashboardSummary.TrendPoint> points) {
        this.points = points != null ? points : new ArrayList<>();
        repaint();
    }

    /**
     * Custom painting procedure rendering chart container borders, legends, grid lines,
     * y-axis currency scale labels, comparative income/expense bar pairs, and x-axis labels.
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

        // 1. Background container with rounded border
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, w - 1, h - 1, 12, 12);
        g2.setColor(UiUtils.COLOR_BORDER);
        g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);

        // 2. Measure fonts and header elements dynamically
        Font titleFont = UiUtils.boldFont();
        Font legFont = UiUtils.subtitleFont();

        g2.setFont(titleFont);
        FontMetrics fmTitle = g2.getFontMetrics(titleFont);
        int titleWidth = fmTitle.stringWidth(title);

        g2.setFont(legFont);
        FontMetrics fmLeg = g2.getFontMetrics(legFont);

        String inLabel = "Income (In)";
        String exLabel = "Expense (Out)";
        int inWidth = fmLeg.stringWidth(inLabel);
        int exWidth = fmLeg.stringWidth(exLabel);
        int boxSize = Math.max(10, Math.round(11 * scale));
        int boxGap = 5;
        int itemGap = 16;
        int totalLegWidth = boxSize + boxGap + inWidth + itemGap + boxSize + boxGap + exWidth;

        boolean twoRowHeader = (18 + titleWidth + 24 + totalLegWidth) > (w - 20);

        // Draw Title
        int titleY = Math.max(22, Math.round(24 * scale));
        g2.setFont(titleFont);
        g2.setColor(new Color(30, 41, 59));
        g2.drawString(title, 18, titleY);

        // Draw Legend dynamically without overlap
        int legendX;
        int legendY;
        if (twoRowHeader) {
            legendX = 18;
            legendY = titleY + fmTitle.getHeight() + 4;
        } else {
            legendX = Math.max(18 + titleWidth + 20, w - totalLegWidth - 20);
            legendY = titleY;
        }

        // Inflow Legend Box & Label
        g2.setColor(UiUtils.COLOR_INCOME);
        g2.fillRect(legendX, legendY - boxSize + 1, boxSize, boxSize);
        g2.setFont(legFont);
        g2.setColor(UiUtils.COLOR_TEXT_MUTED);
        g2.drawString(inLabel, legendX + boxSize + boxGap, legendY);

        // Outflow Legend Box & Label
        int legExX = legendX + boxSize + boxGap + inWidth + itemGap;
        g2.setColor(UiUtils.COLOR_EXPENSE);
        g2.fillRect(legExX, legendY - boxSize + 1, boxSize, boxSize);
        g2.drawString(exLabel, legExX + boxSize + boxGap, legendY);

        if (points.isEmpty()) {
            g2.setFont(UiUtils.regularFont());
            g2.setColor(UiUtils.COLOR_TEXT_MUTED);
            g2.drawString("No transaction data available for this period.", 20, h / 2);
            g2.dispose();
            return;
        }

        // 3. Plot Dimensions and Margins
        int padLeft = Math.max(45, Math.round(48 * scale));
        int padRight = 20;
        int padTop = twoRowHeader ? Math.max(60, Math.round(66 * scale)) : Math.max(44, Math.round(48 * scale));
        int padBottom = Math.max(34, Math.round(38 * scale));
        int plotW = Math.max(10, w - padLeft - padRight);
        int plotH = Math.max(10, h - padTop - padBottom);

        // 4. Calculate dynamic upper ceiling for vertical scaling
        double maxVal = 1000.0;
        for (DashboardSummary.TrendPoint tp : points) {
            if (tp.getIncome() > maxVal) maxVal = tp.getIncome();
            if (tp.getExpense() > maxVal) maxVal = tp.getExpense();
        }
        maxVal = Math.ceil(maxVal * 1.15); // Add 15% headroom

        // 5. Draw horizontal guideline grid steps and y-axis metric markers
        g2.setColor(new Color(241, 245, 249));
        g2.setStroke(new BasicStroke(1.0f));
        int gridSteps = 4;
        for (int i = 0; i <= gridSteps; i++) {
            int y = padTop + (int) (plotH * (1.0 - (double) i / gridSteps));
            g2.drawLine(padLeft, y, w - padRight, y);

            double v = (maxVal * i) / gridSteps;
            String vLabel = v >= 1000 ? String.format("%.0fk", v / 1000) : String.format("%.0f", v);
            g2.setFont(UiUtils.regularFont().deriveFont(Math.max(9f, 10f * scale)));
            g2.setColor(new Color(148, 163, 184));
            g2.drawString(vLabel, 6, y + 4);
        }

        // 6. Draw side-by-side comparative bars for each time slot
        int count = points.size();
        int slotW = plotW / Math.max(count, 1);
        int barW = Math.max(4, Math.min(22, (slotW - 14) / 2));

        Font xFont = UiUtils.regularFont().deriveFont(Math.max(9f, 10f * scale));
        g2.setFont(xFont);
        FontMetrics fmX = g2.getFontMetrics(xFont);

        // Calculate label skip interval to prevent horizontal label collision
        int approxStrW = count > 0 ? fmX.stringWidth(points.get(0).getLabel()) : 30;
        int labelSkip = 1;
        if (slotW < approxStrW + 6) {
            labelSkip = (int) Math.ceil((double) (approxStrW + 8) / Math.max(1, slotW));
        }

        for (int i = 0; i < count; i++) {
            DashboardSummary.TrendPoint tp = points.get(i);
            int slotCenterX = padLeft + i * slotW + (slotW / 2);

            int inH = (int) ((tp.getIncome() / maxVal) * plotH);
            int exH = (int) ((tp.getExpense() / maxVal) * plotH);

            int inX = slotCenterX - barW - 2;
            int inY = padTop + plotH - inH;

            int exX = slotCenterX + 2;
            int exY = padTop + plotH - exH;

            // Inflow bar (Green)
            if (inH > 0) {
                g2.setColor(UiUtils.COLOR_INCOME);
                g2.fillRoundRect(inX, inY, barW, inH, 4, 4);
            }

            // Outflow bar (Red)
            if (exH > 0) {
                g2.setColor(UiUtils.COLOR_EXPENSE);
                g2.fillRoundRect(exX, exY, barW, exH, 4, 4);
            }

            // X-Axis centered date/label with collision prevention
            if (i % labelSkip == 0 || i == count - 1) {
                g2.setColor(UiUtils.COLOR_TEXT_MUTED);
                int strW = fmX.stringWidth(tp.getLabel());
                g2.drawString(tp.getLabel(), slotCenterX - (strW / 2), padTop + plotH + Math.round(14 * scale));
            }
        }

        g2.dispose();
    }
}
