package com.navfinance.ui.panels;

import com.navfinance.config.AppConfig;
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
 * </p>
 */
public class AuditLogPanel extends JPanel {

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
        JLabel lblTitle = new JLabel("Audit Log & System Change History");
        lblTitle.setFont(UiUtils.FONT_TITLE);
        lblTitle.setForeground(new Color(15, 23, 42));

        JLabel lblSub = new JLabel("Immutable trail of creations, modifications, and deletions with timestamps & diffs.");
        lblSub.setFont(UiUtils.FONT_SUBTITLE);
        lblSub.setForeground(UiUtils.COLOR_TEXT_MUTED);

        titlePanel.add(lblTitle, BorderLayout.NORTH);
        titlePanel.add(lblSub, BorderLayout.SOUTH);
        topPanel.add(titlePanel, BorderLayout.WEST);

        // Filter / Search bar on the right
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchBar.setOpaque(false);

        searchBar.add(new JLabel("Action:"));
        cmbActionFilter = new JComboBox<>(new String[]{"All Actions", "CREATE", "UPDATE", "DELETE"});
        cmbActionFilter.addActionListener(e -> refreshLogs());
        searchBar.add(cmbActionFilter);

        searchBar.add(new JLabel("Search:"));
        txtSearch = new JTextField(14);
        txtSearch.setToolTipText("Filter details or entity");
        txtSearch.addActionListener(e -> refreshLogs());
        searchBar.add(txtSearch);

        JButton btnSearch = UiUtils.createPrimaryButton("Filter", NavIcons.filter(12, Color.WHITE));
        btnSearch.addActionListener(e -> refreshLogs());
        searchBar.add(btnSearch);

        JButton btnRefresh = UiUtils.createSecondaryButton("Refresh", NavIcons.refresh(12, UiUtils.COLOR_TEXT_PRIMARY));
        btnRefresh.addActionListener(e -> {
            txtSearch.setText("");
            cmbActionFilter.setSelectedIndex(0);
            refreshLogs();
        });
        searchBar.add(btnRefresh);

        topPanel.add(searchBar, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 2. CENTER: Audit Table
        String[] cols = {"Log ID", "Date & Time", "Action", "Entity", "Entity ID", "Change Details / Diff"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        tblAudit = new JTable(tableModel);
        tblAudit.setRowHeight(30);
        tblAudit.setFont(UiUtils.FONT_REGULAR);
        tblAudit.getTableHeader().setFont(UiUtils.FONT_BOLD);
        tblAudit.setShowGrid(false);
        tblAudit.setIntercellSpacing(new Dimension(0, 0));

        // Format Action column with vector icon
        tblAudit.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(CENTER);
                l.setFont(UiUtils.FONT_BOLD);
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

        String actFilter = (String) cmbActionFilter.getSelectedItem();
        String search = txtSearch.getText().trim().toLowerCase();

        for (AuditLog log : logs) {
            // Filter by Action Type
            if (actFilter != null && !actFilter.equals("All Actions") && !actFilter.equalsIgnoreCase(log.getAction())) {
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
}
