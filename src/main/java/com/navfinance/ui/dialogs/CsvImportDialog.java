package com.navfinance.ui.dialogs;

import com.navfinance.config.AppConfig;
import com.navfinance.i18n.I18n;
import com.navfinance.service.CsvImportService;
import com.navfinance.ui.util.NavIcons;
import com.navfinance.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Interactive bulk CSV Import, Update, and Deletion management dialog.
 * <p>
 * Allows users to download standardized CSV sample templates, select arbitrary CSV input files,
 * preview parsed rows in a color-coded table before execution, inspect line-level validation
 * errors, and commit valid operations to the database ledger in a batch transaction.
 * </p>
 */
public class CsvImportDialog extends JDialog {

    /** Service handling CSV parsing, row validation, and batch execution. */
    private final CsvImportService importService = CsvImportService.getInstance();

    /** In-memory list of validated and parsed CSV rows from the currently opened file. */
    private List<CsvImportService.CsvImportRecord> currentRecords = new ArrayList<>();

    /** Flag indicating whether the import batch completed successfully. */
    private boolean imported = false;

    /** Label displaying the count of loaded, valid, and errored rows. */
    private JLabel lblStatus;

    /** Table displaying parsed CSV records for user verification. */
    private JTable tblPreview;

    /** Table model backing the preview grid. */
    private DefaultTableModel modelPreview;

    /** Button triggering commit of verified CSV records. */
    private JButton btnExecute;

    /**
     * Constructs a modal CSV Import Dialog.
     *
     * @param parent owner window frame
     */
    public CsvImportDialog(JFrame parent) {
        super(parent, I18n.get("dialog.csv.title"), true);
        setPreferredSize(new Dimension(1000, 600));
        setMinimumSize(new Dimension(850, 480));

        initComponents();
        pack();
        setLocationRelativeTo(parent);
    }

    /**
     * Initializes dialog header, file chooser triggers, preview table, and action bar.
     */
    private void initComponents() {
        setLayout(new BorderLayout(0, 10));
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        root.setBackground(new Color(248, 250, 252));

        // 1. TOP: Instructions & File Selection Buttons
        JPanel topPanel = new JPanel(new BorderLayout(10, 8));
        topPanel.setOpaque(false);

        JPanel headerText = new JPanel(new BorderLayout(0, 3));
        headerText.setOpaque(false);
        JLabel lblTitle = new JLabel(I18n.get("dialog.csv.title"));
        lblTitle.setFont(UiUtils.titleFont());
        lblTitle.setForeground(new Color(15, 23, 42));

        JLabel lblSub = new JLabel("<html>Select a CSV file to create new transactions, update existing records by ID, or delete entries. Download the template below for reference.</html>");
        lblSub.setFont(UiUtils.subtitleFont());
        lblSub.setForeground(UiUtils.COLOR_TEXT_MUTED);

        headerText.add(lblTitle, BorderLayout.NORTH);
        headerText.add(lblSub, BorderLayout.SOUTH);
        topPanel.add(headerText, BorderLayout.NORTH);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnBar.setOpaque(false);

        JButton btnChooseFile = UiUtils.createPrimaryButton(I18n.get("btn.import"), NavIcons.importIcon(13, Color.WHITE));
        btnChooseFile.setFont(UiUtils.boldFont());
        btnChooseFile.addActionListener(e -> onSelectFile());

        JButton btnDownloadTemplate = UiUtils.createSecondaryButton(I18n.get("transactions.export.template"), NavIcons.export(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnDownloadTemplate.setFont(UiUtils.boldFont());
        btnDownloadTemplate.addActionListener(e -> onDownloadTemplate());

        lblStatus = new JLabel("No file selected yet.");
        lblStatus.setFont(UiUtils.boldFont());
        lblStatus.setForeground(UiUtils.COLOR_TEXT_MUTED);

        btnBar.add(btnChooseFile);
        btnBar.add(btnDownloadTemplate);
        btnBar.add(new JLabel("   |   "));
        btnBar.add(lblStatus);

        topPanel.add(btnBar, BorderLayout.SOUTH);

        root.add(topPanel, BorderLayout.NORTH);

        // 2. CENTER: Preview Table
        String[] cols = {"Row", "Action", "ID", "Type", "Date", "Amount", "Via", "Category", "Sub-Category", "Person", "Validation Status"};
        modelPreview = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        tblPreview = new JTable(modelPreview);
        tblPreview.setRowHeight(28);
        tblPreview.setFont(UiUtils.FONT_REGULAR);
        tblPreview.getTableHeader().setFont(UiUtils.FONT_BOLD);
        tblPreview.setShowGrid(false);

        // Action Renderer with vector icons (CREATE: Green, UPDATE: Blue, DELETE: Red)
        tblPreview.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(CENTER);
                l.setFont(UiUtils.FONT_BOLD);
                String act = String.valueOf(value);
                if ("CREATE".equals(act)) {
                    l.setForeground(UiUtils.COLOR_INCOME);
                    l.setIcon(NavIcons.plus(11, UiUtils.COLOR_INCOME));
                } else if ("UPDATE".equals(act)) {
                    l.setForeground(UiUtils.COLOR_PRIMARY);
                    l.setIcon(NavIcons.edit(11, UiUtils.COLOR_PRIMARY));
                } else if ("DELETE".equals(act)) {
                    l.setForeground(UiUtils.COLOR_EXPENSE);
                    l.setIcon(NavIcons.trash(11, UiUtils.COLOR_EXPENSE));
                }
                return l;
            }
        });

