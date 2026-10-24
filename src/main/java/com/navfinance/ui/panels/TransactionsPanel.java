package com.navfinance.ui.panels;

import com.navfinance.config.AppConfig;
import com.navfinance.model.FilterCriteria;
import com.navfinance.model.Person;
import com.navfinance.model.Transaction;
import com.navfinance.model.TransactionType;
import com.navfinance.service.CsvImportService;
import com.navfinance.service.ExportService;
import com.navfinance.service.FinanceService;
import com.navfinance.i18n.AppLanguage;
import com.navfinance.i18n.I18n;
import com.navfinance.i18n.LanguageChangeListener;
import com.navfinance.ui.components.DatePickerField;
import com.navfinance.ui.dialogs.CsvImportDialog;
import com.navfinance.ui.dialogs.TransactionDialog;
import com.navfinance.ui.util.NavIcons;
import com.navfinance.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
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
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Primary transactional ledger management panel for NavFinance.
 */
public class TransactionsPanel extends JPanel implements LanguageChangeListener {

    /** Business service managing transaction CRUD operations and master catalog dependencies. */
    private final FinanceService financeService = FinanceService.getInstance();

    /** Business export service handling PDF, Excel, and CSV file generation. */
    private final ExportService exportService = ExportService.getInstance();

    /** Filtered snapshot list of transactions currently loaded in the active view. */
    private List<Transaction> currentTransactions = new ArrayList<>();

    // Filter controls
    /** Dropdown filter selector for transaction flow (Income, Expense, All). */
    private JComboBox<String> cmbTypeFilter;

    /** Dynamic category selector filtered based on selected transaction type. */
    private JComboBox<String> cmbCategoryFilter;

    /** Dynamic subcategory selector filtered based on selected category. */
    private JComboBox<String> cmbSubCategoryFilter;

    /** Dropdown selector for rapid calendar presets (Today, Month, Quarter, Year, etc.). */
    private JComboBox<String> cmbDatePreset;

    /** Date picker field for start boundary in custom date range queries. */
    private DatePickerField txtStartDate;

    /** Date picker field for end boundary in custom date range queries. */
    private DatePickerField txtEndDate;

    /** Dropdown selector for payment channel (Via: Cash, UPI, Paytm, PhonePe, Cards, etc.). */
    private JComboBox<String> cmbViaFilter;

    /** Dropdown selector for person counterparties (Udhar, loan, or relative entries). */
    private JComboBox<Object> cmbPersonFilter;

    /** Text field specifying minimum transaction amount filter. */
    private JTextField txtMinAmount;

    /** Text field specifying maximum transaction amount filter. */
    private JTextField txtMaxAmount;

    /** Search text field for keyword matching across notes, category, person, or mode. */
    private JTextField txtSearch;

    // Sorting controls
    /** Dropdown selector for sort column (Date, Amount, Type, Category, Sub Category). */
    private JComboBox<String> cmbSortBy;

    /** Dropdown selector for sort order direction (Ascending vs Descending). */
    private JComboBox<String> cmbSortDir;

    // Table
    /** Swing table displaying transaction records. */
    private JTable tblTransactions;

    /** Table model backing the transactions grid. */
    private DefaultTableModel tableModel;

    // Summary footer
    /** Status footer label summarizing count, Total In, Total Out, and Net Balance. */
    private JLabel lblSummaryTotal;

    /**
     * Constructs a new {@code TransactionsPanel}, assembling top action bars,
     * the collapsible filter card, the transactional table, and bottom summary aggregates.
     */
    public TransactionsPanel() {
        setLayout(new BorderLayout(0, 10));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        initComponents();
        I18n.addListener(this);
        applyFilters();
    }

