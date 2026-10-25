package com.navfinance.ui.panels;

import com.navfinance.config.AppConfig;
import com.navfinance.i18n.AppLanguage;
import com.navfinance.i18n.I18n;
import com.navfinance.i18n.LanguageChangeListener;
import com.navfinance.model.Bill;
import com.navfinance.model.CreditCard;
import com.navfinance.model.Emi;
import com.navfinance.model.Person;
import com.navfinance.service.FinanceService;
import com.navfinance.ui.dialogs.BillDialog;
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
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
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

    /** Table displaying registered EMI loan schedules. */
    private JTable tblEmis;

    /** Table model backing the EMI master grid. */
    private DefaultTableModel modelEmis;

    /** Table displaying registered Credit Card instruments. */
    private JTable tblCards;

    /** Table model backing the Credit Card grid. */
    private DefaultTableModel modelCards;

    /** Table displaying customizable payment channels and modes. */
    private JTable tblModes;

    /** Table model backing the Payment Modes grid. */
    private DefaultTableModel modelModes;

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
        panel.add(topBar, BorderLayout.NORTH);

        String[] cols = {
                "ID",
                I18n.get("masters.table.name"),
                I18n.get("masters.table.provider"),
                I18n.get("masters.table.defaultAmount"),
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
        panel.add(topBar, BorderLayout.NORTH);

        String[] cols = {
                "ID",
                I18n.get("masters.table.name"),
                I18n.get("masters.table.bank"),
                I18n.get("masters.table.monthlyAmount"),
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
        panel.add(topBar, BorderLayout.NORTH);

        String[] cols = {
                "ID",
                I18n.get("masters.table.name"),
                I18n.get("masters.table.bank"),
                I18n.get("masters.table.lastDigits")
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
        for (Bill b : list) {
            modelBills.addRow(new Object[]{
                    b.getId(),
                    b.getBillName(),
                    b.getProvider(),
                    b.getDefaultAmount() != null ? AppConfig.formatCurrency(b.getDefaultAmount()) : "-",
                    b.getNotes()
            });
        }
    }

    /**
     * Queries and re-populates the EMIs master table.
     */
    private void refreshEmis() {
        modelEmis.setRowCount(0);
        List<Emi> list = financeService.getAllEmis();
        for (Emi e : list) {
            modelEmis.addRow(new Object[]{
                    e.getId(),
                    e.getEmiName(),
                    e.getBankLender(),
                    e.getMonthlyAmount() != null ? AppConfig.formatCurrency(e.getMonthlyAmount()) : "-",
                    e.getNotes()
            });
        }
    }

    /**
     * Queries and re-populates the Credit Cards master table.
     */
    private void refreshCards() {
        modelCards.setRowCount(0);
        List<CreditCard> list = financeService.getAllCreditCards();
        for (CreditCard c : list) {
            modelCards.addRow(new Object[]{c.getId(), c.getCardName(), c.getBank(), c.getLastDigits()});
        }
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
}

