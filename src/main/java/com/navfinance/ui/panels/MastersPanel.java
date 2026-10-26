package com.navfinance.ui.panels;

import com.navfinance.config.AppConfig;
import com.navfinance.i18n.AppLanguage;
import com.navfinance.i18n.I18n;
import com.navfinance.i18n.LanguageChangeListener;
import com.navfinance.model.Bill;
import com.navfinance.model.Category;
import com.navfinance.model.CreditCard;
import com.navfinance.model.Emi;
import com.navfinance.model.Person;
import com.navfinance.model.SubCategory;
import com.navfinance.service.FinanceService;
import com.navfinance.ui.components.KpiCard;
import com.navfinance.ui.dialogs.BillDialog;
import com.navfinance.ui.dialogs.CategoryDialog;
import com.navfinance.ui.dialogs.CreditCardDialog;
import com.navfinance.ui.dialogs.EmiDialog;
import com.navfinance.ui.dialogs.PersonDialog;
import com.navfinance.ui.util.NavIcons;
import com.navfinance.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Masters and Catalog Management panel providing unified CRUD administration for
 * core master reference data across NavFinance.
 * <p>
 * This panel organizes 5 foundational catalogs into dedicated sub-tabs:
 * <ul>
 *   <li><b>Persons:</b> Contacts, relatives, and counterparties for tracking loans and dues.</li>
 *   <li><b>Bills:</b> Utility recurring bills (Electricity, Mobile, Broadband, Water, Gas, Maintenance) with typical amounts.</li>
 *   <li><b>EMIs:</b> Structured loan repayment schedules, lending institutions, and monthly commitments.</li>
 *   <li><b>Credit Cards:</b> Credit card identifiers, issuing banks, and card masking digits for expense tracking.</li>
 *   <li><b>Payment Modes:</b> Dynamic transactional channels (Cash, UPI, Paytm, PhonePe, NetBanking, Cards).</li>
 * </ul>
 */
public class MastersPanel extends JPanel implements LanguageChangeListener {

    /** Business service managing persistent master data and transactional entities. */
    private final FinanceService financeService = FinanceService.getInstance();

    /** Table displaying Person master entities. */
    private JTable tblPersons;

    /** Table model backing the Person master grid. */
    private DefaultTableModel modelPersons;

    /** Table displaying recurring Bill definitions. */
    private JTable tblBills;

    /** Table model backing the Bill master grid. */
    private DefaultTableModel modelBills;

    // KPI summary indicators for Utility Bills
    private KpiCard kpiBillTotalCommitment;
    private KpiCard kpiBillFixed;
    private KpiCard kpiBillVariable;
    private KpiCard kpiBillTotal;

    /** Table displaying registered EMI loan schedules. */
    private JTable tblEmis;

    /** Table model backing the EMI master grid. */
    private DefaultTableModel modelEmis;

    /** Table displaying registered Credit Card instruments. */
    private JTable tblCards;

    /** Table model backing the Credit Card grid. */
    private DefaultTableModel modelCards;

    // KPI summary indicators for Credit Cards
    private KpiCard kpiCardTotalLimit;
    private KpiCard kpiCardOutstanding;
    private KpiCard kpiCardAvailable;
    private KpiCard kpiCardDue;

    // KPI summary indicators for EMIs / Loans
    private KpiCard kpiEmiSanctioned;
    private KpiCard kpiEmiOutstanding;
    private KpiCard kpiEmiMonthly;
    private KpiCard kpiEmiDue;

    /** Table displaying customizable payment channels and modes. */
    private JTable tblModes;

    /** Table model backing the Payment Modes grid. */
    private DefaultTableModel modelModes;

    /** Table displaying Category master entities. */
    private JTable tblCategories;

    /** Table model backing the Category master grid. */
    private DefaultTableModel modelCategories;

    /** Table displaying associated Sub-Category entities. */
    private JTable tblSubCategories;

    /** Table model backing the Sub-Category grid. */
    private DefaultTableModel modelSubCategories;

    /** Currently selected Category entity in the master-detail view. */
    private Category selectedCategory;

    /** Header label indicating current selected Category for sub-categories. */
    private JLabel lblSubCategoryHeader;

    /**
     * Constructs a new {@code MastersPanel}, setting up header labels, sub-tabs for
     * each catalog type, and loading initial catalog data from the database.
     */
    public MastersPanel() {
        setLayout(new BorderLayout(0, 12));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        initComponents();
        I18n.addListener(this);
        refreshAll();
    }