    /**
     * Initializes visual components, styling, action triggers, table column renderers, and event bindings.
     */
    private void initComponents() {
        removeAll();

        // TOP SECTION: Action bar + Advanced Filter Card
        JPanel topContainer = new JPanel(new BorderLayout(0, 10));
        topContainer.setOpaque(false);

        // 1. Action Header Row
        JPanel actionHeader = new JPanel(new BorderLayout());
        actionHeader.setOpaque(false);

        JLabel lblTitle = new JLabel(I18n.get("transactions.title"));
        lblTitle.setFont(UiUtils.titleFont());
        lblTitle.setForeground(new Color(15, 23, 42));
        actionHeader.add(lblTitle, BorderLayout.WEST);

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
        btnGroup.add(btnImportCsv);
        btnGroup.add(btnExport);

        actionHeader.add(btnGroup, BorderLayout.EAST);
        topContainer.add(actionHeader, BorderLayout.NORTH);

        // 2. Filters Card Panel
        JPanel filterCard = new JPanel(new GridBagLayout());
        filterCard.setBackground(Color.WHITE);
        filterCard.setBorder(UiUtils.createCardBorder());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Row 1: Search, Type, Category, SubCategory
        int row = 0;

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

        // Row 2: Date Filter, Via, Person, Sorting
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
        filterCard.add(new JLabel(I18n.get("transactions.filter.person")), gbc);
        gbc.gridx = 5; gbc.gridy = row;
        cmbPersonFilter = new JComboBox<>();
        cmbPersonFilter.addActionListener(e -> applyFilters());
        filterCard.add(cmbPersonFilter, gbc);

        gbc.gridx = 6; gbc.gridy = row;
        filterCard.add(new JLabel(I18n.get("transactions.sort.by")), gbc);
        gbc.gridx = 7; gbc.gridy = row;
        JPanel sortBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        sortBox.setOpaque(false);
        cmbSortBy = new JComboBox<>(new String[]{"Date", "Amount", "Type", "Category", "Sub Category"});
        cmbSortDir = new JComboBox<>(new String[]{"Desc ▼", "Asc ▲"});
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
        add(topContainer, BorderLayout.NORTH);

        // CENTER: Transactions Table configuration
        String[] columns = {
                I18n.get("transactions.table.id"),
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

        // Format Type Column (In = Green +, Out = Red -)
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

        // Format Status Column (Expected = Amber Dot, Settled = Green Dot)
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

        // Format Amount Column (Right aligned, colored)
        tblTransactions.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(RIGHT);
                setFont(UiUtils.FONT_BOLD);
                Object typeVal = table.getValueAt(row, 1);
                if ("Income".equalsIgnoreCase(String.valueOf(typeVal))) {
                    setForeground(UiUtils.COLOR_INCOME);
                } else {
                    setForeground(UiUtils.COLOR_EXPENSE);
                }
                return c;
            }
        });

        // Double click to edit selected record
        tblTransactions.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onEditSelected();
                }
            }
        });

        JScrollPane sp = new JScrollPane(tblTransactions);
        sp.setBorder(UiUtils.createCardBorder());
        add(sp, BorderLayout.CENTER);

        // BOTTOM: Summary totals footer
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(Color.WHITE);
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiUtils.COLOR_BORDER),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));

        lblSummaryTotal = new JLabel("Loading totals...");
        lblSummaryTotal.setFont(UiUtils.boldFont());
        bottomBar.add(lblSummaryTotal, BorderLayout.WEST);

        JLabel lblHelp = new JLabel(I18n.get("transactions.hint.doubleClick"), NavIcons.userGuide(14, UiUtils.COLOR_TEXT_MUTED), JLabel.LEFT);
        lblHelp.setFont(UiUtils.subtitleFont());
        lblHelp.setForeground(UiUtils.COLOR_TEXT_MUTED);
        bottomBar.add(lblHelp, BorderLayout.EAST);

        add(bottomBar, BorderLayout.SOUTH);

        // Populate dropdown options on startup
        populateFilterDropdowns();
    }

    /**
     * Refreshes dynamic dropdown options for payment channels and person contacts.
     */
    private void populateFilterDropdowns() {
        // Payment Via dropdown options
        cmbViaFilter.removeAllItems();
        cmbViaFilter.addItem(I18n.get("transactions.filter.allGeneric"));
        for (String mode : financeService.getAllPaymentModes()) {
            cmbViaFilter.addItem(mode);
        }

        // Person counterparty dropdown options
        cmbPersonFilter.removeAllItems();
        cmbPersonFilter.addItem(I18n.get("transactions.filter.allPersons"));
        for (Person p : financeService.getAllPersons()) {
            cmbPersonFilter.addItem(p);
        }

        updateCategoryOptions();
    }

    /**
     * Updates category dropdown choices based on the currently selected TransactionType filter.
     */
    private void updateCategoryOptions() {
        cmbCategoryFilter.removeAllItems();
        cmbCategoryFilter.addItem("All");

        String selectedTypeStr = (String) cmbTypeFilter.getSelectedItem();
        TransactionType type = null;
        if ("Income (In)".equals(selectedTypeStr)) {
            type = TransactionType.INCOME;
        } else if ("Expense (Out)".equals(selectedTypeStr)) {
            type = TransactionType.EXPENSE;
        }

        List<String> categories = financeService.getCategories(type);
        for (String cat : categories) {
            cmbCategoryFilter.addItem(cat);
        }
        updateSubCategoryOptions();
    }

    /**
     * Updates sub-category choices based on the currently selected category.
     */
    private void updateSubCategoryOptions() {
        cmbSubCategoryFilter.removeAllItems();
        cmbSubCategoryFilter.addItem("All");
        String cat = (String) cmbCategoryFilter.getSelectedItem();
        List<String> subCats = financeService.getSubCategories(cat);
        for (String sub : subCats) {
            cmbSubCategoryFilter.addItem(sub);
        }
    }

    /**
     * Calculates and sets date picker boundary values based on preset selections
     * such as Today, This Week, This Month, This Quarter, or This Year.
     */
    private void onDatePresetChanged() {
        String preset = (String) cmbDatePreset.getSelectedItem();
        LocalDate now = LocalDate.now();

        if (preset != null) {
            switch (preset) {
                case "All Time" -> {
                    txtStartDate.setDate(null);
                    txtEndDate.setDate(null);
                }
                case "Today" -> {
                    txtStartDate.setDate(now);
                    txtEndDate.setDate(now);
                }
                case "This Week" -> {
                    LocalDate start = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    LocalDate end = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
                    txtStartDate.setDate(start);
                    txtEndDate.setDate(end);
                }
                case "This Month" -> {
                    LocalDate start = now.withDayOfMonth(1);
                    LocalDate end = now.with(TemporalAdjusters.lastDayOfMonth());
                    txtStartDate.setDate(start);
                    txtEndDate.setDate(end);
                }
                case "This Quarter" -> {
                    int q = (now.getMonthValue() - 1) / 3 + 1;
                    LocalDate start = LocalDate.of(now.getYear(), (q - 1) * 3 + 1, 1);
                    LocalDate end = start.plusMonths(3).minusDays(1);
                    txtStartDate.setDate(start);
                    txtEndDate.setDate(end);
                }
                case "This Year" -> {
                    LocalDate start = LocalDate.of(now.getYear(), 1, 1);
                    LocalDate end = LocalDate.of(now.getYear(), 12, 31);
                    txtStartDate.setDate(start);
                    txtEndDate.setDate(end);
                }
                default -> {}
            }
        }
        applyFilters();
    }

    /**
     * Collects all criteria from UI filters and queries {@link FinanceService} to populate the table.
     */
    public void applyFilters() {
        FilterCriteria criteria = new FilterCriteria();

        // Transaction Type
        String typeStr = (String) cmbTypeFilter.getSelectedItem();
        if ("Income (In)".equals(typeStr)) criteria.setType(TransactionType.INCOME);
        else if ("Expense (Out)".equals(typeStr)) criteria.setType(TransactionType.EXPENSE);

        // Category & Sub Category
        String cat = (String) cmbCategoryFilter.getSelectedItem();
        if (cat != null && !cat.equalsIgnoreCase("All")) criteria.setCategory(cat);

        String sub = (String) cmbSubCategoryFilter.getSelectedItem();
        if (sub != null && !sub.equalsIgnoreCase("All")) criteria.setSubCategory(sub);

        // Via (Payment Channel)
        String via = (String) cmbViaFilter.getSelectedItem();
        if (via != null && !via.equalsIgnoreCase("All")) criteria.setPaymentVia(via);

        // Person Counterparty
        Object personObj = cmbPersonFilter.getSelectedItem();
        if (personObj instanceof Person p) {
            criteria.setPersonId(p.getId());
        }

        // Custom Date Range
        criteria.setStartDate(txtStartDate.getDate());
        criteria.setEndDate(txtEndDate.getDate());

        // Amount Limits
        String strMin = txtMinAmount.getText().trim();
        if (!strMin.isEmpty()) {
            try {
                criteria.setMinAmount(Double.parseDouble(strMin));
            } catch (Exception ignored) {}
        }

        String strMax = txtMaxAmount.getText().trim();
        if (!strMax.isEmpty()) {
            try {
                criteria.setMaxAmount(Double.parseDouble(strMax));
            } catch (Exception ignored) {}
        }

        // Search Keyword
        String kw = txtSearch.getText().trim();
        if (!kw.isEmpty()) criteria.setSearchKeyword(kw);

        // Sorting Column
        String sortBy = (String) cmbSortBy.getSelectedItem();
        String sortCol = switch (sortBy != null ? sortBy.toLowerCase() : "") {
            case "amount" -> "amount";
            case "type" -> "type";
            case "category" -> "category";
            case "sub category" -> "sub_category";
            default -> "tx_date";
        };
        criteria.setSortBy(sortCol);

        String sortDir = (String) cmbSortDir.getSelectedItem();
        criteria.setSortAscending(sortDir != null && sortDir.contains("Asc"));

        // Execute query against database
        currentTransactions = financeService.filterTransactions(criteria);

        // Update Table Rows and Aggregate Financial Totals
        tableModel.setRowCount(0);
        double totalIn = 0;
        double totalOut = 0;

        for (Transaction tx : currentTransactions) {
            if (tx.getType() == TransactionType.INCOME) totalIn += tx.getAmount();
            else totalOut += tx.getAmount();

            tableModel.addRow(new Object[]{
                    tx.getId(),
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

        // Update Summary Footer Display
        double net = totalIn - totalOut;
        lblSummaryTotal.setText(String.format("%s  |  %s  |  %s  |  %s",
                I18n.get("transactions.summary.records", currentTransactions.size()),
                I18n.get("transactions.summary.totalIn", AppConfig.formatCurrency(totalIn)),
                I18n.get("transactions.summary.totalOut", AppConfig.formatCurrency(totalOut)),
                I18n.get("transactions.summary.balance", AppConfig.formatCurrency(net))));
    }

    /**
     * Resets all search filters, date ranges, and sorting preferences to their default states.
     */
    private void resetFilters() {
        txtSearch.setText("");
        cmbTypeFilter.setSelectedIndex(0);
        cmbDatePreset.setSelectedIndex(0);
        txtStartDate.setDate(null);
        txtEndDate.setDate(null);
        txtMinAmount.setText("");
        txtMaxAmount.setText("");
        cmbViaFilter.setSelectedIndex(0);
        cmbPersonFilter.setSelectedIndex(0);
        cmbSortBy.setSelectedIndex(0);
        cmbSortDir.setSelectedIndex(0);
        applyFilters();
    }

    /**
     * Opens the transaction creation dialog for recording an Income or Expense.
     *
     * @param type the transaction type to record
     */
    private void openAddDialog(TransactionType type) {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        TransactionDialog dialog = new TransactionDialog(frame, null, type);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            applyFilters();
            populateFilterDropdowns();
        }
    }

    /**
     * Handles editing of the currently selected transaction in the table.
     */
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

    /**
     * Prompts for confirmation and deletes the selected transaction record.
     */
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

    /**
     * Exports the active filtered transaction list to the chosen file format (PDF, Excel, or CSV).
     *
     * @param format target format identifier ("PDF", "EXCEL", or "CSV")
     */
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
                        exportService.exportToPdf(currentTransactions, null, target);
                        UiUtils.showSuccess(this, "Exported successfully to PDF:\n" + target.getAbsolutePath());
                    }
                    case "EXCEL" -> {
                        if (!target.getName().toLowerCase().endsWith(".xlsx")) {
                            target = new File(target.getAbsolutePath() + ".xlsx");
                        }
                        exportService.exportToExcel(currentTransactions, null, target);
                        UiUtils.showSuccess(this, "Exported successfully to Excel:\n" + target.getAbsolutePath());
                    }
                    default -> {
                        if (!target.getName().toLowerCase().endsWith(".csv")) {
                            target = new File(target.getAbsolutePath() + ".csv");
                        }
                        exportService.exportToCsv(currentTransactions, target);
                        UiUtils.showSuccess(this, "Exported successfully to CSV:\n" + target.getAbsolutePath());
                    }
                }
            } catch (Exception ex) {
                UiUtils.showError(this, "Export failed: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    /**
     * Opens the modal CSV import dialog for batch importing or updating transactions.
     */
    private void onOpenImportCsv() {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        CsvImportDialog dialog = new CsvImportDialog(frame);
        dialog.setVisible(true);
        if (dialog.isImported()) {
            applyFilters();
            populateFilterDropdowns();
        }
    }

    /**
     * Prompts the user to save a blank CSV template pre-populated with schema headers
     * and demo rows for INSERT, UPDATE, and DELETE operations.
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
                CsvImportService.getInstance().generateCsvTemplate(target);
                UiUtils.showSuccess(this, "CSV Template saved successfully to:\n" + target.getAbsolutePath());
            } catch (Exception ex) {
                UiUtils.showError(this, "Failed to save template: " + ex.getMessage());
            }
        }
    }

    /**
     * Responds to language updates by rebuilding localized visual labels, columns, and filter texts.
     *
     * @param newLanguage the newly selected system language
     */
    @Override
    public void onLanguageChanged(AppLanguage newLanguage) {
        initComponents();
        applyFilters();
    }
}

