package com.navfinance.ui.panels;

import com.navfinance.config.AppConfig;
import com.navfinance.model.DashboardSummary;
import com.navfinance.model.Transaction;
import com.navfinance.model.TransactionType;
import com.navfinance.service.AnalyticsService;
import com.navfinance.ui.components.CategoryPieChartPanel;
import com.navfinance.ui.components.KpiCard;
import com.navfinance.ui.components.TrendChartPanel;
import com.navfinance.ui.dialogs.TransactionDialog;
import com.navfinance.i18n.AppLanguage;
import com.navfinance.i18n.I18n;
import com.navfinance.i18n.LanguageChangeListener;
import com.navfinance.ui.util.NavIcons;
import com.navfinance.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

/**
 * Primary overview Dashboard panel synthesizing real-time financial health indicators.
 * <p>
 * Displays KPI headline summary cards (Total Income, Total Expense, Net Balance, Loan Position),
 * multi-temporal cash flow trend charts, category-wise expense distribution donut charts,
 * quick entry shortcuts, and a recent transactions ledger preview.
 * </p>
 */
public class DashboardPanel extends JPanel implements LanguageChangeListener {

    /** Service coordinator for financial statistics and time-series aggregation. */
    private final AnalyticsService analyticsService = AnalyticsService.getInstance();

    /** Active temporal period resolution (Weekly, Monthly, Quarterly, Annually). */
    private String currentPeriodType = "Monthly";

    /** Temporal offset relative to current date (0 = current period, -1 = previous, +1 = next). */
    private int currentOffset = 0;

    // Controls
    /** Label displaying the human-readable date bounds of the active period. */
    private JLabel lblPeriodLabel;

    /** KPI card component displaying cumulative income. */
    private KpiCard kpiIncome;

    /** KPI card component displaying cumulative expenses. */
    private KpiCard kpiExpense;

    /** KPI card component displaying net savings or deficit. */
    private KpiCard kpiBalance;

    /** KPI card component displaying net loans and dues lending position. */
    private KpiCard kpiLoans;

    /** Bar chart component illustrating inflows vs. outflows. */
    private TrendChartPanel trendChart;

    /** Donut chart component illustrating expenses grouped by category. */
    private CategoryPieChartPanel categoryPieChart;

    /** Table component displaying recent ledger transactions. */
    private JTable tblRecent;

    /** Table model backing the recent activity grid. */
    private DefaultTableModel recentTableModel;

