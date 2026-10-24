package com.navfinance.ui.util;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * High-performance, anti-aliased Java2D vector icon factory for NavFinance.
 * <p>
 * Completely replaces unreliable OS unicode emoji fonts with resolution-independent,
 * DPI-aware, scalable vector graphics. These icons render flawlessly across all Windows,
 * Linux, and macOS platforms with zero missing glyphs or square box rendering bugs.
 * </p>
 */
public final class NavIcons {

    private NavIcons() {}

    // =========================================================================
    // Core Vector Icon Implementation
    // =========================================================================

    /**
     * Internal implementation of {@link Icon} that delegates painting to a lambda Consumer
     * while enabling full anti-aliasing and stroke normalization.
     */
    public static class VectorIcon implements Icon {
        private final int width;
        private final int height;
        private final Consumer<Graphics2D> painter;

        /**
         * Constructs a square vector icon with specified dimensions and drawing routine.
         *
         * @param size pixel width and height
         * @param painter rendering function applied to translated Graphics2D context
         */
        public VectorIcon(int size, Consumer<Graphics2D> painter) {
            this(size, size, painter);
        }

        /**
         * Constructs a rectangular vector icon.
         *
         * @param width pixel width
         * @param height pixel height
         * @param painter rendering function
         */
        public VectorIcon(int width, int height, Consumer<Graphics2D> painter) {
            this.width = width;
            this.height = height;
            this.painter = Objects.requireNonNull(painter);
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            painter.accept(g2);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return width;
        }

        @Override
        public int getIconHeight() {
            return height;
        }
    }

    // =========================================================================
    // Application Brand & Primary Navigation Icons
    // =========================================================================

