package com.navfinance.ui.panels;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.navfinance.config.AppConfig;
import com.navfinance.service.ExportService;
import com.navfinance.ui.util.NavIcons;
import com.navfinance.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Settings and Configuration panel providing diagnostics and administrative tools for
 * the embedded H2 database, multi-table CSV backups, system preferences, and UI themes.
 * <p>
 * Key capabilities include:
 * <ul>
 *   <li>Inspection of local database location ({@code ~user/nav-finance/}) and JDBC connection parameters.</li>
 *   <li>One-click full database export producing CSV backups of all 7 core data tables.</li>
 *   <li>Direct operating system file explorer launching to view database artifacts.</li>
 *   <li>Custom currency symbol configuration with instant UI-wide broadcast.</li>
 *   <li>Dynamic Look and Feel theme switching (FlatLaf Light vs Dark modes).</li>
 * </ul>
 */
public class SettingsPanel extends JPanel {

    /** Business export service handling multi-table database backup operations. */
    private final ExportService exportService = ExportService.getInstance();

    /** Callback runnable invoked when global settings change to refresh other panels. */
    private final Runnable onDataUpdated;

    /** Read-only text field displaying active embedded database file storage directory. */
    private JTextField txtDbPath;

    /** Text field for editing global currency symbol (e.g. ₹, $, €). */
    private JTextField txtCurrency;

    /** Dropdown combo box for switching UI theme (Light vs Dark). */
    private JComboBox<String> cmbTheme;

    /**
     * Constructs a new {@code SettingsPanel} bound to the parent refresh listener.
     *
     * @param onDataUpdated callback invoked when currency or global preferences are updated
     */
    public SettingsPanel(Runnable onDataUpdated) {
        this.onDataUpdated = onDataUpdated;
        setLayout(new BorderLayout(0, 16));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        initComponents();
    }

    /**
     * Initializes header labels and card containers for Database, Backups, and Preferences.
     */
    private void initComponents() {
        // TOP: Header title and descriptive subtitle
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel lblTitle = new JLabel("Database Storage & Application Settings");
        lblTitle.setFont(UiUtils.FONT_TITLE);
        lblTitle.setForeground(new Color(15, 23, 42));
        header.add(lblTitle, BorderLayout.NORTH);

        JLabel lblSub = new JLabel("Manage your local H2 database storage, full CSV backups, appearance, and currency options.");
        lblSub.setFont(UiUtils.FONT_SUBTITLE);
        lblSub.setForeground(UiUtils.COLOR_TEXT_MUTED);
        header.add(lblSub, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);

        // CENTER: Stacked configuration cards
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        int row = 0;

        // Card 1: H2 Embedded Database Location and Details
        gbc.gridy = row++;
        centerPanel.add(createDatabaseCard(), gbc);

        // Card 2: Full Database CSV Export / Backup
        gbc.gridy = row++;
        centerPanel.add(createBackupCard(), gbc);

        // Card 3: Display Preferences (Currency Symbol, Look & Feel Theme)
        gbc.gridy = row++;
        centerPanel.add(createPreferencesCard(), gbc);

        add(centerPanel, BorderLayout.CENTER);
    }

    /**
     * Builds the card presenting H2 storage path, connection strings, and multi-process architecture details.
     *
     * @return JPanel containing database configuration details
     */
    private JPanel createDatabaseCard() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(UiUtils.createCardBorder());

        JLabel lblCardTitle = new JLabel("H2 Embedded Database Location (~user/nav-finance)", NavIcons.settings(16, UiUtils.COLOR_PRIMARY), JLabel.LEFT);
        lblCardTitle.setFont(UiUtils.FONT_HEADER);
        card.add(lblCardTitle, BorderLayout.NORTH);

