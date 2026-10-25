package com.navfinance.ui.panels;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.navfinance.config.AppConfig;
import com.navfinance.database.DatabaseConfig;
import com.navfinance.database.DatabaseManager;
import com.navfinance.database.DatabaseType;
import com.navfinance.i18n.AppLanguage;
import com.navfinance.i18n.I18n;
import com.navfinance.i18n.LanguageChangeListener;
import com.navfinance.service.ExportService;
import com.navfinance.ui.components.ResponsiveBarPanel;
import com.navfinance.ui.util.NavIcons;
import com.navfinance.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Settings and Configuration panel providing diagnostics and administrative tools for
 * database configuration (embedded H2 default, MySQL, PostgreSQL, MariaDB, SQLite, Custom JDBC),
 * multi-table CSV backups, system preferences, multi-language selection, and UI themes.
 *
 * @author Nav Finance Team
 * @version 1.1.0
 */
public class SettingsPanel extends JPanel implements LanguageChangeListener {

    /** Business export service handling multi-table database backup operations. */
    private final ExportService exportService = ExportService.getInstance();

    /** Callback runnable invoked when global settings or database changes to refresh application panels. */
    private final Runnable onDataUpdated;

    private JLabel lblTitle;
    private JLabel lblSub;
    private JPanel centerPanel;

    // --- Database Card Components ---
    private JComboBox<DatabaseType> cmbDbType;
    private JLabel lblActiveBadge;
    private JLabel lblDbDesc;
    private JTextField txtHost;
    private JTextField txtPort;
    private JTextField txtDbName;
    private JTextField txtUser;
    private JPasswordField txtPassword;
    private JTextField txtFilePath;
    private JButton btnBrowseFile;
    private JTextField txtCustomUrl;
    private JTextField txtCustomDriver;
    private JTextField txtPreviewUrl;
    private JLabel lblTestStatus;

    private JPanel rowServer;
    private JPanel rowDbName;
    private JPanel rowAuth;
    private JPanel rowFile;
    private JPanel rowCustomUrl;
    private JPanel rowCustomDriver;

    // --- Preferences & Theme Components ---
    private JTextField txtCurrency;
    private JComboBox<String> cmbTheme;
    private JComboBox<AppLanguage> cmbLanguage;

    /**
     * Constructs a new {@code SettingsPanel} bound to the parent refresh listener.
     *
     * @param onDataUpdated callback invoked when currency, preferences, or active database updates
     */
    public SettingsPanel(Runnable onDataUpdated) {
        this.onDataUpdated = onDataUpdated;
        setLayout(new BorderLayout(0, 16));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        initComponents();
        I18n.addListener(this);
    }