    /**
     * Creates a circular gold currency coin application logo with an inner border and Indian Rupee symbol.
     *
     * @param size diameter in pixels
     * @return vector {@link Icon}
     */
    public static Icon appLogo(int size) {
        return new VectorIcon(size, g2 -> {
            float s = size;
            // Outer golden gradient circle
            GradientPaint gp = new GradientPaint(0, 0, new Color(251, 191, 36), s, s, new Color(217, 119, 6));
            g2.setPaint(gp);
            g2.fill(new Ellipse2D.Float(1, 1, s - 2, s - 2));

            // Inner gold rim
            g2.setColor(new Color(254, 243, 199));
            g2.setStroke(new BasicStroke(Math.max(1.2f, s * 0.05f)));
            g2.draw(new Ellipse2D.Float(s * 0.12f, s * 0.12f, s * 0.76f, s * 0.76f));

            // Currency symbol stroke (₹ stylized)
            g2.setColor(new Color(69, 26, 3));
            g2.setStroke(new BasicStroke(Math.max(1.5f, s * 0.1f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float cx = s * 0.32f;
            float cy = s * 0.28f;
            float rw = s * 0.36f;
            // Top horizontal line
            g2.draw(new Line2D.Float(cx, cy, cx + rw, cy));
            // Second horizontal line
            g2.draw(new Line2D.Float(cx, cy + s * 0.14f, cx + rw * 0.85f, cy + s * 0.14f));
            // Curve & diagonal leg
            GeneralPath p = new GeneralPath();
            p.moveTo(cx + s * 0.08f, cy);
            p.lineTo(cx + s * 0.08f, cy + s * 0.28f);
            p.curveTo(cx + rw * 0.9f, cy + s * 0.28f, cx + rw * 0.9f, cy + s * 0.44f, cx + s * 0.12f, cy + s * 0.44f);
            p.lineTo(cx + rw, cy + s * 0.52f);
            g2.draw(p);
        });
    }

    /**
     * Creates a Dashboard metric chart icon (3 vertical ascending bars with baseline).
     *
     * @param size dimension in pixels
     * @param color fill color
     * @return vector {@link Icon}
     */
    public static Icon dashboard(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float w = size;
            float barW = w * 0.22f;
            float gap = w * 0.08f;
            float bY = w * 0.85f;

            // Bar 1 (low)
            g2.fill(new RoundRectangle2D.Float(w * 0.10f, bY - w * 0.32f, barW, w * 0.32f, 2, 2));
            // Bar 2 (medium)
            g2.fill(new RoundRectangle2D.Float(w * 0.10f + barW + gap, bY - w * 0.55f, barW, w * 0.55f, 2, 2));
            // Bar 3 (high)
            g2.fill(new RoundRectangle2D.Float(w * 0.10f + (barW + gap) * 2, bY - w * 0.75f, barW, w * 0.75f, 2, 2));
            // Baseline
            g2.setStroke(new BasicStroke(Math.max(1.5f, w * 0.08f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Line2D.Float(w * 0.05f, bY + 1, w * 0.95f, bY + 1));
        });
    }

    /**
     * Creates a Transactions ledger icon (credit card outline with magnetic strip and chip).
     *
     * @param size dimension in pixels
     * @param color stroke and fill color
     * @return vector {@link Icon}
     */
    public static Icon transactions(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            float cardW = s * 0.88f;
            float cardH = s * 0.65f;
            float x = (s - cardW) / 2f;
            float y = (s - cardH) / 2f;

            g2.setStroke(new BasicStroke(Math.max(1.2f, s * 0.08f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new RoundRectangle2D.Float(x, y, cardW, cardH, 4, 4));

            // Magnetic stripe
            g2.fill(new RoundRectangle2D.Float(x, y + cardH * 0.22f, cardW, cardH * 0.22f, 0, 0));

            // Chip box
            g2.fill(new RoundRectangle2D.Float(x + cardW * 0.15f, y + cardH * 0.58f, cardW * 0.25f, cardH * 0.24f, 1, 1));
        });
    }

    /**
     * Creates an upcoming Due Payments / Alarm Clock icon.
     *
     * @param size dimension in pixels
     * @param color stroke color
     * @return vector {@link Icon}
     */
    public static Icon duePayments(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            float r = s * 0.38f;
            float cx = s / 2f;
            float cy = s / 2f + s * 0.05f;

            g2.setStroke(new BasicStroke(Math.max(1.4f, s * 0.09f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Clock face circle
            g2.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));

            // Clock hands (hour to 12, minute to 3)
            g2.draw(new Line2D.Float(cx, cy, cx, cy - r * 0.62f));
            g2.draw(new Line2D.Float(cx, cy, cx + r * 0.52f, cy));

            // Top alarm bells
            g2.draw(new Line2D.Float(cx - r * 0.7f, cy - r * 0.7f, cx - r * 0.9f, cy - r * 0.9f));
            g2.draw(new Line2D.Float(cx + r * 0.7f, cy - r * 0.7f, cx + r * 0.9f, cy - r * 0.9f));
        });
    }

    /**
     * Creates a Loans &amp; Dues exchange vector icon.
     *
     * @param size dimension in pixels
     * @param color stroke color
     * @return vector {@link Icon}
     */
    public static Icon loan(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            g2.setStroke(new BasicStroke(Math.max(1.4f, s * 0.09f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // Two interconnected transfer hands / exchange arrows
            GeneralPath p1 = new GeneralPath();
            p1.moveTo(s * 0.15f, s * 0.38f);
            p1.lineTo(s * 0.68f, s * 0.38f);
            p1.lineTo(s * 0.52f, s * 0.22f);
            g2.draw(p1);

            GeneralPath p2 = new GeneralPath();
            p2.moveTo(s * 0.85f, s * 0.62f);
            p2.lineTo(s * 0.32f, s * 0.62f);
            p2.lineTo(s * 0.48f, s * 0.78f);
            g2.draw(p2);
        });
    }



    /**
     * Creates a Catalog / Masters folder layer icon.
     *
     * @param size dimension in pixels
     * @param color stroke and fill color
     * @return vector {@link Icon}
     */
    public static Icon masters(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            g2.setStroke(new BasicStroke(Math.max(1.3f, s * 0.08f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // Three stacked perspective rectangular planes
            float w = s * 0.8f;
            float h = s * 0.28f;
            float x = (s - w) / 2f;

            // Layer 1
            g2.draw(new RoundRectangle2D.Float(x, s * 0.18f, w, h, 2, 2));
            // Layer 2
            g2.draw(new RoundRectangle2D.Float(x, s * 0.38f, w, h, 2, 2));
            // Layer 3
            g2.draw(new RoundRectangle2D.Float(x, s * 0.58f, w, h, 2, 2));
        });
    }

    /**
     * Creates an Audit Trail clipboard / document icon.
     *
     * @param size dimension in pixels
     * @param color stroke color
     * @return vector {@link Icon}
     */
    public static Icon audit(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            g2.setStroke(new BasicStroke(Math.max(1.3f, s * 0.08f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            float w = s * 0.65f;
            float h = s * 0.80f;
            float x = (s - w) / 2f;
            float y = (s - h) / 2f;

            // Sheet outline
            g2.draw(new RoundRectangle2D.Float(x, y, w, h, 3, 3));

            // Top clip
            float clipW = w * 0.45f;
            g2.draw(new RoundRectangle2D.Float((s - clipW) / 2f, y - 2, clipW, 4, 1, 1));

            // Document lines
            g2.draw(new Line2D.Float(x + w * 0.2f, y + h * 0.35f, x + w * 0.8f, y + h * 0.35f));
            g2.draw(new Line2D.Float(x + w * 0.2f, y + h * 0.55f, x + w * 0.8f, y + h * 0.55f));
            g2.draw(new Line2D.Float(x + w * 0.2f, y + h * 0.75f, x + w * 0.55f, y + h * 0.75f));
        });
    }

    /**
     * Creates an Open Book handbook icon for the User Guide.
     *
     * @param size dimension in pixels
     * @param color stroke color
     * @return vector {@link Icon}
     */
    public static Icon userGuide(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            g2.setStroke(new BasicStroke(Math.max(1.3f, s * 0.08f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            float midX = s / 2f;

            // Open book spine & left page
            GeneralPath pLeft = new GeneralPath();
            pLeft.moveTo(midX, s * 0.75f);
            pLeft.quadTo(s * 0.3f, s * 0.85f, s * 0.12f, s * 0.75f);
            pLeft.lineTo(s * 0.12f, s * 0.25f);
            pLeft.quadTo(s * 0.3f, s * 0.35f, midX, s * 0.25f);
            pLeft.closePath();
            g2.draw(pLeft);

            // Right page
            GeneralPath pRight = new GeneralPath();
            pRight.moveTo(midX, s * 0.75f);
            pRight.quadTo(s * 0.7f, s * 0.85f, s * 0.88f, s * 0.75f);
            pRight.lineTo(s * 0.88f, s * 0.25f);
            pRight.quadTo(s * 0.7f, s * 0.35f, midX, s * 0.25f);
            pRight.closePath();
            g2.draw(pRight);
        });
    }

    /**
     * Creates an Information circle "i" icon for About Us.
     *
     * @param size dimension in pixels
     * @param color stroke and fill color
     * @return vector {@link Icon}
     */
    public static Icon aboutUs(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            g2.setStroke(new BasicStroke(Math.max(1.4f, s * 0.09f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            float r = s * 0.42f;
            float cx = s / 2f;
            float cy = s / 2f;
            g2.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));

            // Dot
            g2.fill(new Ellipse2D.Float(cx - s * 0.06f, cy - s * 0.25f, s * 0.12f, s * 0.12f));

            // Stem
            g2.draw(new Line2D.Float(cx, cy - s * 0.05f, cx, cy + s * 0.24f));
        });
    }

    /**
     * Creates a Configuration Gear / Settings cogwheel icon.
     *
     * @param size dimension in pixels
     * @param color stroke color
     * @return vector {@link Icon}
     */
    public static Icon settings(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            float cx = s / 2f;
            float cy = s / 2f;
            float r = s * 0.32f;

            g2.setStroke(new BasicStroke(Math.max(1.4f, s * 0.09f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));

            // Inner hole
            float rHole = r * 0.45f;
            g2.draw(new Ellipse2D.Float(cx - rHole, cy - rHole, rHole * 2, rHole * 2));

            // 6 Radial cog teeth
            for (int i = 0; i < 6; i++) {
                double angle = Math.toRadians(i * 60);
                float x1 = (float) (cx + Math.cos(angle) * r);
                float y1 = (float) (cy + Math.sin(angle) * r);
                float x2 = (float) (cx + Math.cos(angle) * (r + s * 0.12f));
                float y2 = (float) (cy + Math.sin(angle) * (r + s * 0.12f));
                g2.draw(new Line2D.Float(x1, y1, x2, y2));
            }
        });
    }

    // =========================================================================
    // Action & Button Utility Icons
    // =========================================================================

    /**
     * Creates a mathematical plus icon for Add / Create actions.
     *
     * @param size dimension in pixels
     * @param color stroke color
     * @return vector {@link Icon}
     */
    public static Icon plus(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            g2.setStroke(new BasicStroke(Math.max(1.8f, s * 0.14f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float p = s * 0.22f;
            g2.draw(new Line2D.Float(s / 2f, p, s / 2f, s - p));
            g2.draw(new Line2D.Float(p, s / 2f, s - p, s / 2f));
        });
    }

    /**
     * Creates a mathematical minus icon for Expense / Subtraction actions.
     *
     * @param size dimension in pixels
     * @param color stroke color
     * @return vector {@link Icon}
     */
    public static Icon minus(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            g2.setStroke(new BasicStroke(Math.max(1.8f, s * 0.14f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float p = s * 0.22f;
            g2.draw(new Line2D.Float(p, s / 2f, s - p, s / 2f));
        });
    }

    /**
     * Creates a checkmark icon for Settle / Confirm actions.
     *
     * @param size dimension in pixels
     * @param color stroke color
     * @return vector {@link Icon}
     */
    public static Icon check(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            g2.setStroke(new BasicStroke(Math.max(2.0f, s * 0.15f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            GeneralPath p = new GeneralPath();
            p.moveTo(s * 0.18f, s * 0.50f);
            p.lineTo(s * 0.42f, s * 0.76f);
            p.lineTo(s * 0.82f, s * 0.24f);
            g2.draw(p);
        });
    }

    /**
     * Creates a cross icon for Delete / Cancel / Close actions.
     *
     * @param size dimension in pixels
     * @param color stroke color
     * @return vector {@link Icon}
     */
    public static Icon cross(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            g2.setStroke(new BasicStroke(Math.max(1.8f, s * 0.14f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float p = s * 0.24f;
            g2.draw(new Line2D.Float(p, p, s - p, s - p));
            g2.draw(new Line2D.Float(s - p, p, p, s - p));
        });
    }

    /**
     * Creates a pencil / stylus icon for Edit actions.
     *
     * @param size dimension in pixels
     * @param color stroke color
     * @return vector {@link Icon}
     */
    public static Icon edit(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            g2.setStroke(new BasicStroke(Math.max(1.4f, s * 0.09f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            GeneralPath p = new GeneralPath();
            p.moveTo(s * 0.20f, s * 0.80f);
            p.lineTo(s * 0.22f, s * 0.65f);
            p.lineTo(s * 0.65f, s * 0.22f);
            p.lineTo(s * 0.78f, s * 0.35f);
            p.lineTo(s * 0.35f, s * 0.78f);
            p.closePath();
            g2.draw(p);

            // Pencil tip line
            g2.draw(new Line2D.Float(s * 0.20f, s * 0.80f, s * 0.32f, s * 0.72f));
        });
    }

    /**
     * Creates a trash can icon for Delete actions.
     *
     * @param size dimension in pixels
     * @param color stroke color
     * @return vector {@link Icon}
     */
    public static Icon trash(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            g2.setStroke(new BasicStroke(Math.max(1.3f, s * 0.09f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            float topY = s * 0.28f;
            float bottomY = s * 0.85f;

            // Lid line & handle
            g2.draw(new Line2D.Float(s * 0.15f, topY, s * 0.85f, topY));
            g2.draw(new RoundRectangle2D.Float(s * 0.38f, topY - s * 0.12f, s * 0.24f, s * 0.12f, 1, 1));

            // Can body
            GeneralPath p = new GeneralPath();
            p.moveTo(s * 0.24f, topY);
            p.lineTo(s * 0.28f, bottomY);
            p.lineTo(s * 0.72f, bottomY);
            p.lineTo(s * 0.76f, topY);
            g2.draw(p);

            // Interior ribs
            g2.draw(new Line2D.Float(s * 0.42f, topY + s * 0.12f, s * 0.42f, bottomY - s * 0.1f));
            g2.draw(new Line2D.Float(s * 0.58f, topY + s * 0.12f, s * 0.58f, bottomY - s * 0.1f));
        });
    }

    /**
     * Creates a circular synchronization arrow icon for Refresh actions.
     *
     * @param size dimension in pixels
     * @param color stroke color
     * @return vector {@link Icon}
     */
    public static Icon refresh(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            g2.setStroke(new BasicStroke(Math.max(1.5f, s * 0.10f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            float r = s * 0.35f;
            float cx = s / 2f;
            float cy = s / 2f;

            // 270 degree circular arc
            g2.drawArc((int)(cx - r), (int)(cy - r), (int)(r * 2), (int)(r * 2), 45, 280);

            // Arrow head
            float ax = (float) (cx + Math.cos(Math.toRadians(45)) * r);
            float ay = (float) (cy - Math.sin(Math.toRadians(45)) * r);
            GeneralPath head = new GeneralPath();
            head.moveTo(ax - s * 0.15f, ay - s * 0.05f);
            head.lineTo(ax, ay);
            head.lineTo(ax - s * 0.05f, ay + s * 0.15f);
            g2.draw(head);
        });
    }

    /**
     * Creates a magnifying glass with a plus sign for Zoom In actions.
     *
     * @param size dimension in pixels
     * @param color stroke color
     * @return vector {@link Icon}
     */
    public static Icon zoomIn(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            float r = s * 0.32f;
            float cx = s * 0.42f;
            float cy = s * 0.42f;

            g2.setStroke(new BasicStroke(Math.max(1.4f, s * 0.09f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
            // Handle
            g2.draw(new Line2D.Float(cx + r * 0.7f, cy + r * 0.7f, s * 0.88f, s * 0.88f));

            // Plus inside
            float p = r * 0.45f;
            g2.draw(new Line2D.Float(cx, cy - p, cx, cy + p));
            g2.draw(new Line2D.Float(cx - p, cy, cx + p, cy));
        });
    }

    /**
     * Creates a magnifying glass with a minus sign for Zoom Out actions.
     *
     * @param size dimension in pixels
     * @param color stroke color
     * @return vector {@link Icon}
     */
    public static Icon zoomOut(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            float r = s * 0.32f;
            float cx = s * 0.42f;
            float cy = s * 0.42f;

            g2.setStroke(new BasicStroke(Math.max(1.4f, s * 0.09f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
            // Handle
            g2.draw(new Line2D.Float(cx + r * 0.7f, cy + r * 0.7f, s * 0.88f, s * 0.88f));

            // Minus inside
            float p = r * 0.45f;
            g2.draw(new Line2D.Float(cx - p, cy, cx + p, cy));
        });
    }

    /**
     * Creates a crosshair / target icon for Reset Zoom actions.
     *
     * @param size dimension in pixels
     * @param color stroke color
     * @return vector {@link Icon}
     */
    public static Icon zoomReset(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            float cx = s / 2f;
            float cy = s / 2f;
            float r = s * 0.38f;

            g2.setStroke(new BasicStroke(Math.max(1.3f, s * 0.09f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
            g2.draw(new Line2D.Float(cx - r * 0.4f, cy, cx + r * 0.4f, cy));
            g2.draw(new Line2D.Float(cx, cy - r * 0.4f, cx, cy + r * 0.4f));
        });
    }

    /**
     * Creates a calendar grid icon for Date picking &amp; Scheduling.
     *
     * @param size dimension in pixels
     * @param color stroke and fill color
     * @return vector {@link Icon}
     */
    public static Icon calendar(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            g2.setStroke(new BasicStroke(Math.max(1.3f, s * 0.08f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            float w = s * 0.76f;
            float h = s * 0.70f;
            float x = (s - w) / 2f;
            float y = (s - h) / 2f + s * 0.05f;

            // Calendar body
            g2.draw(new RoundRectangle2D.Float(x, y, w, h, 3, 3));

            // Header stripe
            float headerH = h * 0.28f;
            g2.fill(new RoundRectangle2D.Float(x, y, w, headerH, 2, 2));

            // Hanging binder loops
            g2.draw(new Line2D.Float(x + w * 0.25f, y - s * 0.08f, x + w * 0.25f, y));
            g2.draw(new Line2D.Float(x + w * 0.75f, y - s * 0.08f, x + w * 0.75f, y));

            // 4 Grid day dots
            float dR = Math.max(1.5f, s * 0.04f);
            float dY1 = y + h * 0.52f;
            float dY2 = y + h * 0.78f;
            g2.fill(new Ellipse2D.Float(x + w * 0.3f - dR, dY1 - dR, dR * 2, dR * 2));
            g2.fill(new Ellipse2D.Float(x + w * 0.7f - dR, dY1 - dR, dR * 2, dR * 2));
            g2.fill(new Ellipse2D.Float(x + w * 0.3f - dR, dY2 - dR, dR * 2, dR * 2));
            g2.fill(new Ellipse2D.Float(x + w * 0.7f - dR, dY2 - dR, dR * 2, dR * 2));
        });
    }

    /**
     * Creates a download arrow / export tray icon.
     *
     * @param size dimension in pixels
     * @param color stroke color
     * @return vector {@link Icon}
     */
    public static Icon export(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            g2.setStroke(new BasicStroke(Math.max(1.4f, s * 0.09f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // Tray
            GeneralPath tray = new GeneralPath();
            tray.moveTo(s * 0.18f, s * 0.60f);
            tray.lineTo(s * 0.18f, s * 0.82f);
            tray.lineTo(s * 0.82f, s * 0.82f);
            tray.lineTo(s * 0.82f, s * 0.60f);
            g2.draw(tray);

            // Down arrow
            g2.draw(new Line2D.Float(s / 2f, s * 0.18f, s / 2f, s * 0.62f));
            GeneralPath head = new GeneralPath();
            head.moveTo(s * 0.32f, s * 0.46f);
            head.lineTo(s / 2f, s * 0.62f);
            head.lineTo(s * 0.68f, s * 0.46f);
            g2.draw(head);
        });
    }

    /**
     * Creates an upload arrow / import tray icon.
     *
     * @param size dimension in pixels
     * @param color stroke color
     * @return vector {@link Icon}
     */
    public static Icon importIcon(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            g2.setStroke(new BasicStroke(Math.max(1.4f, s * 0.09f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // Tray
            GeneralPath tray = new GeneralPath();
            tray.moveTo(s * 0.18f, s * 0.60f);
            tray.lineTo(s * 0.18f, s * 0.82f);
            tray.lineTo(s * 0.82f, s * 0.82f);
            tray.lineTo(s * 0.82f, s * 0.60f);
            g2.draw(tray);

            // Up arrow
            g2.draw(new Line2D.Float(s / 2f, s * 0.62f, s / 2f, s * 0.18f));
            GeneralPath head = new GeneralPath();
            head.moveTo(s * 0.32f, s * 0.34f);
            head.lineTo(s / 2f, s * 0.18f);
            head.lineTo(s * 0.68f, s * 0.34f);
            g2.draw(head);
        });
    }

    /**
     * Creates a funnel icon for Filtering.
     *
     * @param size dimension in pixels
     * @param color stroke color
     * @return vector {@link Icon}
     */
    public static Icon filter(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            g2.setStroke(new BasicStroke(Math.max(1.3f, s * 0.09f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            GeneralPath p = new GeneralPath();
            p.moveTo(s * 0.15f, s * 0.22f);
            p.lineTo(s * 0.85f, s * 0.22f);
            p.lineTo(s * 0.58f, s * 0.55f);
            p.lineTo(s * 0.58f, s * 0.82f);
            p.lineTo(s * 0.42f, s * 0.82f);
            p.lineTo(s * 0.42f, s * 0.55f);
            p.closePath();
            g2.draw(p);
        });
    }

    /**
     * Creates a lightning bolt icon for Auto-Payment execution mode.
     *
     * @param size dimension in pixels
     * @param color fill color
     * @return vector {@link Icon}
     */
    public static Icon autoPay(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            GeneralPath p = new GeneralPath();
            p.moveTo(s * 0.52f, s * 0.12f);
            p.lineTo(s * 0.22f, s * 0.52f);
            p.lineTo(s * 0.48f, s * 0.52f);
            p.lineTo(s * 0.44f, s * 0.88f);
            p.lineTo(s * 0.78f, s * 0.44f);
            p.lineTo(s * 0.52f, s * 0.44f);
            p.closePath();
            g2.fill(p);
        });
    }

    /**
     * Creates a hand palm icon for Manual Payment execution mode.
     *
     * @param size dimension in pixels
     * @param color stroke color
     * @return vector {@link Icon}
     */
    public static Icon manualPay(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            g2.setStroke(new BasicStroke(Math.max(1.3f, s * 0.08f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // Hand contour with fingers
            GeneralPath p = new GeneralPath();
            p.moveTo(s * 0.32f, s * 0.85f);
            p.lineTo(s * 0.32f, s * 0.42f);
            p.lineTo(s * 0.44f, s * 0.22f);
            p.lineTo(s * 0.56f, s * 0.25f);
            p.lineTo(s * 0.68f, s * 0.32f);
            p.lineTo(s * 0.75f, s * 0.48f);
            p.lineTo(s * 0.72f, s * 0.85f);
            p.closePath();
            g2.draw(p);
        });
    }

    /**
     * Creates a Person profile avatar icon.
     *
     * @param size dimension in pixels
     * @param color stroke and fill color
     * @return vector {@link Icon}
     */
    public static Icon person(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            g2.setStroke(new BasicStroke(Math.max(1.3f, s * 0.08f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // Head
            float headR = s * 0.22f;
            float cx = s / 2f;
            float headY = s * 0.32f;
            g2.draw(new Ellipse2D.Float(cx - headR, headY - headR, headR * 2, headR * 2));

            // Shoulders arc
            GeneralPath p = new GeneralPath();
            p.moveTo(s * 0.15f, s * 0.85f);
            p.quadTo(cx, s * 0.55f, s * 0.85f, s * 0.85f);
            g2.draw(p);
        });
    }

    /**
     * Creates a Utility Bill receipt icon with serrated bottom.
     *
     * @param size dimension in pixels
     * @param color stroke color
     * @return vector {@link Icon}
     */
    public static Icon bill(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            g2.setStroke(new BasicStroke(Math.max(1.3f, s * 0.08f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            float w = s * 0.65f;
            float x = (s - w) / 2f;
            float topY = s * 0.15f;
            float botY = s * 0.82f;

            GeneralPath p = new GeneralPath();
            p.moveTo(x, topY);
            p.lineTo(x + w, topY);
            p.lineTo(x + w, botY);
            // Zig-zag bottom
            p.lineTo(x + w * 0.75f, botY - s * 0.06f);
            p.lineTo(x + w * 0.50f, botY);
            p.lineTo(x + w * 0.25f, botY - s * 0.06f);
            p.lineTo(x, botY);
            p.closePath();
            g2.draw(p);

            // Lines
            g2.draw(new Line2D.Float(x + w * 0.2f, topY + s * 0.18f, x + w * 0.8f, topY + s * 0.18f));
            g2.draw(new Line2D.Float(x + w * 0.2f, topY + s * 0.35f, x + w * 0.8f, topY + s * 0.35f));
            g2.draw(new Line2D.Float(x + w * 0.2f, topY + s * 0.52f, x + w * 0.6f, topY + s * 0.52f));
        });
    }

    /**
     * Creates an EMI Loan / Bank institution icon.
     *
     * @param size dimension in pixels
     * @param color stroke color
     * @return vector {@link Icon}
     */
    public static Icon emi(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            g2.setStroke(new BasicStroke(Math.max(1.3f, s * 0.08f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // Bank pediment triangular roof
            GeneralPath roof = new GeneralPath();
            roof.moveTo(s / 2f, s * 0.15f);
            roof.lineTo(s * 0.12f, s * 0.38f);
            roof.lineTo(s * 0.88f, s * 0.38f);
            roof.closePath();
            g2.draw(roof);

            // Columns
            g2.draw(new Line2D.Float(s * 0.25f, s * 0.38f, s * 0.25f, s * 0.75f));
            g2.draw(new Line2D.Float(s * 0.50f, s * 0.38f, s * 0.50f, s * 0.75f));
            g2.draw(new Line2D.Float(s * 0.75f, s * 0.38f, s * 0.75f, s * 0.75f));

            // Base
            g2.draw(new RoundRectangle2D.Float(s * 0.10f, s * 0.75f, s * 0.80f, s * 0.10f, 1, 1));
        });
    }

    /**
     * Creates a Credit Card icon.
     *
     * @param size dimension in pixels
     * @param color stroke and fill color
     * @return vector {@link Icon}
     */
    public static Icon card(int size, Color color) {
        return transactions(size, color);
    }

    /**
     * Creates a payment channel (Via) icon.
     *
     * @param size dimension in pixels
     * @param color stroke color
     * @return vector {@link Icon}
     */
    public static Icon paymentMode(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            g2.setStroke(new BasicStroke(Math.max(1.3f, s * 0.08f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // Mobile phone device outline representing UPI / Digital payments
            float w = s * 0.55f;
            float h = s * 0.82f;
            float x = (s - w) / 2f;
            float y = (s - h) / 2f;

            g2.draw(new RoundRectangle2D.Float(x, y, w, h, 4, 4));
            // Home button circle
            g2.draw(new Ellipse2D.Float(s / 2f - s * 0.05f, y + h - s * 0.12f, s * 0.10f, s * 0.10f));
            // Top speaker notch
            g2.draw(new Line2D.Float(s / 2f - s * 0.08f, y + s * 0.08f, s / 2f + s * 0.08f, y + s * 0.08f));
        });
    }

    /**
     * Creates an anti-aliased solid circular status dot icon.
     *
     * @param size diameter in pixels
     * @param color fill color
     * @return vector {@link Icon}
     */
    public static Icon statusDot(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            g2.fill(new Ellipse2D.Float(1, 1, size - 2, size - 2));
        });
    }

    /**
     * Creates a left chevron arrow icon for calendar and timeline pagination.
     *
     * @param size dimension in pixels
     * @param color stroke color
     * @return vector {@link Icon}
     */
    public static Icon chevronLeft(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            g2.setStroke(new BasicStroke(Math.max(1.8f, s * 0.15f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            GeneralPath p = new GeneralPath();
            p.moveTo(s * 0.65f, s * 0.20f);
            p.lineTo(s * 0.32f, s * 0.50f);
            p.lineTo(s * 0.65f, s * 0.80f);
            g2.draw(p);
        });
    }

    /**
     * Creates a right chevron arrow icon for calendar and timeline pagination.
     *
     * @param size dimension in pixels
     * @param color stroke color
     * @return vector {@link Icon}
     */
    public static Icon chevronRight(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            g2.setStroke(new BasicStroke(Math.max(1.8f, s * 0.15f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            GeneralPath p = new GeneralPath();
            p.moveTo(s * 0.35f, s * 0.20f);
            p.lineTo(s * 0.68f, s * 0.50f);
            p.lineTo(s * 0.35f, s * 0.80f);
            g2.draw(p);
        });
    }

    /**
     * Creates a globe / language vector icon for UI language selection.
     *
     * @param size dimension in pixels
     * @param color stroke color
     * @return vector {@link Icon}
     */
    public static Icon language(int size, Color color) {
        return new VectorIcon(size, g2 -> {
            g2.setColor(color);
            float s = size;
            g2.setStroke(new BasicStroke(Math.max(1.3f, s * 0.08f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Outer circle
            g2.draw(new Ellipse2D.Float(s * 0.10f, s * 0.10f, s * 0.80f, s * 0.80f));
            // Equator
            g2.draw(new Line2D.Float(s * 0.10f, s * 0.50f, s * 0.90f, s * 0.50f));
            // Vertical meridian ellipse
            g2.draw(new Ellipse2D.Float(s * 0.30f, s * 0.10f, s * 0.40f, s * 0.80f));
        });
    }
}
