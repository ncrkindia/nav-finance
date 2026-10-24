package com.navfinance.ui.panels;

import com.navfinance.config.AppConfig;
import com.navfinance.i18n.AppLanguage;
import com.navfinance.i18n.I18n;
import com.navfinance.i18n.LanguageChangeListener;
import com.navfinance.model.AuditLog;
import com.navfinance.service.FinanceService;
import com.navfinance.ui.util.NavIcons;
import com.navfinance.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;

/**
 * Historical audit trail viewer panel displaying an immutable ledger of system actions.
 * <p>
 * Provides real-time searching, action type filtering (CREATE, UPDATE, DELETE), color-coded
 * status indicators, and detailed before-and-after change diffs with timestamps.
 * Fully localized for English, Hindi, and Hinglish.
 * </p>
 */
public class AuditLogPanel extends JPanel implements LanguageChangeListener {

    /** Service layer coordinating audit record retrieval. */
    private final FinanceService financeService = FinanceService.getInstance();

    /** Audit history table component. */
    private JTable tblAudit;

    /** Table model backing the audit log grid. */
    private DefaultTableModel tableModel;

    /** Text input field for keyword searching within change details and entity names. */
    private JTextField txtSearch;

    /** Dropdown filter for isolating specific audit actions (All, CREATE, UPDATE, DELETE). */
    private JComboBox<String> cmbActionFilter;

