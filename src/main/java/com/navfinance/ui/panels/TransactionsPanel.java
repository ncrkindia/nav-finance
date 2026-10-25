package com.navfinance.ui.panels;

import com.navfinance.config.AppConfig;
import com.navfinance.i18n.AppLanguage;
import com.navfinance.i18n.I18n;
import com.navfinance.i18n.LanguageChangeListener;
import com.navfinance.model.Bill;
import com.navfinance.model.CreditCard;
import com.navfinance.model.Emi;
import com.navfinance.model.EntityTrackerSummary;
import com.navfinance.model.FilterCriteria;
import com.navfinance.model.Person;
import com.navfinance.model.SettlementStatus;
import com.navfinance.model.Transaction;
import com.navfinance.model.TransactionSettlementInfo;
import com.navfinance.model.TransactionType;
import com.navfinance.service.CsvImportService;
import com.navfinance.service.ExportService;
import com.navfinance.service.FinanceService;
import com.navfinance.ui.components.DatePickerField;
import com.navfinance.ui.components.KpiCard;
import com.navfinance.ui.components.TrendChartPanel;
import com.navfinance.ui.dialogs.CsvImportDialog;
import com.navfinance.ui.dialogs.TransactionDialog;
import com.navfinance.ui.dialogs.TransactionLinkDialog;
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
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Primary transactional ledger and analytical entity tracking management panel for NavFinance.
 * <p>
 * Offers a unified dual-tab workspace:
 * <ul>
 *   <li><b>Tab 1: All Transactions</b> - Comprehensive ledger grid with multi-dimensional filtering
 *       (across Types, Categories, Associated Persons, Bills, EMIs, Cards, Settlement states),
 *       sorting, inline editing, counterpart linking, and report exports.</li>
 *   <li><b>Tab 2: Track &amp; Analyze</b> - Deep entity-centric inspection dashboard tracking transactions
 *       for a Person, Bill, EMI, Credit Card, or Category across Days, Weeks, Months, Quarters, or Years,
 *       featuring visual bar charts, KPI cards, and itemized ledger records.</li>
 * </ul>
 * </p>
 *
 * @author Nav Finance Team
 * @version 2.0.0
 */
public class TransactionsPanel extends JPanel implements LanguageChangeListener {

    private final FinanceService financeService = FinanceService.getInstance();
    private final ExportService exportService = ExportService.getInstance();

    private JTabbedPane tabbedPane;

    // =========================================================================
    // TAB 1: ALL TRANSACTIONS
    // =========================================================================
    private List<Transaction> currentTransactions = new ArrayList<>();
    private FilterCriteria currentCriteria;

    // Filter controls
    private JComboBox<String> cmbTypeFilter;
    private JComboBox<String> cmbCategoryFilter;
    private JComboBox<String> cmbSubCategoryFilter;
    private JComboBox<String> cmbDatePreset;
    private DatePickerField txtStartDate;
    private DatePickerField txtEndDate;
    private JComboBox<String> cmbViaFilter;
    private JComboBox<Object> cmbMasterFilter; // Person, Bill, EMI, Card
    private JComboBox<String> cmbSettlementFilter;
    private JTextField txtMinAmount;
    private JTextField txtMaxAmount;
    private JTextField txtSearch;

    // Sorting controls
    private JComboBox<String> cmbSortBy;
    private JComboBox<String> cmbSortDir;

    // Table
    private JTable tblTransactions;
    private DefaultTableModel tableModel;

    // Summary footer
    private JLabel lblSummaryTotal;

    // =========================================================================
    // TAB 2: TRACK & ANALYZE
    // =========================================================================
    private JComboBox<String> cmbTrackerType;
    private JComboBox<Object> cmbTrackerEntity;
    private JComboBox<String> cmbTrackerGranularity;
    private DatePickerField txtTrackerStartDate;
    private DatePickerField txtTrackerEndDate;
    private JComboBox<String> cmbTrackerDatePreset;

    private KpiCard kpiTrackerCount;
    private KpiCard kpiTrackerInflow;
    private KpiCard kpiTrackerOutflow;
    private KpiCard kpiTrackerNet;

    private TrendChartPanel trackerChartPanel;
    private JTable tblTrackerTransactions;
    private DefaultTableModel trackerTableModel;
    private EntityTrackerSummary activeTrackerSummary;

    public TransactionsPanel() {
        setLayout(new BorderLayout(0, 8));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        initComponents();
        I18n.addListener(this);
        applyFilters();
    }