    /**
     * Initializes visual components, styling, and sub-tabs.
     */
    private void initComponents() {
        // Header Section
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel lblTitle = new JLabel(I18n.get("masters.title"));
        lblTitle.setFont(UiUtils.titleFont());
        lblTitle.setForeground(new Color(15, 23, 42));
        header.add(lblTitle, BorderLayout.WEST);

        JLabel lblDesc = new JLabel(I18n.get("masters.subtitle"));
        lblDesc.setFont(UiUtils.subtitleFont());
        lblDesc.setForeground(UiUtils.COLOR_TEXT_MUTED);
        header.add(lblDesc, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);

        // Tabbed Pane for Master Catalogs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UiUtils.boldFont());

        tabbedPane.addTab(I18n.get("masters.tab.persons"), NavIcons.person(15, UiUtils.COLOR_PRIMARY), createPersonsPanel());
        tabbedPane.addTab(I18n.get("masters.tab.bills"), NavIcons.bill(15, UiUtils.COLOR_PRIMARY), createBillsPanel());
        tabbedPane.addTab(I18n.get("masters.tab.emis"), NavIcons.emi(15, UiUtils.COLOR_PRIMARY), createEmisPanel());
        tabbedPane.addTab(I18n.get("masters.tab.cards"), NavIcons.card(15, UiUtils.COLOR_PRIMARY), createCardsPanel());
        tabbedPane.addTab(I18n.get("masters.tab.paymentModes"), NavIcons.paymentMode(15, UiUtils.COLOR_PRIMARY), createModesPanel());
        tabbedPane.addTab(I18n.get("masters.tab.categories"), NavIcons.category(15, UiUtils.COLOR_PRIMARY), createCategoriesPanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    // -------------------------------------------------------------------------
    // 1. Persons Panel
    // -------------------------------------------------------------------------

    /**
     * Builds the panel for managing the Person master directory.
     *
     * @return fully configured JPanel for Persons management
     */
    private JPanel createPersonsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topBar.setOpaque(false);

        JButton btnAdd = UiUtils.createPrimaryButton(I18n.get("masters.btn.addPerson"), NavIcons.plus(13, Color.WHITE));
        btnAdd.addActionListener(e -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            PersonDialog d = new PersonDialog(frame, null);
            d.setVisible(true);
            if (d.isSaved()) refreshPersons();
        });

        JButton btnEdit = UiUtils.createSecondaryButton(I18n.get("btn.edit"), NavIcons.edit(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnEdit.addActionListener(e -> onEditPerson());

        JButton btnDelete = UiUtils.createSecondaryButton(I18n.get("btn.delete"), NavIcons.trash(13, UiUtils.COLOR_EXPENSE));
        btnDelete.addActionListener(e -> onDeletePerson());

        JButton btnTrack = UiUtils.createPrimaryButton("Track & Graph 📊", NavIcons.dashboard(13, Color.WHITE));
        btnTrack.addActionListener(e -> onTrackPerson());

        topBar.add(btnAdd);
        topBar.add(btnEdit);
        topBar.add(btnDelete);
        topBar.add(btnTrack);
        panel.add(topBar, BorderLayout.NORTH);

        String[] cols = {
                "ID",
                I18n.get("masters.table.name"),
                I18n.get("masters.table.relation"),
                I18n.get("masters.table.contact"),
                I18n.get("masters.table.notes")
        };
        modelPersons = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblPersons = new JTable(modelPersons);
        tblPersons.setRowHeight(28);
        tblPersons.setFont(UiUtils.regularFont());
        tblPersons.getTableHeader().setFont(UiUtils.boldFont());
        setupTableInteractions(tblPersons, this::onEditPerson, this::onTrackPerson, this::onDeletePerson);

        JScrollPane sp = new JScrollPane(tblPersons);
        sp.setBorder(UiUtils.createCardBorder());
        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }

    // -------------------------------------------------------------------------
    // 2. Bills Panel
    // -------------------------------------------------------------------------

    /**
     * Builds the panel for managing recurring Bill types and utility accounts.
     *
     * @return fully configured JPanel for Bills management
     */
    private JPanel createBillsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topContainer = new JPanel(new BorderLayout(0, 8));
        topContainer.setOpaque(false);

        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 10, 0));
        kpiRow.setOpaque(false);
        kpiBillTotalCommitment = new KpiCard(
                I18n.get("masters.kpi.totalBillCommitment"),
                AppConfig.formatCurrency(0.0),
                "Fixed + Variables",
                UiUtils.COLOR_PRIMARY,
                NavIcons.bill(18, UiUtils.COLOR_PRIMARY)
        );
        kpiBillFixed = new KpiCard(
                I18n.get("masters.kpi.fixedBills"),
                AppConfig.formatCurrency(0.0),
                "Fixed commitments",
                UiUtils.COLOR_INCOME,
                NavIcons.check(18, UiUtils.COLOR_INCOME)
        );
        Color colorPurple = new Color(147, 51, 234);
        kpiBillVariable = new KpiCard(
                I18n.get("masters.kpi.variableBills"),
                AppConfig.formatCurrency(0.0),
                "Usage-based (est.)",
                colorPurple,
                NavIcons.calendar(18, colorPurple)
        );
        kpiBillTotal = new KpiCard(
                I18n.get("masters.kpi.totalBills"),
                "0",
                "Active utility accounts",
                UiUtils.COLOR_INCOME,
                NavIcons.check(18, UiUtils.COLOR_INCOME)
        );
        kpiRow.add(kpiBillTotalCommitment);
        kpiRow.add(kpiBillFixed);
        kpiRow.add(kpiBillVariable);
        kpiRow.add(kpiBillTotal);
        topContainer.add(kpiRow, BorderLayout.NORTH);

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topBar.setOpaque(false);

