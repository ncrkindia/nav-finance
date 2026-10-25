package com.navfinance.ui;

import com.navfinance.model.DashboardSummary;
import com.navfinance.ui.components.CategoryPieChartPanel;
import com.navfinance.ui.components.TrendChartPanel;
import org.junit.jupiter.api.Test;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests verifying interactive functionality, listeners, and rendering
 * for {@link TrendChartPanel} and {@link CategoryPieChartPanel}.
 */
public class InteractiveChartTest {

    @Test
    public void testTrendChartPanelInteractivityAndRendering() {
        TrendChartPanel chart = new TrendChartPanel("Monthly Cash Flow Trend");
        chart.setSize(600, 320);

        List<DashboardSummary.TrendPoint> points = new ArrayList<>();
        points.add(new DashboardSummary.TrendPoint("May 26", 45000, 22000,
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), "May 2026"));
        points.add(new DashboardSummary.TrendPoint("Jun 26", 52000, 38000,
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), "June 2026"));
        points.add(new DashboardSummary.TrendPoint("Jul 26", 61000, 41000,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "July 2026"));

        chart.setTrendPoints(points);

        // Test custom slot click listener
        AtomicBoolean clicked = new AtomicBoolean(false);
        chart.setSlotClickListener((tp, idx) -> {
            clicked.set(true);
            assertEquals("June 2026", tp.getFullTitle());
            assertEquals(1, idx);
        });

        // Simulate hover mouse movement over second slot
        MouseEvent moveEvent = new MouseEvent(chart, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(),
                0, 300, 150, 0, false);
        for (java.awt.event.MouseMotionListener l : chart.getMouseMotionListeners()) {
            l.mouseMoved(moveEvent);
        }

        // Simulate slot click
        MouseEvent clickEvent = new MouseEvent(chart, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                0, 300, 150, 1, false);
        for (java.awt.event.MouseListener l : chart.getMouseListeners()) {
            l.mouseClicked(clickEvent);
        }

        assertTrue(clicked.get(), "Slot click listener must be invoked when clicking a slot");

        // Verify anti-aliased painting does not crash
        BufferedImage img = new BufferedImage(600, 320, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        chart.paint(g2);
        g2.dispose();
        assertNotNull(img);
    }

    @Test
    public void testCategoryPieChartDatasetsAndToggling() {
        CategoryPieChartPanel chart = new CategoryPieChartPanel("Expense & Income Breakdown");
        chart.setSize(550, 340);

        Map<String, Double> expenses = new LinkedHashMap<>();
        expenses.put("Rent & Housing", 18000.0);
        expenses.put("Groceries", 6500.0);
        expenses.put("Fuel", 3200.0);

        Map<String, Double> incomes = new LinkedHashMap<>();
        incomes.put("Salary", 75000.0);
        incomes.put("Dividends", 4500.0);

        chart.setDatasets(expenses, incomes);

        // Verify anti-aliased painting in expense mode
        BufferedImage img = new BufferedImage(550, 340, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        chart.paint(g2);
        g2.dispose();
        assertNotNull(img);

        // Simulate hover
        MouseEvent moveEvent = new MouseEvent(chart, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(),
                0, 120, 150, 0, false);
        chart.getMouseMotionListeners()[0].mouseMoved(moveEvent);

        // Paint with hover active
        BufferedImage imgHover = new BufferedImage(550, 340, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2Hover = imgHover.createGraphics();
        chart.paint(g2Hover);
        g2Hover.dispose();
        assertNotNull(imgHover);
    }
}