    private void initComponents() {
        removeAll();

        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setFont(UiUtils.boldFont());

        JPanel pnlAll = buildAllTransactionsTab();
        JPanel pnlTrack = buildTrackAndAnalyzeTab();

        tabbedPane.addTab(I18n.get("transactions.title"), NavIcons.transactions(16, UiUtils.COLOR_PRIMARY), pnlAll);
        tabbedPane.addTab("Track & Analyze 📊", NavIcons.dashboard(16, UiUtils.COLOR_PRIMARY), pnlTrack);

        add(tabbedPane, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    // =========================================================================
    // TAB 1 BUILDER: All Transactions
    // =========================================================================
    private JPanel buildAllTransactionsTab() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setOpaque(false);

        // TOP SECTION: Action bar + Advanced Filter Card
        JPanel topContainer = new JPanel(new BorderLayout(0, 10));
        topContainer.setOpaque(false);

        JLabel lblTitle = new JLabel(I18n.get("transactions.title"));
        lblTitle.setFont(UiUtils.titleFont());
        lblTitle.setForeground(new Color(15, 23, 42));

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnGroup.setOpaque(false);

        JButton btnAddIncome = UiUtils.createSuccessButton(I18n.get("action.recordIncome"), NavIcons.plus(13, Color.WHITE));
        btnAddIncome.addActionListener(e -> openAddDialog(TransactionType.INCOME));

        JButton btnAddExpense = UiUtils.createDangerButton(I18n.get("action.recordExpense"), NavIcons.minus(13, Color.WHITE));
        btnAddExpense.addActionListener(e -> openAddDialog(TransactionType.EXPENSE));

        JButton btnEdit = UiUtils.createSecondaryButton(I18n.get("btn.edit"), NavIcons.edit(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnEdit.addActionListener(e -> onEditSelected());

        JButton btnDelete = UiUtils.createSecondaryButton(I18n.get("btn.delete"), NavIcons.trash(13, UiUtils.COLOR_EXPENSE));
        btnDelete.addActionListener(e -> onDeleteSelected());

        JButton btnTrack = UiUtils.createPrimaryButton("Track Entity 📊", NavIcons.dashboard(13, Color.WHITE));
        btnTrack.setToolTipText("Open deep analytical graph and ledger for the selected entity");
        btnTrack.addActionListener(e -> onTrackEntityFromTable());

        JButton btnLink = UiUtils.createSecondaryButton(I18n.get("link.action.linkCounterpart"), NavIcons.link(13, UiUtils.COLOR_PRIMARY));
        btnLink.addActionListener(e -> onLinkSelected());

        JButton btnImportCsv = UiUtils.createSecondaryButton(I18n.get("btn.import"), NavIcons.importIcon(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnImportCsv.addActionListener(e -> onOpenImportCsv());

        // Export Menu Button with Dropdown Popup
        JButton btnExport = UiUtils.createPrimaryButton(I18n.get("btn.export"), NavIcons.export(13, Color.WHITE));
        JPopupMenu exportMenu = new JPopupMenu();
        JMenuItem itemPdf = new JMenuItem(I18n.get("transactions.export.pdf"), NavIcons.export(12, UiUtils.COLOR_PRIMARY));
        JMenuItem itemExcel = new JMenuItem(I18n.get("transactions.export.excel"), NavIcons.export(12, UiUtils.COLOR_INCOME));
        JMenuItem itemCsv = new JMenuItem(I18n.get("transactions.export.csv"), NavIcons.export(12, UiUtils.COLOR_AMBER));
        JMenuItem itemTemplate = new JMenuItem(I18n.get("transactions.export.template"), NavIcons.importIcon(12, UiUtils.COLOR_PRIMARY));

        itemPdf.addActionListener(e -> exportCurrentView("PDF"));
        itemExcel.addActionListener(e -> exportCurrentView("EXCEL"));
        itemCsv.addActionListener(e -> exportCurrentView("CSV"));
        itemTemplate.addActionListener(e -> onDownloadTemplate());

        exportMenu.add(itemPdf);
        exportMenu.add(itemExcel);
        exportMenu.add(itemCsv);
        exportMenu.addSeparator();
        exportMenu.add(itemTemplate);

        btnExport.addActionListener(e -> exportMenu.show(btnExport, 0, btnExport.getHeight()));

        btnGroup.add(btnAddIncome);
        btnGroup.add(btnAddExpense);
        btnGroup.add(btnEdit);
        btnGroup.add(btnDelete);
        btnGroup.add(btnTrack);
        btnGroup.add(btnLink);
        btnGroup.add(btnImportCsv);
        btnGroup.add(btnExport);

        com.navfinance.ui.components.ResponsiveBarPanel actionHeader =
                new com.navfinance.ui.components.ResponsiveBarPanel(lblTitle, btnGroup, 14, 8, true);
        topContainer.add(actionHeader, BorderLayout.NORTH);

        // 2. Filters Card Panel
        JPanel filterCard = new JPanel(new GridBagLayout());
        filterCard.setBackground(Color.WHITE);
        filterCard.setBorder(UiUtils.createCardBorder());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // Row 1: Search, Type, Category, SubCategory
        gbc.gridx = 0; gbc.gridy = row;
        filterCard.add(new JLabel(I18n.get("transactions.filter.search")), gbc);
        gbc.gridx = 1; gbc.gridy = row;
        txtSearch = new JTextField(12);
        txtSearch.setToolTipText(I18n.get("transactions.search.placeholder"));
        txtSearch.addActionListener(e -> applyFilters());
        filterCard.add(txtSearch, gbc);

        gbc.gridx = 2; gbc.gridy = row;
        filterCard.add(new JLabel(I18n.get("transactions.filter.type")), gbc);
        gbc.gridx = 3; gbc.gridy = row;
        cmbTypeFilter = new JComboBox<>(new String[]{
                I18n.get("transactions.filter.all"),
                I18n.get("transactions.filter.income"),
                I18n.get("transactions.filter.expense")
        });
        cmbTypeFilter.addActionListener(e -> {
            updateCategoryOptions();
            applyFilters();
        });
        filterCard.add(cmbTypeFilter, gbc);

        gbc.gridx = 4; gbc.gridy = row;
        filterCard.add(new JLabel(I18n.get("transactions.filter.category")), gbc);
        gbc.gridx = 5; gbc.gridy = row;
        cmbCategoryFilter = new JComboBox<>();
        cmbCategoryFilter.addActionListener(e -> {
            updateSubCategoryOptions();
            applyFilters();
        });
        filterCard.add(cmbCategoryFilter, gbc);

        gbc.gridx = 6; gbc.gridy = row;
        filterCard.add(new JLabel(I18n.get("transactions.filter.subCategory")), gbc);
        gbc.gridx = 7; gbc.gridy = row;
        cmbSubCategoryFilter = new JComboBox<>();
        cmbSubCategoryFilter.addActionListener(e -> applyFilters());
        filterCard.add(cmbSubCategoryFilter, gbc);

        // Row 2: Date Filter, Via, Associated Master, Sorting
        row++;

        gbc.gridx = 0; gbc.gridy = row;
        filterCard.add(new JLabel(I18n.get("transactions.filter.datePreset")), gbc);
        gbc.gridx = 1; gbc.gridy = row;
        cmbDatePreset = new JComboBox<>(new String[]{
                I18n.get("transactions.date.allTime"),
                I18n.get("transactions.date.today"),
                I18n.get("transactions.date.thisWeek"),
                I18n.get("transactions.date.thisMonth"),
                I18n.get("transactions.date.thisQuarter"),
                I18n.get("transactions.date.thisYear"),
                "Custom"
        });
        cmbDatePreset.addActionListener(e -> onDatePresetChanged());
        filterCard.add(cmbDatePreset, gbc);

        gbc.gridx = 2; gbc.gridy = row;
        filterCard.add(new JLabel(I18n.get("transactions.filter.via")), gbc);
        gbc.gridx = 3; gbc.gridy = row;
        cmbViaFilter = new JComboBox<>();
        cmbViaFilter.addActionListener(e -> applyFilters());
        filterCard.add(cmbViaFilter, gbc);

        gbc.gridx = 4; gbc.gridy = row;
        filterCard.add(new JLabel(I18n.get("transactions.table.person") + " / Master:"), gbc);
        gbc.gridx = 5; gbc.gridy = row;
        cmbMasterFilter = new JComboBox<>();
        cmbMasterFilter.addActionListener(e -> applyFilters());
        filterCard.add(cmbMasterFilter, gbc);

        gbc.gridx = 6; gbc.gridy = row;
        filterCard.add(new JLabel(I18n.get("transactions.sort.by")), gbc);
        gbc.gridx = 7; gbc.gridy = row;
        JPanel sortBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        sortBox.setOpaque(false);
        cmbSortBy = new JComboBox<>(new String[]{"Date", "Amount", "Type", "Category", "Sub Category"});
        cmbSortDir = new JComboBox<>(new String[]{"Descending", "Ascending"});
        cmbSortDir.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if ("Descending".equals(value)) {
                    setIcon(NavIcons.minus(11, UiUtils.COLOR_PRIMARY));
                } else if ("Ascending".equals(value)) {
                    setIcon(NavIcons.plus(11, UiUtils.COLOR_PRIMARY));
                }
                return this;
            }
        });
        cmbSortBy.addActionListener(e -> applyFilters());
        cmbSortDir.addActionListener(e -> applyFilters());
        sortBox.add(cmbSortBy);
        sortBox.add(cmbSortDir);
        filterCard.add(sortBox, gbc);

        // Row 3: Custom Date input & Amount min/max + Filter Buttons
        row++;

        gbc.gridx = 0; gbc.gridy = row;
        filterCard.add(new JLabel(I18n.get("transactions.filter.fromTo")), gbc);
        gbc.gridx = 1; gbc.gridy = row;
        JPanel dateInputs = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        dateInputs.setOpaque(false);
        txtStartDate = new DatePickerField(null, 8);
        txtEndDate = new DatePickerField(null, 8);
        txtStartDate.addActionListener(e -> applyFilters());
        txtEndDate.addActionListener(e -> applyFilters());
        dateInputs.add(txtStartDate);
        dateInputs.add(new JLabel("-"));
        dateInputs.add(txtEndDate);
        filterCard.add(dateInputs, gbc);

        gbc.gridx = 2; gbc.gridy = row;
        filterCard.add(new JLabel(I18n.get("transactions.filter.amountRange")), gbc);
        gbc.gridx = 3; gbc.gridy = row;
        JPanel amtInputs = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        amtInputs.setOpaque(false);
        txtMinAmount = new JTextField(5);
        txtMaxAmount = new JTextField(5);
        amtInputs.add(txtMinAmount);
        amtInputs.add(new JLabel("-"));
        amtInputs.add(txtMaxAmount);
        filterCard.add(amtInputs, gbc);

        gbc.gridx = 4; gbc.gridy = row;
        filterCard.add(new JLabel(I18n.get("link.filter.title")), gbc);
        gbc.gridx = 5; gbc.gridy = row;
        cmbSettlementFilter = new JComboBox<>(new String[]{
                I18n.get("transactions.filter.allGeneric"),
                I18n.get("link.filter.nullifiedOnly"),
                I18n.get("link.filter.partialOnly"),
                I18n.get("link.filter.unlinkedOnly")
        });
        cmbSettlementFilter.addActionListener(e -> applyFilters());
        filterCard.add(cmbSettlementFilter, gbc);

        gbc.gridx = 6; gbc.gridy = row; gbc.gridwidth = 2;
        JPanel btnFilterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnFilterPanel.setOpaque(false);
        JButton btnApplyFilter = UiUtils.createPrimaryButton(I18n.get("transactions.filter.apply"), NavIcons.filter(13, Color.WHITE));
        btnApplyFilter.addActionListener(e -> applyFilters());

        JButton btnResetFilter = UiUtils.createSecondaryButton(I18n.get("transactions.filter.reset"), NavIcons.refresh(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnResetFilter.addActionListener(e -> resetFilters());

        btnFilterPanel.add(btnResetFilter);
        btnFilterPanel.add(btnApplyFilter);
        filterCard.add(btnFilterPanel, gbc);

        topContainer.add(filterCard, BorderLayout.CENTER);
        root.add(topContainer, BorderLayout.NORTH);

        // CENTER: Transactions Table configuration
        String[] columns = {
                I18n.get("transactions.table.id"),
                I18n.get("transactions.table.type"),
                I18n.get("transactions.table.date"),
                I18n.get("transactions.table.status"),
                I18n.get("transactions.table.amount"),
                I18n.get("link.column.settlement"),
                I18n.get("transactions.table.via"),
                I18n.get("transactions.table.category"),
                I18n.get("transactions.table.subCategory"),
                I18n.get("transactions.table.person") + " / Master",
                I18n.get("transactions.table.notes")
        };
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        tblTransactions = new JTable(tableModel);
        tblTransactions.setRowHeight(30);
        tblTransactions.getTableHeader().setFont(UiUtils.boldFont());
        tblTransactions.setFont(UiUtils.regularFont());
        tblTransactions.setShowGrid(false);
        tblTransactions.setIntercellSpacing(new Dimension(0, 0));

        // Format Type Column
        tblTransactions.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(CENTER);
                l.setFont(UiUtils.FONT_BOLD);
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

        // Format Status Column
        tblTransactions.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(CENTER);
                l.setFont(UiUtils.FONT_REGULAR);
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

        // Format Amount Column
        tblTransactions.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(RIGHT);
                l.setFont(UiUtils.FONT_BOLD);

                Object settlementObj = table.getValueAt(row, 5);
                boolean isNullified = (settlementObj instanceof TransactionSettlementInfo sInfo) && sInfo.isNullified();

                if (isNullified) {
                    l.setForeground(new Color(109, 40, 217));
                    if (!isSelected) l.setBackground(new Color(245, 243, 255));
                    l.setToolTipText(((TransactionSettlementInfo) settlementObj).getHtmlTooltip());
                } else {
                    Object typeVal = table.getValueAt(row, 1);
                    if ("Income".equalsIgnoreCase(String.valueOf(typeVal))) {
                        l.setForeground(UiUtils.COLOR_INCOME);
                    } else {
                        l.setForeground(UiUtils.COLOR_EXPENSE);
                    }
                    if (!isSelected) l.setBackground(Color.WHITE);
                    l.setToolTipText(null);
                }
                return l;
            }
        });

        // Format Settlement Column
        tblTransactions.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(CENTER);

                if (value instanceof TransactionSettlementInfo sInfo) {
                    if (sInfo.isNullified()) {
                        l.setText("Nullified");
                        l.setForeground(UiUtils.COLOR_INCOME);
                        l.setIcon(NavIcons.checkCircle(12, UiUtils.COLOR_INCOME));
                    } else if (sInfo.getStatus() == SettlementStatus.PARTIAL) {
                        l.setText(String.format("Partial (%.0f%%)", sInfo.getSettledPercentage()));
                        l.setForeground(UiUtils.COLOR_AMBER);
                        l.setIcon(NavIcons.statusDot(8, UiUtils.COLOR_AMBER));
                    } else if (sInfo.getStatus() == SettlementStatus.UNLINKED) {
                        l.setText("Unlinked");
                        l.setForeground(UiUtils.COLOR_TEXT_MUTED);
                        l.setIcon(null);
                    } else {
                        l.setText("-");
                        l.setForeground(UiUtils.COLOR_TEXT_MUTED);
                        l.setIcon(null);
                    }
                    l.setFont(UiUtils.FONT_BOLD);
                    l.setToolTipText(sInfo.getHtmlTooltip());
                } else {
                    l.setText(value != null ? value.toString() : "-");
                    l.setIcon(null);
                    l.setToolTipText(null);
                }
                return l;
            }
        });

        tblTransactions.getColumnModel().getColumn(0).setPreferredWidth(50);
        tblTransactions.getColumnModel().getColumn(1).setPreferredWidth(75);
        tblTransactions.getColumnModel().getColumn(2).setPreferredWidth(90);
        tblTransactions.getColumnModel().getColumn(3).setPreferredWidth(85);
        tblTransactions.getColumnModel().getColumn(4).setPreferredWidth(100);
        tblTransactions.getColumnModel().getColumn(5).setPreferredWidth(125);
        tblTransactions.getColumnModel().getColumn(6).setPreferredWidth(90);
        tblTransactions.getColumnModel().getColumn(7).setPreferredWidth(110);
        tblTransactions.getColumnModel().getColumn(8).setPreferredWidth(120);
        tblTransactions.getColumnModel().getColumn(9).setPreferredWidth(130);
        tblTransactions.getColumnModel().getColumn(10).setPreferredWidth(160);

        tblTransactions.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tblTransactions.getSelectedRow() >= 0) {
                    onEditSelected();
                }
            }
        });

        setupTableContextMenu();

        JScrollPane scrollPane = new JScrollPane(tblTransactions);
        scrollPane.setBorder(UiUtils.createCardBorder());
        root.add(scrollPane, BorderLayout.CENTER);

        // SOUTH: Summary Totals Footer
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(Color.WHITE);
        footerPanel.setBorder(BorderFactory.createCompoundBorder(
                UiUtils.createCardBorder(),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));

        lblSummaryTotal = new JLabel(I18n.get("transactions.footer.loading"));
        lblSummaryTotal.setFont(UiUtils.boldFont());
        lblSummaryTotal.setForeground(new Color(15, 23, 42));

        footerPanel.add(lblSummaryTotal, BorderLayout.CENTER);
        root.add(footerPanel, BorderLayout.SOUTH);

        populateFilterDropdowns();
        return root;
    }

    // =========================================================================
    // TAB 2 BUILDER: Track & Analyze (Entity Centric)
    // =========================================================================
    private JPanel buildTrackAndAnalyzeTab() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setOpaque(false);

        // 1. TOP CONTROL BAR
        JPanel pnlHeader = new JPanel(new BorderLayout(0, 8));
        pnlHeader.setOpaque(false);

        JPanel pnlControlsCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        pnlControlsCard.setBackground(Color.WHITE);
        pnlControlsCard.setBorder(UiUtils.createCardBorder());

        JLabel lblType = new JLabel("Entity Type:");
        lblType.setFont(UiUtils.boldFont());
        pnlControlsCard.add(lblType);

        cmbTrackerType = new JComboBox<>(new String[]{
                "Person", "Bill", "EMI", "Credit Card", "Category"
        });
        cmbTrackerType.setFont(UiUtils.boldFont());
        cmbTrackerType.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if ("Person".equals(value)) setIcon(NavIcons.person(14, UiUtils.COLOR_PRIMARY));
                else if ("Bill".equals(value)) setIcon(NavIcons.bill(14, UiUtils.COLOR_PRIMARY));
                else if ("EMI".equals(value)) setIcon(NavIcons.emi(14, UiUtils.COLOR_PRIMARY));
                else if ("Credit Card".equals(value)) setIcon(NavIcons.card(14, UiUtils.COLOR_PRIMARY));
                else if ("Category".equals(value)) setIcon(NavIcons.filter(14, UiUtils.COLOR_PRIMARY));
                return this;
            }
        });
        cmbTrackerType.addActionListener(e -> {
            populateTrackerEntities();
            loadEntityTrackerData();
        });
        pnlControlsCard.add(cmbTrackerType);

        JLabel lblTarget = new JLabel("Target Entity:");
        lblTarget.setFont(UiUtils.boldFont());
        pnlControlsCard.add(lblTarget);

        cmbTrackerEntity = new JComboBox<>();
        cmbTrackerEntity.setFont(UiUtils.boldFont());
        cmbTrackerEntity.setPreferredSize(new Dimension(170, 28));
        cmbTrackerEntity.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Person p) {
                    setText(p.getName());
                    setIcon(NavIcons.person(14, UiUtils.COLOR_PRIMARY));
                } else if (value instanceof Bill b) {
                    setText(b.getBillName());
                    setIcon(NavIcons.bill(14, UiUtils.COLOR_PRIMARY));
                } else if (value instanceof Emi e) {
                    setText(e.getEmiName());
                    setIcon(NavIcons.emi(14, UiUtils.COLOR_PRIMARY));
                } else if (value instanceof CreditCard c) {
                    setText(c.getCardName());
                    setIcon(NavIcons.card(14, UiUtils.COLOR_PRIMARY));
                } else if (value != null) {
                    setText(value.toString());
                    setIcon(NavIcons.filter(14, UiUtils.COLOR_PRIMARY));
                }
                return this;
            }
        });
        cmbTrackerEntity.addActionListener(e -> loadEntityTrackerData());
        pnlControlsCard.add(cmbTrackerEntity);

        JLabel lblGran = new JLabel("Granularity:");
        lblGran.setFont(UiUtils.boldFont());
        pnlControlsCard.add(lblGran);

        cmbTrackerGranularity = new JComboBox<>(new String[]{
                "Months", "Days", "Week", "Quarter", "Years"
        });
        cmbTrackerGranularity.setFont(UiUtils.boldFont());
        cmbTrackerGranularity.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if ("Months".equals(value) || "Days".equals(value) || "Week".equals(value)) {
                    setIcon(NavIcons.calendar(14, UiUtils.COLOR_PRIMARY));
                } else {
                    setIcon(NavIcons.dashboard(14, UiUtils.COLOR_PRIMARY));
                }
                return this;
            }
        });
        cmbTrackerGranularity.addActionListener(e -> loadEntityTrackerData());
        pnlControlsCard.add(cmbTrackerGranularity);

        JLabel lblRange = new JLabel("Range:");
        lblRange.setFont(UiUtils.boldFont());
        pnlControlsCard.add(lblRange);

        txtTrackerStartDate = new DatePickerField(null, 8);
        txtTrackerEndDate = new DatePickerField(null, 8);
        pnlControlsCard.add(txtTrackerStartDate);
        pnlControlsCard.add(new JLabel("-"));
        pnlControlsCard.add(txtTrackerEndDate);

        cmbTrackerDatePreset = new JComboBox<>(new String[]{
                "All Time", "This Year", "Last 6 Months", "This Month", "Last 30 Days"
        });
        cmbTrackerDatePreset.addActionListener(e -> onTrackerDatePresetChanged());
        pnlControlsCard.add(cmbTrackerDatePreset);

        JButton btnAnalyze = UiUtils.createPrimaryButton("Analyze 📊", NavIcons.filter(13, Color.WHITE));
        btnAnalyze.addActionListener(e -> loadEntityTrackerData());
        pnlControlsCard.add(btnAnalyze);

        pnlHeader.add(pnlControlsCard, BorderLayout.NORTH);

        // 2. KPI METRICS STRIP
        JPanel pnlKpis = new JPanel(new GridLayout(1, 4, 10, 0));
        pnlKpis.setOpaque(false);

        kpiTrackerCount = new KpiCard("Total Transactions", "0 tx", "Matching entries", UiUtils.COLOR_PRIMARY, NavIcons.transactions(22, UiUtils.COLOR_PRIMARY));
        kpiTrackerInflow = new KpiCard("Total Inflow", "₹ 0.00", "Money Received / Income", UiUtils.COLOR_INCOME, NavIcons.plus(22, UiUtils.COLOR_INCOME));
        kpiTrackerOutflow = new KpiCard("Total Outflow", "₹ 0.00", "Payments & Outgoings", UiUtils.COLOR_EXPENSE, NavIcons.minus(22, UiUtils.COLOR_EXPENSE));
        kpiTrackerNet = new KpiCard("Net Position / Avg", "₹ 0.00", "Balance & bucket average", UiUtils.COLOR_AMBER, NavIcons.dashboard(22, UiUtils.COLOR_AMBER));

        pnlKpis.add(kpiTrackerCount);
        pnlKpis.add(kpiTrackerInflow);
        pnlKpis.add(kpiTrackerOutflow);
        pnlKpis.add(kpiTrackerNet);

        pnlHeader.add(pnlKpis, BorderLayout.SOUTH);
        root.add(pnlHeader, BorderLayout.NORTH);

        // 3. CENTER SPLIT: Visual Bar Chart (Top) + Itemized Transactions Table (Bottom)
        JPanel pnlCenter = new JPanel(new BorderLayout(0, 10));
        pnlCenter.setOpaque(false);

        trackerChartPanel = new TrendChartPanel("Temporal Cash Flow Comparison");
        trackerChartPanel.setPreferredSize(new Dimension(540, 220));
        trackerChartPanel.setSlotClickListener((tp, idx) -> {
            if (tp != null && tp.getStartDate() != null && tp.getEndDate() != null) {
                txtTrackerStartDate.setDate(tp.getStartDate());
                txtTrackerEndDate.setDate(tp.getEndDate());
                cmbTrackerDatePreset.setSelectedItem("Custom");
                loadEntityTrackerData();
            }
        });
        pnlCenter.add(trackerChartPanel, BorderLayout.NORTH);

        // Itemized table for tracker
        String[] trackerCols = {"Date", "Type", "Amount", "Category", "Sub Category", "Via", "Status", "Notes"};
        trackerTableModel = new DefaultTableModel(trackerCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblTrackerTransactions = new JTable(trackerTableModel);
        tblTrackerTransactions.setRowHeight(28);
        tblTrackerTransactions.setFont(UiUtils.regularFont());
        tblTrackerTransactions.getTableHeader().setFont(UiUtils.boldFont());
        tblTrackerTransactions.setShowGrid(false);

        tblTrackerTransactions.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(CENTER);
                l.setFont(UiUtils.FONT_BOLD);
                if ("Income".equalsIgnoreCase(String.valueOf(value))) {
                    l.setForeground(UiUtils.COLOR_INCOME);
                } else {
                    l.setForeground(UiUtils.COLOR_EXPENSE);
                }
                return l;
            }
        });

        tblTrackerTransactions.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(RIGHT);
                l.setFont(UiUtils.FONT_BOLD);
                Object typeVal = table.getValueAt(row, 1);
                if ("Income".equalsIgnoreCase(String.valueOf(typeVal))) l.setForeground(UiUtils.COLOR_INCOME);
                else l.setForeground(UiUtils.COLOR_EXPENSE);
                return l;
            }
        });

        tblTrackerTransactions.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tblTrackerTransactions.getSelectedRow() >= 0) {
                    onEditTrackerSelected();
                }
            }
        });

        JScrollPane trackerScroll = new JScrollPane(tblTrackerTransactions);
        trackerScroll.setBorder(UiUtils.createCardBorder());

        JPanel pnlTableCard = new JPanel(new BorderLayout(0, 6));
        pnlTableCard.setOpaque(false);

        JPanel pnlTableHeader = new JPanel(new BorderLayout());
        pnlTableHeader.setOpaque(false);
        JLabel lblLedgerTitle = new JLabel("Itemized Ledger Entries for Selected Entity");
        lblLedgerTitle.setFont(UiUtils.boldFont());
        lblLedgerTitle.setForeground(new Color(15, 23, 42));

        JPanel pnlTableActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        pnlTableActions.setOpaque(false);
        JButton btnEditTx = UiUtils.createSecondaryButton("Edit Transaction ✏️", NavIcons.edit(12, UiUtils.COLOR_TEXT_PRIMARY));
        btnEditTx.addActionListener(e -> onEditTrackerSelected());

        JButton btnExportEntity = UiUtils.createPrimaryButton("Export Statement 📄", NavIcons.export(12, Color.WHITE));
        btnExportEntity.addActionListener(e -> onExportEntityStatement());

        pnlTableActions.add(btnEditTx);
        pnlTableActions.add(btnExportEntity);

        pnlTableHeader.add(lblLedgerTitle, BorderLayout.WEST);
        pnlTableHeader.add(pnlTableActions, BorderLayout.EAST);

        pnlTableCard.add(pnlTableHeader, BorderLayout.NORTH);
        pnlTableCard.add(trackerScroll, BorderLayout.CENTER);

        pnlCenter.add(pnlTableCard, BorderLayout.CENTER);
        root.add(pnlCenter, BorderLayout.CENTER);

        populateTrackerEntities();
        return root;
    }

    // =========================================================================
    // POPULATION & EVENT LOGIC
    // =========================================================================

    private void populateTrackerEntities() {
        if (cmbTrackerEntity == null || cmbTrackerType == null) return;
        cmbTrackerEntity.removeAllItems();

        int typeIdx = cmbTrackerType.getSelectedIndex();
        switch (typeIdx) {
            case 0 -> { // Person
                for (Person p : financeService.getAllPersons()) cmbTrackerEntity.addItem(p);
            }
            case 1 -> { // Bill
                for (Bill b : financeService.getAllBills()) cmbTrackerEntity.addItem(b);
            }
            case 2 -> { // EMI
                for (Emi e : financeService.getAllEmis()) cmbTrackerEntity.addItem(e);
            }
            case 3 -> { // Credit Card
                for (CreditCard c : financeService.getAllCreditCards()) cmbTrackerEntity.addItem(c);
            }
            case 4 -> { // Category
                for (String cat : getDistinctCategories()) cmbTrackerEntity.addItem(cat);
            }
        }
        if (cmbTrackerEntity.getItemCount() > 0) {
            cmbTrackerEntity.setSelectedIndex(0);
        }
    }

    private List<String> getDistinctCategories() {
        List<String> list = new ArrayList<>();
        list.add("All Categories");
        list.add("EMI Payment");
        list.add("Credit Card");
        list.add("Bill");
        list.add("Loan Given");
        list.add("Salary");
        list.add("Return");
        list.add("Special Expense");
        list.add("Groceries & Food");
        list.add("Shopping");
        list.add("Health & Medical");
        list.add("Fuel & Travel");
        list.add("Other Expense");
        list.add("Other Income");
        return list;
    }

    private void onTrackerDatePresetChanged() {
        String preset = (String) cmbTrackerDatePreset.getSelectedItem();
        if (preset == null) return;
        LocalDate now = LocalDate.now();
        switch (preset) {
            case "All Time" -> {
                txtTrackerStartDate.setDate(null);
                txtTrackerEndDate.setDate(null);
            }
            case "This Year" -> {
                txtTrackerStartDate.setDate(now.withDayOfYear(1));
                txtTrackerEndDate.setDate(now.withMonth(12).withDayOfMonth(31));
            }
            case "Last 6 Months" -> {
                txtTrackerStartDate.setDate(now.minusMonths(6).withDayOfMonth(1));
                txtTrackerEndDate.setDate(now.with(TemporalAdjusters.lastDayOfMonth()));
            }
            case "This Month" -> {
                txtTrackerStartDate.setDate(now.withDayOfMonth(1));
                txtTrackerEndDate.setDate(now.with(TemporalAdjusters.lastDayOfMonth()));
            }
            case "Last 30 Days" -> {
                txtTrackerStartDate.setDate(now.minusDays(30));
                txtTrackerEndDate.setDate(now);
            }
        }
        loadEntityTrackerData();
    }

    public void loadEntityTrackerData() {
        if (cmbTrackerType == null || cmbTrackerEntity == null || cmbTrackerGranularity == null) return;

        int typeIdx = cmbTrackerType.getSelectedIndex();
        String entityType = switch (typeIdx) {
            case 0 -> "PERSON";
            case 1 -> "BILL";
            case 2 -> "EMI";
            case 3 -> "CREDIT_CARD";
            default -> "CATEGORY";
        };

        Long entityId = null;
        String categoryName = null;
        Object selected = cmbTrackerEntity.getSelectedItem();

        if (selected instanceof Person p) entityId = p.getId();
        else if (selected instanceof Bill b) entityId = b.getId();
        else if (selected instanceof Emi e) entityId = e.getId();
        else if (selected instanceof CreditCard c) entityId = c.getId();
        else if (selected instanceof String s) {
            categoryName = "All Categories".equalsIgnoreCase(s) ? null : s;
        }

        String rawGran = (String) cmbTrackerGranularity.getSelectedItem();
        String gran = "MONTHS";
        if (rawGran != null) {
            if (rawGran.contains("Days")) gran = "DAYS";
            else if (rawGran.contains("Week")) gran = "WEEK";
            else if (rawGran.contains("Quarter")) gran = "QUARTER";
            else if (rawGran.contains("Years")) gran = "YEARS";
        }

        LocalDate start = txtTrackerStartDate.getDate();
        LocalDate end = txtTrackerEndDate.getDate();

        activeTrackerSummary = financeService.getEntityTrackerSummary(entityType, entityId, categoryName, gran, start, end);

        // Update KPIs
        kpiTrackerCount.setValue(activeTrackerSummary.getTotalCount() + " tx");
        kpiTrackerInflow.setValue(AppConfig.formatCurrency(activeTrackerSummary.getTotalInflow()));
        kpiTrackerOutflow.setValue(AppConfig.formatCurrency(activeTrackerSummary.getTotalOutflow()));
        kpiTrackerNet.setValue(AppConfig.formatCurrency(activeTrackerSummary.getNetBalance()));
        kpiTrackerNet.setSubtitle("Avg: " + AppConfig.formatCurrency(activeTrackerSummary.getAveragePerBucket()) + " / " + gran.toLowerCase());

        // Update Chart
        String chartTitle = activeTrackerSummary.getEntityName() + " (" + gran + " Trend)";
        trackerChartPanel.setTitle(chartTitle);
        trackerChartPanel.setTrendPoints(activeTrackerSummary.getTrendPoints());

        // Populate Table
        trackerTableModel.setRowCount(0);
        for (Transaction tx : activeTrackerSummary.getTransactions()) {
            trackerTableModel.addRow(new Object[]{
                    tx.getTxDate() != null ? tx.getTxDate().toString() : "-",
                    tx.getType() == TransactionType.INCOME ? "Income" : "Expense",
                    AppConfig.formatCurrency(tx.getAmount()),
                    tx.getCategory() != null ? tx.getCategory() : "-",
                    tx.getSubCategory() != null ? tx.getSubCategory() : "-",
                    tx.getPaymentVia() != null ? tx.getPaymentVia() : "-",
                    tx.isExpected() ? "Expected" : "Settled",
                    tx.getNotes() != null ? tx.getNotes() : ""
            });
        }
    }

    private void onEditTrackerSelected() {
        int row = tblTrackerTransactions.getSelectedRow();
        if (row < 0 || activeTrackerSummary == null || row >= activeTrackerSummary.getTransactions().size()) {
            UiUtils.showError(this, "Please select a transaction to edit.");
            return;
        }
        Transaction tx = activeTrackerSummary.getTransactions().get(row);
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        TransactionDialog dlg = new TransactionDialog(frame, tx, tx.getType());
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            loadEntityTrackerData();
            applyFilters();
        }
    }

    private void onExportEntityStatement() {
        if (activeTrackerSummary == null || activeTrackerSummary.getTransactions().isEmpty()) {
            UiUtils.showError(this, "No transactions available for this entity to export.");
            return;
        }

        FilterCriteria criteria = new FilterCriteria();
        criteria.setSortBy("tx_date");
        criteria.setStartDate(txtTrackerStartDate.getDate());
        criteria.setEndDate(txtTrackerEndDate.getDate());

        String cleanName = activeTrackerSummary.getEntityName().replaceAll("[^a-zA-Z0-9_-]", "_");
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("statement_" + cleanName + "_" + LocalDate.now() + ".pdf"));
        fc.setFileFilter(new FileNameExtensionFilter("PDF Documents (*.pdf)", "pdf"));

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File target = fc.getSelectedFile();
            if (!target.getName().toLowerCase().endsWith(".pdf")) {
                target = new File(target.getAbsolutePath() + ".pdf");
            }
            try {
                exportService.exportToPdf(activeTrackerSummary.getTransactions(), criteria, target);
                UiUtils.showSuccess(this, "Statement exported successfully to:\n" + target.getAbsolutePath());
            } catch (Exception ex) {
                UiUtils.showError(this, "Export failed: " + ex.getMessage());
            }
        }
    }

    private void onTrackEntityFromTable() {
        int row = tblTransactions.getSelectedRow();
        if (row < 0) {
            tabbedPane.setSelectedIndex(1);
            return;
        }

        Long id = (Long) tblTransactions.getValueAt(row, 0);
        Transaction tx = financeService.getTransactionById(id);
        if (tx == null) {
            tabbedPane.setSelectedIndex(1);
            return;
        }

        if (tx.getPersonId() != null) {
            openEntityTracker("PERSON", tx.getPersonId(), null, "MONTHS");
        } else if (tx.getEmiId() != null) {
            openEntityTracker("EMI", tx.getEmiId(), null, "MONTHS");
        } else if (tx.getCreditCardId() != null) {
            openEntityTracker("CREDIT_CARD", tx.getCreditCardId(), null, "MONTHS");
        } else if (tx.getBillId() != null) {
            openEntityTracker("BILL", tx.getBillId(), null, "MONTHS");
        } else {
            openEntityTracker("CATEGORY", null, tx.getCategory(), "MONTHS");
        }
    }

    /**
     * Programmatically switches to Tab 2 and loads the tracker for the designated entity.
     *
     * @param entityType "PERSON", "BILL", "EMI", "CREDIT_CARD", or "CATEGORY"
     * @param entityId Primary key ID of entity (or null if category)
     * @param categoryName Category string name
     * @param granularity Time bucket: "DAYS", "WEEK", "MONTHS", "QUARTER", or "YEARS"
     */
    public void openEntityTracker(String entityType, Long entityId, String categoryName, String granularity) {
        if (tabbedPane != null) {
            tabbedPane.setSelectedIndex(1);
        }

        if (entityType != null && cmbTrackerType != null) {
            for (int i = 0; i < cmbTrackerType.getItemCount(); i++) {
                String item = cmbTrackerType.getItemAt(i);
                if (item != null && item.toUpperCase().contains(entityType.toUpperCase())) {
                    cmbTrackerType.setSelectedIndex(i);
                    break;
                }
            }
            populateTrackerEntities();

            if (entityId != null && cmbTrackerEntity != null) {
                for (int i = 0; i < cmbTrackerEntity.getItemCount(); i++) {
                    Object obj = cmbTrackerEntity.getItemAt(i);
                    if (obj instanceof Person p && entityId.equals(p.getId())) {
                        cmbTrackerEntity.setSelectedIndex(i);
                        break;
                    } else if (obj instanceof Bill b && entityId.equals(b.getId())) {
                        cmbTrackerEntity.setSelectedIndex(i);
                        break;
                    } else if (obj instanceof Emi e && entityId.equals(e.getId())) {
                        cmbTrackerEntity.setSelectedIndex(i);
                        break;
                    } else if (obj instanceof CreditCard c && entityId.equals(c.getId())) {
                        cmbTrackerEntity.setSelectedIndex(i);
                        break;
                    }
                }
            } else if (categoryName != null && cmbTrackerEntity != null) {
                for (int i = 0; i < cmbTrackerEntity.getItemCount(); i++) {
                    Object obj = cmbTrackerEntity.getItemAt(i);
                    if (categoryName.equalsIgnoreCase(String.valueOf(obj))) {
                        cmbTrackerEntity.setSelectedIndex(i);
                        break;
                    }
                }
            }
        }

        if (granularity != null && cmbTrackerGranularity != null) {
            for (int i = 0; i < cmbTrackerGranularity.getItemCount(); i++) {
                String item = cmbTrackerGranularity.getItemAt(i);
                if (item != null && item.toUpperCase().contains(granularity.toUpperCase())) {
                    cmbTrackerGranularity.setSelectedIndex(i);
                    break;
                }
            }
        }

        loadEntityTrackerData();
    }

    /**
     * Programmatically switches to the All Transactions ledger view and filters
     * records by the specified inclusive date window.
     *
     * @param start start date
     * @param end end date
     */
    public void filterByDateRange(LocalDate start, LocalDate end) {
        if (tabbedPane != null) {
            tabbedPane.setSelectedIndex(0);
        }
        if (cmbDatePreset != null) {
            cmbDatePreset.setSelectedItem("Custom");
        }
        if (txtStartDate != null) {
            txtStartDate.setDate(start);
        }
        if (txtEndDate != null) {
            txtEndDate.setDate(end);
        }
        applyFilters();
    }

    // =========================================================================
    // ALL TRANSACTIONS TAB HELPERS & FILTERS
    // =========================================================================

    private void populateFilterDropdowns() {
        updateCategoryOptions();

        // Payment channels
        cmbViaFilter.removeAllItems();
        cmbViaFilter.addItem(I18n.get("transactions.filter.allGeneric"));
        for (String m : financeService.getAllPaymentModes()) cmbViaFilter.addItem(m);

        // Master Entities Filter (Persons, Bills, EMIs, Cards)
        cmbMasterFilter.removeAllItems();
        cmbMasterFilter.addItem(I18n.get("transactions.filter.allGeneric"));
        for (Person p : financeService.getAllPersons()) cmbMasterFilter.addItem(p);
        for (Bill b : financeService.getAllBills()) cmbMasterFilter.addItem(b);
        for (Emi e : financeService.getAllEmis()) cmbMasterFilter.addItem(e);
        for (CreditCard c : financeService.getAllCreditCards()) cmbMasterFilter.addItem(c);
    }

    private boolean containsFilterItem(JComboBox<String> cmb, String item) {
        if (item == null) return false;
        for (int i = 0; i < cmb.getItemCount(); i++) {
            if (item.equalsIgnoreCase(cmb.getItemAt(i))) return true;
        }
        return false;
    }

    private void updateCategoryOptions() {
        cmbCategoryFilter.removeAllItems();
        cmbCategoryFilter.addItem(I18n.get("transactions.filter.allGeneric"));

        int typeIdx = cmbTypeFilter.getSelectedIndex();
        TransactionType type = null;
        if (typeIdx == 1) type = TransactionType.INCOME;
        else if (typeIdx == 2) type = TransactionType.EXPENSE;

        List<String> categories = financeService.getCategoryNames(type);
        for (String c : categories) {
            cmbCategoryFilter.addItem(c);
        }
        updateSubCategoryOptions();
    }

    private void updateSubCategoryOptions() {
        cmbSubCategoryFilter.removeAllItems();
        cmbSubCategoryFilter.addItem(I18n.get("transactions.filter.allGeneric"));

        String cat = (String) cmbCategoryFilter.getSelectedItem();
        if (cat == null || cat.equalsIgnoreCase(I18n.get("transactions.filter.allGeneric"))) return;

        int typeIdx = cmbTypeFilter.getSelectedIndex();
        TransactionType type = null;
        if (typeIdx == 1) type = TransactionType.INCOME;
        else if (typeIdx == 2) type = TransactionType.EXPENSE;

        List<String> subs = financeService.getSubCategoryNames(cat, type);
        for (String s : subs) {
            cmbSubCategoryFilter.addItem(s);
        }

        if ("Bill".equalsIgnoreCase(cat)) {
            for (Bill b : financeService.getAllBills()) {
                if (!containsFilterItem(cmbSubCategoryFilter, b.getBillName())) {
                    cmbSubCategoryFilter.addItem(b.getBillName());
                }
            }
        } else if ("Credit Card".equalsIgnoreCase(cat)) {
            for (CreditCard c : financeService.getAllCreditCards()) {
                if (!containsFilterItem(cmbSubCategoryFilter, c.getCardName())) {
                    cmbSubCategoryFilter.addItem(c.getCardName());
                }
            }
        } else if ("EMI Payment".equalsIgnoreCase(cat)) {
            for (Emi e : financeService.getAllEmis()) {
                if (!containsFilterItem(cmbSubCategoryFilter, e.getEmiName())) {
                    cmbSubCategoryFilter.addItem(e.getEmiName());
                }
            }
        } else if ("Loan Given".equalsIgnoreCase(cat) || "Return".equalsIgnoreCase(cat) || "Relative".equalsIgnoreCase(cat)) {
            for (Person p : financeService.getAllPersons()) {
                if (!containsFilterItem(cmbSubCategoryFilter, p.getName())) {
                    cmbSubCategoryFilter.addItem(p.getName());
                }
            }
        }
    }

    private void onDatePresetChanged() {
        int idx = cmbDatePreset.getSelectedIndex();
        LocalDate now = LocalDate.now();
        switch (idx) {
            case 0 -> { // All Time
                txtStartDate.setDate(null);
                txtEndDate.setDate(null);
            }
            case 1 -> { // Today
                txtStartDate.setDate(now);
                txtEndDate.setDate(now);
            }
            case 2 -> { // This Week
                txtStartDate.setDate(now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
                txtEndDate.setDate(now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
            }
            case 3 -> { // This Month
                txtStartDate.setDate(now.withDayOfMonth(1));
                txtEndDate.setDate(now.with(TemporalAdjusters.lastDayOfMonth()));
            }
            case 4 -> { // This Quarter
                int currentQuarter = (now.getMonthValue() - 1) / 3 + 1;
                LocalDate qStart = LocalDate.of(now.getYear(), (currentQuarter - 1) * 3 + 1, 1);
                txtStartDate.setDate(qStart);
                txtEndDate.setDate(qStart.plusMonths(3).minusDays(1));
            }
            case 5 -> { // This Year
                txtStartDate.setDate(LocalDate.of(now.getYear(), 1, 1));
                txtEndDate.setDate(LocalDate.of(now.getYear(), 12, 31));
            }
            default -> {} // Custom
        }
        applyFilters();
    }

    public void applyFilters() {
        currentCriteria = new FilterCriteria();

        // Type
        int typeIdx = cmbTypeFilter.getSelectedIndex();
        if (typeIdx == 1) currentCriteria.setType(TransactionType.INCOME);
        else if (typeIdx == 2) currentCriteria.setType(TransactionType.EXPENSE);

        // Category & Sub Category
        String cat = (String) cmbCategoryFilter.getSelectedItem();
        if (cat != null && !cat.equalsIgnoreCase(I18n.get("transactions.filter.allGeneric"))) currentCriteria.setCategory(cat);

        String sub = (String) cmbSubCategoryFilter.getSelectedItem();
        if (sub != null && !sub.equalsIgnoreCase(I18n.get("transactions.filter.allGeneric"))) currentCriteria.setSubCategory(sub);

        // Payment Channel
        String via = (String) cmbViaFilter.getSelectedItem();
        if (via != null && !via.equalsIgnoreCase(I18n.get("transactions.filter.allGeneric"))) currentCriteria.setPaymentVia(via);

        // Associated Master (Person, Bill, EMI, Card)
        Object masterObj = cmbMasterFilter.getSelectedItem();
        if (masterObj instanceof Person p) {
            currentCriteria.setPersonId(p.getId());
            currentCriteria.setPersonName(p.getName());
        } else if (masterObj instanceof Bill b) {
            currentCriteria.setBillId(b.getId());
            currentCriteria.setBillName(b.getBillName());
        } else if (masterObj instanceof Emi e) {
            currentCriteria.setEmiId(e.getId());
            currentCriteria.setEmiName(e.getEmiName());
        } else if (masterObj instanceof CreditCard c) {
            currentCriteria.setCreditCardId(c.getId());
            currentCriteria.setCardName(c.getCardName());
        }

        // Active Date Preset
        if (cmbDatePreset != null && cmbDatePreset.getSelectedIndex() >= 0) {
            currentCriteria.setDatePresetName((String) cmbDatePreset.getSelectedItem());
        }

        // Custom Date Range
        currentCriteria.setStartDate(txtStartDate.getDate());
        currentCriteria.setEndDate(txtEndDate.getDate());

        // Amount Limits
        String strMin = txtMinAmount.getText().trim();
        if (!strMin.isEmpty()) {
            try {
                currentCriteria.setMinAmount(Double.parseDouble(strMin));
            } catch (NumberFormatException ignored) {}
        }

        String strMax = txtMaxAmount.getText().trim();
        if (!strMax.isEmpty()) {
            try {
                currentCriteria.setMaxAmount(Double.parseDouble(strMax));
            } catch (NumberFormatException ignored) {}
        }

        // Search Keyword
        String kw = txtSearch.getText().trim();
        if (!kw.isEmpty()) currentCriteria.setSearchKeyword(kw);

        // Sorting
        String sortBy = (String) cmbSortBy.getSelectedItem();
        String sortCol = switch (sortBy != null ? sortBy.toLowerCase() : "") {
            case "amount" -> "amount";
            case "type" -> "type";
            case "category" -> "category";
            case "sub category" -> "sub_category";
            default -> "tx_date";
        };
        currentCriteria.setSortBy(sortCol);
        currentCriteria.setSortAscending(cmbSortDir.getSelectedIndex() == 1 || "Ascending".equals(cmbSortDir.getSelectedItem()) || "Asc ▲".equals(cmbSortDir.getSelectedItem()));

        // Query Database
        currentTransactions = financeService.filterTransactions(currentCriteria);

        // Settlement Status Filtering
        int settleIdx = cmbSettlementFilter != null ? cmbSettlementFilter.getSelectedIndex() : 0;
        Map<Long, TransactionSettlementInfo> settlementMap = financeService.getSettlementInfoMap(currentTransactions);

        if (settleIdx > 0) {
            List<Transaction> filtered = new ArrayList<>();
            for (Transaction tx : currentTransactions) {
                TransactionSettlementInfo info = settlementMap.get(tx.getId());
                if (info == null) continue;
                if (settleIdx == 1 && info.isNullified()) filtered.add(tx);
                else if (settleIdx == 2 && info.getStatus() == SettlementStatus.PARTIAL) filtered.add(tx);
                else if (settleIdx == 3 && info.getStatus() == SettlementStatus.UNLINKED) filtered.add(tx);
            }
            currentTransactions = filtered;
        }

        // Populate Table Model
        tableModel.setRowCount(0);
        double totalIn = 0;
        double totalOut = 0;

        for (Transaction tx : currentTransactions) {
            if (tx.getType() == TransactionType.INCOME) totalIn += tx.getAmount();
            else totalOut += tx.getAmount();

            TransactionSettlementInfo sInfo = settlementMap.get(tx.getId());

            String masterDisplay = "-";
            if (tx.getPersonName() != null && !tx.getPersonName().isBlank()) {
                masterDisplay = "👤 " + tx.getPersonName();
            } else if (tx.getBillName() != null && !tx.getBillName().isBlank()) {
                masterDisplay = "🧾 " + tx.getBillName();
            } else if (tx.getEmiName() != null && !tx.getEmiName().isBlank()) {
                masterDisplay = "🏦 " + tx.getEmiName();
            } else if (tx.getCardName() != null && !tx.getCardName().isBlank()) {
                masterDisplay = "💳 " + tx.getCardName();
            }

            tableModel.addRow(new Object[]{
                    tx.getId(),
                    tx.getType() == TransactionType.INCOME ? "Income" : "Expense",
                    tx.getTxDate() != null ? tx.getTxDate().toString() : "-",
                    tx.isExpected() ? "Expected" : "Settled",
                    AppConfig.formatCurrency(tx.getAmount()),
                    sInfo != null ? sInfo : "Unlinked",
                    tx.getPaymentVia() != null ? tx.getPaymentVia() : "-",
                    tx.getCategory() != null ? tx.getCategory() : "-",
                    tx.getSubCategory() != null ? tx.getSubCategory() : "-",
                    masterDisplay,
                    tx.getNotes() != null ? tx.getNotes() : ""
            });
        }

        // Update footer totals
        double net = totalIn - totalOut;
        String netStr = (net >= 0 ? "+" : "") + AppConfig.formatCurrency(net);
        lblSummaryTotal.setText(String.format("Showing %d transactions | Total In: %s | Total Out: %s | Net Balance: %s",
                currentTransactions.size(), AppConfig.formatCurrency(totalIn), AppConfig.formatCurrency(totalOut), netStr));
    }

    private void resetFilters() {
        txtSearch.setText("");
        cmbTypeFilter.setSelectedIndex(0);
        cmbCategoryFilter.setSelectedIndex(0);
        cmbDatePreset.setSelectedIndex(0);
        txtStartDate.setDate(null);
        txtEndDate.setDate(null);
        cmbViaFilter.setSelectedIndex(0);
        cmbMasterFilter.setSelectedIndex(0);
        cmbSettlementFilter.setSelectedIndex(0);
        txtMinAmount.setText("");
        txtMaxAmount.setText("");
        cmbSortBy.setSelectedIndex(0);
        cmbSortDir.setSelectedIndex(0);
        applyFilters();
    }

    private void openAddDialog(TransactionType type) {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        TransactionDialog dialog = new TransactionDialog(frame, null, type);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            applyFilters();
            populateFilterDropdowns();
        }
    }

    private void onEditSelected() {
        int row = tblTransactions.getSelectedRow();
        if (row < 0) {
            UiUtils.showError(this, "Please select a transaction to edit.");
            return;
        }

        Long id = (Long) tblTransactions.getValueAt(row, 0);
        Transaction tx = financeService.getTransactionById(id);
        if (tx != null) {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            TransactionDialog dialog = new TransactionDialog(frame, tx, tx.getType());
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                applyFilters();
                populateFilterDropdowns();
            }
        }
    }

    private void onDeleteSelected() {
        int row = tblTransactions.getSelectedRow();
        if (row < 0) {
            UiUtils.showError(this, "Please select a transaction to delete.");
            return;
        }

        Long id = (Long) tblTransactions.getValueAt(row, 0);
        if (UiUtils.confirm(this, "Are you sure you want to delete Transaction #" + id + "?")) {
            boolean ok = financeService.deleteTransaction(id);
            if (ok) {
                UiUtils.showSuccess(this, "Transaction #" + id + " deleted successfully.");
                applyFilters();
            } else {
                UiUtils.showError(this, "Failed to delete transaction.");
            }
        }
    }

    private void onLinkSelected() {
        int row = tblTransactions.getSelectedRow();
        if (row < 0) {
            UiUtils.showError(this, "Please select a transaction to link counterparts.");
            return;
        }

        Long id = (Long) tblTransactions.getValueAt(row, 0);
        Transaction tx = financeService.getTransactionById(id);
        if (tx == null) return;

        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        TransactionLinkDialog dialog = new TransactionLinkDialog(frame, tx, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            applyFilters();
        }
    }

    private void exportCurrentView(String format) {
        if (currentTransactions.isEmpty()) {
            UiUtils.showError(this, "No transactions to export. Adjust filters and try again.");
            return;
        }

        JFileChooser fc = new JFileChooser();
        String defaultName = "nav_finance_export_" + LocalDate.now();

        if ("PDF".equalsIgnoreCase(format)) {
            fc.setSelectedFile(new File(defaultName + ".pdf"));
            fc.setFileFilter(new FileNameExtensionFilter("PDF Documents (*.pdf)", "pdf"));
        } else if ("EXCEL".equalsIgnoreCase(format)) {
            fc.setSelectedFile(new File(defaultName + ".xlsx"));
            fc.setFileFilter(new FileNameExtensionFilter("Excel Spreadsheets (*.xlsx)", "xlsx"));
        } else {
            fc.setSelectedFile(new File(defaultName + ".csv"));
            fc.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
        }

        int res = fc.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File target = fc.getSelectedFile();
            try {
                switch (format.toUpperCase()) {
                    case "PDF" -> {
                        if (!target.getName().toLowerCase().endsWith(".pdf")) {
                            target = new File(target.getAbsolutePath() + ".pdf");
                        }
                        exportService.exportToPdf(currentTransactions, currentCriteria, target);
                        UiUtils.showSuccess(this, "Exported successfully to PDF:\n" + target.getAbsolutePath());
                    }
                    case "EXCEL" -> {
                        if (!target.getName().toLowerCase().endsWith(".xlsx")) {
                            target = new File(target.getAbsolutePath() + ".xlsx");
                        }
                        exportService.exportToExcel(currentTransactions, currentCriteria, target);
                        UiUtils.showSuccess(this, "Exported successfully to Excel:\n" + target.getAbsolutePath());
                    }
                    default -> {
                        if (!target.getName().toLowerCase().endsWith(".csv")) {
                            target = new File(target.getAbsolutePath() + ".csv");
                        }
                        exportService.exportToCsv(currentTransactions, currentCriteria, target);
                        UiUtils.showSuccess(this, "Exported successfully to CSV:\n" + target.getAbsolutePath());
                    }
                }
            } catch (Exception ex) {
                UiUtils.showError(this, "Export failed: " + ex.getMessage());
            }
        }
    }

    private void onOpenImportCsv() {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        CsvImportDialog dialog = new CsvImportDialog(frame);
        dialog.setVisible(true);
        if (dialog.isImported()) {
            applyFilters();
            populateFilterDropdowns();
        }
    }

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
                CsvImportService.getInstance().generateCsvTemplate(target);
                UiUtils.showSuccess(this, "CSV Template saved successfully to:\n" + target.getAbsolutePath());
            } catch (Exception ex) {
                UiUtils.showError(this, "Failed to save template: " + ex.getMessage());
            }
        }
    }

    private void setupTableContextMenu() {
        JPopupMenu popup = new JPopupMenu();

        JMenuItem itmEdit = new JMenuItem(I18n.get("btn.edit"), NavIcons.edit(12, UiUtils.COLOR_PRIMARY));
        itmEdit.addActionListener(e -> onEditSelected());

        JMenuItem itmTrack = new JMenuItem("Track Entity & Trends 📊", NavIcons.dashboard(12, UiUtils.COLOR_PRIMARY));
        itmTrack.addActionListener(e -> onTrackEntityFromTable());

        JMenuItem itmLink = new JMenuItem(I18n.get("link.action.linkCounterpart"), NavIcons.link(12, UiUtils.COLOR_PRIMARY));
        itmLink.addActionListener(e -> onLinkSelected());

        JMenuItem itmDelete = new JMenuItem(I18n.get("btn.delete"), NavIcons.trash(12, UiUtils.COLOR_EXPENSE));
        itmDelete.addActionListener(e -> onDeleteSelected());

        popup.add(itmEdit);
        popup.add(itmTrack);
        popup.add(itmLink);
        popup.addSeparator();
        popup.add(itmDelete);

        tblTransactions.setComponentPopupMenu(popup);
    }

    @Override
    public void onLanguageChanged(AppLanguage newLanguage) {
        initComponents();
        applyFilters();
    }
}
