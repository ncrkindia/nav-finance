package com.navfinance.ui.components;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.LayoutManager;

/**
 * Responsive layout container designed to prevent component collision and overlapping.
 * <p>
 * Arranges a left component (such as titles, branding, or filters) and a right component
 * (such as action buttons or search bars) side-by-side on a single row when width permits.
 * When magnification/zoom is active or window width is constrained, it automatically wraps
 * into two vertically stacked, properly spaced rows without any element overlapping or clipping.
 * </p>
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public class ResponsiveBarPanel extends JPanel {

    private final JComponent leftComponent;
    private final JComponent rightComponent;
    private final int hGap;
    private final int vGap;
    private final boolean alignRightOnWrap;

    private JPanel leftContainer;
    private JPanel rightContainer;

    /**
     * Constructs an empty responsive bar panel with specified horizontal and vertical gaps.
     * Use {@link #addLeft(Component)} and {@link #addRight(Component)} to populate items.
     *
     * @param hGap horizontal gap between components
     * @param vGap vertical gap when wrapped to two rows
     */
    public ResponsiveBarPanel(int hGap, int vGap) {
        this(hGap, vGap, true);
    }

    /**
     * Constructs an empty responsive bar panel with custom alignment options.
     *
     * @param hGap horizontal gap between components
     * @param vGap vertical gap when wrapped to two rows
     * @param alignRightOnWrap whether right components stay aligned to the right when wrapped
     */
    public ResponsiveBarPanel(int hGap, int vGap, boolean alignRightOnWrap) {
        this.hGap = hGap;
        this.vGap = vGap;
        this.alignRightOnWrap = alignRightOnWrap;

        this.leftContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, hGap, 0));
        this.leftContainer.setOpaque(false);
        this.rightContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, hGap, 0));
        this.rightContainer.setOpaque(false);

        this.leftComponent = leftContainer;
        this.rightComponent = rightContainer;

        setOpaque(false);
        setLayout(new ResponsiveBarLayout());

        add(leftComponent);
        add(rightComponent);
    }

    /**
     * Constructs a responsive bar panel holding pre-existing left and right components with default gaps.
     */
    public ResponsiveBarPanel(JComponent leftComponent, JComponent rightComponent) {
        this(leftComponent, rightComponent, 12, 8, true);
    }

    /**
     * Constructs a responsive bar panel holding pre-existing left and right components with custom gaps.
     */
    public ResponsiveBarPanel(JComponent leftComponent, JComponent rightComponent, int hGap, int vGap, boolean alignRightOnWrap) {
        this.leftComponent = leftComponent;
        this.rightComponent = rightComponent;
        this.hGap = hGap;
        this.vGap = vGap;
        this.alignRightOnWrap = alignRightOnWrap;

        setOpaque(false);
        setLayout(new ResponsiveBarLayout());

        if (leftComponent != null) add(leftComponent);
        if (rightComponent != null) add(rightComponent);
    }

    /**
     * Adds a component to the left-aligned section of this responsive bar.
     *
     * @param comp component to append
     */
    public void addLeft(Component comp) {
        if (leftContainer != null) {
            leftContainer.add(comp);
            revalidate();
            repaint();
        }
    }

    /**
     * Adds a component to the right-aligned section of this responsive bar.
     *
     * @param comp component to append
     */
    public void addRight(Component comp) {
        if (rightContainer != null) {
            rightContainer.add(comp);
            revalidate();
            repaint();
        }
    }

    private class ResponsiveBarLayout implements LayoutManager {

        @Override
        public void addLayoutComponent(String name, Component comp) {}

        @Override
        public void removeLayoutComponent(Component comp) {}

        @Override
        public Dimension preferredLayoutSize(java.awt.Container parent) {
            Insets insets = parent.getInsets();
            Dimension lDim = leftComponent != null ? leftComponent.getPreferredSize() : new Dimension(0, 0);
            Dimension rDim = rightComponent != null ? rightComponent.getPreferredSize() : new Dimension(0, 0);

            int parentWidth = parent.getWidth();
            // If parent has realized width and it's less than single-row requirement, report two-row height
            if (parentWidth > 0 && parentWidth < (lDim.width + rDim.width + hGap + insets.left + insets.right)) {
                int w = Math.max(lDim.width, rDim.width) + insets.left + insets.right;
                int h = lDim.height + rDim.height + vGap + insets.top + insets.bottom;
                return new Dimension(w, h);
            }

            int w = lDim.width + rDim.width + hGap + insets.left + insets.right;
            int h = Math.max(lDim.height, rDim.height) + insets.top + insets.bottom;
            return new Dimension(w, h);
        }

        @Override
        public Dimension minimumLayoutSize(java.awt.Container parent) {
            Insets insets = parent.getInsets();
            Dimension lDim = leftComponent != null ? leftComponent.getMinimumSize() : new Dimension(0, 0);
            Dimension rDim = rightComponent != null ? rightComponent.getMinimumSize() : new Dimension(0, 0);
            int w = Math.max(lDim.width, rDim.width) + insets.left + insets.right;
            int h = lDim.height + rDim.height + vGap + insets.top + insets.bottom;
            return new Dimension(w, h);
        }

        @Override
        public void layoutContainer(java.awt.Container parent) {
            Insets insets = parent.getInsets();
            int totalW = parent.getWidth();
            int availW = totalW - insets.left - insets.right;

            Dimension lDim = leftComponent != null ? leftComponent.getPreferredSize() : new Dimension(0, 0);
            Dimension rDim = rightComponent != null ? rightComponent.getPreferredSize() : new Dimension(0, 0);

            boolean isSingleRow = availW >= (lDim.width + rDim.width + hGap);

            if (isSingleRow) {
                int totalH = parent.getHeight();
                int contentH = totalH - insets.top - insets.bottom;

                if (leftComponent != null) {
                    int lY = insets.top + Math.max(0, (contentH - lDim.height) / 2);
                    leftComponent.setBounds(insets.left, lY, lDim.width, lDim.height);
                }
                if (rightComponent != null) {
                    int rY = insets.top + Math.max(0, (contentH - rDim.height) / 2);
                    int rX = totalW - insets.right - rDim.width;
                    rightComponent.setBounds(rX, rY, rDim.width, rDim.height);
                }
            } else {
                // Two rows: row 1 is LeftComponent, row 2 is RightComponent
                int curY = insets.top;
                if (leftComponent != null) {
                    int lW = Math.min(availW, lDim.width);
                    leftComponent.setBounds(insets.left, curY, lW, lDim.height);
                    curY += lDim.height + vGap;
                }
                if (rightComponent != null) {
                    int rW = Math.min(availW, rDim.width);
                    int rX = insets.left;
                    if (alignRightOnWrap && availW >= rDim.width) {
                        rX = totalW - insets.right - rDim.width;
                    }
                    rightComponent.setBounds(rX, curY, rW, rDim.height);
                }
            }
        }
    }
}