    /**
     * Initializes header labels and card containers for Database, Backups, and Preferences.
     * Encloses configuration cards in a responsive scroll pane to support large UI zoom scaling.
     */
    private void initComponents() {
        removeAll();

        // TOP: Header title and descriptive subtitle
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        lblTitle = new JLabel(I18n.get("settings.title"));
        lblTitle.setFont(UiUtils.titleFont());
        lblTitle.setForeground(new Color(15, 23, 42));
        header.add(lblTitle, BorderLayout.NORTH);

        lblSub = new JLabel(I18n.get("settings.language.note"));
        lblSub.setFont(UiUtils.subtitleFont());
        lblSub.setForeground(UiUtils.COLOR_TEXT_MUTED);
        header.add(lblSub, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);

        // CENTER: Stacked configuration cards inside a smooth vertical scroll pane
        centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        int row = 0;

        // Card 1: Multi-Language & Regional Script Settings
        gbc.gridy = row++;
        centerPanel.add(createLanguageCard(), gbc);

        // Card 2: Interactive Database Configuration & Connection Management
        gbc.gridy = row++;
        centerPanel.add(createDatabaseCard(), gbc);

        // Card 3: Full Database CSV Export / Backup
        gbc.gridy = row++;
        centerPanel.add(createBackupCard(), gbc);

        // Card 4: Display Preferences (Currency Symbol, Look & Feel Theme)
        gbc.gridy = row++;
        centerPanel.add(createPreferencesCard(), gbc);

        // Vertical space filler at bottom so cards remain compact at the top
        gbc.gridy = row++;
        gbc.weighty = 1.0;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        centerPanel.add(filler, gbc);

        JScrollPane scrollPane = new JScrollPane(centerPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    /**
     * Builds the card presenting Multi-Language selection (English, Hindi, Hinglish).
     */
    private JPanel createLanguageCard() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(UiUtils.createCardBorder());

        JLabel lblCardTitle = new JLabel(I18n.get("settings.language.card"), NavIcons.language(18, UiUtils.COLOR_PRIMARY), JLabel.LEFT);
        lblCardTitle.setFont(UiUtils.headerFont());
        card.add(lblCardTitle, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        content.add(new JLabel(I18n.get("settings.language.select")), gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        cmbLanguage = new JComboBox<>(AppLanguage.values());
        cmbLanguage.setFont(UiUtils.regularFont());
        cmbLanguage.setSelectedItem(I18n.getCurrentLanguage());
        cmbLanguage.addActionListener(e -> {
            AppLanguage lang = (AppLanguage) cmbLanguage.getSelectedItem();
            if (lang != null && lang != I18n.getCurrentLanguage()) {
                I18n.setLanguage(lang);
            }
        });
        content.add(cmbLanguage, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        JLabel lblNote = new JLabel("<html><span style='color:#64748b; font-size:11px;'>"
                + "• <b>English:</b> Standard International Financial English.<br/>"
                + "• <b>हिन्दी (Hindi):</b> High-clarity Devanagari script rendered with Nirmala UI.<br/>"
                + "• <b>Hinglish:</b> Hindi words written phonetically in English (e.g. Aamdani, Kharcha, Udhaar, Chukta).<br/>"
                + "• <i>Note: CSV import/export data templates remain in Standard English for universal interoperability.</i>"
                + "</span></html>");
        content.add(lblNote, gbc);

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    /**
     * Builds the interactive card providing full configuration, live preview, testing,
     * switching, and resetting of application database connections.
     */
    private JPanel createDatabaseCard() {
        JPanel card = new JPanel(new BorderLayout(12, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(UiUtils.createCardBorder());

        // Header: Title on the left, Active Connection Badge on the right
        ResponsiveBarPanel cardHeader = new ResponsiveBarPanel(8, 6);
        JLabel lblCardTitle = new JLabel(I18n.get("settings.database.configCard"), NavIcons.settings(16, UiUtils.COLOR_PRIMARY), JLabel.LEFT);
        lblCardTitle.setFont(UiUtils.headerFont());
        cardHeader.addLeft(lblCardTitle);

        lblActiveBadge = new JLabel();
        lblActiveBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblActiveBadge.setOpaque(true);
        updateActiveBadge();
        cardHeader.addRight(lblActiveBadge);

        card.add(cardHeader, BorderLayout.NORTH);

        // Center: Form Rows in a flexible GridBagLayout
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        DatabaseConfig active = AppConfig.getActiveDatabaseConfig();

        int rowIdx = 0;

        // 1. Database Engine Dropdown
        gbc.gridx = 0; gbc.gridy = rowIdx; gbc.weightx = 0;
        form.add(new JLabel(I18n.get("settings.database.engine")), gbc);

        gbc.gridx = 1; gbc.gridy = rowIdx++; gbc.weightx = 1.0;
        cmbDbType = new JComboBox<>(DatabaseType.values());
        cmbDbType.setFont(UiUtils.regularFont());
        cmbDbType.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof DatabaseType dt) {
                    setText(dt.getTypeName());
                }
                return this;
            }
        });
        cmbDbType.setSelectedItem(active.getType());
        form.add(cmbDbType, gbc);

        // 2. Engine Description
        gbc.gridx = 0; gbc.gridy = rowIdx++; gbc.gridwidth = 2;
        lblDbDesc = new JLabel();
        lblDbDesc.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblDbDesc.setForeground(UiUtils.COLOR_TEXT_MUTED);
        updateDbDesc(active.getType());
        form.add(lblDbDesc, gbc);
        gbc.gridwidth = 1;

        // Document Listener for live URL Preview
        DocumentListener docListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updateUrlPreview(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updateUrlPreview(); }
            @Override
            public void changedUpdate(DocumentEvent e) { updateUrlPreview(); }
        };

        // 3. File Path Row (for H2 & SQLite)
        rowFile = new JPanel(new BorderLayout(8, 0));
        rowFile.setOpaque(false);
        txtFilePath = new JTextField(active.getLocalPath() != null ? active.getLocalPath() : AppConfig.DB_PATH);
        txtFilePath.getDocument().addDocumentListener(docListener);
        btnBrowseFile = UiUtils.createSecondaryButton(I18n.get("settings.database.browse"), NavIcons.importIcon(12, UiUtils.COLOR_PRIMARY));
        btnBrowseFile.addActionListener(e -> onBrowseDbFile());
        rowFile.add(txtFilePath, BorderLayout.CENTER);
        rowFile.add(btnBrowseFile, BorderLayout.EAST);

        gbc.gridx = 0; gbc.gridy = rowIdx; gbc.weightx = 0;
        JLabel lblFileLabel = new JLabel(I18n.get("settings.database.filePath"));
        form.add(lblFileLabel, gbc);
        gbc.gridx = 1; gbc.gridy = rowIdx++; gbc.weightx = 1.0;
        form.add(rowFile, gbc);

        // 4. Server Host & Port Row (for MySQL, PostgreSQL, MariaDB)
        rowServer = new JPanel(new BorderLayout(8, 0));
        rowServer.setOpaque(false);
        txtHost = new JTextField(active.getHost() != null ? active.getHost() : "localhost");
        txtHost.getDocument().addDocumentListener(docListener);

        JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        portPanel.setOpaque(false);
        portPanel.add(new JLabel(I18n.get("settings.database.port")));
        txtPort = new JTextField(String.valueOf(active.getPort()), 5);
        txtPort.getDocument().addDocumentListener(docListener);
        portPanel.add(txtPort);

        rowServer.add(txtHost, BorderLayout.CENTER);
        rowServer.add(portPanel, BorderLayout.EAST);

        gbc.gridx = 0; gbc.gridy = rowIdx; gbc.weightx = 0;
        JLabel lblHostLabel = new JLabel(I18n.get("settings.database.host"));
        form.add(lblHostLabel, gbc);
        gbc.gridx = 1; gbc.gridy = rowIdx++; gbc.weightx = 1.0;
        form.add(rowServer, gbc);

        // 5. Database Name Row
        rowDbName = new JPanel(new BorderLayout());
        rowDbName.setOpaque(false);
        txtDbName = new JTextField(active.getDatabaseName() != null ? active.getDatabaseName() : "navfinance");
        txtDbName.getDocument().addDocumentListener(docListener);
        rowDbName.add(txtDbName, BorderLayout.CENTER);

        gbc.gridx = 0; gbc.gridy = rowIdx; gbc.weightx = 0;
        JLabel lblDbNameLabel = new JLabel(I18n.get("settings.database.name"));
        form.add(lblDbNameLabel, gbc);
        gbc.gridx = 1; gbc.gridy = rowIdx++; gbc.weightx = 1.0;
        form.add(rowDbName, gbc);

        // 6. Custom JDBC URL & Driver Class Rows
        rowCustomUrl = new JPanel(new BorderLayout());
        rowCustomUrl.setOpaque(false);
        txtCustomUrl = new JTextField(active.getCustomUrl() != null ? active.getCustomUrl() : "");
        txtCustomUrl.getDocument().addDocumentListener(docListener);
        rowCustomUrl.add(txtCustomUrl, BorderLayout.CENTER);

        gbc.gridx = 0; gbc.gridy = rowIdx; gbc.weightx = 0;
        JLabel lblCustomUrl = new JLabel(I18n.get("settings.database.customUrl"));
        form.add(lblCustomUrl, gbc);
        gbc.gridx = 1; gbc.gridy = rowIdx++; gbc.weightx = 1.0;
        form.add(rowCustomUrl, gbc);

        rowCustomDriver = new JPanel(new BorderLayout());
        rowCustomDriver.setOpaque(false);
        txtCustomDriver = new JTextField(active.getCustomDriver() != null ? active.getCustomDriver() : "");
        txtCustomDriver.getDocument().addDocumentListener(docListener);
        rowCustomDriver.add(txtCustomDriver, BorderLayout.CENTER);

        gbc.gridx = 0; gbc.gridy = rowIdx; gbc.weightx = 0;
        JLabel lblCustomDriver = new JLabel(I18n.get("settings.database.customDriver"));
        form.add(lblCustomDriver, gbc);
        gbc.gridx = 1; gbc.gridy = rowIdx++; gbc.weightx = 1.0;
        form.add(rowCustomDriver, gbc);

        // 7. Authentication Credentials Row (Username & Password)
        rowAuth = new JPanel(new GridBagLayout());
        rowAuth.setOpaque(false);
        GridBagConstraints authGbc = new GridBagConstraints();
        authGbc.fill = GridBagConstraints.HORIZONTAL;
        authGbc.insets = new Insets(0, 0, 0, 8);

        authGbc.gridx = 0; authGbc.weightx = 0;
        rowAuth.add(new JLabel(I18n.get("settings.database.user")), authGbc);
        authGbc.gridx = 1; authGbc.weightx = 0.5;
        txtUser = new JTextField(active.getUsername() != null ? active.getUsername() : "");
        txtUser.getDocument().addDocumentListener(docListener);
        rowAuth.add(txtUser, authGbc);

        authGbc.gridx = 2; authGbc.weightx = 0;
        rowAuth.add(new JLabel(I18n.get("settings.database.password")), authGbc);
        authGbc.gridx = 3; authGbc.weightx = 0.5;
        txtPassword = new JPasswordField(active.getPassword() != null ? active.getPassword() : "");
        txtPassword.getDocument().addDocumentListener(docListener);
        rowAuth.add(txtPassword, authGbc);

        gbc.gridx = 0; gbc.gridy = rowIdx; gbc.weightx = 0;
        JLabel lblAuthLabel = new JLabel(I18n.get("settings.database.user"));
        form.add(lblAuthLabel, gbc);
        gbc.gridx = 1; gbc.gridy = rowIdx++; gbc.weightx = 1.0;
        form.add(rowAuth, gbc);

        // 8. Live JDBC URL Preview Row
        gbc.gridx = 0; gbc.gridy = rowIdx; gbc.weightx = 0;
        JLabel lblPreview = new JLabel(I18n.get("settings.database.previewUrl"));
        form.add(lblPreview, gbc);

        gbc.gridx = 1; gbc.gridy = rowIdx++; gbc.weightx = 1.0;
        txtPreviewUrl = new JTextField();
        txtPreviewUrl.setEditable(false);
        txtPreviewUrl.setBackground(new Color(241, 245, 249));
        txtPreviewUrl.setForeground(new Color(51, 65, 85));
        form.add(txtPreviewUrl, gbc);

        // 9. Diagnostic Status Feedback Row
        gbc.gridx = 0; gbc.gridy = rowIdx++; gbc.gridwidth = 2;
        lblTestStatus = new JLabel(" ");
        lblTestStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        form.add(lblTestStatus, gbc);
        gbc.gridwidth = 1;

        // ComboBox Action Listener to switch field visibility
        cmbDbType.addActionListener(e -> {
            DatabaseType selected = (DatabaseType) cmbDbType.getSelectedItem();
            if (selected != null) {
                updateDbDesc(selected);
                updateFieldVisibility(selected, lblFileLabel, lblHostLabel, lblDbNameLabel, lblCustomUrl, lblCustomDriver, lblAuthLabel);
                // Pre-fill port if field is empty or matched previous default
                if (!selected.isEmbeddedOrLocal()) {
                    txtPort.setText(String.valueOf(selected.getDefaultPort()));
                    if (txtHost.getText().trim().isEmpty()) {
                        txtHost.setText("localhost");
                    }
                    if (txtDbName.getText().trim().isEmpty()) {
                        txtDbName.setText("navfinance");
                    }
                }
                updateUrlPreview();
                lblTestStatus.setText(" ");
            }
        });

        // Initialize visibility and URL preview
        updateFieldVisibility(active.getType(), lblFileLabel, lblHostLabel, lblDbNameLabel, lblCustomUrl, lblCustomDriver, lblAuthLabel);
        updateUrlPreview();

        card.add(form, BorderLayout.CENTER);

        // Bottom Action Bar using ResponsiveBarPanel to eliminate Zoom In wrapping overlap
        ResponsiveBarPanel actionPanel = new ResponsiveBarPanel(8, 8);

        JButton btnTest = UiUtils.createSecondaryButton(I18n.get("settings.database.testBtn"), NavIcons.check(13, UiUtils.COLOR_PRIMARY));
        btnTest.addActionListener(e -> onTestConnection());

        JButton btnSave = UiUtils.createPrimaryButton(I18n.get("settings.database.saveBtn"), NavIcons.settings(13, Color.WHITE));
        btnSave.addActionListener(e -> onSaveAndConnect());

        JButton btnReset = UiUtils.createSecondaryButton(I18n.get("settings.database.resetBtn"), NavIcons.refresh(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnReset.addActionListener(e -> onResetToDefaultH2());

        actionPanel.addLeft(btnTest);
        actionPanel.addLeft(btnSave);
        actionPanel.addRight(btnReset);

        card.add(actionPanel, BorderLayout.SOUTH);

        return card;
    }

    /**
     * Updates visibility of database parameter input rows depending on the chosen database engine.
     */
    private void updateFieldVisibility(DatabaseType type, JLabel lblFile, JLabel lblHost, JLabel lblDbName,
                                       JLabel lblCustomUrl, JLabel lblCustomDriver, JLabel lblAuth) {
        boolean isLocal = (type == DatabaseType.H2 || type == DatabaseType.SQLITE);
        boolean isCustom = (type == DatabaseType.CUSTOM);
        boolean isServer = !isLocal && !isCustom;

        rowFile.setVisible(isLocal);
        lblFile.setVisible(isLocal);

        rowServer.setVisible(isServer);
        lblHost.setVisible(isServer);

        rowDbName.setVisible(isServer);
        lblDbName.setVisible(isServer);

        rowCustomUrl.setVisible(isCustom);
        lblCustomUrl.setVisible(isCustom);

        rowCustomDriver.setVisible(isCustom);
        lblCustomDriver.setVisible(isCustom);

        rowAuth.setVisible(isServer || isCustom);
        lblAuth.setVisible(isServer || isCustom);

        revalidate();
        repaint();
    }

    /**
     * Dynamically updates the informational caption beneath the database engine selector.
     */
    private void updateDbDesc(DatabaseType type) {
        if (type == null) return;
        switch (type) {
            case H2 -> lblDbDesc.setText("Default embedded zero-configuration engine stored locally in ~/.nav-finance. No server required.");
            case MYSQL -> lblDbDesc.setText("Connects to a running MySQL 8.x/5.7 server over TCP. Standard schema initialized automatically.");
            case POSTGRESQL -> lblDbDesc.setText("Connects to a PostgreSQL 12+ database server. Identity columns and timestamps supported.");
            case MARIADB -> lblDbDesc.setText("Connects to MariaDB Community / Enterprise server via official MariaDB driver.");
            case SQLITE -> lblDbDesc.setText("Single-file embedded database. Self-contained, lightweight and serverless.");
            case CUSTOM -> lblDbDesc.setText("Custom JDBC database connection. Specify driver class, custom connection URL, and credentials.");
        }
    }

    /**
     * Computes the live JDBC connection string from user inputs and updates the preview field.
     */
    private void updateUrlPreview() {
        if (cmbDbType == null || txtPreviewUrl == null) return;
        try {
            DatabaseConfig tempConfig = buildConfigFromFields();
            txtPreviewUrl.setText(tempConfig.getEffectiveJdbcUrl());
        } catch (Exception e) {
            txtPreviewUrl.setText("");
        }
    }

    /**
     * Constructs a {@link DatabaseConfig} model instance reflecting all current form input values.
     */
    private DatabaseConfig buildConfigFromFields() {
        DatabaseType type = (DatabaseType) cmbDbType.getSelectedItem();
        if (type == null) type = DatabaseType.H2;

        int port = type.getDefaultPort();
        try {
            String portText = txtPort.getText().trim();
            if (!portText.isEmpty()) {
                port = Integer.parseInt(portText);
            }
        } catch (NumberFormatException ignored) {}

        String host = txtHost.getText().trim();
        if (host.isEmpty() && !type.isEmbeddedOrLocal()) {
            host = "localhost";
        }

        String dbName = txtDbName.getText().trim();
        if (dbName.isEmpty()) {
            dbName = "navfinance";
        }

        String user = txtUser.getText().trim();
        String pass = new String(txtPassword.getPassword());
        String localPath = txtFilePath.getText().trim();
        if (localPath.isEmpty()) {
            localPath = AppConfig.DB_PATH;
        }
        String customUrl = txtCustomUrl.getText().trim();
        String customDriver = txtCustomDriver.getText().trim();

        return new DatabaseConfig(type, host, port, dbName, user, pass, localPath, customUrl, customDriver);
    }

    /**
     * Refreshes the visual badge indicating the active database connection state and destination.
     */
    private void updateActiveBadge() {
        DatabaseConfig active = AppConfig.getActiveDatabaseConfig();
        DatabaseType type = active.getType();
        if (type == DatabaseType.H2) {
            lblActiveBadge.setText("● Active: H2 Embedded (Default)");
            lblActiveBadge.setBackground(new Color(240, 253, 244));
            lblActiveBadge.setForeground(new Color(22, 101, 52));
            lblActiveBadge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(187, 247, 208), 1, true),
                    BorderFactory.createEmptyBorder(4, 10, 4, 10)
            ));
        } else {
            String info = type.getDisplayName();
            if (!type.isEmbeddedOrLocal()) {
                info += " (" + active.getHost() + ":" + active.getPort() + "/" + active.getDatabaseName() + ")";
            }
            lblActiveBadge.setText("● Active: " + info);
            lblActiveBadge.setBackground(new Color(238, 242, 255));
            lblActiveBadge.setForeground(new Color(67, 56, 202));
            lblActiveBadge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(199, 210, 254), 1, true),
                    BorderFactory.createEmptyBorder(4, 10, 4, 10)
            ));
        }
    }

    /**
     * Executes non-blocking diagnostic connection testing against the configured database settings.
     */
    private void onTestConnection() {
        DatabaseConfig testConfig = buildConfigFromFields();
        lblTestStatus.setText("Testing connection to " + testConfig.getType().getTypeName() + "...");
        lblTestStatus.setForeground(new Color(100, 116, 139));

        SwingWorker<DatabaseConfig.ConnectionTestResult, Void> worker = new SwingWorker<>() {
            @Override
            protected DatabaseConfig.ConnectionTestResult doInBackground() {
                return testConfig.testConnection();
            }

            @Override
            protected void done() {
                try {
                    DatabaseConfig.ConnectionTestResult result = get();
                    if (result.success()) {
                        lblTestStatus.setText("✓ " + I18n.get("settings.database.testSuccess",
                                result.serverInfo().isEmpty() ? testConfig.getType().getTypeName() : result.serverInfo(),
                                result.latencyMs()));
                        lblTestStatus.setForeground(new Color(22, 101, 52));
                    } else {
                        lblTestStatus.setText("✗ " + I18n.get("settings.database.testFail", result.message()));
                        lblTestStatus.setForeground(new Color(185, 28, 28));
                    }
                } catch (Exception ex) {
                    lblTestStatus.setText("✗ " + I18n.get("settings.database.testFail", ex.getMessage()));
                    lblTestStatus.setForeground(new Color(185, 28, 28));
                }
            }
        };
        worker.execute();
    }

    /**
     * Switches the application database connection to the newly configured engine, initializes
     * schema tables, persists settings to disk, and triggers application data refresh.
     */
    private void onSaveAndConnect() {
        DatabaseConfig newConfig = buildConfigFromFields();
        lblTestStatus.setText("Connecting and verifying database schema...");
        lblTestStatus.setForeground(new Color(100, 116, 139));

        SwingWorker<DatabaseConfig.ConnectionTestResult, Void> worker = new SwingWorker<>() {
            @Override
            protected DatabaseConfig.ConnectionTestResult doInBackground() {
                return DatabaseManager.getInstance().switchDatabase(newConfig);
            }

            @Override
            protected void done() {
                try {
                    DatabaseConfig.ConnectionTestResult result = get();
                    if (result.success()) {
                        lblTestStatus.setText("✓ " + result.message());
                        lblTestStatus.setForeground(new Color(22, 101, 52));
                        updateActiveBadge();
                        UiUtils.showSuccess(SettingsPanel.this, I18n.get("settings.database.switchSuccess", newConfig.getType().getTypeName()));
                        if (onDataUpdated != null) {
                            onDataUpdated.run();
                        }
                    } else {
                        lblTestStatus.setText("✗ " + result.message());
                        lblTestStatus.setForeground(new Color(185, 28, 28));
                        UiUtils.showError(SettingsPanel.this, I18n.get("settings.database.switchFail", result.message()));
                    }
                } catch (Exception ex) {
                    lblTestStatus.setText("✗ Error: " + ex.getMessage());
                    lblTestStatus.setForeground(new Color(185, 28, 28));
                    UiUtils.showError(SettingsPanel.this, I18n.get("settings.database.switchFail", ex.getMessage()));
                }
            }
        };
        worker.execute();
    }

    /**
     * Reverts application database configuration back to the default embedded H2 engine.
     */
    private void onResetToDefaultH2() {
        DatabaseConfig active = AppConfig.getActiveDatabaseConfig();
        if (active.getType() == DatabaseType.H2 && AppConfig.DB_PATH.equals(active.getLocalPath())) {
            UiUtils.showInfo(this, "The default embedded H2 database is already active.");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to revert to the default embedded H2 database?",
                "Reset Database",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        DatabaseConfig.ConnectionTestResult result = DatabaseManager.getInstance().resetToDefaultH2();
        if (result.success()) {
            DatabaseConfig defH2 = AppConfig.getActiveDatabaseConfig();
            populateFieldsFromConfig(defH2);
            updateActiveBadge();
            lblTestStatus.setText("✓ " + I18n.get("settings.database.resetSuccess"));
            lblTestStatus.setForeground(new Color(22, 101, 52));
            UiUtils.showSuccess(this, I18n.get("settings.database.resetSuccess"));
            if (onDataUpdated != null) {
                onDataUpdated.run();
            }
        } else {
            lblTestStatus.setText("✗ Reset failed: " + result.message());
            lblTestStatus.setForeground(new Color(185, 28, 28));
            UiUtils.showError(this, "Reset failed: " + result.message());
        }
    }

    /**
     * Populates form fields with the properties of the provided database configuration.
     */
    private void populateFieldsFromConfig(DatabaseConfig config) {
        cmbDbType.setSelectedItem(config.getType());
        txtHost.setText(config.getHost() != null ? config.getHost() : "localhost");
        txtPort.setText(String.valueOf(config.getPort()));
        txtDbName.setText(config.getDatabaseName() != null ? config.getDatabaseName() : "navfinance");
        txtUser.setText(config.getUsername() != null ? config.getUsername() : "");
        txtPassword.setText(config.getPassword() != null ? config.getPassword() : "");
        txtFilePath.setText(config.getLocalPath() != null ? config.getLocalPath() : AppConfig.DB_PATH);
        txtCustomUrl.setText(config.getCustomUrl() != null ? config.getCustomUrl() : "");
        txtCustomDriver.setText(config.getCustomDriver() != null ? config.getCustomDriver() : "");
        updateDbDesc(config.getType());
        updateUrlPreview();
    }

    /**
     * Opens a file chooser allowing the user to select an existing local database file (for SQLite or custom H2).
     */
    private void onBrowseDbFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select Database File");
        File cur = new File(txtFilePath.getText().trim());
        if (cur.exists()) {
            fc.setSelectedFile(cur);
        } else {
            fc.setCurrentDirectory(AppConfig.APP_DATA_DIR);
        }
        int res = fc.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            txtFilePath.setText(fc.getSelectedFile().getAbsolutePath());
            updateUrlPreview();
        }
    }

    /**
     * Builds the card offering automated full database exports into timestamped CSV directories.
     */
    private JPanel createBackupCard() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(UiUtils.createCardBorder());

        JLabel lblCardTitle = new JLabel(I18n.get("settings.database.exportAll"), NavIcons.export(16, UiUtils.COLOR_PRIMARY), JLabel.LEFT);
        lblCardTitle.setFont(UiUtils.headerFont());
        card.add(lblCardTitle, BorderLayout.NORTH);

        JLabel lblDesc = new JLabel("<html>Export all database tables into individual CSV files. Ideal for offline backups, migration, and spreadsheet analysis. Compatible across all supported database engines.</html>");
        lblDesc.setFont(UiUtils.regularFont());
        card.add(lblDesc, BorderLayout.CENTER);

        ResponsiveBarPanel btnBar = new ResponsiveBarPanel(8, 8);

        JButton btnExportAll = UiUtils.createPrimaryButton(I18n.get("settings.database.exportAll"), NavIcons.export(13, Color.WHITE));
        btnExportAll.addActionListener(e -> onExportAllDatabase());

        JButton btnOpenFolder = UiUtils.createSecondaryButton(I18n.get("settings.database.openFolder"), NavIcons.importIcon(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnOpenFolder.addActionListener(e -> onOpenDbFolder());

        btnBar.addLeft(btnExportAll);
        btnBar.addLeft(btnOpenFolder);
        card.add(btnBar, BorderLayout.SOUTH);

        return card;
    }

    /**
     * Builds the card offering user preferences for currency symbol and theme options.
     */
    private JPanel createPreferencesCard() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(UiUtils.createCardBorder());

        JLabel lblCardTitle = new JLabel(I18n.get("settings.theme.card"), NavIcons.settings(16, UiUtils.COLOR_PRIMARY), JLabel.LEFT);
        lblCardTitle.setFont(UiUtils.headerFont());
        card.add(lblCardTitle, BorderLayout.NORTH);

        ResponsiveBarPanel form = new ResponsiveBarPanel(16, 8);

        // Left section: Currency Symbol
        JPanel pnlCurrency = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlCurrency.setOpaque(false);
        pnlCurrency.add(new JLabel(I18n.get("settings.currency.symbol")));
        txtCurrency = new JTextField(AppConfig.getCurrencySymbol(), 4);
        pnlCurrency.add(txtCurrency);

        JButton btnSaveCurrency = UiUtils.createSecondaryButton(I18n.get("settings.currency.save"), NavIcons.check(13, UiUtils.COLOR_PRIMARY));
        btnSaveCurrency.addActionListener(e -> {
            AppConfig.setCurrencySymbol(txtCurrency.getText().trim());
            UiUtils.showSuccess(this, I18n.get("settings.currency.success"));
            if (onDataUpdated != null) {
                onDataUpdated.run();
            }
        });
        pnlCurrency.add(btnSaveCurrency);
        form.addLeft(pnlCurrency);

        // Right section: Theme Selection
        JPanel pnlTheme = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlTheme.setOpaque(false);
        pnlTheme.add(new JLabel(I18n.get("settings.theme.card") + ":"));
        cmbTheme = new JComboBox<>(new String[]{I18n.get("settings.theme.light"), I18n.get("settings.theme.dark")});
        cmbTheme.addActionListener(e -> onThemeToggle());
        pnlTheme.add(cmbTheme);
        form.addRight(pnlTheme);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    /**
     * Opens a directory chooser and executes a complete export of all database tables to CSV format.
     */
    private void onExportAllDatabase() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select Target Directory to Export All CSV Files");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
        File defaultDir = new File(AppConfig.APP_DATA_DIR, "backups/backup_" + timeStamp);
        fc.setSelectedFile(defaultDir);

        int res = fc.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File targetDir = fc.getSelectedFile();
            try {
                exportService.exportAllDatabaseToCsv(targetDir);
                UiUtils.showSuccess(this, I18n.get("settings.database.exportSuccess", 10, targetDir.getAbsolutePath()));
            } catch (Exception ex) {
                UiUtils.showError(this, "Export failed: " + ex.getMessage());
            }
        }
    }

    /**
     * Launches the host operating system's native file explorer opened to the NavFinance app data directory.
     */
    private void onOpenDbFolder() {
        try {
            java.awt.Desktop.getDesktop().open(AppConfig.APP_DATA_DIR);
        } catch (Exception ex) {
            UiUtils.showError(this, "Could not open folder automatically: " + AppConfig.APP_DATA_DIR.getAbsolutePath());
        }
    }

    /**
     * Dynamically switches the active FlatLaf Look and Feel between Light and Dark mode.
     */
    private void onThemeToggle() {
        int idx = cmbTheme.getSelectedIndex();
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        try {
            if (idx == 1) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
            }
            if (frame != null) {
                SwingUtilities.updateComponentTreeUI(frame);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onLanguageChanged(AppLanguage newLanguage) {
        initComponents();
    }
}
