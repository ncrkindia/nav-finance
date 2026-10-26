package com.navfinance.ui.panels;

import com.navfinance.config.AppConfig;
import com.navfinance.i18n.AppLanguage;
import com.navfinance.i18n.I18n;
import com.navfinance.i18n.LanguageChangeListener;
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
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
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
public class DuePaymentsPanel extends JPanel implements LanguageChangeListener {

    /** Singleton business service managing recurring schedules and due payment state transitions. */
    private final RecurringService recurringService = RecurringService.getInstance();

    /** CardLayout for swapping top KPI metrics between Tracker dues and Recurring commitments. */
    private CardLayout kpiCardLayout;

    /** Top container panel hosting Tracker KPIs deck and Rules Commitment KPIs deck. */
    private JPanel pnlKpiContainer;

    /** KPI metric card displaying aggregated overdue payments and count. */
    private KpiCard kpiOverdue;

    /** KPI metric card displaying upcoming pending payment obligations. */
    private KpiCard kpiDuePending;

    /** KPI metric card displaying total settled payments amount and count. */
    private KpiCard kpiSettled;

    /** KPI metric card displaying breakdown of auto-debit vs manual payments. */
    private KpiCard kpiAutoPay;

    /** KPI metric card displaying active monthly loan EMI commitments. */
    private KpiCard kpiRuleEmi;

    /** KPI metric card displaying total recurring commitments of Fixed Utility Bills + Variables. */
    private KpiCard kpiRuleTotalBills;

    /** KPI metric card displaying recurring commitment total for fixed-amount utility bills. */
    private KpiCard kpiRuleFixedBills;

    /** KPI metric card displaying estimated recurring commitment total for variable utility bills. */
    private KpiCard kpiRuleVariableBills;

