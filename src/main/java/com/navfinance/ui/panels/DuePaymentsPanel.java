package com.navfinance.ui.panels;

import com.navfinance.config.AppConfig;
import com.navfinance.model.DuePayment;
import com.navfinance.model.RecurringSchedule;
import com.navfinance.model.TransactionType;
import com.navfinance.service.RecurringService;
import com.navfinance.ui.components.KpiCard;
import com.navfinance.ui.dialogs.RecurringScheduleDialog;
import com.navfinance.ui.dialogs.SettlePaymentDialog;
import com.navfinance.ui.util.NavIcons;
import com.navfinance.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Interactive management panel for tracking and settling upcoming, due, and overdue payment obligations,
 * as well as registering and maintaining recurring payment rules in NavFinance.
 * <p>
 * This dashboard provides:
 * <ul>
 *   <li>Top-level real-time KPI indicators for overdue totals, pending dues, settled amounts, and auto-debit ratios.</li>
 *   <li><b>Tab 1: Due &amp; Overdue Tracker</b> - A searchable, multi-criteria filtered grid displaying due obligations
 *       with direct one-click settlement dialog integration, ledger transaction generation, and un-settlement rollback.</li>
 *   <li><b>Tab 2: Registered Recurring Rules</b> - Full CRUD control over monthly schedules (e.g. EMIs, utility bills,
 *       credit card statements, rental income) with auto-pay flags and variable/fixed amount support.</li>
 * </ul>
 */
public class DuePaymentsPanel extends JPanel {

    /** Singleton business service managing recurring schedules and due payment state transitions. */
    private final RecurringService recurringService = RecurringService.getInstance();

    /** KPI metric card displaying aggregated overdue payments and count. */
    private KpiCard kpiOverdue;

    /** KPI metric card displaying upcoming pending payment obligations. */
    private KpiCard kpiDuePending;

    /** KPI metric card displaying total settled payments amount and count. */
    private KpiCard kpiSettled;

    /** KPI metric card displaying breakdown of auto-debit vs manual payments. */
    private KpiCard kpiAutoPay;

    /** Swing table displaying active due payment records. */
    private JTable tblDue;

    /** Underlying table model for due payments. */
    private DefaultTableModel modelDue;

    /** Filtered snapshot list of due payments currently rendered in the table. */
    private final List<DuePayment> currentDueList = new ArrayList<>();

    /** Search text field for keyword filtering across title, category, subcategory, and notes. */
    private JTextField txtSearch;

    /** Dropdown filter selector for due payment status (Overdue, Due/Pending, Settled, All). */
    private JComboBox<String> cmbStatusFilter;

    /** Dropdown filter selector for transaction flow (Expense, Income, All). */
    private JComboBox<String> cmbTypeFilter;

    /** Dropdown filter selector for execution mode (Auto-Payment vs Manual). */
    private JComboBox<String> cmbExecutionFilter;

    /** Dropdown filter selector for calendar period range. */
    private JComboBox<String> cmbPeriodFilter;

    /** Status label reporting total filtered entries out of all database records. */
    private JLabel lblFilteredCount;

    /** Swing table displaying registered recurring schedule templates. */
    private JTable tblRules;

    /** Underlying table model for recurring schedule rules. */
    private DefaultTableModel modelRules;

    /** Cached snapshot list of recurring rules currently loaded. */
    private List<RecurringSchedule> currentRulesList = new ArrayList<>();