        JPanel details = new JPanel(new GridBagLayout());
        details.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        details.add(new JLabel("Storage Directory:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        txtDbPath = new JTextField(AppConfig.APP_DATA_DIR.getAbsolutePath());
        txtDbPath.setEditable(false);
        details.add(txtDbPath, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        details.add(new JLabel("JDBC URL:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        JTextField txtUrl = new JTextField(AppConfig.JDBC_URL);
        txtUrl.setEditable(false);
        details.add(txtUrl, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        details.add(new JLabel("Engine & Mode:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        JLabel lblMode = new JLabel("H2 2.2 Embedded Multi-Process (AUTO_SERVER=TRUE), Zero external dependencies.");
        lblMode.setFont(UiUtils.FONT_REGULAR);
        details.add(lblMode, gbc);

        card.add(details, BorderLayout.CENTER);
        return card;
    }

    /**
     * Builds the card offering automated full database exports into timestamped CSV directories.
     *
     * @return JPanel containing backup and export action controls
     */
    private JPanel createBackupCard() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(UiUtils.createCardBorder());

        JLabel lblCardTitle = new JLabel("Full Database Export (All Tables as CSV)", NavIcons.export(16, UiUtils.COLOR_PRIMARY), JLabel.LEFT);
        lblCardTitle.setFont(UiUtils.FONT_HEADER);
        card.add(lblCardTitle, BorderLayout.NORTH);

        JLabel lblDesc = new JLabel("<html>Export all 7 database tables (transactions, persons, bills, emis, credit_cards, payment_modes, audit_logs) into individual CSV files. Ideal for offline backups, migration, and spreadsheet analysis.</html>");
        lblDesc.setFont(UiUtils.FONT_REGULAR);
        card.add(lblDesc, BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
        btnBar.setOpaque(false);

        JButton btnExportAll = UiUtils.createPrimaryButton("Export All Database as CSV Files", NavIcons.export(13, Color.WHITE));
        btnExportAll.addActionListener(e -> onExportAllDatabase());

        JButton btnOpenFolder = UiUtils.createSecondaryButton("Open Database Folder", NavIcons.importIcon(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnOpenFolder.addActionListener(e -> onOpenDbFolder());

        btnBar.add(btnExportAll);
        btnBar.add(btnOpenFolder);
        card.add(btnBar, BorderLayout.SOUTH);

        return card;
    }

    /**
     * Builds the card offering user preferences for currency symbol and theme options.
     *
     * @return JPanel containing display preference controls
     */
    private JPanel createPreferencesCard() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(UiUtils.createCardBorder());

        JLabel lblCardTitle = new JLabel("Display Preferences", NavIcons.settings(16, UiUtils.COLOR_PRIMARY), JLabel.LEFT);
        lblCardTitle.setFont(UiUtils.FONT_HEADER);
        card.add(lblCardTitle, BorderLayout.NORTH);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 8));
        form.setOpaque(false);

        form.add(new JLabel("Currency Symbol:"));
        txtCurrency = new JTextField(AppConfig.getCurrencySymbol(), 4);
        form.add(txtCurrency);

        JButton btnSaveCurrency = UiUtils.createSecondaryButton("Update Symbol", NavIcons.check(13, UiUtils.COLOR_PRIMARY));
        btnSaveCurrency.addActionListener(e -> {
            AppConfig.setCurrencySymbol(txtCurrency.getText().trim());
            UiUtils.showSuccess(this, "Currency symbol updated to: " + AppConfig.getCurrencySymbol());
            if (onDataUpdated != null) {
                onDataUpdated.run();
            }
        });
        form.add(btnSaveCurrency);

        form.add(new JLabel("   Theme:"));
        cmbTheme = new JComboBox<>(new String[]{"Light Mode", "Dark Mode"});
        cmbTheme.addActionListener(e -> onThemeToggle());
        form.add(cmbTheme);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    /**
     * Opens a directory chooser and executes a complete export of all 7 database tables to CSV format.
     */
    private void onExportAllDatabase() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select Target Directory to Export All CSV Files");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        // Default suggestion inside ~/nav-finance/backups with ISO timestamp
        String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
        File defaultDir = new File(AppConfig.APP_DATA_DIR, "backups/backup_" + timeStamp);
        fc.setSelectedFile(defaultDir);

        int res = fc.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File targetDir = fc.getSelectedFile();
            try {
                exportService.exportAllDatabaseToCsv(targetDir);
                UiUtils.showSuccess(this, "All database tables exported successfully to:\n" + targetDir.getAbsolutePath() +
                        "\n\nFiles generated:\n- transactions.csv\n- persons.csv\n- bills.csv\n- emis.csv\n- credit_cards.csv\n- payment_modes.csv\n- audit_logs.csv");
            } catch (Exception ex) {
                UiUtils.showError(this, "Export failed: " + ex.getMessage());
                ex.printStackTrace();
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
     * Dynamically switches the active FlatLaf Look and Feel between Light and Dark mode,
     * instantly updating all Swing component hierarchies.
     */
    private void onThemeToggle() {
        String theme = (String) cmbTheme.getSelectedItem();
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        try {
            if ("Dark Mode".equals(theme)) {
                FlatDarkLaf.setup();
            } else {
                FlatLightLaf.setup();
            }
            if (frame != null) {
                SwingUtilities.updateComponentTreeUI(frame);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