        JButton btnAdd = UiUtils.createPrimaryButton(I18n.get("masters.btn.addBill"), NavIcons.plus(13, Color.WHITE));
        btnAdd.addActionListener(e -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            BillDialog d = new BillDialog(frame, null);
            d.setVisible(true);
            if (d.isSaved()) refreshBills();
        });

        JButton btnEdit = UiUtils.createSecondaryButton(I18n.get("btn.edit"), NavIcons.edit(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnEdit.addActionListener(e -> onEditBill());

        JButton btnDelete = UiUtils.createSecondaryButton(I18n.get("btn.delete"), NavIcons.trash(13, UiUtils.COLOR_EXPENSE));
        btnDelete.addActionListener(e -> onDeleteBill());

        JButton btnTrack = UiUtils.createPrimaryButton("Track & Graph 📊", NavIcons.dashboard(13, Color.WHITE));
        btnTrack.addActionListener(e -> onTrackBill());

        topBar.add(btnAdd);
        topBar.add(btnEdit);
        topBar.add(btnDelete);
        topBar.add(btnTrack);
        topContainer.add(topBar, BorderLayout.SOUTH);
        panel.add(topContainer, BorderLayout.NORTH);

        String[] cols = {
                "ID",
                I18n.get("masters.table.name"),
                I18n.get("masters.table.provider"),
                I18n.get("masters.table.billType"),
                I18n.get("masters.table.defaultAmount"),
                I18n.get("masters.table.dueDay"),
                I18n.get("masters.table.dueAmount"),
                I18n.get("masters.table.notes")
        };
        modelBills = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblBills = new JTable(modelBills);
        tblBills.setRowHeight(28);
        tblBills.setFont(UiUtils.regularFont());
        tblBills.getTableHeader().setFont(UiUtils.boldFont());
        setupTableInteractions(tblBills, this::onEditBill, this::onTrackBill, this::onDeleteBill);

        JScrollPane sp = new JScrollPane(tblBills);
        sp.setBorder(UiUtils.createCardBorder());
        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }

    // -------------------------------------------------------------------------
    // 3. EMIs Panel
    // -------------------------------------------------------------------------

    /**
     * Builds the panel for managing the EMI master catalog of loans and installments.
     *
     * @return fully configured JPanel for EMI catalog management
     */
    private JPanel createEmisPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topContainer = new JPanel(new BorderLayout(0, 8));
        topContainer.setOpaque(false);

        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 10, 0));
        kpiRow.setOpaque(false);
        kpiEmiSanctioned = new KpiCard(I18n.get("masters.kpi.totalSanctioned"), AppConfig.formatCurrency(0.0), "Total loan sanctioned limit", UiUtils.COLOR_PRIMARY, NavIcons.emi(18, UiUtils.COLOR_PRIMARY));
        kpiEmiOutstanding = new KpiCard(I18n.get("masters.kpi.totalOutstanding"), AppConfig.formatCurrency(0.0), "Remaining loan balance", UiUtils.COLOR_EXPENSE, NavIcons.minus(18, UiUtils.COLOR_EXPENSE));
        kpiEmiMonthly = new KpiCard(I18n.get("masters.kpi.monthlyEmi"), AppConfig.formatCurrency(0.0), "Monthly commitment", UiUtils.COLOR_AMBER, NavIcons.dashboard(18, UiUtils.COLOR_AMBER));
        kpiEmiDue = new KpiCard(I18n.get("masters.kpi.totalDue"), AppConfig.formatCurrency(0.0), "Current due amount", UiUtils.COLOR_EXPENSE, NavIcons.bill(18, UiUtils.COLOR_EXPENSE));
        kpiRow.add(kpiEmiSanctioned);
        kpiRow.add(kpiEmiOutstanding);
        kpiRow.add(kpiEmiMonthly);
        kpiRow.add(kpiEmiDue);
        topContainer.add(kpiRow, BorderLayout.NORTH);

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topBar.setOpaque(false);

        JButton btnAdd = UiUtils.createPrimaryButton(I18n.get("masters.btn.addEmi"), NavIcons.plus(13, Color.WHITE));
        btnAdd.addActionListener(e -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            EmiDialog d = new EmiDialog(frame, null);
            d.setVisible(true);
            if (d.isSaved()) refreshEmis();
        });

        JButton btnEdit = UiUtils.createSecondaryButton(I18n.get("btn.edit"), NavIcons.edit(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnEdit.addActionListener(e -> onEditEmi());

        JButton btnDelete = UiUtils.createSecondaryButton(I18n.get("btn.delete"), NavIcons.trash(13, UiUtils.COLOR_EXPENSE));
        btnDelete.addActionListener(e -> onDeleteEmi());

        JButton btnTrack = UiUtils.createPrimaryButton("Track & Graph 📊", NavIcons.dashboard(13, Color.WHITE));
        btnTrack.addActionListener(e -> onTrackEmi());

        topBar.add(btnAdd);
        topBar.add(btnEdit);
        topBar.add(btnDelete);
        topBar.add(btnTrack);
        topContainer.add(topBar, BorderLayout.SOUTH);
        panel.add(topContainer, BorderLayout.NORTH);

        String[] cols = {
                "ID",
                I18n.get("masters.table.name"),
                I18n.get("masters.table.bank"),
                I18n.get("masters.table.loanType"),
                I18n.get("masters.table.totalLimit"),
                I18n.get("masters.table.monthlyAmount"),
                I18n.get("masters.table.outstanding"),
                I18n.get("masters.table.dueAmount"),
                I18n.get("masters.table.dueDay"),
                I18n.get("masters.table.notes")
        };
        modelEmis = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblEmis = new JTable(modelEmis);
        tblEmis.setRowHeight(28);
        tblEmis.setFont(UiUtils.regularFont());
        tblEmis.getTableHeader().setFont(UiUtils.boldFont());
        setupTableInteractions(tblEmis, this::onEditEmi, this::onTrackEmi, this::onDeleteEmi);

        JScrollPane sp = new JScrollPane(tblEmis);
        sp.setBorder(UiUtils.createCardBorder());
        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }

    // -------------------------------------------------------------------------
    // 4. Cards Panel
    // -------------------------------------------------------------------------

    /**
     * Builds the panel for managing registered Credit Card accounts.
     *
     * @return fully configured JPanel for Credit Card catalog management
     */
    private JPanel createCardsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topContainer = new JPanel(new BorderLayout(0, 8));
        topContainer.setOpaque(false);

        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 10, 0));
        kpiRow.setOpaque(false);
        kpiCardTotalLimit = new KpiCard(I18n.get("masters.kpi.totalCreditLimit"), AppConfig.formatCurrency(0.0), "Total combined limit", UiUtils.COLOR_PRIMARY, NavIcons.card(18, UiUtils.COLOR_PRIMARY));
        kpiCardOutstanding = new KpiCard(I18n.get("masters.kpi.totalOutstanding"), AppConfig.formatCurrency(0.0), "Utilized credit balance", UiUtils.COLOR_EXPENSE, NavIcons.minus(18, UiUtils.COLOR_EXPENSE));
        kpiCardAvailable = new KpiCard(I18n.get("masters.kpi.availableCredit"), AppConfig.formatCurrency(0.0), "Remaining spend power", UiUtils.COLOR_INCOME, NavIcons.plus(18, UiUtils.COLOR_INCOME));
        kpiCardDue = new KpiCard(I18n.get("masters.kpi.totalDue"), AppConfig.formatCurrency(0.0), "Statement bills due", UiUtils.COLOR_AMBER, NavIcons.bill(18, UiUtils.COLOR_AMBER));
        kpiRow.add(kpiCardTotalLimit);
        kpiRow.add(kpiCardOutstanding);
        kpiRow.add(kpiCardAvailable);
        kpiRow.add(kpiCardDue);
        topContainer.add(kpiRow, BorderLayout.NORTH);

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topBar.setOpaque(false);

        JButton btnAdd = UiUtils.createPrimaryButton(I18n.get("masters.btn.addCard"), NavIcons.plus(13, Color.WHITE));
        btnAdd.addActionListener(e -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            CreditCardDialog d = new CreditCardDialog(frame, null);
            d.setVisible(true);
            if (d.isSaved()) refreshCards();
        });

        JButton btnEdit = UiUtils.createSecondaryButton(I18n.get("btn.edit"), NavIcons.edit(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnEdit.addActionListener(e -> onEditCard());

        JButton btnDelete = UiUtils.createSecondaryButton(I18n.get("btn.delete"), NavIcons.trash(13, UiUtils.COLOR_EXPENSE));
        btnDelete.addActionListener(e -> onDeleteCard());

        JButton btnTrack = UiUtils.createPrimaryButton("Track & Graph 📊", NavIcons.dashboard(13, Color.WHITE));
        btnTrack.addActionListener(e -> onTrackCard());

        topBar.add(btnAdd);
        topBar.add(btnEdit);
        topBar.add(btnDelete);
        topBar.add(btnTrack);
        topContainer.add(topBar, BorderLayout.SOUTH);
        panel.add(topContainer, BorderLayout.NORTH);

        String[] cols = {
                "ID",
                I18n.get("masters.table.name"),
                I18n.get("masters.table.bank"),
                I18n.get("masters.table.lastDigits"),
                I18n.get("masters.table.totalLimit"),
                I18n.get("masters.table.outstanding"),
                I18n.get("masters.table.availableLimit"),
                I18n.get("masters.table.dueAmount"),
                I18n.get("masters.table.dueDay"),
                "Active CC EMIs"
        };
        modelCards = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblCards = new JTable(modelCards);
        tblCards.setRowHeight(28);
        tblCards.setFont(UiUtils.regularFont());
        tblCards.getTableHeader().setFont(UiUtils.boldFont());
        setupTableInteractions(tblCards, this::onEditCard, this::onTrackCard, this::onDeleteCard);

        JScrollPane sp = new JScrollPane(tblCards);
        sp.setBorder(UiUtils.createCardBorder());
        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }

    // -------------------------------------------------------------------------
    // 5. Modes Panel
    // -------------------------------------------------------------------------

    /**
     * Builds the panel for managing custom payment channels (Via: Cash, UPI, Cards, etc.).
     *
     * @return fully configured JPanel for Payment Modes catalog management
     */
    private JPanel createModesPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        topBar.setOpaque(false);

        JButton btnAdd = UiUtils.createPrimaryButton(I18n.get("masters.btn.addMode"), NavIcons.plus(13, Color.WHITE));
        btnAdd.addActionListener(e -> {
            String mode = JOptionPane.showInputDialog(this, I18n.get("masters.prompt.addMode"), I18n.get("masters.btn.addMode"), JOptionPane.PLAIN_MESSAGE);
            if (mode != null && !mode.trim().isEmpty()) {
                financeService.addPaymentMode(mode.trim());
                refreshModes();
            }
        });

        JButton btnEdit = UiUtils.createSecondaryButton(I18n.get("btn.edit"), NavIcons.edit(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnEdit.addActionListener(e -> onEditMode());

        JButton btnDelete = UiUtils.createSecondaryButton(I18n.get("btn.delete"), NavIcons.trash(13, UiUtils.COLOR_EXPENSE));
        btnDelete.addActionListener(e -> onDeleteMode());

        topBar.add(btnAdd);
        topBar.add(btnEdit);
        topBar.add(btnDelete);
        panel.add(topBar, BorderLayout.NORTH);

        String[] cols = {"#", I18n.get("masters.table.modeName")};
        modelModes = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblModes = new JTable(modelModes);
        tblModes.setRowHeight(28);
        tblModes.setFont(UiUtils.regularFont());
        tblModes.getTableHeader().setFont(UiUtils.boldFont());
        setupTableInteractions(tblModes, this::onEditMode, null, this::onDeleteMode);

        JScrollPane sp = new JScrollPane(tblModes);
        sp.setBorder(UiUtils.createCardBorder());
        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }

    // -------------------------------------------------------------------------
    // 6. Categories & Sub-Categories Panel (Master-Detail)
    // -------------------------------------------------------------------------

    /**
     * Builds the panel for managing Categories and their associated Sub-Categories.
     * Uses a responsive Master-Detail JSplitPane layout.
     *
     * @return fully configured JPanel for Category catalog management
     */
    private JPanel createCategoriesPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // LEFT: Categories Panel
        JPanel leftPanel = new JPanel(new BorderLayout(0, 8));
        leftPanel.setOpaque(false);

        JPanel leftTopBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        leftTopBar.setOpaque(false);

        JButton btnAddCat = UiUtils.createPrimaryButton(I18n.get("masters.btn.addCategory"), NavIcons.plus(13, Color.WHITE));
        btnAddCat.addActionListener(e -> onAddCategory());

        JButton btnEditCat = UiUtils.createSecondaryButton(I18n.get("btn.edit"), NavIcons.edit(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnEditCat.addActionListener(e -> onEditCategory());

        JButton btnDeleteCat = UiUtils.createSecondaryButton(I18n.get("btn.delete"), NavIcons.trash(13, UiUtils.COLOR_EXPENSE));
        btnDeleteCat.addActionListener(e -> onDeleteCategory());

        leftTopBar.add(btnAddCat);
        leftTopBar.add(btnEditCat);
        leftTopBar.add(btnDeleteCat);
        leftPanel.add(leftTopBar, BorderLayout.NORTH);

        String[] catCols = {"ID", I18n.get("masters.table.categoryName"), I18n.get("masters.table.categoryType"), I18n.get("masters.table.subCategoryCount")};
        modelCategories = new DefaultTableModel(catCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblCategories = new JTable(modelCategories);
        tblCategories.setRowHeight(28);
        tblCategories.setFont(UiUtils.regularFont());
        tblCategories.getTableHeader().setFont(UiUtils.boldFont());
        tblCategories.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblCategories.getColumnModel().getColumn(0).setMaxWidth(60);
        tblCategories.getColumnModel().getColumn(0).setPreferredWidth(50);
        tblCategories.getColumnModel().getColumn(2).setPreferredWidth(85);
        tblCategories.getColumnModel().getColumn(3).setPreferredWidth(95);

        // Type color badge renderer
        tblCategories.getColumnModel().getColumn(2).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(CENTER);
                l.setFont(UiUtils.boldFont());
                if (!isSelected) {
                    if ("EXPENSE".equals(value)) {
                        l.setForeground(UiUtils.COLOR_EXPENSE);
                    } else if ("INCOME".equals(value)) {
                        l.setForeground(UiUtils.COLOR_INCOME);
                    } else {
                        l.setForeground(UiUtils.COLOR_PRIMARY);
                    }
                }
                return l;
            }
        });

        tblCategories.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tblCategories.getSelectedRow();
                if (row >= 0 && row < tblCategories.getRowCount()) {
                    Long id = (Long) tblCategories.getValueAt(row, 0);
                    selectedCategory = financeService.getAllCategories().stream()
                            .filter(c -> c.getId().equals(id))
                            .findFirst().orElse(null);
                } else {
                    selectedCategory = null;
                }
                refreshSubCategories();
            }
        });

        setupTableInteractions(tblCategories, this::onEditCategory, null, this::onDeleteCategory);

        JScrollPane spCat = new JScrollPane(tblCategories);
        spCat.setBorder(UiUtils.createCardBorder());
        leftPanel.add(spCat, BorderLayout.CENTER);

        // RIGHT: Sub-Categories Panel
        JPanel rightPanel = new JPanel(new BorderLayout(0, 8));
        rightPanel.setOpaque(false);

        JPanel rightTopBar = new JPanel(new BorderLayout(8, 0));
        rightTopBar.setOpaque(false);

        lblSubCategoryHeader = new JLabel("Associated Sub-Categories");
        lblSubCategoryHeader.setFont(UiUtils.boldFont());
        lblSubCategoryHeader.setForeground(UiUtils.COLOR_TEXT_PRIMARY);
        rightTopBar.add(lblSubCategoryHeader, BorderLayout.WEST);

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightBtns.setOpaque(false);

        JButton btnAddSub = UiUtils.createPrimaryButton(I18n.get("masters.btn.addSubCategory"), NavIcons.plus(13, Color.WHITE));
        btnAddSub.addActionListener(e -> onAddSubCategory());

        JButton btnEditSub = UiUtils.createSecondaryButton(I18n.get("btn.edit"), NavIcons.edit(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnEditSub.addActionListener(e -> onEditSubCategory());

        JButton btnDeleteSub = UiUtils.createSecondaryButton(I18n.get("btn.delete"), NavIcons.trash(13, UiUtils.COLOR_EXPENSE));
        btnDeleteSub.addActionListener(e -> onDeleteSubCategory());

        rightBtns.add(btnAddSub);
        rightBtns.add(btnEditSub);
        rightBtns.add(btnDeleteSub);
        rightTopBar.add(rightBtns, BorderLayout.EAST);
        rightPanel.add(rightTopBar, BorderLayout.NORTH);

        String[] subCols = {"ID", I18n.get("masters.table.subCategoryName")};
        modelSubCategories = new DefaultTableModel(subCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblSubCategories = new JTable(modelSubCategories);
        tblSubCategories.setRowHeight(28);
        tblSubCategories.setFont(UiUtils.regularFont());
        tblSubCategories.getTableHeader().setFont(UiUtils.boldFont());
        tblSubCategories.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblSubCategories.getColumnModel().getColumn(0).setMaxWidth(60);
        tblSubCategories.getColumnModel().getColumn(0).setPreferredWidth(50);

        setupTableInteractions(tblSubCategories, this::onEditSubCategory, null, this::onDeleteSubCategory);

        JScrollPane spSub = new JScrollPane(tblSubCategories);
        spSub.setBorder(UiUtils.createCardBorder());
        rightPanel.add(spSub, BorderLayout.CENTER);

        // Split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setResizeWeight(0.55);
        splitPane.setBorder(null);
        splitPane.setOpaque(false);
        splitPane.setContinuousLayout(true);

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    // -------------------------------------------------------------------------
    // Master Actions & Table Interactions
    // -------------------------------------------------------------------------

    private void onEditPerson() {
        int row = tblPersons.getSelectedRow();
        if (row < 0) {
            UiUtils.showError(this, "Select a person to edit.");
            return;
        }
        Long id = (Long) tblPersons.getValueAt(row, 0);
        Person p = financeService.getAllPersons().stream().filter(x -> x.getId().equals(id)).findFirst().orElse(null);
        if (p != null) {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            PersonDialog d = new PersonDialog(frame, p);
            d.setVisible(true);
            if (d.isSaved()) refreshPersons();
        }
    }

    private void onDeletePerson() {
        int row = tblPersons.getSelectedRow();
        if (row < 0) {
            UiUtils.showError(this, "Select a person to delete.");
            return;
        }
        Long id = (Long) tblPersons.getValueAt(row, 0);
        if (UiUtils.confirm(this, "Delete person #" + id + "?")) {
            financeService.deletePerson(id);
            refreshPersons();
        }
    }

    private void onTrackPerson() {
        int row = tblPersons.getSelectedRow();
        if (row < 0) {
            UiUtils.showError(this, "Select a person to track.");
            return;
        }
        Long id = (Long) tblPersons.getValueAt(row, 0);
        com.navfinance.ui.MainFrame.getInstance().switchToEntityTracker("PERSON", id, null, "MONTHS");
    }

    private void onEditBill() {
        int row = tblBills.getSelectedRow();
        if (row < 0) {
            UiUtils.showError(this, "Select a bill to edit.");
            return;
        }
        Long id = (Long) tblBills.getValueAt(row, 0);
        Bill b = financeService.getAllBills().stream().filter(x -> x.getId().equals(id)).findFirst().orElse(null);
        if (b != null) {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            BillDialog d = new BillDialog(frame, b);
            d.setVisible(true);
            if (d.isSaved()) refreshBills();
        }
    }

    private void onDeleteBill() {
        int row = tblBills.getSelectedRow();
        if (row < 0) {
            UiUtils.showError(this, "Select a bill to delete.");
            return;
        }
        Long id = (Long) tblBills.getValueAt(row, 0);
        if (UiUtils.confirm(this, "Delete bill #" + id + "?")) {
            financeService.deleteBill(id);
            refreshBills();
        }
    }

    private void onTrackBill() {
        int row = tblBills.getSelectedRow();
        if (row < 0) {
            UiUtils.showError(this, "Select a bill to track.");
            return;
        }
        Long id = (Long) tblBills.getValueAt(row, 0);
        com.navfinance.ui.MainFrame.getInstance().switchToEntityTracker("BILL", id, null, "MONTHS");
    }

    private void onEditEmi() {
        int row = tblEmis.getSelectedRow();
        if (row < 0) {
            UiUtils.showError(this, "Select an EMI to edit.");
            return;
        }
        Long id = (Long) tblEmis.getValueAt(row, 0);
        Emi emi = financeService.getAllEmis().stream().filter(x -> x.getId().equals(id)).findFirst().orElse(null);
        if (emi != null) {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            EmiDialog d = new EmiDialog(frame, emi);
            d.setVisible(true);
            if (d.isSaved()) refreshEmis();
        }
    }

    private void onDeleteEmi() {
        int row = tblEmis.getSelectedRow();
        if (row < 0) {
            UiUtils.showError(this, "Select an EMI to delete.");
            return;
        }
        Long id = (Long) tblEmis.getValueAt(row, 0);
        if (UiUtils.confirm(this, "Delete EMI #" + id + "?")) {
            financeService.deleteEmi(id);
            refreshEmis();
        }
    }

    private void onTrackEmi() {
        int row = tblEmis.getSelectedRow();
        if (row < 0) {
            UiUtils.showError(this, "Select an EMI to track.");
            return;
        }
        Long id = (Long) tblEmis.getValueAt(row, 0);
        com.navfinance.ui.MainFrame.getInstance().switchToEntityTracker("EMI", id, null, "MONTHS");
    }

    private void onEditCard() {
        int row = tblCards.getSelectedRow();
        if (row < 0) {
            UiUtils.showError(this, "Select a card to edit.");
            return;
        }
        Long id = (Long) tblCards.getValueAt(row, 0);
        CreditCard card = financeService.getAllCreditCards().stream().filter(x -> x.getId().equals(id)).findFirst().orElse(null);
        if (card != null) {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            CreditCardDialog d = new CreditCardDialog(frame, card);
            d.setVisible(true);
            if (d.isSaved()) refreshCards();
        }
    }

    private void onDeleteCard() {
        int row = tblCards.getSelectedRow();
        if (row < 0) {
            UiUtils.showError(this, "Select a card to delete.");
            return;
        }
        Long id = (Long) tblCards.getValueAt(row, 0);
        if (UiUtils.confirm(this, "Delete Card #" + id + "?")) {
            financeService.deleteCreditCard(id);
            refreshCards();
        }
    }

    private void onTrackCard() {
        int row = tblCards.getSelectedRow();
        if (row < 0) {
            UiUtils.showError(this, "Select a card to track.");
            return;
        }
        Long id = (Long) tblCards.getValueAt(row, 0);
        com.navfinance.ui.MainFrame.getInstance().switchToEntityTracker("CREDIT_CARD", id, null, "MONTHS");
    }

    private void onEditMode() {
        int row = tblModes.getSelectedRow();
        if (row < 0) {
            UiUtils.showError(this, "Select a payment mode to edit.");
            return;
        }
        String oldMode = (String) tblModes.getValueAt(row, 1);
        String prompt = I18n.get("masters.prompt.editMode");
        String title = I18n.get("btn.edit");
        String newMode = (String) JOptionPane.showInputDialog(
                this,
                prompt,
                title,
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                oldMode
        );
        if (newMode != null && !newMode.trim().isEmpty() && !newMode.trim().equalsIgnoreCase(oldMode.trim())) {
            boolean success = financeService.updatePaymentMode(oldMode, newMode.trim());
            if (success) {
                refreshModes();
            } else {
                UiUtils.showError(this, "Failed to update payment mode. A mode with that name may already exist.");
            }
        }
    }

    private void onDeleteMode() {
        int row = tblModes.getSelectedRow();
        if (row < 0) {
            UiUtils.showError(this, "Select a payment mode to delete.");
            return;
        }
        String mode = (String) tblModes.getValueAt(row, 1);
        String msg = I18n.get("masters.confirm.deleteMode", mode);
        if (UiUtils.confirm(this, msg)) {
            boolean success = financeService.deletePaymentMode(mode);
            if (success) {
                refreshModes();
            } else {
                UiUtils.showError(this, "Failed to delete payment mode.");
            }
        }
    }

    private void onAddCategory() {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        CategoryDialog dialog = new CategoryDialog(frame, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshCategories();
        }
    }

    private void onEditCategory() {
        int row = tblCategories.getSelectedRow();
        if (row < 0) {
            UiUtils.showError(this, "Select a category to edit.");
            return;
        }
        Long id = (Long) tblCategories.getValueAt(row, 0);
        Category cat = financeService.getAllCategories().stream()
                .filter(c -> c.getId().equals(id))
                .findFirst().orElse(null);
        if (cat != null) {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            CategoryDialog dialog = new CategoryDialog(frame, cat);
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                refreshCategories();
            }
        }
    }

    private void onDeleteCategory() {
        int row = tblCategories.getSelectedRow();
        if (row < 0) {
            UiUtils.showError(this, "Select a category to delete.");
            return;
        }
        Long id = (Long) tblCategories.getValueAt(row, 0);
        String name = (String) tblCategories.getValueAt(row, 1);
        int txCount = financeService.countTransactionsForCategory(name);
        if (txCount > 0) {
            UiUtils.showError(this, String.format(
                    "Cannot delete Category '%s' because it is referenced by %d transaction(s).\nPlease reassign those transactions before deleting.",
                    name, txCount));
            return;
        }

        String msg = I18n.get("masters.confirm.deleteCategory", name);
        if (UiUtils.confirm(this, msg)) {
            boolean success = financeService.deleteCategory(id);
            if (success) {
                selectedCategory = null;
                refreshCategories();
            } else {
                UiUtils.showError(this, "Failed to delete category.");
            }
        }
    }

    private void onAddSubCategory() {
        if (selectedCategory == null) {
            UiUtils.showError(this, "Please select a Category from the left table first.");
            return;
        }
        String prompt = I18n.get("masters.prompt.addSubCategory");
        String title = I18n.get("masters.btn.addSubCategory");
        String subName = (String) JOptionPane.showInputDialog(
                this, prompt, title, JOptionPane.PLAIN_MESSAGE, null, null, "");
        if (subName != null && !subName.trim().isEmpty()) {
            SubCategory sub = financeService.addSubCategory(selectedCategory.getId(), subName.trim());
            if (sub != null) {
                refreshCategories();
            } else {
                UiUtils.showError(this, "Failed to add sub-category. A sub-category with that name may already exist.");
            }
        }
    }

    private void onEditSubCategory() {
        if (selectedCategory == null || tblSubCategories.getSelectedRow() < 0) {
            UiUtils.showError(this, "Select a sub-category to edit.");
            return;
        }
        int row = tblSubCategories.getSelectedRow();
        Long subId = (Long) tblSubCategories.getValueAt(row, 0);
        String oldName = (String) tblSubCategories.getValueAt(row, 1);
        String prompt = I18n.get("masters.prompt.editSubCategory");
        String title = I18n.get("btn.edit");
        String newName = (String) JOptionPane.showInputDialog(
                this, prompt, title, JOptionPane.PLAIN_MESSAGE, null, null, oldName);
        if (newName != null && !newName.trim().isEmpty() && !newName.trim().equalsIgnoreCase(oldName.trim())) {
            boolean success = financeService.updateSubCategory(subId, newName.trim(), oldName, selectedCategory.getName());
            if (success) {
                refreshCategories();
            } else {
                UiUtils.showError(this, "Failed to update sub-category.");
            }
        }
    }

    private void onDeleteSubCategory() {
        if (selectedCategory == null || tblSubCategories.getSelectedRow() < 0) {
            UiUtils.showError(this, "Select a sub-category to delete.");
            return;
        }
        int row = tblSubCategories.getSelectedRow();
        Long subId = (Long) tblSubCategories.getValueAt(row, 0);
        String subName = (String) tblSubCategories.getValueAt(row, 1);
        String msg = I18n.get("masters.confirm.deleteSubCategory", subName);
        if (UiUtils.confirm(this, msg)) {
            boolean success = financeService.deleteSubCategory(subId, subName);
            if (success) {
                refreshCategories();
            } else {
                UiUtils.showError(this, "Failed to delete sub-category.");
            }
        }
    }

    /**
     * Attaches double-click editing and right-click context menu (Edit, Track, Delete) to a master table.
     * Ensures right-clicking an unselected row selects that row before displaying the context menu,
     * and double-clicking triggers edit directly on the clicked row.
     *
     * @param table Target JTable
     * @param onEdit Action to invoke for editing the selected entry
     * @param onTrack Action to invoke for tracking the selected entry (or null if not trackable)
     * @param onDelete Action to invoke for deleting the selected entry
     */
    private void setupTableInteractions(JTable table, Runnable onEdit, Runnable onTrack, Runnable onDelete) {
        JPopupMenu popup = new JPopupMenu();

        JMenuItem itmEdit = new JMenuItem(I18n.get("btn.edit"), NavIcons.edit(12, UiUtils.COLOR_PRIMARY));
        itmEdit.setFont(UiUtils.regularFont());
        itmEdit.addActionListener(e -> {
            if (table.getSelectedRow() >= 0 && onEdit != null) {
                onEdit.run();
            }
        });
        popup.add(itmEdit);

        JMenuItem itmTrack = null;
        if (onTrack != null) {
            itmTrack = new JMenuItem("Track & Graph 📊", NavIcons.dashboard(12, UiUtils.COLOR_PRIMARY));
            itmTrack.setFont(UiUtils.regularFont());
            itmTrack.addActionListener(e -> {
                if (table.getSelectedRow() >= 0) {
                    onTrack.run();
                }
            });
            popup.add(itmTrack);
        }

        JMenuItem itmDelete = null;
        if (onDelete != null) {
            popup.addSeparator();
            itmDelete = new JMenuItem(I18n.get("btn.delete"), NavIcons.trash(12, UiUtils.COLOR_EXPENSE));
            itmDelete.setFont(UiUtils.regularFont());
            itmDelete.addActionListener(e -> {
                if (table.getSelectedRow() >= 0) {
                    onDelete.run();
                }
            });
            popup.add(itmDelete);
        }

        final JMenuItem finalItmTrack = itmTrack;
        final JMenuItem finalItmDelete = itmDelete;

        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                Point p = table.getMousePosition();
                if (p != null) {
                    int row = table.rowAtPoint(p);
                    if (row >= 0 && !table.isRowSelected(row)) {
                        table.setRowSelectionInterval(row, row);
                    }
                }
                boolean hasSelection = table.getSelectedRow() >= 0;
                itmEdit.setEnabled(hasSelection);
                if (finalItmTrack != null) finalItmTrack.setEnabled(hasSelection);
                if (finalItmDelete != null) finalItmDelete.setEnabled(hasSelection);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {}
        });

        table.setComponentPopupMenu(popup);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleRowSelection(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleRowSelection(e);
            }

            private void handleRowSelection(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0 && !table.isRowSelected(row)) {
                        table.setRowSelectionInterval(row, row);
                    }
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        table.setRowSelectionInterval(row, row);
                        if (onEdit != null) {
                            onEdit.run();
                        }
                    }
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    // Refresh Logic
    // -------------------------------------------------------------------------

    /**
     * Refreshes all 5 master catalog tables concurrently with updated database records.
     * Declared final to safeguard constructor invocation.
     */
    public final void refreshAll() {
        refreshPersons();
        refreshBills();
        refreshEmis();
        refreshCards();
        refreshModes();
        refreshCategories();
    }

    /**
     * Queries and re-populates the Persons master table.
     */
    private void refreshPersons() {
        modelPersons.setRowCount(0);
        List<Person> list = financeService.getAllPersons();
        for (Person p : list) {
            modelPersons.addRow(new Object[]{p.getId(), p.getName(), p.getRelation(), p.getContact(), p.getNotes()});
        }
    }

    /**
     * Queries and re-populates the Bills master table.
     */
    private void refreshBills() {
        modelBills.setRowCount(0);
        List<Bill> list = financeService.getAllBills();
        double totalFixed = 0.0;
        double totalVariable = 0.0;
        double totalDue = 0.0;
        int countFixed = 0;
        int countVariable = 0;

        for (Bill b : list) {
            double amt = b.getDefaultAmount() != null ? b.getDefaultAmount() : 0.0;
            double due = b.getDueAmount() != null ? b.getDueAmount() : 0.0;
            totalDue += due;

            boolean isVar = b.isVariable();
            if (isVar) {
                totalVariable += amt;
                countVariable++;
            } else {
                totalFixed += amt;
                countFixed++;
            }

            String typeStr = isVar ? "Variable" : "Fixed";
            String dueDayStr = b.getDueDay() != null && b.getDueDay() > 0 ? b.getDueDay() + "th" : "-";

            modelBills.addRow(new Object[]{
                    b.getId(),
                    b.getBillName(),
                    b.getProvider() != null ? b.getProvider() : "-",
                    typeStr,
                    amt > 0 ? AppConfig.formatCurrency(amt) : "-",
                    dueDayStr,
                    due > 0 ? AppConfig.formatCurrency(due) : "-",
                    b.getNotes() != null ? b.getNotes() : "-"
            });
        }

        double totalCommitments = totalFixed + totalVariable;
        if (kpiBillTotalCommitment != null) {
            kpiBillTotalCommitment.updateValue(
                    AppConfig.formatCurrency(totalCommitments),
                    "Fixed: " + AppConfig.formatCurrency(totalFixed) + " + Var: " + AppConfig.formatCurrency(totalVariable)
            );
        }
        if (kpiBillFixed != null) {
            kpiBillFixed.updateValue(AppConfig.formatCurrency(totalFixed), countFixed + " fixed utility bills");
        }
        if (kpiBillVariable != null) {
            kpiBillVariable.updateValue(AppConfig.formatCurrency(totalVariable), countVariable + " usage-based bills (est.)");
        }
        if (kpiBillTotal != null) {
            kpiBillTotal.updateValue(String.valueOf(list.size()), "Total Due: " + AppConfig.formatCurrency(totalDue));
        }
    }

    /**
     * Queries and re-populates the EMIs master table.
     */
    private void refreshEmis() {
        modelEmis.setRowCount(0);
        List<Emi> list = financeService.getAllEmis();
        double totalSanctioned = 0.0;
        double totalOut = 0.0;
        double totalMonthly = 0.0;
        double totalDue = 0.0;

        for (Emi e : list) {
            double sanc = e.getTotalAmount() != null ? e.getTotalAmount() : 0.0;
            double out = e.getOutstandingAmount() != null ? e.getOutstandingAmount() : 0.0;
            double monthly = e.getMonthlyAmount() != null ? e.getMonthlyAmount() : 0.0;
            double due = e.getDueAmount() != null ? e.getDueAmount() : 0.0;
            totalSanctioned += sanc;
            totalOut += out;
            totalMonthly += monthly;
            totalDue += due;

            String typeStr = e.isCreditCardEmi() ? I18n.get("masters.table.ccEmi") : I18n.get("masters.table.bankLoan");
            String dueDayStr = e.getDueDay() != null && e.getDueDay() > 0 ? e.getDueDay() + "th" : "-";

            modelEmis.addRow(new Object[]{
                    e.getId(),
                    e.getEmiName(),
                    e.getBankLender() != null ? e.getBankLender() : "-",
                    typeStr,
                    sanc > 0 ? AppConfig.formatCurrency(sanc) : "-",
                    monthly > 0 ? AppConfig.formatCurrency(monthly) : "-",
                    out > 0 ? AppConfig.formatCurrency(out) : "-",
                    due > 0 ? AppConfig.formatCurrency(due) : "-",
                    dueDayStr,
                    e.getNotes() != null ? e.getNotes() : "-"
            });
        }

        if (kpiEmiSanctioned != null) kpiEmiSanctioned.updateValue(AppConfig.formatCurrency(totalSanctioned), list.size() + " active loans/EMIs");
        if (kpiEmiOutstanding != null) kpiEmiOutstanding.updateValue(AppConfig.formatCurrency(totalOut), "Remaining principal balance");
        if (kpiEmiMonthly != null) kpiEmiMonthly.updateValue(AppConfig.formatCurrency(totalMonthly), "Monthly commitment");
        if (kpiEmiDue != null) kpiEmiDue.updateValue(AppConfig.formatCurrency(totalDue), "Current overdue / due amount");
    }

    /**
     * Queries and re-populates the Credit Cards master table.
     */
    private void refreshCards() {
        modelCards.setRowCount(0);
        List<CreditCard> list = financeService.getAllCreditCards();
        double totalLimit = 0.0;
        double totalOut = 0.0;
        double totalAvail = 0.0;
        double totalDue = 0.0;

        for (CreditCard c : list) {
            double lim = c.getTotalLimit() != null ? c.getTotalLimit() : 0.0;
            double out = c.getOutstandingAmount() != null ? c.getOutstandingAmount() : 0.0;
            double due = c.getDueAmount() != null ? c.getDueAmount() : 0.0;
            double avail = c.getAvailableLimit() != null ? c.getAvailableLimit() : 0.0;
            totalLimit += lim;
            totalOut += out;
            totalAvail += avail;
            totalDue += due;

            List<Emi> cardEmis = financeService.getEmisByCreditCard(c.getId());
            double emiMonthly = cardEmis.stream().mapToDouble(e -> e.getMonthlyAmount() != null ? e.getMonthlyAmount() : 0.0).sum();
            String ccEmiInfo = cardEmis.isEmpty() ? "-" : cardEmis.size() + " (" + AppConfig.formatCurrency(emiMonthly) + "/mo)";
            String dueDayStr = c.getDueDay() != null && c.getDueDay() > 0 ? c.getDueDay() + "th" : "-";
            String last4Str = c.getLastDigits() != null && !c.getLastDigits().isEmpty() ? ".." + c.getLastDigits() : "-";

            modelCards.addRow(new Object[]{
                    c.getId(),
                    c.getCardName(),
                    c.getBank() != null ? c.getBank() : "-",
                    last4Str,
                    lim > 0 ? AppConfig.formatCurrency(lim) : "-",
                    out > 0 ? AppConfig.formatCurrency(out) : "-",
                    AppConfig.formatCurrency(avail),
                    due > 0 ? AppConfig.formatCurrency(due) : "-",
                    dueDayStr,
                    ccEmiInfo
            });
        }

        if (kpiCardTotalLimit != null) kpiCardTotalLimit.updateValue(AppConfig.formatCurrency(totalLimit), list.size() + " credit cards");
        if (kpiCardOutstanding != null) {
            double util = totalLimit > 0 ? (totalOut / totalLimit) * 100.0 : 0.0;
            kpiCardOutstanding.updateValue(AppConfig.formatCurrency(totalOut), String.format("Utilization: %.1f%%", util));
        }
        if (kpiCardAvailable != null) kpiCardAvailable.updateValue(AppConfig.formatCurrency(totalAvail), "Available to spend");
        if (kpiCardDue != null) kpiCardDue.updateValue(AppConfig.formatCurrency(totalDue), "Statements due to pay");
    }

    /**
     * Queries and re-populates the Payment Modes master table.
     */
    private void refreshModes() {
        modelModes.setRowCount(0);
        List<String> list = financeService.getAllPaymentModes();
        int seq = 1;
        for (String m : list) {
            modelModes.addRow(new Object[]{seq++, m});
        }
    }

    /**
     * Queries and re-populates the Categories master table.
     */
    private void refreshCategories() {
        Long prevSelectedId = selectedCategory != null ? selectedCategory.getId() : null;
        modelCategories.setRowCount(0);
        List<Category> list = financeService.getAllCategories();
        int rowToSelect = -1;

        for (int i = 0; i < list.size(); i++) {
            Category c = list.get(i);
            int subCount = c.getSubCategories() != null ? c.getSubCategories().size() : 0;
            String typeStr = c.getType() != null ? c.getType().name() : "ALL";
            modelCategories.addRow(new Object[]{c.getId(), c.getName(), typeStr, subCount});
            if (prevSelectedId != null && prevSelectedId.equals(c.getId())) {
                rowToSelect = i;
                selectedCategory = c;
            }
        }

        if (rowToSelect >= 0 && rowToSelect < tblCategories.getRowCount()) {
            tblCategories.setRowSelectionInterval(rowToSelect, rowToSelect);
        } else if (!list.isEmpty()) {
            tblCategories.setRowSelectionInterval(0, 0);
            selectedCategory = list.get(0);
        } else {
            selectedCategory = null;
        }

        refreshSubCategories();
    }

    /**
     * Populates the Sub-Categories table for the currently selected category.
     */
    private void refreshSubCategories() {
        modelSubCategories.setRowCount(0);
        if (selectedCategory == null) {
            if (lblSubCategoryHeader != null) {
                lblSubCategoryHeader.setText("Associated Sub-Categories (Select a category)");
            }
            return;
        }

        if (lblSubCategoryHeader != null) {
            String typeStr = selectedCategory.getType() != null ? " [" + selectedCategory.getType().name() + "]" : "";
            lblSubCategoryHeader.setText("Associated Sub-Categories for: " + selectedCategory.getName() + typeStr);
        }

        Category latest = financeService.getAllCategories().stream()
                .filter(c -> c.getId().equals(selectedCategory.getId()))
                .findFirst().orElse(selectedCategory);
        selectedCategory = latest;

        if (latest.getSubCategories() != null) {
            for (SubCategory s : latest.getSubCategories()) {
                modelSubCategories.addRow(new Object[]{s.getId(), s.getName()});
            }
        }
    }

    @Override
    public void onLanguageChanged(AppLanguage newLanguage) {
        removeAll();
        initComponents();
        revalidate();
        repaint();
        refreshAll();
    }

    // Public getters for testing and component inspection
    public JTable getTblPersons() { return tblPersons; }
    public JTable getTblBills() { return tblBills; }
    public JTable getTblEmis() { return tblEmis; }
    public JTable getTblCards() { return tblCards; }
    public JTable getTblModes() { return tblModes; }
    public JTable getTblCategories() { return tblCategories; }
    public JTable getTblSubCategories() { return tblSubCategories; }
    public KpiCard getKpiBillTotalCommitment() { return kpiBillTotalCommitment; }
    public KpiCard getKpiBillFixed() { return kpiBillFixed; }
    public KpiCard getKpiBillVariable() { return kpiBillVariable; }
    public KpiCard getKpiBillTotal() { return kpiBillTotal; }
}