    /**
     * Constructs a new {@code DuePaymentsPanel}, configuring responsive layout managers,
     * visual themes, KPI indicators, tabs, table renderers, and initial data binding.
     */
    public DuePaymentsPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        initComponents();
        refreshAll();
    }

    /**
     * Initializes all UI components, arranging KPI cards at the top and the sub-tabbed
     * panes in the center workspace.
     */
    private void initComponents() {
        // TOP: Four-column KPI metrics row
        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 12, 0));
        kpiRow.setOpaque(false);

        kpiOverdue = new KpiCard("Overdue Payments", "₹ 0.00", "0 past due date", UiUtils.COLOR_EXPENSE, NavIcons.cross(22, UiUtils.COLOR_EXPENSE));
        kpiDuePending = new KpiCard("Upcoming Due Payments", "₹ 0.00", "0 awaiting payment", UiUtils.COLOR_AMBER, NavIcons.calendar(22, UiUtils.COLOR_AMBER));
        kpiSettled = new KpiCard("Settled Payments", "₹ 0.00", "0 successfully paid", UiUtils.COLOR_INCOME, NavIcons.check(22, UiUtils.COLOR_INCOME));
        kpiAutoPay = new KpiCard("Auto-Debit vs Manual", "0 / 0", "Auto-debited vs user paid", UiUtils.COLOR_PRIMARY, NavIcons.autoPay(22, UiUtils.COLOR_PRIMARY));

        kpiRow.add(kpiOverdue);
        kpiRow.add(kpiDuePending);
        kpiRow.add(kpiSettled);
        kpiRow.add(kpiAutoPay);
        add(kpiRow, BorderLayout.NORTH);

        // CENTER: Tabbed View (Due Payments Tracker + Recurring Rules Manager)
        JTabbedPane subTabs = new JTabbedPane();
        subTabs.setFont(new Font("Segoe UI", Font.BOLD, 12));

        subTabs.addTab("Due & Overdue Tracker", NavIcons.calendar(14, UiUtils.COLOR_PRIMARY), createDueTrackerPanel());
        subTabs.addTab("Registered Recurring Rules", NavIcons.settings(14, UiUtils.COLOR_PRIMARY), createRulesManagerPanel());

        subTabs.addChangeListener(e -> {
            if (subTabs.getSelectedIndex() == 0) {
                applyDueFilters();
            } else {
                refreshRulesTable();
            }
        });

        add(subTabs, BorderLayout.CENTER);
    }

    // =========================================================================
    // Tab 1: Due & Overdue Tracker Panel
    // =========================================================================

    /**
     * Constructs the primary Due &amp; Overdue Tracker tab panel containing action buttons,
     * multi-parameter filter controls, interactive JTable, and footer statistics.
     *
     * @return fully assembled JPanel for due payment tracking
     */
    private JPanel createDueTrackerPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        // 1. Action & Filter Bar
        JPanel topContainer = new JPanel(new BorderLayout(0, 8));
        topContainer.setOpaque(false);

        // Action Buttons Row
        JPanel actionRow = new JPanel(new BorderLayout());
        actionRow.setOpaque(false);

        JLabel lblTitle = new JLabel("Payment Due Schedule & Settlement");
        lblTitle.setFont(UiUtils.FONT_TITLE);
        lblTitle.setForeground(new Color(15, 23, 42));
        actionRow.add(lblTitle, BorderLayout.WEST);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBar.setOpaque(false);

        JButton btnSettle = UiUtils.createSuccessButton("Settle / Pay Selected", NavIcons.check(13, Color.WHITE));
        btnSettle.addActionListener(e -> onSettleSelected());

        JButton btnUnsettle = UiUtils.createSecondaryButton("Mark Unsettled", NavIcons.refresh(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnUnsettle.addActionListener(e -> onMarkUnsettled());

        JButton btnNewSchedule = UiUtils.createPrimaryButton("Register Recurring Schedule", NavIcons.plus(13, Color.WHITE));
        btnNewSchedule.addActionListener(e -> onNewSchedule());

        JButton btnScan = UiUtils.createSecondaryButton("Scan Due Dates", NavIcons.calendar(13, UiUtils.COLOR_PRIMARY));
        btnScan.addActionListener(e -> {
            recurringService.generateDuePayments(2);
            refreshAll();
            UiUtils.showSuccess(this, "Due payment occurrences scanned and refreshed!");
        });

        JButton btnDelete = UiUtils.createSecondaryButton("Delete Entry", NavIcons.trash(13, UiUtils.COLOR_EXPENSE));
        btnDelete.addActionListener(e -> onDeleteSelectedDue());

        btnBar.add(btnSettle);
        btnBar.add(btnUnsettle);
        btnBar.add(btnNewSchedule);
        btnBar.add(btnScan);
        btnBar.add(btnDelete);

        actionRow.add(btnBar, BorderLayout.EAST);
        topContainer.add(actionRow, BorderLayout.NORTH);

        // Filters Card
        JPanel filterCard = new JPanel(new GridBagLayout());
        filterCard.setBackground(Color.WHITE);
        filterCard.setBorder(UiUtils.createCardBorder());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        filterCard.add(new JLabel("Search:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        txtSearch = new JTextField(12);
        txtSearch.setToolTipText("Search title, category, subcategory, or notes");
        txtSearch.addActionListener(e -> applyDueFilters());
        filterCard.add(txtSearch, gbc);

        gbc.gridx = 2; gbc.gridy = 0;
        filterCard.add(new JLabel("Status:"), gbc);
        gbc.gridx = 3; gbc.gridy = 0;
        cmbStatusFilter = new JComboBox<>(new String[]{"All", "Overdue Only", "Due / Pending", "Settled Only"});
        cmbStatusFilter.addActionListener(e -> applyDueFilters());
        filterCard.add(cmbStatusFilter, gbc);

        gbc.gridx = 4; gbc.gridy = 0;
        filterCard.add(new JLabel("Period:"), gbc);
        gbc.gridx = 5; gbc.gridy = 0;
        cmbPeriodFilter = new JComboBox<>(new String[]{"All Time", "Overdue + Next 30 Days", "Current Month", "Past 3 Months", "Next 3 Months"});
        cmbPeriodFilter.setSelectedIndex(0);
        cmbPeriodFilter.addActionListener(e -> applyDueFilters());
        filterCard.add(cmbPeriodFilter, gbc);

        gbc.gridx = 6; gbc.gridy = 0;
        filterCard.add(new JLabel("Mode:"), gbc);
        gbc.gridx = 7; gbc.gridy = 0;
        cmbExecutionFilter = new JComboBox<>(new String[]{"All", "Auto-Payment", "Manual Payment"});
        cmbExecutionFilter.addActionListener(e -> applyDueFilters());
        filterCard.add(cmbExecutionFilter, gbc);

        gbc.gridx = 8; gbc.gridy = 0;
        filterCard.add(new JLabel("Type:"), gbc);
        gbc.gridx = 9; gbc.gridy = 0;
        cmbTypeFilter = new JComboBox<>(new String[]{"All", "Expense (Out)", "Income (In)"});
        cmbTypeFilter.addActionListener(e -> applyDueFilters());
        filterCard.add(cmbTypeFilter, gbc);

        JButton btnApply = UiUtils.createSecondaryButton("Filter", NavIcons.filter(12, UiUtils.COLOR_PRIMARY));
        btnApply.addActionListener(e -> applyDueFilters());
        gbc.gridx = 10; gbc.gridy = 0;
        filterCard.add(btnApply, gbc);

        topContainer.add(filterCard, BorderLayout.SOUTH);
        panel.add(topContainer, BorderLayout.NORTH);

        // 2. Due Payments Table configuration
        String[] cols = {"ID", "Status", "Due Date", "Title", "Category", "Sub-Category", "Expected Amount", "Execution", "Settlement Info", "Via", "Notes"};
        modelDue = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        tblDue = new JTable(modelDue);
        tblDue.setRowHeight(30);
        tblDue.setFont(UiUtils.FONT_REGULAR);
        tblDue.getTableHeader().setFont(UiUtils.FONT_BOLD);
        tblDue.setShowGrid(false);

        // Hide ID column from UI display while preserving access to row entity IDs
        tblDue.getColumnModel().getColumn(0).setMinWidth(0);
        tblDue.getColumnModel().getColumn(0).setMaxWidth(0);
        tblDue.getColumnModel().getColumn(0).setPreferredWidth(0);

        // Custom Cell Renderers:
        // Status Column with vector status dot
        tblDue.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(CENTER);
                l.setFont(UiUtils.FONT_BOLD);
                String val = String.valueOf(value);
                if (val.contains("OVERDUE")) {
                    l.setForeground(UiUtils.COLOR_EXPENSE);
                    l.setIcon(NavIcons.statusDot(9, UiUtils.COLOR_EXPENSE));
                } else if (val.contains("DUE") || val.contains("UPCOMING")) {
                    l.setForeground(UiUtils.COLOR_AMBER);
                    l.setIcon(NavIcons.statusDot(9, UiUtils.COLOR_AMBER));
                } else if (val.contains("SETTLED")) {
                    l.setForeground(UiUtils.COLOR_INCOME);
                    l.setIcon(NavIcons.statusDot(9, UiUtils.COLOR_INCOME));
                }
                return l;
            }
        });

        // Execution Mode Column with vector icon
        tblDue.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(CENTER);
                l.setFont(UiUtils.FONT_BOLD);
                String val = String.valueOf(value);
                if (val.contains("Auto")) {
                    Color c = new Color(14, 116, 144);
                    l.setForeground(c);
                    l.setIcon(NavIcons.autoPay(12, c));
                } else {
                    Color c = new Color(180, 83, 9);
                    l.setForeground(c);
                    l.setIcon(NavIcons.manualPay(12, c));
                }
                return l;
            }
        });

        // Double-click row: Prompt settlement dialog directly
        tblDue.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onSettleSelected();
                }
            }
        });

        JScrollPane spDue = new JScrollPane(tblDue);
        spDue.setBorder(UiUtils.createCardBorder());
        panel.add(spDue, BorderLayout.CENTER);

        // Bottom status count indicator
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bottomBar.setOpaque(false);
        lblFilteredCount = new JLabel("Showing 0 due entries");
        lblFilteredCount.setFont(UiUtils.FONT_SUBTITLE);
        lblFilteredCount.setForeground(UiUtils.COLOR_TEXT_MUTED);
        bottomBar.add(lblFilteredCount);
        panel.add(bottomBar, BorderLayout.SOUTH);

        return panel;
    }

    // =========================================================================
    // Tab 2: Recurring Schedule Rules Manager
    // =========================================================================

    /**
     * Constructs the Recurring Rules Manager tab panel allowing users to define,
     * edit, inspect, and delete monthly recurring payment rules.
     *
     * @return fully assembled JPanel for schedule rules configuration
     */
    private JPanel createRulesManagerPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JLabel lblTitle = new JLabel("Registered Recurring Payment Schedules");
        lblTitle.setFont(UiUtils.FONT_TITLE);
        lblTitle.setForeground(new Color(15, 23, 42));
        topBar.add(lblTitle, BorderLayout.WEST);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBar.setOpaque(false);

        JButton btnAddRule = UiUtils.createPrimaryButton("Add Recurring Rule", NavIcons.plus(13, Color.WHITE));
        btnAddRule.addActionListener(e -> onNewSchedule());

        JButton btnEditRule = UiUtils.createSecondaryButton("Edit Rule", NavIcons.edit(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnEditRule.addActionListener(e -> onEditSelectedRule());

        JButton btnDeleteRule = UiUtils.createSecondaryButton("Delete Rule", NavIcons.trash(13, UiUtils.COLOR_EXPENSE));
        btnDeleteRule.addActionListener(e -> onDeleteSelectedRule());

        btnBar.add(btnAddRule);
        btnBar.add(btnEditRule);
        btnBar.add(btnDeleteRule);
        topBar.add(btnBar, BorderLayout.EAST);

        panel.add(topBar, BorderLayout.NORTH);

        String[] cols = {"ID", "Name", "Type", "Category", "Sub-Category", "Day of Month", "Amount Type", "Default Amount", "Date Range", "Execution Mode", "Channel", "Active"};
        modelRules = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        tblRules = new JTable(modelRules);
        tblRules.setRowHeight(28);
        tblRules.setFont(UiUtils.FONT_REGULAR);
        tblRules.getTableHeader().setFont(UiUtils.FONT_BOLD);
        tblRules.setShowGrid(false);

        // Hide ID column
        tblRules.getColumnModel().getColumn(0).setMinWidth(0);
        tblRules.getColumnModel().getColumn(0).setMaxWidth(0);
        tblRules.getColumnModel().getColumn(0).setPreferredWidth(0);

        tblRules.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onEditSelectedRule();
                }
            }
        });

        JScrollPane spRules = new JScrollPane(tblRules);
        spRules.setBorder(UiUtils.createCardBorder());
        panel.add(spRules, BorderLayout.CENTER);

        return panel;
    }

    // =========================================================================
    // Filter & Refresh Logic
    // =========================================================================

    /**
     * Refreshes both tabs and recalculates all top-level KPI metrics.
     * Declared final to safeguard constructor invocation.
     */
    public final void refreshAll() {
        applyDueFilters();
        refreshRulesTable();
    }

    /**
     * Queries all due payment records from {@link RecurringService}, applies user-selected
     * search text, status, calendar period, payment mode, and transaction type filters,
     * updates the JTable model, and updates the top KPI metric cards.
     */
    public void applyDueFilters() {
        List<DuePayment> all = recurringService.getAllDuePayments();
        currentDueList.clear();
        modelDue.setRowCount(0);

        String query = txtSearch != null ? txtSearch.getText().trim().toLowerCase() : "";
        String statusFilter = cmbStatusFilter != null ? (String) cmbStatusFilter.getSelectedItem() : "All";
        String periodFilter = cmbPeriodFilter != null ? (String) cmbPeriodFilter.getSelectedItem() : "All Time";
        String modeFilter = cmbExecutionFilter != null ? (String) cmbExecutionFilter.getSelectedItem() : "All";
        String typeFilter = cmbTypeFilter != null ? (String) cmbTypeFilter.getSelectedItem() : "All";

        LocalDate today = LocalDate.now();

        for (DuePayment p : all) {
            // Text Search filter across multiple descriptive fields
            if (!query.isEmpty()) {
                String hay = (p.getTitle() + " " + p.getCategory() + " " + p.getSubCategory() + " " + p.getNotes()).toLowerCase();
                if (!hay.contains(query)) {
                    continue;
                }
            }

            // Status Filter validation
            boolean isOverdue = p.isOverdue();
            boolean isSettled = p.isSettled();
            if ("Overdue Only".equals(statusFilter) && !isOverdue) continue;
            if ("Due / Pending".equals(statusFilter) && (isOverdue || isSettled)) continue;
            if ("Settled Only".equals(statusFilter) && !isSettled) continue;

            // Execution Mode Filter (Auto-Pay vs Manual)
            if ("Auto-Payment".equals(modeFilter) && !p.isAutoPay()) continue;
            if ("Manual Payment".equals(modeFilter) && p.isAutoPay()) continue;

            // Transaction Type Filter (Expense vs Income)
            if ("Expense (Out)".equals(typeFilter) && p.getType() != TransactionType.EXPENSE) continue;
            if ("Income (In)".equals(typeFilter) && p.getType() != TransactionType.INCOME) continue;

            // Calendar Period Filter
            if (p.getDueDate() != null && periodFilter != null) {
                boolean skip = switch (periodFilter) {
                    case "Overdue + Next 30 Days" -> p.getDueDate().isAfter(today.plusDays(30));
                    case "Current Month" -> p.getDueDate().getYear() != today.getYear() || p.getDueDate().getMonth() != today.getMonth();
                    case "Past 3 Months" -> p.getDueDate().isBefore(today.minusMonths(3)) || p.getDueDate().isAfter(today);
                    case "Next 3 Months" -> p.getDueDate().isBefore(today) || p.getDueDate().isAfter(today.plusMonths(3));
                    default -> false;
                };
                if (skip) {
                    continue;
                }
            }

            currentDueList.add(p);

            // Build visual status badge
            String statusBadge;
            if (p.isSettled()) {
                statusBadge = "SETTLED";
            } else if (p.isOverdue()) {
                statusBadge = "OVERDUE (+" + p.getDaysOverdue() + "d)";
            } else {
                long days = ChronoUnit.DAYS.between(today, p.getDueDate());
                statusBadge = days == 0 ? "DUE TODAY" : "DUE (" + days + "d)";
            }

            String strAmount = p.isVariable() ?
                    (p.getExpectedAmount() > 0 ? "Variable (~" + AppConfig.formatCurrency(p.getExpectedAmount()) + ")" : "Variable (Enter amount)") :
                    AppConfig.formatCurrency(p.getExpectedAmount());

            String settlementInfo = p.isSettled() ?
                    AppConfig.formatCurrency(p.getSettledAmount()) + " on " + (p.getSettledDate() != null ? p.getSettledDate().format(AppConfig.DATE_FORMATTER) : "-") :
                    "-";

            modelDue.addRow(new Object[]{
                    p.getId(),
                    statusBadge,
                    p.getDueDate() != null ? p.getDueDate().format(AppConfig.DATE_FORMATTER) : "-",
                    p.getTitle(),
                    p.getCategory(),
                    p.getSubCategory() != null ? p.getSubCategory() : "-",
                    strAmount,
                    p.getAutoPayDisplay(),
                    settlementInfo,
                    p.isSettled() ? p.getSettledVia() : "-",
                    p.getNotes() != null ? p.getNotes() : ""
            });
        }

        // Recompute KPI Cards metrics from all active database entries
        RecurringService.DueSummary summary = recurringService.computeSummary(all);
        kpiOverdue.setValue(AppConfig.formatCurrency(summary.getOverdueTotal()));
        kpiOverdue.setSubtitle(summary.getOverdueCount() + " payments overdue");

        kpiDuePending.setValue(AppConfig.formatCurrency(summary.getDuePendingTotal()));
        kpiDuePending.setSubtitle(summary.getDuePendingCount() + " upcoming due");

        kpiSettled.setValue(AppConfig.formatCurrency(summary.getSettledTotal()));
        kpiSettled.setSubtitle(summary.getSettledCount() + " settled payments");

        kpiAutoPay.setValue(summary.getAutoPayCount() + " Auto / " + summary.getManualPayCount() + " Manual");
        kpiAutoPay.setSubtitle("Auto-debits vs Manual checks");

        if (lblFilteredCount != null) {
            lblFilteredCount.setText("Showing " + currentDueList.size() + " of " + all.size() + " total due entries");
        }
    }

    /**
     * Reloads all recurring schedule definitions from {@link RecurringService} and populates
     * the schedule rules table.
     */
    private void refreshRulesTable() {
        currentRulesList = recurringService.getAllSchedules();
        modelRules.setRowCount(0);

        for (RecurringSchedule s : currentRulesList) {
            String dateRange = (s.getStartDate() != null ? s.getStartDate().format(DateTimeFormatter.ofPattern("MMM yyyy")) : "-") +
                    " to " + (s.getEndDate() != null ? s.getEndDate().format(DateTimeFormatter.ofPattern("MMM yyyy")) : "Ongoing");

            modelRules.addRow(new Object[]{
                    s.getId(),
                    s.getName(),
                    s.getType(),
                    s.getCategory(),
                    s.getSubCategory() != null ? s.getSubCategory() : "-",
                    s.getDayOfMonth() + "th of month",
                    s.getAmountType().getDisplayName(),
                    s.getDefaultAmount() > 0 ? AppConfig.formatCurrency(s.getDefaultAmount()) : "Variable",
                    dateRange,
                    s.getAutoPayDisplay(),
                    s.getPaymentVia(),
                    s.isActive() ? "Active" : "Paused"
            });
        }
    }

    // =========================================================================
    // User Action Handlers
    // =========================================================================

    /**
     * Handles payment settlement for the currently selected record in the due table.
     * Opens {@link SettlePaymentDialog} allowing the user to confirm final paid amount,
     * payment channel, and optionally record an expense transaction in the primary ledger.
     */
    private void onSettleSelected() {
        int selectedRow = tblDue.getSelectedRow();
        if (selectedRow < 0) {
            UiUtils.showWarning(this, "Please select a due payment entry to settle.");
            return;
        }

        Long id = (Long) modelDue.getValueAt(selectedRow, 0);
        DuePayment payment = recurringService.getDuePaymentById(id);
        if (payment == null) return;

        if (payment.isSettled()) {
            UiUtils.showInfo(this, "This payment is already marked as SETTLED for " +
                    AppConfig.formatCurrency(payment.getSettledAmount()) + " on " + payment.getSettledDate());
            return;
        }

        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        SettlePaymentDialog dlg = new SettlePaymentDialog(parent, payment);
        dlg.setVisible(true);

        if (dlg.isSaved()) {
            refreshAll();
        }
    }

    /**
     * Reverts a previously settled payment back to pending status.
     * If a ledger transaction was generated during settlement, the user is prompted
     * with the option to delete or retain the linked transaction record.
     */
    private void onMarkUnsettled() {
        int selectedRow = tblDue.getSelectedRow();
        if (selectedRow < 0) {
            UiUtils.showWarning(this, "Please select an entry to mark unsettled.");
            return;
        }

        Long id = (Long) modelDue.getValueAt(selectedRow, 0);
        DuePayment payment = recurringService.getDuePaymentById(id);
        if (payment == null) return;

        if (!payment.isSettled()) {
            UiUtils.showInfo(this, "This payment is already pending / unsettled.");
            return;
        }

        boolean deleteTx = false;
        if (payment.getTransactionId() != null) {
            deleteTx = UiUtils.confirm(this,
                    "This payment has a linked transaction in your Expenses/Income ledger (#" + payment.getTransactionId() + ").\n\n" +
                            "Would you like to DELETE the linked transaction from your ledger as well?");
        } else {
            if (!UiUtils.confirm(this, "Are you sure you want to revert this payment to Unsettled?")) {
                return;
            }
        }

        boolean ok = recurringService.markUnsettled(id, deleteTx);
        if (ok) {
            UiUtils.showSuccess(this, "Payment marked as Unsettled successfully.");
            refreshAll();
        } else {
            UiUtils.showError(this, "Failed to revert payment.");
        }
    }

    /**
     * Opens the modal dialog to configure and register a new recurring payment schedule.
     */
    private void onNewSchedule() {
        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        RecurringScheduleDialog dlg = new RecurringScheduleDialog(parent, null);
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            refreshAll();
        }
    }

    /**
     * Opens the modal dialog to edit the selected recurring payment rule template.
     */
    private void onEditSelectedRule() {
        int selectedRow = tblRules.getSelectedRow();
        if (selectedRow < 0) {
            UiUtils.showWarning(this, "Please select a recurring rule to edit.");
            return;
        }

        Long id = (Long) modelRules.getValueAt(selectedRow, 0);
        RecurringSchedule rule = recurringService.getScheduleById(id);
        if (rule == null) return;

        JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
        RecurringScheduleDialog dlg = new RecurringScheduleDialog(parent, rule);
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            refreshAll();
        }
    }

    /**
     * Prompts for confirmation and deletes the selected recurring payment schedule rule.
     */
    private void onDeleteSelectedRule() {
        int selectedRow = tblRules.getSelectedRow();
        if (selectedRow < 0) {
            UiUtils.showWarning(this, "Please select a recurring rule to delete.");
            return;
        }

        Long id = (Long) modelRules.getValueAt(selectedRow, 0);
        RecurringSchedule rule = recurringService.getScheduleById(id);
        if (rule == null) return;

        if (UiUtils.confirm(this, "Are you sure you want to delete recurring rule '" + rule.getName() + "'?\n" +
                "Future generated due payments will not be affected unless manually removed.")) {
            boolean ok = recurringService.deleteSchedule(id);
            if (ok) {
                UiUtils.showSuccess(this, "Recurring rule deleted.");
                refreshRulesTable();
            } else {
                UiUtils.showError(this, "Failed to delete recurring rule.");
            }
        }
    }

    /**
     * Prompts for confirmation and removes an individual due payment record from the database.
     */
    private void onDeleteSelectedDue() {
        int selectedRow = tblDue.getSelectedRow();
        if (selectedRow < 0) {
            UiUtils.showWarning(this, "Please select a due entry to delete.");
            return;
        }

        Long id = (Long) modelDue.getValueAt(selectedRow, 0);
        DuePayment payment = recurringService.getDuePaymentById(id);
        if (payment == null) return;

        if (UiUtils.confirm(this, "Are you sure you want to delete due entry: " + payment.getTitle() + " (Due: " + payment.getDueDate() + ")?")) {
            boolean ok = recurringService.deleteDuePayment(id);
            if (ok) {
                UiUtils.showSuccess(this, "Due payment entry removed.");
                applyDueFilters();
            } else {
                UiUtils.showError(this, "Failed to delete entry.");
            }
        }
    }
}
