package com.navfinance.ui;

import com.navfinance.config.AppConfig;
import com.navfinance.model.TransactionType;
import com.navfinance.ui.dialogs.TransactionDialog;
import com.navfinance.ui.panels.AboutUsPanel;
import com.navfinance.ui.panels.AuditLogPanel;
import com.navfinance.ui.panels.DashboardPanel;
import com.navfinance.ui.panels.DuePaymentsPanel;
import com.navfinance.ui.panels.LoansDuesPanel;
import com.navfinance.ui.panels.MastersPanel;
import com.navfinance.ui.panels.SettingsPanel;
import com.navfinance.ui.panels.TransactionsPanel;
import com.navfinance.ui.panels.UserGuidePanel;
import com.navfinance.i18n.AppLanguage;
import com.navfinance.i18n.I18n;
import com.navfinance.i18n.LanguageChangeListener;
import com.navfinance.ui.util.NavIcons;
import com.navfinance.ui.util.UiUtils;
import com.navfinance.ui.util.ZoomManager;

import java.time.LocalDate;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * Main application frame and primary navigation window for NavFinance.
 * <p>
 * This frame organizes and orchestrates the user interface into three distinct regions:
 * <ul>
 *   <li><b>Top Header Bar:</b> Application branding, responsive zoom controllers (80% to 175%),
 *       and quick action shortcut buttons ({@code + New Income}, {@code - New Expense}, {@code 🔄 Refresh}).</li>
 *   <li><b>Center Workspace (Tabbed Navigation):</b> Houses the 9 core functional modules:
 *     <ol>
 *       <li><b>Dashboard:</b> Financial KPIs, income vs expense trend bar charts, and category donut distribution.</li>
 *       <li><b>Transactions:</b> Searchable income/expense ledger with multi-column sorting and report exports.</li>
 *       <li><b>Due &amp; Recurring:</b> Tracking due and overdue bills/EMIs with one-click settlements.</li>
 *       <li><b>Loans &amp; Dues:</b> Personal loans ledger tracking counterparty balances.</li>
 *       <li><b>Masters:</b> Management of reusable catalogs (Persons, Bills, EMIs, Cards, Modes).</li>
 *       <li><b>Audit Trail:</b> Immutable historical activity log of all inserts, updates, and deletes.</li>
 *       <li><b>User Guide:</b> Interactive searchable documentation handbook.</li>
 *       <li><b>About Us:</b> Developer credits, system specifications, and technology architecture.</li>
 *       <li><b>Settings &amp; Database:</b> H2 database diagnostics, full backups, themes, and currency symbols.</li>
 *     </ol>
 *   </li>
 *   <li><b>Bottom Status Bar:</b> Real-time indicators of embedded H2 connection health and active runtime version.</li>
 * </ul>
 * Also registers global keyboard shortcuts for UI scaling:
 * <ul>
 *   <li>{@code Ctrl + Plus} or {@code Ctrl + =}: Zoom In</li>
 *   <li>{@code Ctrl + Minus}: Zoom Out</li>
 *   <li>{@code Ctrl + 0}: Reset Zoom to 100%</li>
 * </ul>
 */
public class MainFrame extends JFrame implements LanguageChangeListener {

    private static MainFrame instance;

    /** Primary tabbed container hosting the application's functional modules. */
    private JTabbedPane mainTabs;

    /** Panel displaying aggregated metrics, financial charts, and KPI summary cards. */
    private DashboardPanel dashboardPanel;

    /** Panel managing the itemized transactional income and expense ledger. */
    private TransactionsPanel transactionsPanel;

    /** Panel tracking upcoming, due, and overdue payment obligations and recurring rules. */
    private DuePaymentsPanel duePaymentsPanel;

    /** Panel tracking personal loans and informal debts across individual persons. */
    private LoansDuesPanel loansDuesPanel;

    /** Panel managing reference catalogs for Persons, Bills, EMIs, Cards, and Modes. */
    private MastersPanel mastersPanel;

    /** Panel rendering immutable chronological audit event logs. */
    private AuditLogPanel auditPanel;

