package com.navfinance.ui.panels;

import com.navfinance.config.AppConfig;
import com.navfinance.ui.util.NavIcons;
import com.navfinance.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Information and specifications panel displaying application provenance, technology stack,
 * runtime diagnostics, license terms, and privacy guarantees.
 * <p>
 * Highlights the offline-first, telemetry-free architecture and embedded H2 database storage
 * located in the user's home directory.
 * </p>
 */
public class AboutUsPanel extends JPanel {

    /**
     * Constructs the About Us panel layout.
     */
    public AboutUsPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        initComponents();
    }

    /**
     * Builds the vertical stack containing the branding card, runtime specifications card,
     * and offline privacy philosophy card.
     */
    private void initComponents() {
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        int row = 0;

        // 1. Brand Banner Card
        gbc.gridy = row++;
        content.add(createBrandCard(), gbc);

        // 2. System & Architecture Card
        gbc.gridy = row++;
        content.add(createSystemCard(), gbc);

        // 3. Vision & Privacy Guarantee Card
        gbc.gridy = row++;
        content.add(createVisionCard(), gbc);

        JScrollPane sp = new JScrollPane(content);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        add(sp, BorderLayout.CENTER);
    }

    /**
     * Creates the top branding banner card featuring application name, version badge, and tagline.
     *
     * @return styled brand card {@link JPanel}
     */
    private JPanel createBrandCard() {
        JPanel card = new JPanel(new BorderLayout(16, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(UiUtils.createCardBorder());

        JLabel lblLogo = new JLabel(NavIcons.appLogo(56), JLabel.CENTER);
        card.add(lblLogo, BorderLayout.WEST);

        JPanel info = new JPanel(new BorderLayout(0, 6));
        info.setOpaque(false);

        JLabel lblTitle = new JLabel(AppConfig.APP_NAME + " - Personal Finance Manager");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(new Color(15, 23, 42));

        JLabel lblTag = new JLabel("Version " + AppConfig.APP_VERSION + " | Production Release | Windows 64-bit");
        lblTag.setFont(UiUtils.FONT_BOLD);
        lblTag.setForeground(UiUtils.COLOR_PRIMARY);

        JLabel lblDesc = new JLabel("<html>A state-of-the-art offline desktop application crafted to provide households and professionals with complete clarity, privacy, and control over income, expenses, recurring bills, EMIs, and personal loans.</html>");
        lblDesc.setFont(UiUtils.FONT_REGULAR);
        lblDesc.setForeground(UiUtils.COLOR_TEXT_MUTED);

        info.add(lblTitle, BorderLayout.NORTH);
        info.add(lblTag, BorderLayout.CENTER);
        info.add(lblDesc, BorderLayout.SOUTH);

        card.add(info, BorderLayout.CENTER);
        return card;
    }

    /**
     * Creates the system specifications card presenting Java, OS, H2 database, and library versions.
     *
     * @return specifications {@link JPanel}
     */
    private JPanel createSystemCard() {
        JPanel card = new JPanel(new BorderLayout(10, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(UiUtils.createCardBorder());

        JLabel lblHeader = new JLabel("Technology & Runtime Specifications", NavIcons.settings(16, UiUtils.COLOR_PRIMARY), JLabel.LEFT);
        lblHeader.setFont(UiUtils.FONT_HEADER);
        card.add(lblHeader, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        addSpecRow(grid, gbc, 0, "Java Runtime:", System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        addSpecRow(grid, gbc, 1, "Operating System:", System.getProperty("os.name") + " " + System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")");
        addSpecRow(grid, gbc, 2, "Database Engine:", "H2 Embedded Engine 2.2 (Multi-process, AUTO_SERVER=TRUE)");
        addSpecRow(grid, gbc, 3, "Database Location:", AppConfig.APP_DATA_DIR.getAbsolutePath());
        addSpecRow(grid, gbc, 4, "UI Look & Feel:", "FlatLaf 3.5 with High-DPI Scaling & Dynamic Zooming");
        addSpecRow(grid, gbc, 5, "Reporting Engines:", "Apache POI 5.2 (Excel .xlsx) & OpenPDF 1.3 (PDF)");
        addSpecRow(grid, gbc, 6, "License:", "MIT License - Free and Open Source");

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    /**
     * Helper appending a label/value specification row into a 2-column GridBag layout.
     *
     * @param panel target container
     * @param gbc layout constraints
     * @param row zero-based row index
     * @param label specification name
     * @param value specification detail value
     */
    private void addSpecRow(JPanel panel, GridBagConstraints gbc, int row, String label, String value) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.25;
        JLabel lbl = new JLabel(label);
        lbl.setFont(UiUtils.FONT_BOLD);
        lbl.setForeground(new Color(51, 65, 85));
        panel.add(lbl, gbc);

        gbc.gridx = 1; gbc.gridy = row; gbc.weightx = 0.75;
        JLabel val = new JLabel(value);
        val.setFont(UiUtils.FONT_REGULAR);
        panel.add(val, gbc);
    }

    /**
     * Creates the philosophy card detailing zero-telemetry and local-first data ownership guarantees.
     *
     * @return philosophy card {@link JPanel}
     */
    private JPanel createVisionCard() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(UiUtils.createCardBorder());

        JLabel lblHeader = new JLabel("Privacy-First & Offline Philosophy", NavIcons.check(16, UiUtils.COLOR_INCOME), JLabel.LEFT);
        lblHeader.setFont(UiUtils.FONT_HEADER);
        card.add(lblHeader, BorderLayout.NORTH);

        JLabel lblText = new JLabel("<html>" +
                "<p>Unlike cloud-based budgeting trackers that sell behavioral data or require monthly subscriptions, <b>Nav Finance</b> is 100% offline, local-first, and private.</p><br>" +
                "<p>• <b>Your Data Stays With You:</b> All tables, logs, and transactions are stored directly inside your user directory (<code>~user/nav-finance/</code>).</p>" +
                "<p>• <b>No Internet Connection Required:</b> Operates completely autonomously with zero telemetries.</p>" +
                "<p>• <b>Easy Backups:</b> You can backup the database file or use the built-in one-click CSV export anytime.</p>" +
                "<p>• <b>Audit Trail:</b> Full accountability with immutable modification logs and timestamps.</p>" +
                "</html>");
        lblText.setFont(UiUtils.FONT_REGULAR);
        card.add(lblText, BorderLayout.CENTER);

        return card;
    }
}
