package com.navfinance.ui;

import com.navfinance.ui.components.ResponsiveBarPanel;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import java.awt.Dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ResponsiveBarPanel} ensuring responsive wrapping and prevention
 * of component overlapping under constrained widths and zoomed typography.
 */
public class ResponsiveBarPanelTest {

    @Test
    public void testDirectComponentsSingleRowAndWrap() {
        JButton btnLeft = new JButton("Left");
        btnLeft.setPreferredSize(new Dimension(200, 40));
        JButton btnRight = new JButton("Right");
        btnRight.setPreferredSize(new Dimension(150, 40));

        ResponsiveBarPanel panel = new ResponsiveBarPanel(btnLeft, btnRight, 10, 8, true);

        // 1. Wide panel: single row (vertically centered)
        panel.setSize(600, 100);
        panel.doLayout();

        assertEquals(0, btnLeft.getX());
        assertEquals(30, btnLeft.getY());
        assertEquals(600 - 150, btnRight.getX());
        assertEquals(30, btnRight.getY());

        // 2. Narrow / Zoomed panel: wraps to 2 rows without overlapping
        panel.setSize(300, 100);
        panel.doLayout();

        assertEquals(0, btnLeft.getY());
        assertTrue(btnRight.getY() >= btnLeft.getY() + btnLeft.getHeight(),
                "Right component must wrap to row 2 beneath left component to prevent overlapping");
    }

    @Test
    public void testContainerModeAddLeftAndAddRight() {
        ResponsiveBarPanel panel = new ResponsiveBarPanel(10, 8);
        JButton btn1 = new JButton("Left 1");
        btn1.setPreferredSize(new Dimension(100, 35));
        JButton btn2 = new JButton("Right 1");
        btn2.setPreferredSize(new Dimension(100, 35));

        panel.addLeft(btn1);
        panel.addRight(btn2);

        panel.setSize(500, 80);
        panel.doLayout();

        // Should calculate preferred size without throwing
        Dimension pref = panel.getPreferredSize();
        assertTrue(pref.width > 0);
        assertTrue(pref.height > 0);
    }
}
