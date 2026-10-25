package com.navfinance.ui.components;

import com.navfinance.config.AppConfig;
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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * High-performance, anti-aliased Donut &amp; Pie Chart component illustrating expense distribution.
 * <p>
 * Key Features:
 * <ul>
 *   <li><b>Larger Dynamic Geometry:</b> Scalable donut diameter (up to 270px) and generous breathing room.</li>
 *   <li><b>Interactive Slices &amp; Radial Pop:</b> Hovering any slice triggers an outward radial explosion
 *       with dimmed non-hovered slices for dramatic focus.</li>
 *   <li><b>Dynamic Hero Center Cutout:</b> Displays cumulative total when idle, and immediately updates
 *       with hovered category title, exact currency amount, and percentage share.</li>
 *   <li><b>Interactive Legend:</b> Synchronized row hover, color pills, formatted amounts, and percentage badges.</li>
 *   <li><b>Floating Tooltip:</b> Dark glassmorphism card near cursor with detailed breakdown.</li>
 *   <li><b>Click-Through Navigation:</b> Clicking any slice or legend item instantly navigates to the
 *       <b>Track &amp; Analyze</b> dashboard for that category.</li>
 * </ul>
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

    /** Expanded modern palette of 12 vibrant harmonious colors for slices. */
    private static final Color[] SLICE_COLORS = {
            new Color(59, 130, 246),   // Blue
            new Color(16, 185, 129),   // Emerald
            new Color(245, 158, 11),   // Amber
            new Color(239, 68, 68),    // Red
            new Color(139, 92, 246),   // Purple
            new Color(236, 72, 153),   // Pink
            new Color(20, 184, 166),   // Teal
            new Color(249, 115, 22),   // Orange
            new Color(99, 102, 241),   // Indigo
            new Color(14, 165, 233),   // Sky
            new Color(168, 85, 247),   // Violet
            new Color(100, 116, 139)   // Slate
    };

    /** Processed display items (top categories + optional consolidated 'Others'). */
    private static class SliceItem {
        final String category;
        final double amount;
        final double percentage;
        final Color color;
        final boolean isOther;
        final List<String> otherDetails;

        SliceItem(String category, double amount, double percentage, Color color, boolean isOther, List<String> otherDetails) {
            this.category = category;
            this.amount = amount;
            this.percentage = percentage;
            this.color = color;
            this.isOther = isOther;
            this.otherDetails = otherDetails;
        }
    }

    private final List<SliceItem> displayItems = new ArrayList<>();
    private final List<Rectangle> legendItemBounds = new ArrayList<>();
    private final Rectangle headerBadgeBounds = new Rectangle();
    private final Rectangle expenseToggleBounds = new Rectangle();
    private final Rectangle incomeToggleBounds = new Rectangle();
    private Map<String, Double> expenseData = new LinkedHashMap<>();
    private Map<String, Double> incomeData = new LinkedHashMap<>();
    private boolean showingIncome = false;

    /** Index of the actively hovered slice or legend item (-1 if none). */
    private int hoveredIndex = -1;

    /** Current mouse coordinates for floating tooltip placement. */
    private Point mousePoint = null;

    /** Geometry cache for donut center and radii. */
    private int cachedCenterX = 0;
    private int cachedCenterY = 0;
    private int cachedDonutDiameter = 0;
    private int cachedHoleDiameter = 0;

    /**
     * Constructs a new {@link CategoryPieChartPanel} with interactive listeners and zoom adaptation.
     *
     * @param title headline label
     */
    public CategoryPieChartPanel(String title) {
        this.title = title;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(500, 340));
        setToolTipText(""); // Enable tooltips without Swing default popup

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleMouseMove(e.getPoint());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (hoveredIndex != -1 || mousePoint != null) {
                    hoveredIndex = -1;
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
     * Updates the chart dataset with expense values (default mode).
     *
     * @param rawData map of category names to aggregated spending values
     */
    public void setData(Map<String, Double> rawData) {
        this.expenseData = rawData != null ? rawData : new LinkedHashMap<>();
        if (!showingIncome) {
            this.data = this.expenseData;
            rebuildDisplayItems();
        }
        hoveredIndex = -1;
        repaint();
    }

    /**
     * Supplies both expense and income datasets, enabling interactive toggle switching.
     *
     * @param expenses category map of expenses
     * @param incomes category map of incomes
     */
    public void setDatasets(Map<String, Double> expenses, Map<String, Double> incomes) {
        this.expenseData = expenses != null ? expenses : new LinkedHashMap<>();
        this.incomeData = incomes != null ? incomes : new LinkedHashMap<>();
        this.data = showingIncome ? this.incomeData : this.expenseData;
        rebuildDisplayItems();
        hoveredIndex = -1;
        repaint();
    }

    /**
     * Consolidates raw data entries: displays top categories individually and groups
     * smaller minor categories into a consolidated 'Others' entry if list exceeds 8 items.
     */
    private void rebuildDisplayItems() {
        displayItems.clear();
        if (data == null || data.isEmpty()) return;

        double total = 0.0;
        for (double val : data.values()) {
            if (val > 0) total += val;
        }
        if (total <= 0) return;

        // Sort categories descending by amount, with alphabetical tie-breaker for deterministic layout
        List<Map.Entry<String, Double>> sorted = new ArrayList<>(data.entrySet());
        sorted.sort((a, b) -> {
            int cmp = Double.compare(b.getValue(), a.getValue());
            return cmp != 0 ? cmp : a.getKey().compareToIgnoreCase(b.getKey());
        });

        int maxIndividual = 8;
        int colorIdx = 0;

        if (sorted.size() <= maxIndividual) {
            for (Map.Entry<String, Double> entry : sorted) {
                double amt = entry.getValue();
                if (amt <= 0) continue;
                double pct = (amt / total) * 100.0;
                Color c = SLICE_COLORS[colorIdx % SLICE_COLORS.length];
                displayItems.add(new SliceItem(entry.getKey(), amt, pct, c, false, null));
                colorIdx++;
            }
        } else {
            // Keep top (maxIndividual - 1), aggregate the rest into Others
            int keep = maxIndividual - 1;
            for (int i = 0; i < keep; i++) {
                Map.Entry<String, Double> entry = sorted.get(i);
                double amt = entry.getValue();
                double pct = (amt / total) * 100.0;
                Color c = SLICE_COLORS[colorIdx % SLICE_COLORS.length];
                displayItems.add(new SliceItem(entry.getKey(), amt, pct, c, false, null));
                colorIdx++;
            }

            double othersAmt = 0.0;
            List<String> details = new ArrayList<>();
            for (int i = keep; i < sorted.size(); i++) {
                Map.Entry<String, Double> entry = sorted.get(i);
                othersAmt += entry.getValue();
                details.add(String.format("%s: %s", entry.getKey(), AppConfig.formatCurrency(entry.getValue())));
            }
            if (othersAmt > 0) {
                double pct = (othersAmt / total) * 100.0;
                Color c = new Color(148, 163, 184); // Muted slate gray for Others
                displayItems.add(new SliceItem("Others (" + (sorted.size() - keep) + ")", othersAmt, pct, c, true, details));
            }
        }
    }

    /**
     * Hit-tests mouse coordinates against the donut slices and legend rows.
     */
    private void handleMouseMove(Point p) {
        mousePoint = p;
        int newHovered = -1;

        // 1. Check Header Badge & Toggles
        if (headerBadgeBounds.contains(p) || expenseToggleBounds.contains(p) || incomeToggleBounds.contains(p)) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            if (hoveredIndex != -1) {
                hoveredIndex = -1;
                repaint();
            }
            return;
        }

        // 2. Check Legend rows
        for (int i = 0; i < legendItemBounds.size(); i++) {
            if (i < displayItems.size() && legendItemBounds.get(i).contains(p)) {
                newHovered = i;
                break;
            }
        }

        // 3. Check Donut Ring slices
        if (newHovered == -1 && cachedDonutDiameter > 0) {
            double dx = p.x - cachedCenterX;
            double dy = p.y - cachedCenterY;
            double dist = Math.hypot(dx, dy);

            double rInner = cachedHoleDiameter / 2.0;
            double rOuter = cachedDonutDiameter / 2.0;

            if (dist >= rInner - 2 && dist <= rOuter + 14) {
                // Screen coordinate angle: 90 is 12 o'clock, progressing clockwise
                double cartesianAngle = Math.toDegrees(Math.atan2(-dy, dx));
                double clockwiseFromTop = 90.0 - cartesianAngle;
                while (clockwiseFromTop < 0) clockwiseFromTop += 360.0;
                while (clockwiseFromTop >= 360.0) clockwiseFromTop -= 360.0;

                double accum = 0.0;
                for (int i = 0; i < displayItems.size(); i++) {
                    double sliceAngle = (displayItems.get(i).percentage / 100.0) * 360.0;
                    if (clockwiseFromTop >= accum && clockwiseFromTop < accum + sliceAngle) {
                        newHovered = i;
                        break;
                    }
                    accum += sliceAngle;
                }
            }
        }

        if (newHovered != hoveredIndex) {
            hoveredIndex = newHovered;
            setCursor(hoveredIndex != -1
                    ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    : Cursor.getDefaultCursor());
            repaint();
        } else if (hoveredIndex != -1) {
            repaint(); // Repaint to move floating tooltip with mouse
        }
    }

    /**
     * Handles mouse clicks on slices, legend items, or the header badge,
     * triggering seamless navigation to the Entity Tracker.
     */
    private void handleMouseClick(Point p) {
        MainFrame mainFrame = MainFrame.getInstance();
        if (mainFrame == null) return;

        if (expenseToggleBounds.contains(p)) {
            if (showingIncome) {
                showingIncome = false;
                this.data = expenseData;
                rebuildDisplayItems();
                hoveredIndex = -1;
                repaint();
            }
            return;
        }

        if (incomeToggleBounds.contains(p)) {
            if (!showingIncome) {
                showingIncome = true;
                this.data = incomeData;
                rebuildDisplayItems();
                hoveredIndex = -1;
                repaint();
            }
            return;
        }

        if (headerBadgeBounds.contains(p)) {
            mainFrame.switchToEntityTracker("CATEGORY", null, "", "MONTHS");
            return;
        }

        if (hoveredIndex >= 0 && hoveredIndex < displayItems.size()) {
            SliceItem item = displayItems.get(hoveredIndex);
            if (item.isOther) {
                // Navigate to category tracker without specific single category filter
                mainFrame.switchToEntityTracker("CATEGORY", null, "", "MONTHS");
            } else {
                mainFrame.switchToEntityTracker("CATEGORY", null, item.category, "MONTHS");
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        int w = getWidth();
        int h = getHeight();
        float scale = ZoomManager.getCurrentScale();

        // 1. Modern Card Container with subtle shadow border
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
        g2.setColor(UiUtils.COLOR_BORDER);
        g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);

        // 2. Title Header with Icon and Interactive Tracker Badge
        int titleY = Math.max(22, Math.round(24 * scale));
        g2.setFont(UiUtils.boldFont().deriveFont(Math.max(13f, 13.5f * scale)));
        g2.setColor(new Color(15, 23, 42));
        g2.drawString(title, 20, titleY);

        // Header interactive badges: "Outflow 🔻", "Inflow 🔺", "Analyze 📊"
        String badgeText = "Analyze 📊";
        Font badgeFont = UiUtils.boldFont().deriveFont(Math.max(9.5f, 10f * scale));
        g2.setFont(badgeFont);
        FontMetrics fmBadge = g2.getFontMetrics(badgeFont);
        int badgeW = fmBadge.stringWidth(badgeText) + 14;
        int badgeH = Math.max(20, fmBadge.getHeight() + 4);
        int badgeX = w - badgeW - 16;
        int badgeY = titleY - badgeH + 4;
        headerBadgeBounds.setBounds(badgeX, badgeY, badgeW, badgeH);

        boolean badgeHovered = mousePoint != null && headerBadgeBounds.contains(mousePoint);
        g2.setColor(badgeHovered ? new Color(238, 242, 255) : new Color(248, 250, 252));
        g2.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 8, 8);
        g2.setColor(badgeHovered ? UiUtils.COLOR_PRIMARY : UiUtils.COLOR_BORDER);
        g2.drawRoundRect(badgeX, badgeY, badgeW, badgeH, 8, 8);
        g2.setColor(badgeHovered ? UiUtils.COLOR_PRIMARY : UiUtils.COLOR_TEXT_MUTED);
        g2.drawString(badgeText, badgeX + 7, badgeY + fmBadge.getAscent() + 2);

        // Toggle Buttons: Outflow & Inflow
        String exText = "Expenses 🔻";
        String inText = "Income 🔺";
        int inBtnW = fmBadge.stringWidth(inText) + 12;
        int exBtnW = fmBadge.stringWidth(exText) + 12;

        int inBtnX = badgeX - inBtnW - 6;
        int exBtnX = inBtnX - exBtnW - 4;

        incomeToggleBounds.setBounds(inBtnX, badgeY, inBtnW, badgeH);
        expenseToggleBounds.setBounds(exBtnX, badgeY, exBtnW, badgeH);

        boolean inHover = mousePoint != null && incomeToggleBounds.contains(mousePoint);
        boolean exHover = mousePoint != null && expenseToggleBounds.contains(mousePoint);

        // Render Outflow (Expenses) Button
        if (!showingIncome) {
            g2.setColor(new Color(254, 226, 226));
            g2.fillRoundRect(exBtnX, badgeY, exBtnW, badgeH, 8, 8);
            g2.setColor(new Color(239, 68, 68));
            g2.drawRoundRect(exBtnX, badgeY, exBtnW, badgeH, 8, 8);
            g2.setColor(new Color(185, 28, 28));
        } else {
            g2.setColor(exHover ? new Color(241, 245, 249) : new Color(248, 250, 252));
            g2.fillRoundRect(exBtnX, badgeY, exBtnW, badgeH, 8, 8);
            g2.setColor(exHover ? new Color(203, 213, 225) : UiUtils.COLOR_BORDER);
            g2.drawRoundRect(exBtnX, badgeY, exBtnW, badgeH, 8, 8);
            g2.setColor(exHover ? UiUtils.COLOR_TEXT_PRIMARY : UiUtils.COLOR_TEXT_MUTED);
        }
        g2.drawString(exText, exBtnX + 6, badgeY + fmBadge.getAscent() + 2);

        // Render Inflow (Income) Button
        if (showingIncome) {
            g2.setColor(new Color(209, 250, 229));
            g2.fillRoundRect(inBtnX, badgeY, inBtnW, badgeH, 8, 8);
            g2.setColor(new Color(16, 185, 129));
            g2.drawRoundRect(inBtnX, badgeY, inBtnW, badgeH, 8, 8);
            g2.setColor(new Color(4, 120, 87));
        } else {
            g2.setColor(inHover ? new Color(241, 245, 249) : new Color(248, 250, 252));
            g2.fillRoundRect(inBtnX, badgeY, inBtnW, badgeH, 8, 8);
            g2.setColor(inHover ? new Color(203, 213, 225) : UiUtils.COLOR_BORDER);
            g2.drawRoundRect(inBtnX, badgeY, inBtnW, badgeH, 8, 8);
            g2.setColor(inHover ? UiUtils.COLOR_TEXT_PRIMARY : UiUtils.COLOR_TEXT_MUTED);
        }
        g2.drawString(inText, inBtnX + 6, badgeY + fmBadge.getAscent() + 2);

        // Empty state check
        if (displayItems.isEmpty()) {
            g2.setFont(UiUtils.regularFont().deriveFont(Math.max(12f, 12f * scale)));
            g2.setColor(UiUtils.COLOR_TEXT_MUTED);
            String emptyMsg = showingIncome ? "No income recorded in this period." : "No expense data recorded in this period.";
            FontMetrics fmEmpty = g2.getFontMetrics();
            g2.drawString(emptyMsg, (w - fmEmpty.stringWidth(emptyMsg)) / 2, h / 2);
            g2.dispose();
            return;
        }

        double total = 0.0;
        for (SliceItem item : displayItems) total += item.amount;

        // 3. Dynamic Donut Sizing & Geometry Positioning
        int topMargin = titleY + 16;
        int availW = (int) (w * 0.44);
        int availH = h - topMargin - 22;

        // Donut diameter can now dynamically expand up to 260px!
        int donutDiameter = Math.min(260, Math.min(availW - 16, availH));
        donutDiameter = Math.max(140, donutDiameter);

        int donutX = 22;
        int donutY = topMargin + (availH - donutDiameter) / 2;

        cachedCenterX = donutX + donutDiameter / 2;
        cachedCenterY = donutY + donutDiameter / 2;
        cachedDonutDiameter = donutDiameter;
        cachedHoleDiameter = (int) (donutDiameter * 0.58);

        // 4. Render Donut Slices (with outward explosion if hovered)
        double currentAngle = 90.0;
        for (int i = 0; i < displayItems.size(); i++) {
            SliceItem item = displayItems.get(i);
            double angle = (item.percentage / 100.0) * 360.0;
            boolean isHovered = (i == hoveredIndex);

            double midAngle = currentAngle - (angle / 2.0);
            double shiftX = 0.0;
            double shiftY = 0.0;

            if (isHovered) {
                double rad = Math.toRadians(midAngle);
                double shiftDist = 7.0 * scale;
                shiftX = shiftDist * Math.cos(rad);
                shiftY = -shiftDist * Math.sin(rad);
            }

            // Alpha-fade unhovered slices when a slice is actively focused
            Color c = item.color;
            if (hoveredIndex != -1 && !isHovered) {
                c = new Color(c.getRed(), c.getGreen(), c.getBlue(), 160);
            }

            g2.setColor(c);
            Arc2D.Double arc = new Arc2D.Double(
                    donutX + shiftX,
                    donutY + shiftY,
                    donutDiameter,
                    donutDiameter,
                    currentAngle,
                    -angle,
                    Arc2D.PIE
            );
            g2.fill(arc);

            // Clean crisp slice separation border
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(isHovered ? 3.0f : 2.0f));
            g2.draw(arc);

            currentAngle -= angle;
        }

        // 5. Center Cutout Hole (Donut effect)
        int holeX = cachedCenterX - cachedHoleDiameter / 2;
        int holeY = cachedCenterY - cachedHoleDiameter / 2;
        g2.setColor(getBackground());
        g2.fill(new Ellipse2D.Double(holeX, holeY, cachedHoleDiameter, cachedHoleDiameter));
        g2.setColor(new Color(241, 245, 249));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(new Ellipse2D.Double(holeX, holeY, cachedHoleDiameter, cachedHoleDiameter));

        // 6. Dynamic Hero Center Content
        renderCenterHero(g2, scale, total, holeX, holeY, cachedHoleDiameter);

        // 7. Interactive Modern Legend Table (Right Side)
        renderLegend(g2, scale, topMargin, donutX + donutDiameter + 24, w, h);

        // 8. Floating Hover Tooltip near cursor
        if (hoveredIndex >= 0 && hoveredIndex < displayItems.size() && mousePoint != null) {
            renderFloatingTooltip(g2, scale, w, h, displayItems.get(hoveredIndex));
        }

        g2.dispose();
    }

    /**
     * Renders the dynamic content inside the donut's center hole cutout.
     */
    private void renderCenterHero(Graphics2D g2, float scale, double total, int hx, int hy, int dia) {
        int cx = hx + dia / 2;
        int cy = hy + dia / 2;

        if (hoveredIndex >= 0 && hoveredIndex < displayItems.size()) {
            SliceItem item = displayItems.get(hoveredIndex);

            // Category Title (Top line)
            Font nameFont = UiUtils.boldFont().deriveFont(Math.max(9.5f, 10.5f * scale));
            g2.setFont(nameFont);
            FontMetrics fmName = g2.getFontMetrics(nameFont);
            String catName = item.category;
            if (fmName.stringWidth(catName) > dia - 12) {
                while (catName.length() > 4 && fmName.stringWidth(catName + "..") > dia - 12) {
                    catName = catName.substring(0, catName.length() - 1);
                }
                catName = catName + "..";
            }
            g2.setColor(item.color.darker());
            g2.drawString(catName, cx - fmName.stringWidth(catName) / 2, cy - 14);

            // Amount (Middle line - large bold)
            Font amtFont = UiUtils.boldFont().deriveFont(Math.max(11f, 13f * scale));
            g2.setFont(amtFont);
            FontMetrics fmAmt = g2.getFontMetrics(amtFont);
            String amtStr = AppConfig.formatCurrencyPlain(item.amount);
            if (fmAmt.stringWidth(amtStr) > dia - 10) {
                amtStr = String.format("%.1fk", item.amount / 1000.0);
            }
            g2.setColor(new Color(15, 23, 42));
            g2.drawString(amtStr, cx - fmAmt.stringWidth(amtStr) / 2, cy + 4);

            // Percentage badge (Bottom line)
            Font pctFont = UiUtils.boldFont().deriveFont(Math.max(9f, 9.5f * scale));
            g2.setFont(pctFont);
            FontMetrics fmPct = g2.getFontMetrics(pctFont);
            String pctStr = String.format("%.1f%% of total", item.percentage);
            g2.setColor(UiUtils.COLOR_TEXT_MUTED);
            g2.drawString(pctStr, cx - fmPct.stringWidth(pctStr) / 2, cy + 20);

        } else {
            // Idle State: Total Expenses or Total Inflow
            Font subFont = UiUtils.boldFont().deriveFont(Math.max(8.5f, 9f * scale));
            g2.setFont(subFont);
            FontMetrics fmSub = g2.getFontMetrics(subFont);
            String sub = showingIncome ? "TOTAL INFLOW" : "TOTAL EXPENSES";
            g2.setColor(new Color(100, 116, 139));
            g2.drawString(sub, cx - fmSub.stringWidth(sub) / 2, cy - 14);

            // Total Amount
            Font totalFont = UiUtils.boldFont().deriveFont(Math.max(12f, 14f * scale));
            g2.setFont(totalFont);
            FontMetrics fmTotal = g2.getFontMetrics(totalFont);
            String totalStr = AppConfig.formatCurrencyPlain(total);
            if (fmTotal.stringWidth(totalStr) > dia - 10) {
                totalStr = String.format("%.1fk", total / 1000.0);
            }
            g2.setColor(new Color(15, 23, 42));
            g2.drawString(totalStr, cx - fmTotal.stringWidth(totalStr) / 2, cy + 5);

            // Category count
            Font countFont = UiUtils.regularFont().deriveFont(Math.max(8.5f, 9f * scale));
            g2.setFont(countFont);
            FontMetrics fmCount = g2.getFontMetrics(countFont);
            String countStr = data.size() + (showingIncome ? " Inflow Sources" : " Categories");
            g2.setColor(new Color(148, 163, 184));
            g2.drawString(countStr, cx - fmCount.stringWidth(countStr) / 2, cy + 20);
        }
    }

    /**
     * Renders the interactive itemized category legend on the right-hand side.
     */
    private void renderLegend(Graphics2D g2, float scale, int topMargin, int startX, int totalW, int totalH) {
        legendItemBounds.clear();
        int legendX = startX;
        int legendW = totalW - legendX - 16;
        if (legendW < 120) return;

        int rowH = Math.max(24, Math.round(27 * scale));
        int dotSize = Math.max(9, Math.round(10 * scale));
        int legendY = topMargin + 4;

        Font nameFont = UiUtils.boldFont().deriveFont(Math.max(10f, 11f * scale));
        Font valFont = UiUtils.regularFont().deriveFont(Math.max(10f, 10.5f * scale));
        Font pctFont = UiUtils.boldFont().deriveFont(Math.max(9f, 9.5f * scale));

        FontMetrics fmName = g2.getFontMetrics(nameFont);
        FontMetrics fmVal = g2.getFontMetrics(valFont);
        FontMetrics fmPct = g2.getFontMetrics(pctFont);

        for (int i = 0; i < displayItems.size(); i++) {
            if (legendY + rowH > totalH - 10) break;

            SliceItem item = displayItems.get(i);
            boolean isHovered = (i == hoveredIndex);
            Rectangle rowRect = new Rectangle(legendX, legendY - 2, legendW, rowH);
            legendItemBounds.add(rowRect);

            // Hover pill background
            if (isHovered) {
                g2.setColor(new Color(241, 245, 249));
                g2.fillRoundRect(rowRect.x, rowRect.y, rowRect.width, rowRect.height, 8, 8);
                g2.setColor(new Color(203, 213, 225));
                g2.drawRoundRect(rowRect.x, rowRect.y, rowRect.width, rowRect.height, 8, 8);
            }

            int itemMidY = legendY + (rowH / 2) - 2;

            // 1. Color Pill / Swatch
            int dotY = itemMidY - (dotSize / 2);
            g2.setColor(item.color);
            g2.fillRoundRect(legendX + 6, dotY, dotSize, dotSize, 4, 4);

            // 2. Percentage Badge (Right aligned)
            String pctText = String.format("%.0f%%", item.percentage);
            int pctBadgeW = fmPct.stringWidth(pctText) + 10;
            int pctBadgeH = fmPct.getHeight() + 2;
            int pctBadgeX = legendX + legendW - pctBadgeW - 6;
            int pctBadgeY = itemMidY - (pctBadgeH / 2);

            g2.setColor(isHovered ? item.color : new Color(241, 245, 249));
            g2.fillRoundRect(pctBadgeX, pctBadgeY, pctBadgeW, pctBadgeH, 6, 6);
            g2.setColor(isHovered ? Color.WHITE : new Color(71, 85, 105));
            g2.setFont(pctFont);
            g2.drawString(pctText, pctBadgeX + 5, pctBadgeY + fmPct.getAscent() + 1);

            // 3. Amount String
            String amtText = AppConfig.formatCurrency(item.amount);
            int amtW = fmVal.stringWidth(amtText);
            int amtX = pctBadgeX - amtW - 10;

            if (amtX > legendX + dotSize + 80) {
                g2.setFont(valFont);
                g2.setColor(isHovered ? new Color(15, 23, 42) : new Color(100, 116, 139));
                g2.drawString(amtText, amtX, itemMidY + (fmVal.getAscent() / 2) - 1);
            } else {
                amtX = pctBadgeX; // If narrow, collapse amount space
            }

            // 4. Category Name (truncated with ellipsis if needed)
            int nameMaxW = amtX - (legendX + dotSize + 16) - 6;
            String displayName = item.category;
            g2.setFont(nameFont);
            if (nameMaxW > 40) {
                if (fmName.stringWidth(displayName) > nameMaxW) {
                    while (displayName.length() > 3 && fmName.stringWidth(displayName + "..") > nameMaxW) {
                        displayName = displayName.substring(0, displayName.length() - 1);
                    }
                    displayName = displayName + "..";
                }
                g2.setColor(isHovered ? item.color.darker() : new Color(30, 41, 59));
                g2.drawString(displayName, legendX + dotSize + 14, itemMidY + (fmName.getAscent() / 2) - 1);
            }

            legendY += rowH;
        }
    }

    /**
     * Renders a floating glassmorphic tooltip card near the mouse cursor.
     */
    private void renderFloatingTooltip(Graphics2D g2, float scale, int totalW, int totalH, SliceItem item) {
        if (mousePoint == null) return;

        Font catFont = UiUtils.boldFont().deriveFont(Math.max(11f, 11.5f * scale));
        Font amtFont = UiUtils.boldFont().deriveFont(Math.max(11f, 12f * scale));
        Font subFont = UiUtils.regularFont().deriveFont(Math.max(9f, 9.5f * scale));

        FontMetrics fmCat = g2.getFontMetrics(catFont);
        FontMetrics fmAmt = g2.getFontMetrics(amtFont);
        FontMetrics fmSub = g2.getFontMetrics(subFont);

        String catStr = item.category;
        String amtStr = AppConfig.formatCurrency(item.amount) + " (" + String.format("%.1f%%", item.percentage) + ")";
        String hintStr = "⚡ Click to view in Entity Tracker ↗";

        List<String> subItems = new ArrayList<>();
        if (item.isOther && item.otherDetails != null && !item.otherDetails.isEmpty()) {
            int maxShow = Math.min(3, item.otherDetails.size());
            for (int k = 0; k < maxShow; k++) {
                subItems.add("• " + item.otherDetails.get(k));
            }
            if (item.otherDetails.size() > 3) {
                subItems.add(String.format("• +%d more...", item.otherDetails.size() - 3));
            }
        }

        int maxContentW = Math.max(fmCat.stringWidth(catStr), Math.max(fmAmt.stringWidth(amtStr), fmSub.stringWidth(hintStr)));
        for (String sub : subItems) {
            maxContentW = Math.max(maxContentW, fmSub.stringWidth(sub));
        }

        int cardW = maxContentW + 28;
        int cardH = fmCat.getHeight() + fmAmt.getHeight() + fmSub.getHeight() + (subItems.size() * (fmSub.getHeight() + 2)) + 22;

        int cardX = mousePoint.x + 14;
        int cardY = mousePoint.y + 14;

        // Clamp inside panel boundaries
        if (cardX + cardW > totalW - 10) cardX = mousePoint.x - cardW - 10;
        if (cardY + cardH > totalH - 10) cardY = mousePoint.y - cardH - 10;
        if (cardX < 10) cardX = 10;
        if (cardY < 10) cardY = 10;

        // Dark sleek glassmorphic container
        g2.setColor(new Color(15, 23, 42, 238));
        g2.fill(new RoundRectangle2D.Double(cardX, cardY, cardW, cardH, 10, 10));
        g2.setColor(new Color(51, 65, 85, 220));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(new RoundRectangle2D.Double(cardX, cardY, cardW, cardH, 10, 10));

        // Category with colored swatch
        int curY = cardY + fmCat.getAscent() + 8;
        g2.setColor(item.color);
        g2.fillRoundRect(cardX + 10, curY - 9, 8, 8, 3, 3);
        g2.setColor(Color.WHITE);
        g2.setFont(catFont);
        g2.drawString(catStr, cardX + 24, curY);

        // Amount & Percentage
        curY += fmAmt.getHeight() + 2;
        g2.setColor(new Color(52, 211, 153)); // Soft emerald green
        g2.setFont(amtFont);
        g2.drawString(amtStr, cardX + 10, curY);

        // Sub items breakdown (if Others)
        if (!subItems.isEmpty()) {
            g2.setFont(subFont);
            g2.setColor(new Color(203, 213, 225)); // Slate 300
            for (String sub : subItems) {
                curY += fmSub.getHeight() + 2;
                g2.drawString(sub, cardX + 10, curY);
            }
        }

        // Action navigation hint
        curY += fmSub.getHeight() + 2;
        g2.setColor(new Color(147, 197, 253)); // Soft cyan blue
        g2.setFont(subFont);
        g2.drawString(hintStr, cardX + 10, curY);
    }
}