    /**
     * Constructs the Audit Log panel layout and populates initial history entries.
     */
    public AuditLogPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        initComponents();
        I18n.addListener(this);
        refreshLogs();
    }

    /**
     * Initializes header bar, action filter controls, search text box, and data table.
     */
    private void initComponents() {
        // 1. TOP: Header & Search Bar
        JPanel topPanel = new JPanel(new BorderLayout(15, 0));
        topPanel.setOpaque(false);

        JPanel titlePanel = new JPanel(new BorderLayout(0, 4));
        titlePanel.setOpaque(false);
        JLabel lblTitle = new JLabel(I18n.get("audit.title"));
        lblTitle.setFont(UiUtils.titleFont());
        lblTitle.setForeground(new Color(15, 23, 42));

        JLabel lblSub = new JLabel(I18n.get("audit.subtitle"));
        lblSub.setFont(UiUtils.subtitleFont());
        lblSub.setForeground(UiUtils.COLOR_TEXT_MUTED);

        titlePanel.add(lblTitle, BorderLayout.NORTH);
        titlePanel.add(lblSub, BorderLayout.SOUTH);
        topPanel.add(titlePanel, BorderLayout.WEST);

        // Filter / Search bar on the right
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchBar.setOpaque(false);

        JLabel lblAction = new JLabel(I18n.get("audit.filter.action"));
        lblAction.setFont(UiUtils.regularFont());
        searchBar.add(lblAction);

        cmbActionFilter = new JComboBox<>(new String[]{
                I18n.get("audit.filter.allActions"),
                I18n.get("audit.filter.create"),
                I18n.get("audit.filter.update"),
                I18n.get("audit.filter.delete")
        });
        cmbActionFilter.setFont(UiUtils.regularFont());
        cmbActionFilter.addActionListener(e -> refreshLogs());
        searchBar.add(cmbActionFilter);

        JLabel lblSearch = new JLabel(I18n.get("transactions.filter.search"));
        lblSearch.setFont(UiUtils.regularFont());
        searchBar.add(lblSearch);

        txtSearch = new JTextField(14);
        txtSearch.setFont(UiUtils.regularFont());
        txtSearch.addActionListener(e -> refreshLogs());
        searchBar.add(txtSearch);

        JButton btnSearch = UiUtils.createPrimaryButton(I18n.get("transactions.filter.apply"), NavIcons.filter(12, Color.WHITE));
        btnSearch.setFont(UiUtils.boldFont());
        btnSearch.addActionListener(e -> refreshLogs());
        searchBar.add(btnSearch);

        JButton btnRefresh = UiUtils.createSecondaryButton(I18n.get("transactions.filter.reset"), NavIcons.refresh(12, UiUtils.COLOR_TEXT_PRIMARY));
        btnRefresh.setFont(UiUtils.boldFont());
        btnRefresh.addActionListener(e -> {
            txtSearch.setText("");
            cmbActionFilter.setSelectedIndex(0);
            refreshLogs();
        });
        searchBar.add(btnRefresh);

        topPanel.add(searchBar, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 2. CENTER: Audit Table
        String[] cols = {
                I18n.get("transactions.table.id"),
                I18n.get("audit.table.time"),
                I18n.get("audit.table.action"),
                I18n.get("audit.table.entity"),
                I18n.get("audit.table.entityId"),
                I18n.get("audit.table.details")
        };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        tblAudit = new JTable(tableModel);
        tblAudit.setRowHeight(30);
        tblAudit.setFont(UiUtils.regularFont());
        tblAudit.getTableHeader().setFont(UiUtils.boldFont());
        tblAudit.setShowGrid(false);
        tblAudit.setIntercellSpacing(new Dimension(0, 0));

        // Format Action column with vector icon
        tblAudit.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(CENTER);
                l.setFont(UiUtils.boldFont());
                String act = String.valueOf(value);
                switch (act) {
                    case "CREATE" -> {
                        l.setForeground(UiUtils.COLOR_INCOME);
                        l.setIcon(NavIcons.plus(11, UiUtils.COLOR_INCOME));
                    }
                    case "UPDATE" -> {
                        l.setForeground(UiUtils.COLOR_PRIMARY);
                        l.setIcon(NavIcons.edit(11, UiUtils.COLOR_PRIMARY));
                    }
                    case "DELETE" -> {
                        l.setForeground(UiUtils.COLOR_EXPENSE);
                        l.setIcon(NavIcons.trash(11, UiUtils.COLOR_EXPENSE));
                    }
                    default -> {
                        l.setForeground(UiUtils.COLOR_AMBER);
                        l.setIcon(NavIcons.statusDot(8, UiUtils.COLOR_AMBER));
                    }
                }
                return l;
            }
        });

        // Set column widths
        tblAudit.getColumnModel().getColumn(0).setPreferredWidth(60);
        tblAudit.getColumnModel().getColumn(1).setPreferredWidth(150);
        tblAudit.getColumnModel().getColumn(2).setPreferredWidth(90);
        tblAudit.getColumnModel().getColumn(3).setPreferredWidth(100);
        tblAudit.getColumnModel().getColumn(4).setPreferredWidth(70);
        tblAudit.getColumnModel().getColumn(5).setPreferredWidth(450);

        JScrollPane sp = new JScrollPane(tblAudit);
        sp.setBorder(UiUtils.createCardBorder());
        add(sp, BorderLayout.CENTER);
    }

    /**
     * Queries recent audit log entries from the database, applies action and keyword filters,
     * and refreshes the table view.
     */
    public final void refreshLogs() {
        List<AuditLog> logs = financeService.getAuditLogs(300);
        tableModel.setRowCount(0);

        int selectedAction = cmbActionFilter != null ? cmbActionFilter.getSelectedIndex() : 0;
        String search = txtSearch != null ? txtSearch.getText().trim().toLowerCase() : "";

        for (AuditLog log : logs) {
            // Index 0 is "All Actions", Index 1 is CREATE, Index 2 is UPDATE, Index 3 is DELETE
            if (selectedAction == 1 && !"CREATE".equalsIgnoreCase(log.getAction())) {
                continue;
            }
            if (selectedAction == 2 && !"UPDATE".equalsIgnoreCase(log.getAction())) {
                continue;
            }
            if (selectedAction == 3 && !"DELETE".equalsIgnoreCase(log.getAction())) {
                continue;
            }

            // Filter by search keyword
            if (!search.isEmpty()) {
                String d = log.getDetails() != null ? log.getDetails().toLowerCase() : "";
                String e = log.getEntityType() != null ? log.getEntityType().toLowerCase() : "";
                if (!d.contains(search) && !e.contains(search)) {
                    continue;
                }
            }

            tableModel.addRow(new Object[]{
                    log.getId(),
                    log.getTimestamp() != null ? log.getTimestamp().format(AppConfig.DATE_TIME_FORMATTER) : "-",
                    log.getAction(),
                    log.getEntityType(),
                    log.getEntityId() != null ? log.getEntityId() : "-",
                    log.getDetails()
            });
        }
    }

    @Override
    public void onLanguageChanged(AppLanguage newLanguage) {
        removeAll();
        initComponents();
        revalidate();
        repaint();
        refreshLogs();
    }
}

