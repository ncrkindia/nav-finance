package com.navfinance.ui.components;

import com.navfinance.model.DashboardSummary;
import com.navfinance.ui.util.UiUtils;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
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
 * Supports dynamic scaling, zero-suppression, subtle horizontal guideline grids,
 * side-by-side comparative green/red bars, and centered temporal axis labels.
 * </p>
 */
public class TrendChartPanel extends JPanel {

    /** Sequential list of data points representing time increments (e.g. days or months). */
    private List<DashboardSummary.TrendPoint> points = new ArrayList<>();

    /** Headline title displayed at the top-left of the chart. */
    private final String title;

    /**
     * Constructs a {@link TrendChartPanel} with the specified headline label.
     *
     * @param title headline label
     */
    public TrendChartPanel(String title) {
        this.title = title;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(540, 300));
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

        // 1. Background container with rounded border
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, w - 1, h - 1, 12, 12);
        g2.setColor(UiUtils.COLOR_BORDER);
        g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);

        // 2. Title
        g2.setFont(UiUtils.boldFont());
        g2.setColor(new Color(30, 41, 59));
        g2.drawString(title, 18, 26);

        // 3. Legend (Income vs Expense)
        int legendX = w - 210;
        g2.setColor(UiUtils.COLOR_INCOME);
        g2.fillRect(legendX, 16, 12, 12);
        g2.setFont(UiUtils.subtitleFont());
        g2.setColor(UiUtils.COLOR_TEXT_MUTED);
        g2.drawString("Income (In)", legendX + 18, 26);

        g2.setColor(UiUtils.COLOR_EXPENSE);
        g2.fillRect(legendX + 100, 16, 12, 12);
        g2.drawString("Expense (Out)", legendX + 118, 26);

        if (points.isEmpty()) {
            g2.setFont(UiUtils.regularFont());
            g2.setColor(UiUtils.COLOR_TEXT_MUTED);
            g2.drawString("No transaction data available for this period.", 20, h / 2);
            g2.dispose();
            return;
        }

        // 4. Plot Dimensions and Margins
        int padLeft = 45;
        int padRight = 20;
        int padTop = 50;
        int padBottom = 40;
        int plotW = w - padLeft - padRight;
        int plotH = h - padTop - padBottom;

        // 5. Calculate dynamic upper ceiling for vertical scaling
        double maxVal = 1000.0;
        for (DashboardSummary.TrendPoint tp : points) {
            if (tp.getIncome() > maxVal) maxVal = tp.getIncome();
            if (tp.getExpense() > maxVal) maxVal = tp.getExpense();
        }
        maxVal = Math.ceil(maxVal * 1.15); // Add 15% headroom for aesthetic spacing

        // 6. Draw horizontal guideline grid steps and y-axis metric markers
        g2.setColor(new Color(241, 245, 249));
        g2.setStroke(new BasicStroke(1.0f));
        int gridSteps = 4;
        for (int i = 0; i <= gridSteps; i++) {
            int y = padTop + (int) (plotH * (1.0 - (double) i / gridSteps));
            g2.drawLine(padLeft, y, w - padRight, y);

            double v = (maxVal * i) / gridSteps;
            String vLabel = v >= 1000 ? String.format("%.0fk", v / 1000) : String.format("%.0f", v);
            g2.setFont(UiUtils.regularFont().deriveFont(10f));
            g2.setColor(new Color(148, 163, 184));
            g2.drawString(vLabel, 8, y + 4);
        }

        // 7. Draw side-by-side comparative bars for each time slot
        int count = points.size();
        int slotW = plotW / Math.max(count, 1);
        int barW = Math.max(6, Math.min(22, (slotW - 16) / 2));

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

            // X-Axis centered date/label
            g2.setFont(UiUtils.regularFont().deriveFont(10f));
            g2.setColor(UiUtils.COLOR_TEXT_MUTED);
            FontMetrics fm = g2.getFontMetrics();
            int strW = fm.stringWidth(tp.getLabel());
            g2.drawString(tp.getLabel(), slotCenterX - (strW / 2), padTop + plotH + 16);
        }

        g2.dispose();
    }
}
