package com.navfinance.ui.panels;

import com.navfinance.config.AppConfig;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.util.List;

/**
 * Masters and Catalog Management panel providing unified CRUD administration for
 * core master reference data across NavFinance.
 * <p>
 * This panel organizes 5 foundational catalogs into dedicated sub-tabs:
 * <ul>
 *   <li><b>Persons:</b> Contacts, relatives, and counterparties for tracking Udhar Given/Taken and Loans.</li>
 *   <li><b>Bills:</b> Utility recurring bills (Electricity, Mobile, Broadband, Water, Gas, Maintenance) with typical amounts.</li>
 *   <li><b>EMIs:</b> Structured loan repayment schedules, lending institutions, and monthly commitments.</li>
 *   <li><b>Credit Cards:</b> Credit card identifiers, issuing banks, and card masking digits for expense tracking.</li>
 *   <li><b>Payment Modes:</b> Dynamic transactional channels (Cash, UPI, Paytm, PhonePe, NetBanking, Cards).</li>
 * </ul>
 */
public class MastersPanel extends JPanel {

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
        refreshAll();
    }

    /**
     * Initializes visual components, styling, and sub-tabs.
     */
    private void initComponents() {
        // Header Section
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel lblTitle = new JLabel("Masters & Catalog Management");
        lblTitle.setFont(UiUtils.FONT_TITLE);
        lblTitle.setForeground(new Color(15, 23, 42));
        header.add(lblTitle, BorderLayout.WEST);

        JLabel lblDesc = new JLabel("Manage Persons, Bills, EMIs, Credit Cards, and Payment Modes for auto-suggestions & categorizations.");
        lblDesc.setFont(UiUtils.FONT_SUBTITLE);
        lblDesc.setForeground(UiUtils.COLOR_TEXT_MUTED);
        header.add(lblDesc, BorderLayout.SOUTH);

        add(header, BorderLayout.NORTH);

        // Tabbed Pane for Master Catalogs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UiUtils.FONT_BOLD);

        tabbedPane.addTab("Persons (Udhar / Loans)", NavIcons.person(15, UiUtils.COLOR_PRIMARY), createPersonsPanel());
        tabbedPane.addTab("Bills (Airtel, Electricity Khardoni/Noida)", NavIcons.bill(15, UiUtils.COLOR_PRIMARY), createBillsPanel());
        tabbedPane.addTab("EMI Master", NavIcons.emi(15, UiUtils.COLOR_PRIMARY), createEmisPanel());
        tabbedPane.addTab("Credit Cards", NavIcons.card(15, UiUtils.COLOR_PRIMARY), createCardsPanel());
        tabbedPane.addTab("Payment Modes (Via)", NavIcons.paymentMode(15, UiUtils.COLOR_PRIMARY), createModesPanel());

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

        JButton btnAdd = UiUtils.createPrimaryButton("Add Person", NavIcons.plus(13, Color.WHITE));
        btnAdd.addActionListener(e -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            PersonDialog d = new PersonDialog(frame, null);
            d.setVisible(true);
            if (d.isSaved()) refreshPersons();
        });

        JButton btnEdit = UiUtils.createSecondaryButton("Edit", NavIcons.edit(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnEdit.addActionListener(e -> {
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
        });

        JButton btnDelete = UiUtils.createSecondaryButton("Delete", NavIcons.trash(13, UiUtils.COLOR_EXPENSE));
        btnDelete.addActionListener(e -> {
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
        });

        topBar.add(btnAdd);
        topBar.add(btnEdit);
        topBar.add(btnDelete);
        panel.add(topBar, BorderLayout.NORTH);

        String[] cols = {"ID", "Full Name", "Relation", "Contact", "Notes"};
        modelPersons = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblPersons = new JTable(modelPersons);
        tblPersons.setRowHeight(28);
        tblPersons.setFont(UiUtils.FONT_REGULAR);
        tblPersons.getTableHeader().setFont(UiUtils.FONT_BOLD);

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

        JButton btnAdd = UiUtils.createPrimaryButton("Add Bill", NavIcons.plus(13, Color.WHITE));
        btnAdd.addActionListener(e -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            BillDialog d = new BillDialog(frame, null);
            d.setVisible(true);
            if (d.isSaved()) refreshBills();
        });

        JButton btnEdit = UiUtils.createSecondaryButton("Edit", NavIcons.edit(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnEdit.addActionListener(e -> {
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
        });

        JButton btnDelete = UiUtils.createSecondaryButton("Delete", NavIcons.trash(13, UiUtils.COLOR_EXPENSE));
        btnDelete.addActionListener(e -> {
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
        });

        topBar.add(btnAdd);
        topBar.add(btnEdit);
        topBar.add(btnDelete);
        panel.add(topBar, BorderLayout.NORTH);

        String[] cols = {"ID", "Bill Name", "Provider", "Typical Amount", "Notes"};
        modelBills = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblBills = new JTable(modelBills);
        tblBills.setRowHeight(28);
        tblBills.setFont(UiUtils.FONT_REGULAR);
        tblBills.getTableHeader().setFont(UiUtils.FONT_BOLD);

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

        JButton btnAdd = UiUtils.createPrimaryButton("Add EMI", NavIcons.plus(13, Color.WHITE));
        btnAdd.addActionListener(e -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            EmiDialog d = new EmiDialog(frame, null);
            d.setVisible(true);
            if (d.isSaved()) refreshEmis();
        });

        JButton btnEdit = UiUtils.createSecondaryButton("Edit", NavIcons.edit(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnEdit.addActionListener(e -> {
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
        });

        JButton btnDelete = UiUtils.createSecondaryButton("Delete", NavIcons.trash(13, UiUtils.COLOR_EXPENSE));
        btnDelete.addActionListener(e -> {
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
        });

        topBar.add(btnAdd);
        topBar.add(btnEdit);
        topBar.add(btnDelete);
        panel.add(topBar, BorderLayout.NORTH);

        String[] cols = {"ID", "EMI Name", "Bank / Lender", "Monthly Amount", "Notes"};
        modelEmis = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblEmis = new JTable(modelEmis);
        tblEmis.setRowHeight(28);
        tblEmis.setFont(UiUtils.FONT_REGULAR);
        tblEmis.getTableHeader().setFont(UiUtils.FONT_BOLD);

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

        JButton btnAdd = UiUtils.createPrimaryButton("Add Card", NavIcons.plus(13, Color.WHITE));
        btnAdd.addActionListener(e -> {
            JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
            CreditCardDialog d = new CreditCardDialog(frame, null);
            d.setVisible(true);
            if (d.isSaved()) refreshCards();
        });

        JButton btnEdit = UiUtils.createSecondaryButton("Edit", NavIcons.edit(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnEdit.addActionListener(e -> {
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
        });

        JButton btnDelete = UiUtils.createSecondaryButton("Delete", NavIcons.trash(13, UiUtils.COLOR_EXPENSE));
        btnDelete.addActionListener(e -> {
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
        });

        topBar.add(btnAdd);
        topBar.add(btnEdit);
        topBar.add(btnDelete);
        panel.add(topBar, BorderLayout.NORTH);

        String[] cols = {"ID", "Card Name", "Bank", "Last Digits"};
        modelCards = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblCards = new JTable(modelCards);
        tblCards.setRowHeight(28);
        tblCards.setFont(UiUtils.FONT_REGULAR);
        tblCards.getTableHeader().setFont(UiUtils.FONT_BOLD);

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

        JButton btnAdd = UiUtils.createPrimaryButton("Add Payment Mode", NavIcons.plus(13, Color.WHITE));
        btnAdd.addActionListener(e -> {
            String mode = JOptionPane.showInputDialog(this, "Enter New Payment Mode (Via):", "Add Payment Mode", JOptionPane.PLAIN_MESSAGE);
            if (mode != null && !mode.trim().isEmpty()) {
                financeService.addPaymentMode(mode.trim());
                refreshModes();
            }
        });

        topBar.add(btnAdd);
        panel.add(topBar, BorderLayout.NORTH);

        String[] cols = {"#", "Payment Mode Name (Via)"};
        modelModes = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblModes = new JTable(modelModes);
        tblModes.setRowHeight(28);
        tblModes.setFont(UiUtils.FONT_REGULAR);
        tblModes.getTableHeader().setFont(UiUtils.FONT_BOLD);

        JScrollPane sp = new JScrollPane(tblModes);
        sp.setBorder(UiUtils.createCardBorder());
        panel.add(sp, BorderLayout.CENTER);
        return panel;
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
}