    /**
     * Constructs the Dashboard panel layout and executes the initial analytical data query.
     */
    public DashboardPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        initComponents();
        I18n.addListener(this);
        refreshData();
    }

    /**
     * Initializes the period navigation controls, KPI card grid, charts layout,
     * and recent transactions table.
     */
    private void initComponents() {
        removeAll();

        // TOP: Period Selector & Quick Actions
        JPanel topBar = new JPanel(new BorderLayout(15, 0));
        topBar.setOpaque(false);

        // Left: Period Switcher & Navigator
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        navPanel.setOpaque(false);

        JLabel lblView = new JLabel(I18n.get("transactions.filter.datePreset"));
        lblView.setFont(UiUtils.boldFont());
        navPanel.add(lblView);

        String[] periods = {"Weekly", "Monthly", "Quarterly", "Annually"};
        ButtonGroup bg = new ButtonGroup();
        for (String p : periods) {
            JRadioButton rb = new JRadioButton(p);
            rb.setFont(UiUtils.regularFont());
            rb.setOpaque(false);
            if (p.equals(currentPeriodType)) rb.setSelected(true);
            rb.addActionListener(e -> {
                currentPeriodType = p;
                currentOffset = 0;
                refreshData();
            });
            bg.add(rb);
            navPanel.add(rb);
        }

        // Period Step Navigation (< Prev, [Label], Next >)
        JButton btnPrev = UiUtils.createSecondaryButton("Prev", NavIcons.chevronLeft(12, UiUtils.COLOR_TEXT_PRIMARY));
        btnPrev.addActionListener(e -> {
            currentOffset--;
            refreshData();
        });

        lblPeriodLabel = new JLabel("October 2026", JLabel.CENTER);
        lblPeriodLabel.setFont(UiUtils.boldFont());
        lblPeriodLabel.setPreferredSize(new Dimension(200, 26));

        JButton btnNext = UiUtils.createSecondaryButton("Next", NavIcons.chevronRight(12, UiUtils.COLOR_TEXT_PRIMARY));
        btnNext.addActionListener(e -> {
            currentOffset++;
            refreshData();
        });

        JButton btnToday = UiUtils.createSecondaryButton(I18n.get("btn.today"), NavIcons.calendar(13, UiUtils.COLOR_PRIMARY));
        btnToday.addActionListener(e -> {
            currentOffset = 0;
            refreshData();
        });

        navPanel.add(btnPrev);
        navPanel.add(lblPeriodLabel);
        navPanel.add(btnNext);
        navPanel.add(btnToday);

        // Right: Fast Action Buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionPanel.setOpaque(false);

        JButton btnAddIncome = UiUtils.createSuccessButton(I18n.get("action.recordIncome"), NavIcons.plus(13, Color.WHITE));
        btnAddIncome.addActionListener(e -> openTransactionDialog(TransactionType.INCOME));

        JButton btnAddExpense = UiUtils.createDangerButton(I18n.get("action.recordExpense"), NavIcons.minus(13, Color.WHITE));
        btnAddExpense.addActionListener(e -> openTransactionDialog(TransactionType.EXPENSE));

        actionPanel.add(btnAddIncome);
        actionPanel.add(btnAddExpense);

        // Responsive TopBar prevents period switcher from colliding with fast action buttons
        com.navfinance.ui.components.ResponsiveBarPanel responsiveTopBar =
                new com.navfinance.ui.components.ResponsiveBarPanel(navPanel, actionPanel, 14, 8, true);
        topBar.add(responsiveTopBar, BorderLayout.CENTER);

        add(topBar, BorderLayout.NORTH);

        // CENTER: Main Content (KPI Row, Chart Row, Recent Activity Table)
        JPanel centerPanel = new JPanel(new BorderLayout(0, 14));
        centerPanel.setOpaque(false);

        // 1. KPI Cards Row
        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 14, 0));
        kpiRow.setOpaque(false);

        kpiIncome = new KpiCard(I18n.get("dashboard.kpi.totalIncome"), "₹ 0.00", I18n.get("dashboard.period.thisMonth"), UiUtils.COLOR_INCOME, NavIcons.plus(22, UiUtils.COLOR_INCOME));
        kpiExpense = new KpiCard(I18n.get("dashboard.kpi.totalExpense"), "₹ 0.00", I18n.get("dashboard.period.thisMonth"), UiUtils.COLOR_EXPENSE, NavIcons.minus(22, UiUtils.COLOR_EXPENSE));
        kpiBalance = new KpiCard(I18n.get("dashboard.kpi.netBalance"), "₹ 0.00", I18n.get("dashboard.title"), UiUtils.COLOR_PRIMARY, NavIcons.dashboard(22, UiUtils.COLOR_PRIMARY));
        kpiLoans = new KpiCard(I18n.get("tab.loans"), "₹ 0.00", I18n.get("loans.subtitle"), UiUtils.COLOR_AMBER, NavIcons.loan(22, UiUtils.COLOR_AMBER));

        kpiRow.add(kpiIncome);
        kpiRow.add(kpiExpense);
        kpiRow.add(kpiBalance);
        kpiRow.add(kpiLoans);
        centerPanel.add(kpiRow, BorderLayout.NORTH);

        // 2. Charts Row (Trend Chart + Category Donut Chart - balanced 50/50 layout)
        JPanel chartsRow = new JPanel(new GridLayout(1, 2, 16, 0));
        chartsRow.setOpaque(false);
        int chartsH = Math.max(340, Math.round(360 * com.navfinance.ui.util.ZoomManager.getCurrentScale()));
        chartsRow.setPreferredSize(new Dimension(900, chartsH));

        trendChart = new TrendChartPanel(I18n.get("report.title"));
        categoryPieChart = new CategoryPieChartPanel(I18n.get("dashboard.categoryBreakdown"));

        chartsRow.add(trendChart);
        chartsRow.add(categoryPieChart);

        // 3. Lower Section: Recent Transactions
        JPanel lowerPanel = new JPanel(new BorderLayout(0, 8));
        lowerPanel.setOpaque(false);

        JLabel lblRecentTitle = new JLabel(I18n.get("dashboard.recentTransactions"));
        lblRecentTitle.setFont(UiUtils.headerFont());
        lblRecentTitle.setBorder(BorderFactory.createEmptyBorder(6, 4, 2, 4));
        lowerPanel.add(lblRecentTitle, BorderLayout.NORTH);

        String[] cols = {
                I18n.get("transactions.table.type"),
                I18n.get("transactions.table.date"),
                I18n.get("transactions.table.status"),
                I18n.get("transactions.table.amount"),
                I18n.get("transactions.table.via"),
                I18n.get("transactions.table.category"),
                I18n.get("transactions.table.subCategory"),
                I18n.get("transactions.table.person"),
                I18n.get("transactions.table.notes")
        };
        recentTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        tblRecent = new JTable(recentTableModel);
        tblRecent.setRowHeight(28);
        tblRecent.getTableHeader().setFont(UiUtils.boldFont());
        tblRecent.setFont(UiUtils.regularFont());
        tblRecent.setShowGrid(false);
        tblRecent.setIntercellSpacing(new Dimension(0, 0));

        // Center / Type badge renderer with vector icons
        tblRecent.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(CENTER);
                l.setFont(UiUtils.boldFont());
                if ("Income".equalsIgnoreCase(String.valueOf(value))) {
                    l.setForeground(UiUtils.COLOR_INCOME);
                    l.setIcon(NavIcons.plus(11, UiUtils.COLOR_INCOME));
                } else {
                    l.setForeground(UiUtils.COLOR_EXPENSE);
                    l.setIcon(NavIcons.minus(11, UiUtils.COLOR_EXPENSE));
                }
                return l;
            }
        });

        // Status column renderer with colored status dot
        tblRecent.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(CENTER);
                l.setFont(UiUtils.regularFont());
                if ("Settled".equalsIgnoreCase(String.valueOf(value))) {
                    l.setForeground(UiUtils.COLOR_INCOME);
                    l.setIcon(NavIcons.statusDot(8, UiUtils.COLOR_INCOME));
                } else {
                    l.setForeground(UiUtils.COLOR_AMBER);
                    l.setIcon(NavIcons.statusDot(8, UiUtils.COLOR_AMBER));
                }
                return l;
            }
        });

        // Amount Column Renderer
        tblRecent.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(RIGHT);
                setFont(UiUtils.boldFont());
                Object typeVal = table.getValueAt(row, 0);
                if ("Income".equalsIgnoreCase(String.valueOf(typeVal))) {
                    setForeground(UiUtils.COLOR_INCOME);
                } else {
                    setForeground(UiUtils.COLOR_EXPENSE);
                }
                return c;
            }
        });

        JScrollPane spRecent = new JScrollPane(tblRecent);
        spRecent.setBorder(UiUtils.createCardBorder());
        spRecent.setPreferredSize(new Dimension(900, 180));
        lowerPanel.add(spRecent, BorderLayout.CENTER);

        JPanel middleStack = new JPanel(new BorderLayout(0, 14));
        middleStack.setOpaque(false);
        middleStack.add(chartsRow, BorderLayout.NORTH);
        middleStack.add(lowerPanel, BorderLayout.CENTER);

        centerPanel.add(middleStack, BorderLayout.CENTER);

        JScrollPane scrollPane = new JScrollPane(centerPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Refreshes all dashboard metric cards, charts, and table models based on the
     * active time period selection.
     */
    public final void refreshData() {
        DashboardSummary summary = analyticsService.getSummary(currentPeriodType, currentOffset);

        lblPeriodLabel.setText(summary.getPeriodLabel());

        kpiIncome.setValue(AppConfig.formatCurrency(summary.getTotalIncome()));
        kpiIncome.setSubtitle(summary.getPeriodType() + " Income");

        kpiExpense.setValue(AppConfig.formatCurrency(summary.getTotalExpense()));
        kpiExpense.setSubtitle(summary.getPeriodType() + " Expenses");

        kpiBalance.setValue(AppConfig.formatCurrency(summary.getNetBalance()));
        kpiBalance.setSubtitle(summary.getNetBalance() >= 0 ? "Surplus / Savings" : "Deficit / Overspent");

        String loansSub = String.format("Lent: %s | Borrowed: %s",
                AppConfig.formatCurrencyPlain(summary.getTotalLent()),
                AppConfig.formatCurrencyPlain(summary.getTotalBorrowed()));
        kpiLoans.setValue(AppConfig.formatCurrency(summary.getNetLoanBalance()));
        kpiLoans.setSubtitle(loansSub);

        // Update Charts
        trendChart.setTrendPoints(summary.getTrendPoints());
        categoryPieChart.setData(summary.getExpenseByCategory());

        // Update Recent Transactions Table
        recentTableModel.setRowCount(0);
        List<Transaction> recents = summary.getRecentTransactions();
        for (Transaction tx : recents) {
            recentTableModel.addRow(new Object[]{
                    tx.getType().getDisplayName(),
                    tx.getTxDate() != null ? tx.getTxDate().format(AppConfig.DATE_FORMATTER) : "-",
                    tx.isExpected() ? "Expected" : "Settled",
                    AppConfig.formatCurrency(tx.getAmount()),
                    tx.getPaymentVia() != null ? tx.getPaymentVia() : "-",
                    tx.getCategory() != null ? tx.getCategory() : "-",
                    tx.getSubCategory() != null ? tx.getSubCategory() : "-",
                    tx.getPersonName() != null ? tx.getPersonName() : "-",
                    tx.getNotes() != null ? tx.getNotes() : ""
            });
        }
    }

    /**
     * Opens the modal TransactionDialog configured for the designated type.
     *
     * @param type transaction direction (INCOME or EXPENSE)
     */
    private void openTransactionDialog(TransactionType type) {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        TransactionDialog dialog = new TransactionDialog(frame, null, type);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshData();
        }
    }

    @Override
    public void onLanguageChanged(AppLanguage newLanguage) {
        initComponents();
        refreshData();
    }
}