        // Validation Status Renderer (Valid: Green, Error: Red)
        tblPreview.getColumnModel().getColumn(10).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                String msg = String.valueOf(value);
                l.setFont(UiUtils.FONT_BOLD);
                if ("Valid".equalsIgnoreCase(msg)) {
                    l.setForeground(UiUtils.COLOR_INCOME);
                    l.setIcon(NavIcons.statusDot(8, UiUtils.COLOR_INCOME));
                } else {
                    l.setForeground(UiUtils.COLOR_EXPENSE);
                    l.setIcon(NavIcons.statusDot(8, UiUtils.COLOR_EXPENSE));
                }
                return l;
            }
        });

        JScrollPane sp = new JScrollPane(tblPreview);
        sp.setBorder(UiUtils.createCardBorder());
        root.add(sp, BorderLayout.CENTER);

        // 3. BOTTOM: Action buttons
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bottomBar.setOpaque(false);

        JButton btnCancel = UiUtils.createSecondaryButton(I18n.get("btn.cancel"), NavIcons.cross(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnCancel.setFont(UiUtils.boldFont());
        btnCancel.addActionListener(e -> dispose());

        btnExecute = UiUtils.createSuccessButton(I18n.get("btn.apply"), NavIcons.check(13, Color.WHITE));
        btnExecute.setFont(UiUtils.boldFont());
        btnExecute.setEnabled(false);
        btnExecute.addActionListener(e -> onExecuteImport());

        bottomBar.add(btnCancel);
        bottomBar.add(btnExecute);
        root.add(bottomBar, BorderLayout.SOUTH);

        add(root, BorderLayout.CENTER);
    }

    /**
     * Prompts the user to save a blank CSV template pre-populated with header columns and sample rows.
     */
    private void onDownloadTemplate() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("nav_finance_template.csv"));
        fc.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File target = fc.getSelectedFile();
            if (!target.getName().toLowerCase().endsWith(".csv")) {
                target = new File(target.getAbsolutePath() + ".csv");
            }
            try {
                importService.generateCsvTemplate(target);
                UiUtils.showSuccess(this, "Template saved successfully to:\n" + target.getAbsolutePath());
            } catch (Exception ex) {
                UiUtils.showError(this, "Failed to save template: " + ex.getMessage());
            }
        }
    }

    /**
     * Prompts the user to pick an existing CSV file, parses and validates each row,
     * updates the preview table, and enables the execute button if valid rows exist.
     */
    private void onSelectFile() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            try {
                currentRecords = importService.parseAndValidateCsv(file);
                modelPreview.setRowCount(0);

                int validCount = 0;
                int errorCount = 0;

                for (CsvImportService.CsvImportRecord rec : currentRecords) {
                    if (rec.isValid()) validCount++;
                    else errorCount++;

                    modelPreview.addRow(new Object[]{
                            rec.getRowNumber(),
                            rec.getAction(),
                            rec.getId() != null ? rec.getId() : "-",
                            rec.getType() != null ? rec.getType().getDisplayName() : "-",
                            rec.getDate() != null ? rec.getDate().format(AppConfig.DATE_FORMATTER) : "-",
                            rec.getAmount() > 0 ? AppConfig.formatCurrency(rec.getAmount()) : "-",
                            rec.getPaymentVia() != null ? rec.getPaymentVia() : "-",
                            rec.getCategory() != null ? rec.getCategory() : "-",
                            rec.getSubCategory() != null ? rec.getSubCategory() : "-",
                            rec.getPersonName() != null ? rec.getPersonName() : "-",
                            rec.getStatusMessage()
                    });
                }

                lblStatus.setText(String.format("Loaded %d rows: %d Valid, %d Errors", currentRecords.size(), validCount, errorCount));
                if (errorCount > 0) {
                    lblStatus.setForeground(UiUtils.COLOR_EXPENSE);
                } else {
                    lblStatus.setForeground(UiUtils.COLOR_INCOME);
                }

                btnExecute.setEnabled(validCount > 0);

            } catch (Exception ex) {
                UiUtils.showError(this, "Failed to read CSV file: " + ex.getMessage());
            }
        }
    }

    /**
     * Confirms and executes the valid operations (CREATE, UPDATE, DELETE) against the database.
     */
    private void onExecuteImport() {
        if (currentRecords.isEmpty()) return;

        if (!UiUtils.confirm(this, "Are you sure you want to apply the valid CSV operations to the database?")) {
            return;
        }

        CsvImportService.ImportSummary summary = importService.executeImport(currentRecords);

        String msg = String.format("CSV Import Finished!\n\n- Created: %d\n- Updated: %d\n- Deleted: %d\n- Failed / Skipped: %d",
                summary.getCreatedCount(), summary.getUpdatedCount(), summary.getDeletedCount(), summary.getFailedCount());

        if (!summary.getErrorMessages().isEmpty()) {
            msg += "\n\nErrors encountered:\n" + String.join("\n", summary.getErrorMessages().subList(0, Math.min(5, summary.getErrorMessages().size())));
        }

        UiUtils.showSuccess(this, msg);
        imported = true;
        dispose();
    }

    /**
     * Indicates whether any records were successfully imported or updated.
     *
     * @return {@code true} if import operations were applied
     */
    public boolean isImported() {
        return imported;
    }
}