    /** Panel providing diagnostics, backups, Look and Feel switching, and preferences. */
    private SettingsPanel settingsPanel;

    /** Panel rendering interactive offline user manual chapters. */
    private UserGuidePanel userGuidePanel;

    /** Panel displaying application metadata, author details, and architecture notes. */
    private AboutUsPanel aboutUsPanel;

    private JLabel lblAppName;
    private JLabel lblAppSub;
    private JLabel lblZoom;
    private JLabel lblDbStatus;
    private JLabel lblVersion;
    private JButton btnZoomReset;
    private JButton btnQuickIncome;
    private JButton btnQuickExpense;
    private JButton btnRefreshAll;
    private JComboBox<AppLanguage> cmbLanguage;
    private boolean updatingLanguageCombo = false;

    /**
     * Constructs the main application window, initializing layout managers,
     * child panels, global key bindings, and centering the frame on screen.
     */
    public MainFrame() {
        super(I18n.get("app.name") + " - " + I18n.get("app.subtitle"));
        instance = this;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 750));
        setPreferredSize(new Dimension(1280, 840));

        loadAppIcon();
        initComponents();
        setupKeyBindings();
        I18n.addListener(this);
        onLanguageChanged(I18n.getCurrentLanguage());
        com.navfinance.database.DatabaseManager.getInstance().addDatabaseChangeListener(this::onDatabaseChanged);
        updateDbStatus();
        if (ZoomManager.getCurrentScale() != 1.0f) {
            ZoomManager.applyZoom(this);
        }
        pack();
        restoreWindowBounds();
        setupWindowBoundsPersistence();
    }

    /**
     * Loads the window frame and taskbar application icon from classpath resources.
     */
    private void loadAppIcon() {
        try {
            java.net.URL iconUrl = getClass().getResource("/icon.png");
            if (iconUrl != null) {
                setIconImage(javax.imageio.ImageIO.read(iconUrl));
            }
        } catch (java.io.IOException ignored) {
            // Graceful fallback to standard host platform icon
        }
    }

    /**
     * Instantiates and arranges all visual elements, top action bars,
     * central tabbed panels, and bottom status bar.
     */
    private void initComponents() {
        setLayout(new BorderLayout());

        // TOP APP HEADER
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiUtils.COLOR_BORDER),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));

        // Brand Title
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brandPanel.setOpaque(false);

        JLabel lblLogo = new JLabel(NavIcons.appLogo(28));
        brandPanel.add(lblLogo);

        JPanel titleBlock = new JPanel(new BorderLayout());
        titleBlock.setOpaque(false);
        lblAppName = new JLabel(I18n.get("app.name"));
        lblAppName.setFont(UiUtils.titleFont());
        lblAppName.setForeground(new Color(15, 23, 42));

        lblAppSub = new JLabel(I18n.get("app.subtitle"));
        lblAppSub.setFont(UiUtils.subtitleFont());
        lblAppSub.setForeground(UiUtils.COLOR_TEXT_MUTED);

        titleBlock.add(lblAppName, BorderLayout.NORTH);
        titleBlock.add(lblAppSub, BorderLayout.SOUTH);
        brandPanel.add(titleBlock);

        // Header Action Buttons + Zoom Controls
        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerActions.setOpaque(false);

        // Language Selector Dropdown
        JPanel langPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        langPanel.setOpaque(false);
        JLabel lblLangIcon = new JLabel(NavIcons.language(16, UiUtils.COLOR_PRIMARY));
        cmbLanguage = new JComboBox<>(AppLanguage.values());
        cmbLanguage.setFont(UiUtils.FONT_REGULAR);
        cmbLanguage.setSelectedItem(I18n.getCurrentLanguage());
        cmbLanguage.setToolTipText("Switch Language / भाषा बदलें");
        cmbLanguage.addActionListener(e -> {
            if (updatingLanguageCombo) return;
            AppLanguage selected = (AppLanguage) cmbLanguage.getSelectedItem();
            if (selected != null && selected != I18n.getCurrentLanguage()) {
                I18n.setLanguage(selected);
            }
        });
        langPanel.add(lblLangIcon);
        langPanel.add(cmbLanguage);
        headerActions.add(langPanel);

        headerActions.add(new JLabel("  |  "));

        // Zoom Controller Bar
        JPanel zoomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        zoomBar.setOpaque(false);
        lblZoom = new JLabel("Zoom:");
        lblZoom.setFont(UiUtils.FONT_SUBTITLE);
        lblZoom.setForeground(UiUtils.COLOR_TEXT_MUTED);
        zoomBar.add(lblZoom);

        JButton btnZoomOut = UiUtils.createSecondaryButton("", NavIcons.zoomOut(14, UiUtils.COLOR_TEXT_MUTED));
        btnZoomOut.setToolTipText("Zoom Out (Ctrl + Minus)");
        btnZoomOut.addActionListener(e -> ZoomManager.zoomOut(this));

        btnZoomReset = UiUtils.createSecondaryButton(ZoomManager.getZoomLabel(), NavIcons.zoomReset(13, UiUtils.COLOR_TEXT_MUTED));
        btnZoomReset.setFont(UiUtils.FONT_BOLD);
        btnZoomReset.setToolTipText("Reset Zoom (Ctrl + 0)");
        btnZoomReset.addActionListener(e -> ZoomManager.resetZoom(this));

        JButton btnZoomIn = UiUtils.createSecondaryButton("", NavIcons.zoomIn(14, UiUtils.COLOR_TEXT_MUTED));
        btnZoomIn.setToolTipText("Zoom In (Ctrl + Plus)");
        btnZoomIn.addActionListener(e -> ZoomManager.zoomIn(this));

        ZoomManager.addZoomListener(() -> btnZoomReset.setText(ZoomManager.getZoomLabel()));

        zoomBar.add(btnZoomOut);
        zoomBar.add(btnZoomReset);
        zoomBar.add(btnZoomIn);

        headerActions.add(zoomBar);
        headerActions.add(new JLabel("  |  "));

        // Quick Global Entry Action Buttons
        btnQuickIncome = UiUtils.createSuccessButton(I18n.get("action.recordIncome"), NavIcons.plus(13, Color.WHITE));
        btnQuickIncome.addActionListener(e -> {
            TransactionDialog d = new TransactionDialog(this, null, TransactionType.INCOME);
            d.setVisible(true);
            if (d.isSaved()) {
                refreshAllViews();
            }
        });

        btnQuickExpense = UiUtils.createDangerButton(I18n.get("action.recordExpense"), NavIcons.minus(13, Color.WHITE));
        btnQuickExpense.addActionListener(e -> {
            TransactionDialog d = new TransactionDialog(this, null, TransactionType.EXPENSE);
            d.setVisible(true);
            if (d.isSaved()) {
                refreshAllViews();
            }
        });

        btnRefreshAll = UiUtils.createSecondaryButton(I18n.get("action.refresh"), NavIcons.refresh(13, UiUtils.COLOR_PRIMARY));
        btnRefreshAll.addActionListener(e -> refreshAllViews());

        headerActions.add(btnQuickIncome);
        headerActions.add(btnQuickExpense);
        headerActions.add(btnRefreshAll);

        // Responsive header prevents collision between brand title and action controls
        com.navfinance.ui.components.ResponsiveBarPanel responsiveHeader =
                new com.navfinance.ui.components.ResponsiveBarPanel(brandPanel, headerActions, 16, 8, true);
        headerPanel.add(responsiveHeader, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

        // MAIN CONTENT: Tabbed Navigation
        mainTabs = new JTabbedPane(JTabbedPane.TOP);
        mainTabs.setFont(UiUtils.boldFont());

        dashboardPanel = new DashboardPanel();
        transactionsPanel = new TransactionsPanel();
        duePaymentsPanel = new DuePaymentsPanel();
        loansDuesPanel = new LoansDuesPanel();
        mastersPanel = new MastersPanel();
        auditPanel = new AuditLogPanel();
        userGuidePanel = new UserGuidePanel();
        aboutUsPanel = new AboutUsPanel();
        settingsPanel = new SettingsPanel(this::refreshAllViews);

        mainTabs.addTab(I18n.get("tab.dashboard"), NavIcons.dashboard(16, UiUtils.COLOR_PRIMARY), dashboardPanel);
        mainTabs.addTab(I18n.get("tab.transactions"), NavIcons.transactions(16, UiUtils.COLOR_PRIMARY), transactionsPanel);
        mainTabs.addTab(I18n.get("tab.duePayments"), NavIcons.duePayments(16, UiUtils.COLOR_AMBER), duePaymentsPanel);
        mainTabs.addTab(I18n.get("tab.loans"), NavIcons.loan(16, UiUtils.COLOR_INCOME), loansDuesPanel);
        mainTabs.addTab(I18n.get("tab.masters"), NavIcons.masters(16, UiUtils.COLOR_PRIMARY), mastersPanel);
        mainTabs.addTab(I18n.get("tab.audit"), NavIcons.audit(16, UiUtils.COLOR_TEXT_MUTED), auditPanel);
        mainTabs.addTab(I18n.get("tab.userGuide"), NavIcons.userGuide(16, UiUtils.COLOR_PRIMARY), userGuidePanel);
        mainTabs.addTab(I18n.get("tab.aboutUs"), NavIcons.aboutUs(16, UiUtils.COLOR_PRIMARY), aboutUsPanel);
        mainTabs.addTab(I18n.get("tab.settings"), NavIcons.settings(16, UiUtils.COLOR_TEXT_MUTED), settingsPanel);

        // Auto refresh tab data on tab switch
        mainTabs.addChangeListener(e -> {
            switch (mainTabs.getSelectedIndex()) {
                case 0 -> dashboardPanel.refreshData();
                case 1 -> transactionsPanel.applyFilters();
                case 2 -> duePaymentsPanel.refreshAll();
                case 3 -> loansDuesPanel.refreshData();
                case 4 -> mastersPanel.refreshAll();
                case 5 -> auditPanel.refreshLogs();
                default -> {}
            }
        });

        add(mainTabs, BorderLayout.CENTER);

        // BOTTOM STATUS BAR
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(241, 245, 249));
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiUtils.COLOR_BORDER),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));

        lblDbStatus = new JLabel("● Database: Initializing...");
        lblDbStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblDbStatus.setForeground(new Color(15, 118, 110)); // Teal green dot

        lblVersion = new JLabel(AppConfig.APP_NAME + " v" + AppConfig.APP_VERSION + " | Windows Java 21");
        lblVersion.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblVersion.setForeground(UiUtils.COLOR_TEXT_MUTED);

        com.navfinance.ui.components.ResponsiveBarPanel responsiveStatus =
                new com.navfinance.ui.components.ResponsiveBarPanel(lblDbStatus, lblVersion, 12, 4, true);
        statusBar.add(responsiveStatus, BorderLayout.CENTER);

        add(statusBar, BorderLayout.SOUTH);
    }

    /**
     * Registers global keyboard shortcuts for responsive UI zooming.
     */
    private void setupKeyBindings() {
        // Zoom In: Ctrl + Plus, Ctrl + Equals
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK), "zoomIn");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK), "zoomIn");
        getRootPane().getActionMap().put("zoomIn", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ZoomManager.zoomIn(MainFrame.this);
            }
        });

        // Zoom Out: Ctrl + Minus
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK), "zoomOut");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK), "zoomOut");
        getRootPane().getActionMap().put("zoomOut", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ZoomManager.zoomOut(MainFrame.this);
            }
        });

        // Reset Zoom: Ctrl + 0
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK), "zoomReset");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0, InputEvent.CTRL_DOWN_MASK), "zoomReset");
        getRootPane().getActionMap().put("zoomReset", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ZoomManager.resetZoom(MainFrame.this);
            }
        });
    }

    /**
     * Refreshes the active database connection indicator label in the status bar.
     */
    private void updateDbStatus() {
        if (lblDbStatus == null) return;
        com.navfinance.database.DatabaseConfig cfg = AppConfig.getActiveDatabaseConfig();
        String typeName = cfg.getType().getDisplayName();
        String location = cfg.getType().isEmbeddedOrLocal() ?
                (cfg.getLocalPath().isBlank() ? AppConfig.DB_PATH : cfg.getLocalPath()) :
                (cfg.getHost() + ":" + cfg.getPort() + "/" + cfg.getDatabaseName());
        lblDbStatus.setText("● Database: " + typeName + " (" + location + ") | Connected");
    }

    /**
     * Responds to database switch events.
     */
    private void onDatabaseChanged() {
        updateDbStatus();
        refreshAllViews();
    }

    /**
     * Broadcasts a refresh command to all active panel models, recalculating KPIs,
     * re-running transaction filters, scanning recurring obligations, and updating audit logs.
     */
    public void refreshAllViews() {
        dashboardPanel.refreshData();
        transactionsPanel.applyFilters();
        duePaymentsPanel.refreshAll();
        loansDuesPanel.refreshData();
        mastersPanel.refreshAll();
        auditPanel.refreshLogs();
    }

    /**
     * Responds to dynamic language changes by updating frame titles, branding labels,
     * quick action buttons, and all 9 tab titles.
     *
     * @param newLanguage the newly selected {@link AppLanguage}
     */
    @Override
    public void onLanguageChanged(AppLanguage newLanguage) {
        updatingLanguageCombo = true;
        if (cmbLanguage != null) {
            cmbLanguage.setSelectedItem(newLanguage);
        }
        updatingLanguageCombo = false;

        setTitle(I18n.get("app.name") + " - " + I18n.get("app.subtitle"));
        if (lblAppName != null) {
            lblAppName.setText(I18n.get("app.name"));
            lblAppName.setFont(UiUtils.titleFont());
        }
        if (lblAppSub != null) {
            lblAppSub.setText(I18n.get("app.subtitle"));
            lblAppSub.setFont(UiUtils.subtitleFont());
        }
        if (lblZoom != null) {
            lblZoom.setText("Zoom:");
            lblZoom.setFont(UiUtils.subtitleFont());
        }
        if (btnQuickIncome != null) {
            btnQuickIncome.setText(I18n.get("action.recordIncome"));
            btnQuickIncome.setFont(UiUtils.boldFont());
        }
        if (btnQuickExpense != null) {
            btnQuickExpense.setText(I18n.get("action.recordExpense"));
            btnQuickExpense.setFont(UiUtils.boldFont());
        }
        if (btnRefreshAll != null) {
            btnRefreshAll.setText(I18n.get("action.refresh"));
            btnRefreshAll.setFont(UiUtils.boldFont());
        }

        if (mainTabs != null && mainTabs.getTabCount() >= 9) {
            mainTabs.setFont(UiUtils.boldFont());
            mainTabs.setTitleAt(0, I18n.get("tab.dashboard"));
            mainTabs.setTitleAt(1, I18n.get("tab.transactions"));
            mainTabs.setTitleAt(2, I18n.get("tab.duePayments"));
            mainTabs.setTitleAt(3, I18n.get("tab.loans"));
            mainTabs.setTitleAt(4, I18n.get("tab.masters"));
            mainTabs.setTitleAt(5, I18n.get("tab.audit"));
            mainTabs.setTitleAt(6, I18n.get("tab.userGuide"));
            mainTabs.setTitleAt(7, I18n.get("tab.aboutUs"));
            mainTabs.setTitleAt(8, I18n.get("tab.settings"));
        }

        SwingUtilities.updateComponentTreeUI(this);
        revalidate();
        repaint();
    }

    /**
     * Restores window bounds, position, and maximized state from persisted settings,
     * defaulting to full screen at location (0, 0) on initial launch.
     */
    private void restoreWindowBounds() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        Rectangle maxBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

        int savedWidth = AppConfig.getWindowWidth();
        int savedHeight = AppConfig.getWindowHeight();
        int savedX = AppConfig.getWindowX();
        int savedY = AppConfig.getWindowY();
        boolean savedMaximized = AppConfig.isWindowMaximized();

        if (savedWidth <= 0 || savedHeight <= 0) {
            // First run default: Location (0, 0) and full available desktop bounds
            setBounds(0, 0, maxBounds.width, maxBounds.height);
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        } else {
            // Check if saved position is within any active display
            Rectangle virtualBounds = new Rectangle();
            for (java.awt.GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
                for (java.awt.GraphicsConfiguration gc : gd.getConfigurations()) {
                    virtualBounds = virtualBounds.union(gc.getBounds());
                }
            }

            int w = Math.max(1100, Math.min(savedWidth, virtualBounds.width));
            int h = Math.max(750, Math.min(savedHeight, virtualBounds.height));
            int x = savedX;
            int y = savedY;

            // If completely off-screen, fallback to (0, 0)
            if (!virtualBounds.intersects(x, y, Math.min(w, 200), Math.min(h, 100))) {
                x = 0;
                y = 0;
            }

            setBounds(x, y, w, h);

            if (savedMaximized) {
                setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
        }
    }

    /** Debounced timer to avoid disk-thrashing during active dragging/resizing. */
    private final javax.swing.Timer boundsSaveTimer = new javax.swing.Timer(300, e -> persistCurrentWindowBounds());

    /**
     * Attaches component and window listeners to persist moved and resized positions.
     */
    private void setupWindowBoundsPersistence() {
        boundsSaveTimer.setRepeats(false);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                boundsSaveTimer.restart();
            }

            @Override
            public void componentMoved(java.awt.event.ComponentEvent e) {
                boundsSaveTimer.restart();
            }
        });

        addWindowStateListener(e -> boundsSaveTimer.restart());

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                persistCurrentWindowBounds();
            }
        });
    }

    /**
     * Persists current window coordinates, dimensions, and maximized state to settings.
     */
    private void persistCurrentWindowBounds() {
        boolean isMaximized = (getExtendedState() & JFrame.MAXIMIZED_BOTH) != 0;
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        if (!isMaximized) {
            AppConfig.saveWindowBounds(x, y, w, h, false);
        } else {
            int prevW = AppConfig.getWindowWidth() > 0 ? AppConfig.getWindowWidth() : w;
            int prevH = AppConfig.getWindowHeight() > 0 ? AppConfig.getWindowHeight() : h;
            int prevX = AppConfig.getWindowX();
            int prevY = AppConfig.getWindowY();
            AppConfig.saveWindowBounds(prevX, prevY, prevW, prevH, true);
        }
    }

    /**
     * Returns the active singleton instance of {@link MainFrame}.
     *
     * @return active frame instance
     */
    public static MainFrame getInstance() {
        return instance;
    }

    /**
     * Programmatically switches the active main window tab.
     *
     * @param tabIndex zero-based tab index
     */
    public void switchToTab(int tabIndex) {
        if (mainTabs != null && tabIndex >= 0 && tabIndex < mainTabs.getTabCount()) {
            mainTabs.setSelectedIndex(tabIndex);
        }
    }

    /**
     * Directly routes navigation to the Entity Tracker tab in TransactionsPanel,
     * pre-selecting the entity type, specific entity, and time granularity.
     *
     * @param entityType "PERSON", "BILL", "EMI", "CREDIT_CARD", or "CATEGORY"
     * @param entityId Primary key ID of entity (or null if category)
     * @param categoryName Category string name
     * @param granularity Time bucket: "DAYS", "WEEK", "MONTHS", "QUARTER", or "YEARS"
     */
    public void switchToEntityTracker(String entityType, Long entityId, String categoryName, String granularity) {
        if (mainTabs != null) {
            mainTabs.setSelectedIndex(1);
            if (transactionsPanel != null) {
                transactionsPanel.openEntityTracker(entityType, entityId, categoryName, granularity);
            }
        }
    }

    /**
     * Programmatically routes navigation to the Transactions ledger filtered by a specified date range.
     *
     * @param start inclusive start date
     * @param end inclusive end date
     */
    public void switchToTransactionsWithDateRange(LocalDate start, LocalDate end) {
        if (mainTabs != null) {
            mainTabs.setSelectedIndex(1);
            if (transactionsPanel != null) {
                transactionsPanel.filterByDateRange(start, end);
            }
        }
    }
}