    /** Summary label in recurring rules tab reporting total active rules and grand recurring commitment. */
    private JLabel lblRulesCommitmentSummary;

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
        I18n.addListener(this);
        refreshAll();
    }

    /**
     * Initializes all UI components, arranging KPI cards at the top and the sub-tabbed
     * panes in the center workspace.
     */
    private void initComponents() {
        // TOP: Two-deck CardLayout KPI metrics row (Tracker dues vs Rules commitments)
        kpiCardLayout = new CardLayout();
        pnlKpiContainer = new JPanel(kpiCardLayout);
        pnlKpiContainer.setOpaque(false);

        // Deck 1: Tracker Due/Overdue KPIs
        JPanel kpiRowTracker = new JPanel(new GridLayout(1, 4, 12, 0));
        kpiRowTracker.setOpaque(false);
        kpiOverdue = new KpiCard(I18n.get("due.kpi.overdue"), "₹ 0.00", "0 past due date", UiUtils.COLOR_EXPENSE, NavIcons.cross(22, UiUtils.COLOR_EXPENSE));
        kpiDuePending = new KpiCard(I18n.get("due.kpi.dueThisMonth"), "₹ 0.00", "0 awaiting payment", UiUtils.COLOR_AMBER, NavIcons.calendar(22, UiUtils.COLOR_AMBER));
        kpiSettled = new KpiCard(I18n.get("due.kpi.settledThisMonth"), "₹ 0.00", "0 successfully paid", UiUtils.COLOR_INCOME, NavIcons.check(22, UiUtils.COLOR_INCOME));
        kpiAutoPay = new KpiCard(I18n.get("due.kpi.autoVsManual"), "0 / 0", "Auto-debited vs user paid", UiUtils.COLOR_PRIMARY, NavIcons.autoPay(22, UiUtils.COLOR_PRIMARY));
        kpiRowTracker.add(kpiOverdue);
        kpiRowTracker.add(kpiDuePending);
        kpiRowTracker.add(kpiSettled);
        kpiRowTracker.add(kpiAutoPay);
        pnlKpiContainer.add(kpiRowTracker, "TRACKER");

        // Deck 2: Recurring Rules Commitments KPIs (EMIs + Fixed Utility Bills + Variables)
        JPanel kpiRowRules = new JPanel(new GridLayout(1, 4, 12, 0));
        kpiRowRules.setOpaque(false);
        kpiRuleEmi = new KpiCard(I18n.get("due.kpi.emiCommitment"), AppConfig.formatCurrency(0.0), "Active loan EMIs", UiUtils.COLOR_AMBER, NavIcons.emi(22, UiUtils.COLOR_AMBER));
        kpiRuleTotalBills = new KpiCard(I18n.get("due.kpi.totalBillCommitment"), AppConfig.formatCurrency(0.0), "Fixed + Variable Utility Bills", UiUtils.COLOR_PRIMARY, NavIcons.bill(22, UiUtils.COLOR_PRIMARY));
        kpiRuleFixedBills = new KpiCard(I18n.get("due.kpi.fixedBills"), AppConfig.formatCurrency(0.0), "Fixed utility bills", UiUtils.COLOR_INCOME, NavIcons.check(22, UiUtils.COLOR_INCOME));
        kpiRuleVariableBills = new KpiCard(I18n.get("due.kpi.variableBills"), AppConfig.formatCurrency(0.0), "Variable bills (estimated)", new Color(147, 51, 234), NavIcons.calendar(22, new Color(147, 51, 234)));
        kpiRowRules.add(kpiRuleEmi);
        kpiRowRules.add(kpiRuleTotalBills);
        kpiRowRules.add(kpiRuleFixedBills);
        kpiRowRules.add(kpiRuleVariableBills);
        pnlKpiContainer.add(kpiRowRules, "RULES");

        add(pnlKpiContainer, BorderLayout.NORTH);

        // CENTER: Tabbed View (Due Payments Tracker + Recurring Rules Manager)
        JTabbedPane subTabs = new JTabbedPane();
        subTabs.setFont(UiUtils.boldFont());

        subTabs.addTab(I18n.get("due.tab.tracker"), NavIcons.calendar(14, UiUtils.COLOR_PRIMARY), createDueTrackerPanel());
        subTabs.addTab(I18n.get("due.tab.rules"), NavIcons.settings(14, UiUtils.COLOR_PRIMARY), createRulesManagerPanel());

        subTabs.addChangeListener(e -> {
            if (subTabs.getSelectedIndex() == 0) {
                if (kpiCardLayout != null && pnlKpiContainer != null) {
                    kpiCardLayout.show(pnlKpiContainer, "TRACKER");
                }
                applyDueFilters();
            } else {
                if (kpiCardLayout != null && pnlKpiContainer != null) {
                    kpiCardLayout.show(pnlKpiContainer, "RULES");
                }
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

        JLabel lblTitle = new JLabel(I18n.get("due.title"));
        lblTitle.setFont(UiUtils.titleFont());
        lblTitle.setForeground(new Color(15, 23, 42));
        actionRow.add(lblTitle, BorderLayout.WEST);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBar.setOpaque(false);

        JButton btnSettle = UiUtils.createSuccessButton(I18n.get("due.btn.settleSelected"), NavIcons.check(13, Color.WHITE));
        btnSettle.addActionListener(e -> onSettleSelected());

        JButton btnSettleOnTime = UiUtils.createPrimaryButton(I18n.get("due.btn.settleOnTime"), NavIcons.check(13, Color.WHITE));
        btnSettleOnTime.setToolTipText("Settle selected payments (or all September 2026 dues) on their scheduled due dates");
        btnSettleOnTime.addActionListener(e -> onSettleOnTime());

        JButton btnUnsettle = UiUtils.createSecondaryButton(I18n.get("due.btn.markUnsettled"), NavIcons.refresh(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnUnsettle.addActionListener(e -> onMarkUnsettled());

        JButton btnNewSchedule = UiUtils.createSecondaryButton(I18n.get("due.btn.newSchedule"), NavIcons.plus(13, UiUtils.COLOR_PRIMARY));
        btnNewSchedule.addActionListener(e -> onNewSchedule());

        JButton btnScan = UiUtils.createSecondaryButton(I18n.get("due.btn.scan"), NavIcons.calendar(13, UiUtils.COLOR_PRIMARY));
        btnScan.addActionListener(e -> {
            recurringService.generateDuePayments(2);
            refreshAll();
            UiUtils.showSuccess(this, "Due payment occurrences scanned and refreshed!");
        });

        JButton btnDelete = UiUtils.createSecondaryButton(I18n.get("due.btn.deleteEntry"), NavIcons.trash(13, UiUtils.COLOR_EXPENSE));
        btnDelete.addActionListener(e -> onDeleteSelectedDue());

        btnBar.add(btnSettle);
        btnBar.add(btnSettleOnTime);
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
        filterCard.add(new JLabel(I18n.get("transactions.filter.search")), gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        txtSearch = new JTextField(12);
        txtSearch.setToolTipText("Search title, category, subcategory, or notes");
        txtSearch.addActionListener(e -> applyDueFilters());
        filterCard.add(txtSearch, gbc);

        gbc.gridx = 2; gbc.gridy = 0;
        filterCard.add(new JLabel(I18n.get("transactions.table.status") + ":"), gbc);
        gbc.gridx = 3; gbc.gridy = 0;
        cmbStatusFilter = new JComboBox<>(new String[]{
                I18n.get("due.filter.all"),
                I18n.get("due.filter.overdue"),
                I18n.get("due.status.upcoming"),
                I18n.get("due.filter.settled")
        });
        cmbStatusFilter.addActionListener(e -> applyDueFilters());
        filterCard.add(cmbStatusFilter, gbc);

        gbc.gridx = 4; gbc.gridy = 0;
        filterCard.add(new JLabel(I18n.get("due.filter.period")), gbc);
        gbc.gridx = 5; gbc.gridy = 0;
        cmbPeriodFilter = new JComboBox<>(new String[]{
                I18n.get("due.filter.allPeriods"),
                I18n.get("due.filter.overdue30Days"),
                I18n.get("due.filter.currentMonth"),
                I18n.get("due.filter.past3Months"),
                I18n.get("due.filter.next3Months")
        });
        cmbPeriodFilter.setSelectedIndex(0);
        cmbPeriodFilter.addActionListener(e -> applyDueFilters());
        filterCard.add(cmbPeriodFilter, gbc);

        gbc.gridx = 6; gbc.gridy = 0;
        filterCard.add(new JLabel(I18n.get("due.filter.mode")), gbc);
        gbc.gridx = 7; gbc.gridy = 0;
        cmbExecutionFilter = new JComboBox<>(new String[]{
                I18n.get("due.filter.allModes"),
                I18n.get("due.filter.autoPayment"),
                I18n.get("due.filter.manualPayment")
        });
        cmbExecutionFilter.addActionListener(e -> applyDueFilters());
        filterCard.add(cmbExecutionFilter, gbc);

        gbc.gridx = 8; gbc.gridy = 0;
        filterCard.add(new JLabel(I18n.get("due.filter.type")), gbc);
        gbc.gridx = 9; gbc.gridy = 0;
        cmbTypeFilter = new JComboBox<>(new String[]{
                I18n.get("due.filter.allTypes"),
                I18n.get("due.filter.expense"),
                I18n.get("due.filter.income")
        });
        cmbTypeFilter.addActionListener(e -> applyDueFilters());
        filterCard.add(cmbTypeFilter, gbc);

        JButton btnApply = UiUtils.createSecondaryButton(I18n.get("btn.filter"), NavIcons.filter(12, UiUtils.COLOR_PRIMARY));
        btnApply.addActionListener(e -> applyDueFilters());
        gbc.gridx = 10; gbc.gridy = 0;
        filterCard.add(btnApply, gbc);

        topContainer.add(filterCard, BorderLayout.SOUTH);
        panel.add(topContainer, BorderLayout.NORTH);

        // 2. Due Payments Table configuration
        String[] cols = {
                "ID",
                I18n.get("due.table.status"),
                I18n.get("due.table.dueDate"),
                I18n.get("due.table.ruleName"),
                I18n.get("transactions.table.category"),
                I18n.get("transactions.table.subCategory"),
                I18n.get("due.table.expectedAmount"),
                I18n.get("due.table.execution"),
                I18n.get("due.table.settlementInfo"),
                I18n.get("due.table.via"),
                I18n.get("due.table.notes")
        };
        modelDue = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        tblDue = new JTable(modelDue);
        tblDue.setRowHeight(30);
        tblDue.setFont(UiUtils.regularFont());
        tblDue.getTableHeader().setFont(UiUtils.boldFont());
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

        // Double-click row: Prompt settlement dialog directly; Right-click: context menu
        JPopupMenu popupDue = new JPopupMenu();
        JMenuItem miSettleCustom = new JMenuItem(I18n.get("due.menu.settleCustom"), NavIcons.check(13, UiUtils.COLOR_INCOME));
        miSettleCustom.addActionListener(e -> onSettleSelected());
        JMenuItem miSettleOnTime = new JMenuItem(I18n.get("due.btn.settleOnTime"), NavIcons.check(13, UiUtils.COLOR_PRIMARY));
        miSettleOnTime.addActionListener(e -> onSettleOnTime());
        JMenuItem miUnsettle = new JMenuItem(I18n.get("due.btn.markUnsettled"), NavIcons.refresh(13, UiUtils.COLOR_TEXT_PRIMARY));
        miUnsettle.addActionListener(e -> onMarkUnsettled());
        JMenuItem miDelete = new JMenuItem(I18n.get("due.btn.deleteEntry"), NavIcons.trash(13, UiUtils.COLOR_EXPENSE));
        miDelete.addActionListener(e -> onDeleteSelectedDue());

        popupDue.add(miSettleCustom);
        popupDue.add(miSettleOnTime);
        popupDue.addSeparator();
        popupDue.add(miUnsettle);
        popupDue.addSeparator();
        popupDue.add(miDelete);
        tblDue.setComponentPopupMenu(popupDue);

        tblDue.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int r = tblDue.rowAtPoint(e.getPoint());
                    if (r >= 0 && !tblDue.isRowSelected(r)) {
                        tblDue.setRowSelectionInterval(r, r);
                    }
                }
            }
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

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePanel.setOpaque(false);

        JLabel lblTitle = new JLabel(I18n.get("due.tab.rules"));
        lblTitle.setFont(UiUtils.titleFont());
        lblTitle.setForeground(new Color(15, 23, 42));
        titlePanel.add(lblTitle);

        lblRulesCommitmentSummary = new JLabel();
        lblRulesCommitmentSummary.setFont(UiUtils.regularFont());
        lblRulesCommitmentSummary.setForeground(UiUtils.COLOR_TEXT_MUTED);
        titlePanel.add(lblRulesCommitmentSummary);

        topBar.add(titlePanel, BorderLayout.WEST);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBar.setOpaque(false);

        JButton btnAddRule = UiUtils.createPrimaryButton(I18n.get("due.btn.addRule"), NavIcons.plus(13, Color.WHITE));
        btnAddRule.addActionListener(e -> onNewSchedule());

        JButton btnEditRule = UiUtils.createSecondaryButton(I18n.get("due.btn.editRule"), NavIcons.edit(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnEditRule.addActionListener(e -> onEditSelectedRule());

        JButton btnDeleteRule = UiUtils.createSecondaryButton(I18n.get("due.btn.deleteRule"), NavIcons.trash(13, UiUtils.COLOR_EXPENSE));
        btnDeleteRule.addActionListener(e -> onDeleteSelectedRule());

        btnBar.add(btnAddRule);
        btnBar.add(btnEditRule);
        btnBar.add(btnDeleteRule);
        topBar.add(btnBar, BorderLayout.EAST);

        panel.add(topBar, BorderLayout.NORTH);

        String[] cols = {
                "ID",
                I18n.get("due.table.ruleName"),
                I18n.get("transactions.table.type"),
                I18n.get("transactions.table.category"),
                I18n.get("transactions.table.subCategory"),
                I18n.get("due.table.dayOfMonth"),
                I18n.get("due.table.amount") + " " + I18n.get("transactions.table.type"),
                I18n.get("due.table.expectedAmount"),
                I18n.get("due.table.dateRange"),
                I18n.get("due.table.execution"),
                I18n.get("transactions.table.via"),
                I18n.get("due.table.status")
        };
        modelRules = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        tblRules = new JTable(modelRules);
        tblRules.setRowHeight(28);
        tblRules.setFont(UiUtils.regularFont());
        tblRules.getTableHeader().setFont(UiUtils.boldFont());
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
        int statusIdx = cmbStatusFilter != null ? cmbStatusFilter.getSelectedIndex() : 0;
        int periodIdx = cmbPeriodFilter != null ? cmbPeriodFilter.getSelectedIndex() : 0;
        int modeIdx = cmbExecutionFilter != null ? cmbExecutionFilter.getSelectedIndex() : 0;
        int typeIdx = cmbTypeFilter != null ? cmbTypeFilter.getSelectedIndex() : 0;

        LocalDate today = LocalDate.now();

        for (DuePayment p : all) {
            // Text Search filter across multiple descriptive fields
            if (!query.isEmpty()) {
                String hay = (p.getTitle() + " " + p.getCategory() + " " + p.getSubCategory() + " " + p.getNotes()).toLowerCase();
                if (!hay.contains(query)) {
                    continue;
                }
            }

            // Status Filter validation (0: All, 1: Overdue, 2: Upcoming, 3: Settled)
            boolean isOverdue = p.isOverdue();
            boolean isSettled = p.isSettled();
            if (statusIdx == 1 && !isOverdue) continue;
            if (statusIdx == 2 && (isOverdue || isSettled)) continue;
            if (statusIdx == 3 && !isSettled) continue;

            // Execution Mode Filter (0: All, 1: Auto-Pay, 2: Manual)
            if (modeIdx == 1 && !p.isAutoPay()) continue;
            if (modeIdx == 2 && p.isAutoPay()) continue;

            // Transaction Type Filter (0: All, 1: Expense, 2: Income)
            if (typeIdx == 1 && p.getType() != TransactionType.EXPENSE) continue;
            if (typeIdx == 2 && p.getType() != TransactionType.INCOME) continue;

            // Calendar Period Filter
            if (p.getDueDate() != null && periodIdx > 0) {
                boolean skip = switch (periodIdx) {
                    case 1 -> p.getDueDate().isAfter(today.plusDays(30));
                    case 2 -> p.getDueDate().getYear() != today.getYear() || p.getDueDate().getMonth() != today.getMonth();
                    case 3 -> p.getDueDate().isBefore(today.minusMonths(3)) || p.getDueDate().isAfter(today);
                    case 4 -> p.getDueDate().isBefore(today) || p.getDueDate().isAfter(today.plusMonths(3));
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

        // Recompute commitment KPI cards for recurring rules
        RecurringService.ScheduleCommitmentSummary cs = recurringService.computeCommitmentSummary(currentRulesList);
        if (kpiRuleEmi != null) {
            kpiRuleEmi.updateValue(AppConfig.formatCurrency(cs.getEmiTotal()),
                    cs.getEmiCount() + " active loan EMIs");
        }
        if (kpiRuleTotalBills != null) {
            kpiRuleTotalBills.updateValue(AppConfig.formatCurrency(cs.getTotalBillsCommitment()),
                    cs.getTotalBillsCount() + " bills (" + AppConfig.formatCurrency(cs.getFixedBillsTotal()) + " fixed, " + AppConfig.formatCurrency(cs.getVariableBillsTotal()) + " var)");
        }
        if (kpiRuleFixedBills != null) {
            kpiRuleFixedBills.updateValue(AppConfig.formatCurrency(cs.getFixedBillsTotal()),
                    cs.getFixedBillsCount() + " fixed recurring bills");
        }
        if (kpiRuleVariableBills != null) {
            kpiRuleVariableBills.updateValue(AppConfig.formatCurrency(cs.getVariableBillsTotal()),
                    cs.getVariableBillsCount() + " variable / estimated bills");
        }
        if (lblRulesCommitmentSummary != null) {
            lblRulesCommitmentSummary.setText(String.format("• %d active rules • Total Monthly Commitment: %s",
                    cs.getTotalActiveRulesCount(), AppConfig.formatCurrency(cs.getTotalCommitment())));
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
     * Handles fast "on-time" payment settlements directly on the scheduled due date.
     * If rows are selected, marks selected rows settled on their due dates.
     * If no rows are selected, prompts to settle all pending/overdue dues for September 2026.
     */
    private void onSettleOnTime() {
        int[] selectedRows = tblDue.getSelectedRows();
        if (selectedRows != null && selectedRows.length > 0) {
            String prompt = java.text.MessageFormat.format(I18n.get("due.confirm.settleOnTime"), selectedRows.length);
            int confirm = JOptionPane.showConfirmDialog(this,
                    prompt,
                    "Settle On Time Confirmation",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;

            int count = 0;
            for (int r : selectedRows) {
                Long id = (Long) modelDue.getValueAt(r, 0);
                if (recurringService.settlePaymentOnTime(id, true)) {
                    count++;
                }
            }
            refreshAll();
            String msg = java.text.MessageFormat.format(I18n.get("due.msg.settledOnTimeCount"), count);
            UiUtils.showSuccess(this, msg);
        } else {
            // Prompt to settle all pending/overdue dues for September 2026
            int confirm = JOptionPane.showConfirmDialog(this,
                    I18n.get("due.confirm.settleMonthOnTime"),
                    "Settle All September 2026 On Time",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                int settled = recurringService.settleAllForMonthOnTime(java.time.YearMonth.of(2026, 9), true);
                refreshAll();
                String msg = java.text.MessageFormat.format(I18n.get("due.msg.settledOnTimeCount"), settled);
                UiUtils.showSuccess(this, msg);
            }
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

    /**
     * Responds to language updates by rebuilding localized visual labels, tabs, columns, and filter texts.
     *
     * @param newLanguage the newly selected system language
     */
    @Override
    public void onLanguageChanged(AppLanguage newLanguage) {
        removeAll();
        initComponents();
        revalidate();
        repaint();
        refreshAll();
    }

    // Public getters for testing and component inspection
    public KpiCard getKpiOverdue() { return kpiOverdue; }
    public KpiCard getKpiDuePending() { return kpiDuePending; }
    public KpiCard getKpiSettled() { return kpiSettled; }
    public KpiCard getKpiAutoPay() { return kpiAutoPay; }
    public KpiCard getKpiRuleEmi() { return kpiRuleEmi; }
    public KpiCard getKpiRuleTotalBills() { return kpiRuleTotalBills; }
    public KpiCard getKpiRuleFixedBills() { return kpiRuleFixedBills; }
    public KpiCard getKpiRuleVariableBills() { return kpiRuleVariableBills; }
    public JTable getTblDue() { return tblDue; }
    public JTable getTblRules() { return tblRules; }
    public JLabel getLblRulesCommitmentSummary() { return lblRulesCommitmentSummary; }
}

