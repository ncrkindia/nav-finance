package com.navfinance.ui.panels;

import com.navfinance.config.AppConfig;
import com.navfinance.database.DatabaseConfig;
import com.navfinance.ui.MainFrame;
import com.navfinance.ui.components.ResponsiveBarPanel;
import com.navfinance.ui.util.NavIcons;
import com.navfinance.ui.util.UiUtils;
import com.navfinance.ui.util.ZoomManager;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;

/**
 * Executive About Us and System Diagnostics Center.
 * <p>
 * Displays application provenance, technology architecture, live JVM memory utilization,
 * active database engine status and ping latency, local storage footprint, and privacy guarantees.
 * Provides interactive shortcuts for native file explorer directory inspection, user guide navigation,
 * settings switching, and clipboard diagnostic exports.
 * </p>
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public class AboutUsPanel extends JPanel {

    // Live metrics components
    private JProgressBar memoryBar;
    private JLabel lblMemoryDetail;
    private JLabel lblDbStatusBadge;
    private JLabel lblDbStorageSize;
    private JLabel lblCpuThreads;
    private JLabel lblUiScaling;

    /**
     * Constructs the About Us panel layout.
     */
    public AboutUsPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        initComponents();
        refreshLiveDiagnostics();
    }

    /**
     * Builds the vertical stack containing the branding card, live diagnostics card,
     * architectural pillars grid, and privacy/license compliance card.
     */
    private void initComponents() {
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        int row = 0;

        // 1. Executive Brand & Quick Action Hero Banner
        gbc.gridy = row++;
        content.add(createBrandCard(), gbc);

        // 2. Live System Health & Runtime Diagnostics Card
        gbc.gridy = row++;
        content.add(createDiagnosticsCard(), gbc);

        // 3. Architectural Pillars & Core Capabilities Grid
        gbc.gridy = row++;
        content.add(createPillarsCard(), gbc);

        // 4. Privacy-First Philosophy & License Card
        gbc.gridy = row++;
        content.add(createVisionAndLicenseCard(), gbc);

        JScrollPane sp = new JScrollPane(content);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        add(sp, BorderLayout.CENTER);
    }

    /**
     * Creates the top branding banner card featuring application logo, version badge,
     * description, and quick interactive navigation buttons.
     *
     * @return styled brand card {@link JPanel}
     */
    private JPanel createBrandCard() {
        JPanel card = new JPanel(new BorderLayout(16, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(UiUtils.createCardBorder());

        // West: Circular Logo
        JLabel lblLogo = new JLabel(NavIcons.appLogo(60), JLabel.CENTER);
        card.add(lblLogo, BorderLayout.WEST);

        // Center: App Title, Badges, and Description
        JPanel info = new JPanel(new BorderLayout(0, 6));
        info.setOpaque(false);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleRow.setOpaque(false);

        JLabel lblTitle = new JLabel(AppConfig.APP_NAME);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(new Color(15, 23, 42));
        titleRow.add(lblTitle);

        JLabel lblVersionBadge = new JLabel("v" + AppConfig.APP_VERSION + " Production Release");
        lblVersionBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblVersionBadge.setOpaque(true);
        lblVersionBadge.setBackground(new Color(238, 242, 255));
        lblVersionBadge.setForeground(new Color(67, 56, 202));
        lblVersionBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(199, 210, 254), 1, true),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        titleRow.add(lblVersionBadge);

        JLabel lblDesktopBadge = new JLabel("Offline Desktop Edition");
        lblDesktopBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblDesktopBadge.setOpaque(true);
        lblDesktopBadge.setBackground(new Color(240, 253, 244));
        lblDesktopBadge.setForeground(new Color(22, 101, 52));
        lblDesktopBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(187, 247, 208), 1, true),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        titleRow.add(lblDesktopBadge);

        info.add(titleRow, BorderLayout.NORTH);

        JLabel lblSubtitle = new JLabel(AppConfig.APP_SUBTITLE);
        lblSubtitle.setFont(UiUtils.boldFont());
        lblSubtitle.setForeground(UiUtils.COLOR_PRIMARY);

        JLabel lblDesc = new JLabel("<html>A state-of-the-art offline financial operating system crafted to provide households and professionals with complete clarity, privacy, and control over income, expenses, recurring bills, EMIs, and personal loans. Built with zero-telemetry and a multi-engine relational architecture.</html>");
        lblDesc.setFont(UiUtils.regularFont());
        lblDesc.setForeground(UiUtils.COLOR_TEXT_MUTED);

        JPanel textStack = new JPanel(new BorderLayout(0, 4));
        textStack.setOpaque(false);
        textStack.add(lblSubtitle, BorderLayout.NORTH);
        textStack.add(lblDesc, BorderLayout.CENTER);
        info.add(textStack, BorderLayout.CENTER);

        // South: Quick interactive actions bar
        ResponsiveBarPanel actionPanel = new ResponsiveBarPanel(8, 8);

        JButton btnOpenFolder = UiUtils.createSecondaryButton("Open App Data Folder", NavIcons.importIcon(13, UiUtils.COLOR_PRIMARY));
        btnOpenFolder.setToolTipText("Open the application data folder in native Windows Explorer (~/.nav-finance)");
        btnOpenFolder.addActionListener(e -> onOpenAppDataFolder());

        JButton btnUserGuide = UiUtils.createSecondaryButton("User Manual & Guide", NavIcons.userGuide(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnUserGuide.addActionListener(e -> {
            MainFrame frame = MainFrame.getInstance();
            if (frame != null) frame.switchToTab(6);
        });

        JButton btnSettings = UiUtils.createSecondaryButton("Database & Settings", NavIcons.settings(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnSettings.addActionListener(e -> {
            MainFrame frame = MainFrame.getInstance();
            if (frame != null) frame.switchToTab(8);
        });

        JButton btnCopyDiag = UiUtils.createSecondaryButton("Copy System Snapshot", NavIcons.link(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnCopyDiag.setToolTipText("Copy runtime diagnostic specs to system clipboard");
        btnCopyDiag.addActionListener(e -> onCopyDiagnostics());

        actionPanel.addLeft(btnOpenFolder);
        actionPanel.addLeft(btnUserGuide);
        actionPanel.addLeft(btnSettings);
        actionPanel.addRight(btnCopyDiag);

        info.add(actionPanel, BorderLayout.SOUTH);
        card.add(info, BorderLayout.CENTER);

        return card;
    }

    /**
     * Creates the live diagnostic card reporting JVM memory allocation, active database engine
     * ping latency, storage size, and processor capabilities.
     *
     * @return interactive diagnostics {@link JPanel}
     */
    private JPanel createDiagnosticsCard() {
        JPanel card = new JPanel(new BorderLayout(12, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(UiUtils.createCardBorder());

        // Header: Title and Refresh Button
        ResponsiveBarPanel cardHeader = new ResponsiveBarPanel(8, 6);
        JLabel lblHeader = new JLabel("System Health & Live Runtime Diagnostics", NavIcons.dashboard(16, UiUtils.COLOR_PRIMARY), JLabel.LEFT);
        lblHeader.setFont(UiUtils.headerFont());
        cardHeader.addLeft(lblHeader);

        JButton btnRefresh = UiUtils.createSecondaryButton("Refresh Diagnostics", NavIcons.refresh(12, UiUtils.COLOR_TEXT_PRIMARY));
        btnRefresh.addActionListener(e -> refreshLiveDiagnostics());
        cardHeader.addRight(btnRefresh);

        card.add(cardHeader, BorderLayout.NORTH);

        // Body: 2 Column Grid for JVM Memory Meter and Database Engine Status
        JPanel body = new JPanel(new GridLayout(1, 2, 16, 0));
        body.setOpaque(false);

        // Left Subcard: JVM Memory
        JPanel pnlMemory = new JPanel(new BorderLayout(0, 8));
        pnlMemory.setBackground(new Color(248, 250, 252));
        pnlMemory.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiUtils.COLOR_BORDER, 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JLabel lblMemTitle = new JLabel("JVM Heap Memory Utilization", NavIcons.statusDot(8, UiUtils.COLOR_INCOME), JLabel.LEFT);
        lblMemTitle.setFont(UiUtils.boldFont());
        pnlMemory.add(lblMemTitle, BorderLayout.NORTH);

        memoryBar = new JProgressBar(0, 100);
        memoryBar.setStringPainted(true);
        memoryBar.setPreferredSize(new Dimension(100, 20));
        memoryBar.setForeground(UiUtils.COLOR_PRIMARY);

        lblMemoryDetail = new JLabel("Calculating heap memory...");
        lblMemoryDetail.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblMemoryDetail.setForeground(UiUtils.COLOR_TEXT_MUTED);

        JPanel memStack = new JPanel(new BorderLayout(0, 6));
        memStack.setOpaque(false);
        memStack.add(memoryBar, BorderLayout.CENTER);
        memStack.add(lblMemoryDetail, BorderLayout.SOUTH);
        pnlMemory.add(memStack, BorderLayout.CENTER);

        body.add(pnlMemory);

        // Right Subcard: Database & Storage Health
        JPanel pnlDbHealth = new JPanel(new GridBagLayout());
        pnlDbHealth.setBackground(new Color(248, 250, 252));
        pnlDbHealth.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiUtils.COLOR_BORDER, 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 4, 3, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // DB Status Row
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.35;
        JLabel lblDbLabel = new JLabel("Database Engine:");
        lblDbLabel.setFont(UiUtils.boldFont());
        pnlDbHealth.add(lblDbLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        lblDbStatusBadge = new JLabel("Testing connection...");
        lblDbStatusBadge.setFont(UiUtils.boldFont());
        pnlDbHealth.add(lblDbStatusBadge, gbc);

        // DB File Size Row
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.35;
        JLabel lblSizeLabel = new JLabel("Storage Footprint:");
        lblSizeLabel.setFont(UiUtils.boldFont());
        pnlDbHealth.add(lblSizeLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        lblDbStorageSize = new JLabel("Scanning disk...");
        lblDbStorageSize.setFont(UiUtils.regularFont());
        pnlDbHealth.add(lblDbStorageSize, gbc);

        // CPU & Threads Row
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.35;
        JLabel lblCpuLabel = new JLabel("CPU & Threads:");
        lblCpuLabel.setFont(UiUtils.boldFont());
        pnlDbHealth.add(lblCpuLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        lblCpuThreads = new JLabel(Runtime.getRuntime().availableProcessors() + " Cores | " + Thread.activeCount() + " Active Threads");
        lblCpuThreads.setFont(UiUtils.regularFont());
        pnlDbHealth.add(lblCpuThreads, gbc);

        // UI Scaling Row
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.35;
        JLabel lblScaleLabel = new JLabel("UI Scaling:");
        lblScaleLabel.setFont(UiUtils.boldFont());
        pnlDbHealth.add(lblScaleLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 0.65;
        lblUiScaling = new JLabel("FlatLaf Light (" + Math.round(ZoomManager.getCurrentScale() * 100) + "% DPI Zoom)");
        lblUiScaling.setFont(UiUtils.regularFont());
        pnlDbHealth.add(lblUiScaling, gbc);

        body.add(pnlDbHealth);
        card.add(body, BorderLayout.CENTER);

        return card;
    }

    /**
     * Refreshes real-time JVM heap numbers, database ping status, and disk file sizes.
     */
    public void refreshLiveDiagnostics() {
        // 1. JVM Memory
        Runtime rt = Runtime.getRuntime();
        long maxMb = rt.maxMemory() / (1024 * 1024);
        long totalMb = rt.totalMemory() / (1024 * 1024);
        long freeMb = rt.freeMemory() / (1024 * 1024);
        long usedMb = totalMb - freeMb;
        int pct = (int) Math.min(100, Math.max(0, (usedMb * 100) / Math.max(1, maxMb)));

        if (memoryBar != null) {
            memoryBar.setValue(pct);
            memoryBar.setString(usedMb + " MB / " + maxMb + " MB (" + pct + "% Used)");
            if (pct > 80) {
                memoryBar.setForeground(UiUtils.COLOR_EXPENSE);
            } else if (pct > 60) {
                memoryBar.setForeground(UiUtils.COLOR_AMBER);
            } else {
                memoryBar.setForeground(UiUtils.COLOR_PRIMARY);
            }
        }
        if (lblMemoryDetail != null) {
            lblMemoryDetail.setText("Total Allocated: " + totalMb + " MB | Available Free: " + freeMb + " MB | Peak Heap: " + maxMb + " MB");
        }

        // 2. CPU and Threads
        if (lblCpuThreads != null) {
            lblCpuThreads.setText(Runtime.getRuntime().availableProcessors() + " CPU Cores | " + Thread.activeCount() + " Active Threads");
        }

        // 3. UI Scaling
        if (lblUiScaling != null) {
            lblUiScaling.setText("FlatLaf High-DPI Engine (" + Math.round(ZoomManager.getCurrentScale() * 100) + "% Zoom)");
        }

        // 4. Database Disk Size
        DatabaseConfig active = AppConfig.getActiveDatabaseConfig();
        if (lblDbStorageSize != null) {
            if (active.getType().isEmbeddedOrLocal()) {
                File dbFile = new File(AppConfig.DB_PATH + ".mv.db");
                if (dbFile.exists()) {
                    long bytes = dbFile.length();
                    String sizeStr = bytes > 1024 * 1024
                            ? String.format("%.2f MB", bytes / (1024.0 * 1024.0))
                            : String.format("%.1f KB", bytes / 1024.0);
                    lblDbStorageSize.setText("navfinance_db.mv.db (" + sizeStr + " on disk)");
                } else {
                    lblDbStorageSize.setText("Local storage active (~/.nav-finance)");
                }
            } else {
                lblDbStorageSize.setText("Client-Server (" + active.getHost() + ":" + active.getPort() + "/" + active.getDatabaseName() + ")");
            }
        }

        // 5. Database Ping Test (Async non-blocking)
        if (lblDbStatusBadge != null) {
            lblDbStatusBadge.setText("● Pinging " + active.getType().getTypeName() + "...");
            lblDbStatusBadge.setForeground(UiUtils.COLOR_TEXT_MUTED);

            SwingWorker<DatabaseConfig.ConnectionTestResult, Void> worker = new SwingWorker<>() {
                @Override
                protected DatabaseConfig.ConnectionTestResult doInBackground() {
                    return active.testConnection();
                }

                @Override
                protected void done() {
                    try {
                        DatabaseConfig.ConnectionTestResult res = get();
                        if (res.success()) {
                            lblDbStatusBadge.setText("● " + active.getType().getTypeName() + " Connected (" + res.latencyMs() + " ms)");
                            lblDbStatusBadge.setForeground(UiUtils.COLOR_INCOME);
                        } else {
                            lblDbStatusBadge.setText("✗ " + active.getType().getTypeName() + " (Offline: " + res.message() + ")");
                            lblDbStatusBadge.setForeground(UiUtils.COLOR_EXPENSE);
                        }
                    } catch (Exception ex) {
                        lblDbStatusBadge.setText("✗ " + active.getType().getTypeName() + " Error");
                        lblDbStatusBadge.setForeground(UiUtils.COLOR_EXPENSE);
                    }
                }
            };
            worker.execute();
        }
    }

    /**
     * Builds the architectural pillars card detailing the 4 core engineering capabilities.
     *
     * @return architectural pillars {@link JPanel}
     */
    private JPanel createPillarsCard() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(UiUtils.createCardBorder());

        JLabel lblHeader = new JLabel("Engineering Architecture & Core Capabilities", NavIcons.settings(16, UiUtils.COLOR_PRIMARY), JLabel.LEFT);
        lblHeader.setFont(UiUtils.headerFont());
        card.add(lblHeader, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 2, 14, 14));
        grid.setOpaque(false);

        grid.add(createPillarBox("Zero-Telemetry Local Privacy",
                "100% offline, private financial operating system. All ledgers, audit trails, and profiles remain strictly inside your personal home directory (~/.nav-finance) with zero cloud sync or behavioral monetization.",
                NavIcons.checkCircle(18, UiUtils.COLOR_INCOME)));

        grid.add(createPillarBox("Multi-Engine Database Support",
                "Universal relational layer supporting embedded zero-config H2 and SQLite, alongside enterprise client-server engines (MySQL 8.x, PostgreSQL 12+, MariaDB). Seamless live engine switching and automated migrations.",
                NavIcons.database(18, UiUtils.COLOR_PRIMARY)));

        grid.add(createPillarBox("Smart Bi-Directional Linking",
                "First-class separation of returnable loan balances (Given/Taken) from non-returnable liabilities (Credit Card bills, Utility bills, EMIs). Auto-linking engine calculates net counterparty positions and partial settlements.",
                NavIcons.link(18, UiUtils.COLOR_AMBER)));

        grid.add(createPillarBox("Tri-Script Typography & Reporting",
                "Native multilingual UI rendering in English, Devanagari Hindi via Nirmala UI, and phonetic Hinglish. Dual export engines generate pixel-perfect Excel spreadsheets with formulas and OpenPDF vectorized summaries.",
                NavIcons.export(18, new Color(2, 132, 199))));

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JPanel createPillarBox(String title, String desc, javax.swing.Icon icon) {
        JPanel box = new JPanel(new BorderLayout(8, 6));
        box.setBackground(new Color(248, 250, 252));
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiUtils.COLOR_BORDER, 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JLabel lblTitle = new JLabel(title, icon, JLabel.LEFT);
        lblTitle.setFont(UiUtils.boldFont());
        lblTitle.setForeground(new Color(15, 23, 42));

        JLabel lblDesc = new JLabel("<html><span style='color:#64748b; font-size:11px;'>" + desc + "</span></html>");
        lblDesc.setFont(UiUtils.regularFont());

        box.add(lblTitle, BorderLayout.NORTH);
        box.add(lblDesc, BorderLayout.CENTER);
        return box;
    }

    /**
     * Builds the vision, backup guarantees, and license compliance card.
     *
     * @return vision and license card {@link JPanel}
     */
    private JPanel createVisionAndLicenseCard() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(UiUtils.createCardBorder());

        JLabel lblHeader = new JLabel("Data Ownership, Audit Trail & Open Source License", NavIcons.info(16, UiUtils.COLOR_PRIMARY), JLabel.LEFT);
        lblHeader.setFont(UiUtils.headerFont());
        card.add(lblHeader, BorderLayout.NORTH);

        JPanel details = new JPanel(new GridLayout(1, 2, 14, 0));
        details.setOpaque(false);

        JLabel lblLeft = new JLabel("<html><div style='color:#334155; font-size:11px; line-height:1.4;'>"
                + "<b>Data Safety & Backup Architecture:</b><br/>"
                + "• <b>Directory:</b> <code>" + AppConfig.APP_DATA_DIR.getAbsolutePath() + "</code><br/>"
                + "• <b>Primary Storage:</b> <code>navfinance_db.mv.db</code> (Multi-version concurrency, AUTO_SERVER enabled)<br/>"
                + "• <b>Full Backup:</b> Automated CSV export for all 10 core tables with universal format interoperability.<br/>"
                + "• <b>Audit Trail:</b> Immutable timestamps, operation types, and change records for complete audit compliance."
                + "</div></html>");
        lblLeft.setFont(UiUtils.regularFont());

        JLabel lblRight = new JLabel("<html><div style='color:#334155; font-size:11px; line-height:1.4;'>"
                + "<b>Open Source & Technology Stack:</b><br/>"
                + "• <b>License:</b> MIT License (Permissive, free for personal and commercial usage)<br/>"
                + "• <b>Runtime:</b> Java " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ") on " + System.getProperty("os.name") + "<br/>"
                + "• <b>Libraries:</b> FlatLaf 3.5, Apache POI 5.2.5, OpenPDF 1.3.39, H2 Database 2.2.224<br/>"
                + "• <b>Engineering:</b> Crafted with pride by Nav Finance Team &bull; Version " + AppConfig.APP_VERSION + "<br/>"
                + "• <b>Support:</b> Fully autonomous local ledger requiring zero external web servers."
                + "</div></html>");
        lblRight.setFont(UiUtils.regularFont());

        details.add(lblLeft);
        details.add(lblRight);
        card.add(details, BorderLayout.CENTER);

        return card;
    }

    /**
     * Opens the application home data directory in the host OS's native file explorer.
     */
    private void onOpenAppDataFolder() {
        try {
            if (!AppConfig.APP_DATA_DIR.exists()) {
                AppConfig.APP_DATA_DIR.mkdirs();
            }
            java.awt.Desktop.getDesktop().open(AppConfig.APP_DATA_DIR);
        } catch (Exception ex) {
            UiUtils.showError(this, "Could not open data directory automatically: " + AppConfig.APP_DATA_DIR.getAbsolutePath());
        }
    }

    /**
     * Copies a complete diagnostic report of runtime, Java, database, and storage parameters to the system clipboard.
     */
    private void onCopyDiagnostics() {
        try {
            Runtime rt = Runtime.getRuntime();
            long maxMb = rt.maxMemory() / (1024 * 1024);
            long totalMb = rt.totalMemory() / (1024 * 1024);
            long freeMb = rt.freeMemory() / (1024 * 1024);
            long usedMb = totalMb - freeMb;

            DatabaseConfig dbConfig = AppConfig.getActiveDatabaseConfig();

            String diag = "=== Nav Finance System Diagnostics ===\n"
                    + "Application: " + AppConfig.APP_NAME + " v" + AppConfig.APP_VERSION + "\n"
                    + "Java Runtime: " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")\n"
                    + "Operating System: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")\n"
                    + "Available CPU Cores: " + rt.availableProcessors() + "\n"
                    + "Active Threads: " + Thread.activeCount() + "\n"
                    + "JVM Heap Memory: " + usedMb + " MB used / " + maxMb + " MB max (Allocated: " + totalMb + " MB, Free: " + freeMb + " MB)\n"
                    + "UI Scaling: " + Math.round(ZoomManager.getCurrentScale() * 100) + "% (FlatLaf Light)\n"
                    + "Active Database: " + dbConfig.getType().getTypeName() + "\n"
                    + "Effective JDBC URL: " + dbConfig.getEffectiveJdbcUrl() + "\n"
                    + "App Data Directory: " + AppConfig.APP_DATA_DIR.getAbsolutePath() + "\n"
                    + "Timestamp: " + java.time.LocalDateTime.now() + "\n"
                    + "======================================\n";

            StringSelection sel = new StringSelection(diag);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
            UiUtils.showSuccess(this, "System diagnostic details copied to clipboard!");
        } catch (Exception ex) {
            UiUtils.showError(this, "Failed to copy diagnostics: " + ex.getMessage());
        }
    }
}
