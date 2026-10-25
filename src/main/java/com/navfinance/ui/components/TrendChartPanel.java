package com.navfinance.ui.components;

import com.navfinance.config.AppConfig;
import com.navfinance.model.DashboardSummary;
import com.navfinance.ui.MainFrame;
import com.navfinance.ui.util.UiUtils;
import com.navfinance.ui.util.ZoomManager;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Highly interactive, anti-aliased 2D Bar Chart component plotting temporal cash flow
 * comparisons between cumulative Income inflows and Expense outflows.
 * <p>
 * Key Interactive Features:
 * <ul>
 *   <li><b>Hover Tracking &amp; Focus Spotlight:</b> Translucent column highlight backdrop with rounded borders
 *       elevating the active time slot and focusing attention.</li>
 *   <li><b>Bar Sheen &amp; Elevation:</b> Vibrant emerald and rose gradients, glow borders, and hover micro-expansion.</li>
 *   <li><b>Dynamic Numerical Badges:</b> Formatted currency badges directly above hovered bars for instant at-a-glance figures.</li>
 *   <li><b>Interactive Legend:</b> Hover highlights and click-to-isolate series filters for Income or Expense.</li>
 *   <li><b>Floating Glassmorphic Tooltip:</b> Dark translucent card showing exact currency totals, net savings/deficit, and % rate.</li>
 *   <li><b>Click Drilldown:</b> Clicking any slot navigates directly to the ledger filtered by that date range.</li>
 * </ul>
 * </p>
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public class TrendChartPanel extends JPanel {

    /**
     * Listener callback invoked when the user clicks on a chart slot/bar pair.
     */
    @FunctionalInterface
    public interface SlotClickListener {
        /**
         * Invoked when a time slot is clicked.
         *
         * @param point corresponding trend point
         * @param index index in points list
         */
        void onSlotClicked(DashboardSummary.TrendPoint point, int index);
    }

    /** Sequential list of data points representing time increments. */
    private List<DashboardSummary.TrendPoint> points = new ArrayList<>();

    /** Headline title displayed at the top-left of the chart. */
    private String title;

    /** Optional custom slot click listener. */
    private SlotClickListener slotClickListener;

    /** Actively hovered slot index (-1 if none). */
    private int hoveredSlot = -1;

    /** Actively hovered bar within slot: 0 = column, 1 = Income bar, 2 = Expense bar. */
    private int hoveredBarType = 0;

    /** Actively hovered legend pill: -1 = none, 1 = Income legend, 2 = Expense legend. */
    private int hoveredLegend = -1;

    /** Isolated series toggle: 0 = both, 1 = Income only, 2 = Expense only. */
    private int isolatedSeries = 0;

    /** Current mouse coordinates for floating tooltip and hit testing. */
    private Point mousePoint = null;

    /** Geometry cache for interactive legend buttons. */
    private final Rectangle inLegendBounds = new Rectangle();
    private final Rectangle exLegendBounds = new Rectangle();

    /** Geometry cache for plot boundaries. */
    private int cachedPadLeft = 0;
    private int cachedPadTop = 0;
    private int cachedPlotW = 0;
    private int cachedPlotH = 0;
    private int cachedSlotW = 0;

    /**
     * Constructs an interactive {@link TrendChartPanel} with the specified headline label.
     *
     * @param title headline label
     */
    public TrendChartPanel(String title) {
        this.title = title != null ? title : "";
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(540, 320));
        setToolTipText(""); // Enable tooltips without Swing default popup

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleMouseMove(e.getPoint());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (hoveredSlot != -1 || hoveredLegend != -1 || mousePoint != null) {
                    hoveredSlot = -1;
                    hoveredBarType = 0;
                    hoveredLegend = -1;
                    mousePoint = null;
                    setCursor(Cursor.getDefaultCursor());
                    repaint();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClick(e.getPoint());
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        ZoomManager.addZoomListener(this::repaint);
    }

    /**
     * Registers a custom click listener for bar slots.
     *
     * @param listener callback listener
     */
    public void setSlotClickListener(SlotClickListener listener) {
        this.slotClickListener = listener;
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
        this.hoveredSlot = -1;
        this.mousePoint = null;
        repaint();
    }

    /**
     * Evaluates cursor position to detect hovered legend pills or plot column slots.
     */
    private void handleMouseMove(Point p) {
        this.mousePoint = p;

        // 1. Check Legend interaction
        if (inLegendBounds.contains(p)) {
            if (hoveredLegend != 1 || hoveredSlot != -1) {
                hoveredLegend = 1;
                hoveredSlot = -1;
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                repaint();
            }
            return;
        } else if (exLegendBounds.contains(p)) {
            if (hoveredLegend != 2 || hoveredSlot != -1) {
                hoveredLegend = 2;
                hoveredSlot = -1;
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                repaint();
            }
            return;
        } else {
            hoveredLegend = -1;
        }

        // 2. Check Plot Slots
        if (points.isEmpty()) {
            clearHover();
            return;
        }

        int w = getWidth() > 0 ? getWidth() : 540;
        int h = getHeight() > 0 ? getHeight() : 320;
        float scale = ZoomManager.getCurrentScale();

        int padLeft = cachedPlotW > 20 ? cachedPadLeft : Math.max(50, Math.round(54 * scale));
        int padRight = 20;
        int padTop = cachedPlotH > 20 ? cachedPadTop : Math.max(46, Math.round(50 * scale));
        int plotW = cachedPlotW > 20 ? cachedPlotW : Math.max(10, w - padLeft - padRight);
        int plotH = cachedPlotH > 20 ? cachedPlotH : Math.max(10, h - padTop - Math.max(34, Math.round(38 * scale)));
        int slotW = cachedSlotW > 0 ? cachedSlotW : plotW / Math.max(points.size(), 1);

        int plotRight = padLeft + plotW;
        int plotBottom = padTop + plotH + 28;

        if (p.x >= padLeft && p.x <= plotRight && p.y >= padTop - 10 && p.y <= plotBottom && slotW > 0) {
            int slotIdx = (p.x - padLeft) / slotW;
            if (slotIdx >= 0 && slotIdx < points.size()) {
                hoveredSlot = slotIdx;

                // Hit-test specific bar
                int slotCenterX = padLeft + slotIdx * slotW + (slotW / 2);
                int barW = Math.max(4, Math.min(24, (slotW - 14) / 2));
                int inX = slotCenterX - barW - 2;
                int exX = slotCenterX + 2;

                if (p.x >= inX && p.x <= inX + barW) {
                    hoveredBarType = 1;
                } else if (p.x >= exX && p.x <= exX + barW) {
                    hoveredBarType = 2;
                } else {
                    hoveredBarType = 0;
                }

                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                repaint();
                return;
            }
        }

        clearHover();
    }

    private void clearHover() {
        if (hoveredSlot != -1 || hoveredBarType != 0) {
            hoveredSlot = -1;
            hoveredBarType = 0;
            setCursor(Cursor.getDefaultCursor());
            repaint();
        } else if (mousePoint != null) {
            repaint();
        }
    }

    /**
     * Dispatches user clicks on legend pills (for isolate toggling) or chart slots (for drilldowns).
     */
    private void handleMouseClick(Point p) {
        if (inLegendBounds.contains(p)) {
            isolatedSeries = (isolatedSeries == 1) ? 0 : 1;
            repaint();
            return;
        }

        if (exLegendBounds.contains(p)) {
            isolatedSeries = (isolatedSeries == 2) ? 0 : 2;
            repaint();
            return;
        }

        int targetSlot = hoveredSlot;
        if (targetSlot == -1 && !points.isEmpty()) {
            int w = getWidth() > 0 ? getWidth() : 540;
            float scale = ZoomManager.getCurrentScale();
            int padLeft = cachedPlotW > 20 ? cachedPadLeft : Math.max(50, Math.round(54 * scale));
            int padRight = 20;
            int plotW = cachedPlotW > 20 ? cachedPlotW : Math.max(10, w - padLeft - padRight);
            int slotW = cachedSlotW > 0 ? cachedSlotW : plotW / Math.max(points.size(), 1);
            if (p.x >= padLeft && p.x <= padLeft + plotW && slotW > 0) {
                int calcIdx = (p.x - padLeft) / slotW;
                if (calcIdx >= 0 && calcIdx < points.size()) {
                    targetSlot = calcIdx;
                }
            }
        }

        if (targetSlot >= 0 && targetSlot < points.size()) {
            DashboardSummary.TrendPoint tp = points.get(targetSlot);
            if (slotClickListener != null) {
                slotClickListener.onSlotClicked(tp, targetSlot);
            } else {
                MainFrame mainFrame = MainFrame.getInstance();
                if (mainFrame != null) {
                    mainFrame.switchToTransactionsWithDateRange(tp.getStartDate(), tp.getEndDate());
                }
            }
        }
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

        // 1. Container background with subtle rounded border
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

        String inLabel = isolatedSeries == 1 ? "Income (Isolated) ✓" : "Income (In)";
        String exLabel = isolatedSeries == 2 ? "Expense (Isolated) ✓" : "Expense (Out)";
        int inWidth = fmLeg.stringWidth(inLabel);
        int exWidth = fmLeg.stringWidth(exLabel);
        int boxSize = Math.max(10, Math.round(11 * scale));
        int boxGap = 6;
        int itemPad = 8;
        int inPillW = boxSize + boxGap + inWidth + (itemPad * 2);
        int exPillW = boxSize + boxGap + exWidth + (itemPad * 2);
        int totalLegWidth = inPillW + 8 + exPillW;

        boolean twoRowHeader = (18 + titleWidth + 20 + totalLegWidth) > (w - 20);

        // Draw Title
        int titleY = Math.max(22, Math.round(24 * scale));
        g2.setFont(titleFont);
        g2.setColor(new Color(30, 41, 59));
        g2.drawString(title, 18, titleY);

        // Draw interactive legend pills
        int legendX;
        int legendY;
        if (twoRowHeader) {
            legendX = 18;
            legendY = titleY + fmTitle.getHeight() + 4;
        } else {
            legendX = Math.max(18 + titleWidth + 16, w - totalLegWidth - 18);
            legendY = titleY - 14;
        }

        int pillH = Math.max(22, Math.round(24 * scale));

        // Income Legend Pill
        inLegendBounds.setBounds(legendX, legendY, inPillW, pillH);
        boolean inHover = (hoveredLegend == 1);
        g2.setColor(isolatedSeries == 1 ? new Color(209, 250, 229) : (inHover ? new Color(241, 245, 249) : new Color(248, 250, 252)));
        g2.fillRoundRect(inLegendBounds.x, inLegendBounds.y, inLegendBounds.width, inLegendBounds.height, 8, 8);
        g2.setColor(isolatedSeries == 1 ? new Color(16, 185, 129) : (inHover ? new Color(203, 213, 225) : new Color(226, 232, 240)));
        g2.drawRoundRect(inLegendBounds.x, inLegendBounds.y, inLegendBounds.width, inLegendBounds.height, 8, 8);

        g2.setColor(UiUtils.COLOR_INCOME);
        g2.fillRoundRect(inLegendBounds.x + itemPad, inLegendBounds.y + (pillH - boxSize) / 2, boxSize, boxSize, 3, 3);
        g2.setFont(legFont);
        g2.setColor(isolatedSeries == 1 ? new Color(4, 120, 87) : UiUtils.COLOR_TEXT_PRIMARY);
        g2.drawString(inLabel, inLegendBounds.x + itemPad + boxSize + boxGap, inLegendBounds.y + (pillH + fmLeg.getAscent() - fmLeg.getDescent()) / 2);

        // Expense Legend Pill
        int exLegX = legendX + inPillW + 8;
        exLegendBounds.setBounds(exLegX, legendY, exPillW, pillH);
        boolean exHover = (hoveredLegend == 2);
        g2.setColor(isolatedSeries == 2 ? new Color(254, 226, 226) : (exHover ? new Color(241, 245, 249) : new Color(248, 250, 252)));
        g2.fillRoundRect(exLegendBounds.x, exLegendBounds.y, exLegendBounds.width, exLegendBounds.height, 8, 8);
        g2.setColor(isolatedSeries == 2 ? new Color(239, 68, 68) : (exHover ? new Color(203, 213, 225) : new Color(226, 232, 240)));
        g2.drawRoundRect(exLegendBounds.x, exLegendBounds.y, exLegendBounds.width, exLegendBounds.height, 8, 8);

        g2.setColor(UiUtils.COLOR_EXPENSE);
        g2.fillRoundRect(exLegendBounds.x + itemPad, exLegendBounds.y + (pillH - boxSize) / 2, boxSize, boxSize, 3, 3);
        g2.setFont(legFont);
        g2.setColor(isolatedSeries == 2 ? new Color(185, 28, 28) : UiUtils.COLOR_TEXT_PRIMARY);
        g2.drawString(exLabel, exLegendBounds.x + itemPad + boxSize + boxGap, exLegendBounds.y + (pillH + fmLeg.getAscent() - fmLeg.getDescent()) / 2);

        if (points.isEmpty()) {
            g2.setFont(UiUtils.regularFont());
            g2.setColor(UiUtils.COLOR_TEXT_MUTED);
            g2.drawString("No transaction data available for this period.", 24, h / 2);
            g2.dispose();
            return;
        }

        // 3. Plot Dimensions and Margins
        int padLeft = Math.max(50, Math.round(54 * scale));
        int padRight = 20;
        int padTop = twoRowHeader ? Math.max(66, Math.round(72 * scale)) : Math.max(46, Math.round(50 * scale));
        int padBottom = Math.max(34, Math.round(38 * scale));
        int plotW = Math.max(10, w - padLeft - padRight);
        int plotH = Math.max(10, h - padTop - padBottom);

        cachedPadLeft = padLeft;
        cachedPadTop = padTop;
        cachedPlotW = plotW;
        cachedPlotH = plotH;

        int count = points.size();
        int slotW = plotW / Math.max(count, 1);
        cachedSlotW = slotW;

        // 4. Calculate dynamic upper ceiling for vertical scaling
        double maxVal = 1000.0;
        for (DashboardSummary.TrendPoint tp : points) {
            if (isolatedSeries != 2 && tp.getIncome() > maxVal) maxVal = tp.getIncome();
            if (isolatedSeries != 1 && tp.getExpense() > maxVal) maxVal = tp.getExpense();
        }
        maxVal = Math.ceil(maxVal * 1.18); // Add 18% headroom for dynamic numerical badges

        // 5. Draw horizontal guideline grid steps and y-axis metric markers
        g2.setColor(new Color(241, 245, 249));
        g2.setStroke(new BasicStroke(1.0f));
        int gridSteps = 4;
        for (int i = 0; i <= gridSteps; i++) {
            int y = padTop + (int) (plotH * (1.0 - (double) i / gridSteps));
            g2.drawLine(padLeft, y, w - padRight, y);

            double v = (maxVal * i) / gridSteps;
            String vLabel = formatMetricLabel(v);
            g2.setFont(UiUtils.regularFont().deriveFont(Math.max(9f, 10f * scale)));
            g2.setColor(new Color(148, 163, 184));
            FontMetrics fmV = g2.getFontMetrics();
            g2.drawString(vLabel, padLeft - fmV.stringWidth(vLabel) - 6, y + 4);
        }

        // Baseline (y=0) line
        g2.setColor(new Color(226, 232, 240));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawLine(padLeft, padTop + plotH, w - padRight, padTop + plotH);

        // 6. Draw Spotlight Column Highlight behind hovered slot
        if (hoveredSlot >= 0 && hoveredSlot < count) {
            int slotX = padLeft + hoveredSlot * slotW;
            g2.setColor(new Color(241, 245, 249, 180));
            g2.fillRoundRect(slotX + 2, padTop - 4, slotW - 4, plotH + 6, 8, 8);
            g2.setColor(new Color(203, 213, 225, 220));
            g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{4, 4}, 0));
            g2.drawRoundRect(slotX + 2, padTop - 4, slotW - 4, plotH + 6, 8, 8);
        }

        // 7. Draw side-by-side comparative bars for each time slot
        int barW = Math.max(4, Math.min(22, (slotW - 14) / 2));
        Font xFont = UiUtils.regularFont().deriveFont(Math.max(9f, 10f * scale));
        Font xBoldFont = UiUtils.boldFont().deriveFont(Math.max(9f, 10f * scale));
        Font badgeFont = UiUtils.boldFont().deriveFont(Math.max(8.5f, 9.5f * scale));
        FontMetrics fmX = g2.getFontMetrics(xFont);
        FontMetrics fmBadge = g2.getFontMetrics(badgeFont);

        // Calculate label skip interval to prevent horizontal label collision
        int approxStrW = count > 0 ? fmX.stringWidth(points.get(0).getLabel()) : 30;
        int labelSkip = 1;
        if (slotW < approxStrW + 6) {
            labelSkip = (int) Math.ceil((double) (approxStrW + 8) / Math.max(1, slotW));
        }

        for (int i = 0; i < count; i++) {
            DashboardSummary.TrendPoint tp = points.get(i);
            int slotCenterX = padLeft + i * slotW + (slotW / 2);
            boolean isSlotHovered = (i == hoveredSlot);

            int inH = (int) ((tp.getIncome() / maxVal) * plotH);
            int exH = (int) ((tp.getExpense() / maxVal) * plotH);

            int inX = slotCenterX - barW - 2;
            int inY = padTop + plotH - inH;

            int exX = slotCenterX + 2;
            int exY = padTop + plotH - exH;

            // Inflow Bar (Green Gradient)
            if (inH > 0 && isolatedSeries != 2) {
                boolean inHoverFocus = isSlotHovered && (hoveredBarType == 1 || hoveredBarType == 0);
                boolean dimmed = (hoveredSlot != -1 && !isSlotHovered) || isolatedSeries == 2;

                Color cTop = dimmed ? new Color(134, 239, 172, 140) : (inHoverFocus ? new Color(34, 197, 94) : new Color(16, 185, 129));
                Color cBot = dimmed ? new Color(187, 247, 208, 120) : (inHoverFocus ? new Color(16, 185, 129) : new Color(5, 150, 105));

                g2.setPaint(new GradientPaint(inX, inY, cTop, inX, inY + inH, cBot));
                int roundCap = Math.min(6, barW);
                g2.fillRoundRect(inX, inY, barW, inH, roundCap, roundCap);

                if (inHoverFocus) {
                    g2.setColor(new Color(4, 120, 87));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(inX, inY, barW, inH, roundCap, roundCap);

                    // Dynamic numerical badge above bar
                    String badgeStr = formatMetricLabel(tp.getIncome());
                    int badgeW = fmBadge.stringWidth(badgeStr) + 6;
                    int badgeH = fmBadge.getHeight();
                    int badgeX = inX + (barW - badgeW) / 2;
                    int badgeY = Math.max(padTop - 2, inY - badgeH - 2);

                    g2.setColor(new Color(236, 253, 245));
                    g2.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 4, 4);
                    g2.setColor(new Color(16, 185, 129));
                    g2.drawRoundRect(badgeX, badgeY, badgeW, badgeH, 4, 4);
                    g2.setFont(badgeFont);
                    g2.setColor(new Color(4, 120, 87));
                    g2.drawString(badgeStr, badgeX + 3, badgeY + fmBadge.getAscent());
                }
            }

            // Outflow Bar (Red Gradient)
            if (exH > 0 && isolatedSeries != 1) {
                boolean exHoverFocus = isSlotHovered && (hoveredBarType == 2 || hoveredBarType == 0);
                boolean dimmed = (hoveredSlot != -1 && !isSlotHovered) || isolatedSeries == 1;

                Color cTop = dimmed ? new Color(254, 202, 202, 140) : (exHoverFocus ? new Color(244, 63, 94) : new Color(239, 68, 68));
                Color cBot = dimmed ? new Color(254, 226, 226, 120) : (exHoverFocus ? new Color(225, 29, 72) : new Color(190, 18, 60));

                g2.setPaint(new GradientPaint(exX, exY, cTop, exX, exY + exH, cBot));
                int roundCap = Math.min(6, barW);
                g2.fillRoundRect(exX, exY, barW, exH, roundCap, roundCap);

                if (exHoverFocus) {
                    g2.setColor(new Color(159, 18, 57));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(exX, exY, barW, exH, roundCap, roundCap);

                    // Dynamic numerical badge above bar
                    String badgeStr = formatMetricLabel(tp.getExpense());
                    int badgeW = fmBadge.stringWidth(badgeStr) + 6;
                    int badgeH = fmBadge.getHeight();
                    int badgeX = exX + (barW - badgeW) / 2;
                    int badgeY = Math.max(padTop - 2, exY - badgeH - 2);

                    g2.setColor(new Color(254, 242, 242));
                    g2.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 4, 4);
                    g2.setColor(new Color(239, 68, 68));
                    g2.drawRoundRect(badgeX, badgeY, badgeW, badgeH, 4, 4);
                    g2.setFont(badgeFont);
                    g2.setColor(new Color(159, 18, 57));
                    g2.drawString(badgeStr, badgeX + 3, badgeY + fmBadge.getAscent());
                }
            }

            // X-Axis Centered Date/Label
            if (isSlotHovered) {
                // Highlighted active label pill
                g2.setFont(xBoldFont);
                FontMetrics fmXBold = g2.getFontMetrics(xBoldFont);
                int strW = fmXBold.stringWidth(tp.getLabel());
                int pillWidth = strW + 10;
                int pillHeight = fmXBold.getHeight() + 2;
                int pillX = slotCenterX - (pillWidth / 2);
                int pillY = padTop + plotH + Math.round(5 * scale);

                g2.setColor(UiUtils.COLOR_PRIMARY);
                g2.fillRoundRect(pillX, pillY, pillWidth, pillHeight, 6, 6);
                g2.setColor(Color.WHITE);
                g2.drawString(tp.getLabel(), pillX + 5, pillY + fmXBold.getAscent());

            } else if (i % labelSkip == 0 || i == count - 1) {
                g2.setFont(xFont);
                g2.setColor(UiUtils.COLOR_TEXT_MUTED);
                int strW = fmX.stringWidth(tp.getLabel());
                g2.drawString(tp.getLabel(), slotCenterX - (strW / 2), padTop + plotH + Math.round(15 * scale));
            }
        }

        // 8. Render Floating Glassmorphic Tooltip Card
        if (hoveredSlot >= 0 && hoveredSlot < count && mousePoint != null) {
            renderFloatingTooltip(g2, scale, w, h, points.get(hoveredSlot));
        }

        g2.dispose();
    }

    /**
     * Renders a floating glassmorphic tooltip card near cursor displaying exact amounts and net performance.
     */
    private void renderFloatingTooltip(Graphics2D g2, float scale, int totalW, int totalH, DashboardSummary.TrendPoint tp) {
        Font titleFont = UiUtils.boldFont().deriveFont(Math.max(11f, 11.5f * scale));
        Font rowFont = UiUtils.regularFont().deriveFont(Math.max(10f, 10.5f * scale));
        Font boldRowFont = UiUtils.boldFont().deriveFont(Math.max(10f, 10.5f * scale));
        Font hintFont = UiUtils.regularFont().deriveFont(Math.max(9f, 9.5f * scale));

        FontMetrics fmTitle = g2.getFontMetrics(titleFont);
        FontMetrics fmRow = g2.getFontMetrics(rowFont);
        FontMetrics fmBoldRow = g2.getFontMetrics(boldRowFont);
        FontMetrics fmHint = g2.getFontMetrics(hintFont);

        String titleStr = "🗓️ " + tp.getFullTitle();
        String inStr = "● Income:   +" + AppConfig.formatCurrency(tp.getIncome());
        String exStr = "● Expense:  -" + AppConfig.formatCurrency(tp.getExpense());

        double net = tp.getIncome() - tp.getExpense();
        String netStr;
        if (net >= 0) {
            double rate = tp.getIncome() > 0 ? (net / tp.getIncome()) * 100.0 : 0.0;
            netStr = "● Net Surplus: +" + AppConfig.formatCurrency(net) + String.format(" (%.1f%% saved)", rate);
        } else {
            netStr = "● Net Deficit: -" + AppConfig.formatCurrency(Math.abs(net)) + " (Overspent)";
        }

        String hintStr = "⚡ Click to view itemized transactions ↗";

        int maxTextW = Math.max(fmTitle.stringWidth(titleStr),
                Math.max(fmBoldRow.stringWidth(inStr),
                        Math.max(fmBoldRow.stringWidth(exStr),
                                Math.max(fmBoldRow.stringWidth(netStr), fmHint.stringWidth(hintStr)))));

        int cardW = maxTextW + 28;
        int cardH = fmTitle.getHeight() + fmRow.getHeight() + fmRow.getHeight() + fmRow.getHeight() + fmHint.getHeight() + 26;

        int cardX = mousePoint.x + 14;
        int cardY = mousePoint.y + 14;

        // Clamp inside panel boundaries
        if (cardX + cardW > totalW - 12) cardX = mousePoint.x - cardW - 10;
        if (cardY + cardH > totalH - 12) cardY = mousePoint.y - cardH - 10;
        if (cardX < 12) cardX = 12;
        if (cardY < 12) cardY = 12;

        // Dark glassmorphic card container
        g2.setColor(new Color(15, 23, 42, 245));
        g2.fill(new RoundRectangle2D.Double(cardX, cardY, cardW, cardH, 10, 10));
        g2.setColor(new Color(51, 65, 85, 220));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(new RoundRectangle2D.Double(cardX, cardY, cardW, cardH, 10, 10));

        // Header Title
        int curY = cardY + fmTitle.getAscent() + 8;
        g2.setColor(Color.WHITE);
        g2.setFont(titleFont);
        g2.drawString(titleStr, cardX + 12, curY);

        // Divider
        curY += 6;
        g2.setColor(new Color(51, 65, 85));
        g2.drawLine(cardX + 10, curY, cardX + cardW - 10, curY);

        // Income Row
        curY += fmBoldRow.getHeight() + 2;
        g2.setFont(boldRowFont);
        g2.setColor(new Color(52, 211, 153)); // Soft emerald green
        g2.drawString(inStr, cardX + 12, curY);

        // Expense Row
        curY += fmBoldRow.getHeight() + 1;
        g2.setColor(new Color(248, 113, 113)); // Soft red
        g2.drawString(exStr, cardX + 12, curY);

        // Net Balance Row
        curY += fmBoldRow.getHeight() + 1;
        if (net >= 0) {
            g2.setColor(new Color(45, 212, 191)); // Soft teal
        } else {
            g2.setColor(new Color(251, 146, 60)); // Soft amber
        }
        g2.drawString(netStr, cardX + 12, curY);

        // Action drilldown hint
        curY += fmHint.getHeight() + 3;
        g2.setFont(hintFont);
        g2.setColor(new Color(147, 197, 253)); // Soft cyan blue
        g2.drawString(hintStr, cardX + 12, curY);
    }

    /**
     * Formats metric figures compactly (e.g. 500, 12k, 1.5M).
     */
    private String formatMetricLabel(double val) {
        if (val >= 1_000_000) {
            return String.format("%.1fM", val / 1_000_000.0);
        } else if (val >= 1_000) {
            return String.format("%.0fk", val / 1000.0);
        } else {
            return String.format("%.0f", val);
        }
    }
}
