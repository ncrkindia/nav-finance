package com.navfinance.ui.dialogs;

import com.navfinance.config.AppConfig;
import com.navfinance.i18n.I18n;
import com.navfinance.model.Bill;
import com.navfinance.model.CreditCard;
import com.navfinance.model.Emi;
import com.navfinance.model.Person;
import com.navfinance.model.Transaction;
import com.navfinance.model.TransactionLink;
import com.navfinance.model.TransactionType;
import com.navfinance.service.FinanceService;
import com.navfinance.ui.components.DatePickerField;
import com.navfinance.ui.util.NavIcons;
import com.navfinance.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Primary modal dialog for adding and editing financial income and expense ledger entries.
 * <p>
 * Provides interactive category-to-subcategory cascading, foreign key resolution for
 * persons, bills, credit cards, and EMIs, hybrid date picking (calendar + manual ISO entry),
 * editable payment channels with automatic memory, and planned/expected flags.
 * </p>
 */
public class TransactionDialog extends JDialog {

    /** Service coordinator for finance ledger mutations and master entities. */
    private final FinanceService financeService = FinanceService.getInstance();

    /** The existing transaction being modified, or {@code null} if recording a new transaction. */
    private final Transaction existingTransaction;

    /** Flag indicating whether the transaction entry was successfully saved. */
    private boolean saved = false;

    // UI Fields
    /** Radio button selecting income transaction flow. */
    private JRadioButton rbIncome;

    /** Radio button selecting expense transaction flow. */
    private JRadioButton rbExpense;

    /** Composite date picker supporting calendar clicks and manual text entry. */
    private DatePickerField txtDate;

    /** Checkbox designating planned/expected unrealized amounts. */
    private JCheckBox chkExpected;

    /** Text input field for the financial transaction amount. */
    private JTextField txtAmount;

    /** Editable dropdown for selecting or typing payment channels (UPI, Cash, etc.). */
    private JComboBox<String> cmbPaymentVia;

    /** Dropdown for selecting primary financial category. */
    private JComboBox<String> cmbCategory;

    /** Editable dropdown for selecting or typing sub-category specifics. */
    private JComboBox<String> cmbSubCategory;

    /** Dropdown for attributing loan transactions to a specific contact. */
    private JComboBox<Person> cmbPerson;

    /** Panel and components for linking Give/Return counterpart transactions. */
    private JPanel pnlLinking;
    private JLabel lblLinkingTitle;
    private JLabel lblLinkingSummary;
    private JButton btnManageLinks;
    private List<TransactionLink> pendingLinks = new ArrayList<>();

    /** Text area for transaction memo or notes. */
    private JTextArea txtNotes;

    /** Dynamic label adjusting date title between actual and expected/planned. */
    private JLabel lblDateTitle;

    /** Label for the sub-category selector. */
    private JLabel lblSubCategoryTitle;

    /** Label for the optional associated person selector. */
    private JLabel lblPersonTitle;

    /**
     * Constructs a transaction entry dialog.
     *
     * @param parent owner window frame
     * @param transactionToEdit transaction entity to edit, or {@code null} for a new entry
     * @param defaultType initial transaction type to preselect if creating a new entry
     */
    public TransactionDialog(JFrame parent, Transaction transactionToEdit, TransactionType defaultType) {
        super(parent, transactionToEdit == null ?
                (defaultType == TransactionType.INCOME ? I18n.get("dialog.income.addTitle") : I18n.get("dialog.expense.addTitle")) :
                (transactionToEdit.getType() == TransactionType.INCOME ? I18n.get("dialog.income.editTitle") : I18n.get("dialog.expense.editTitle")) + " #" + transactionToEdit.getId(), true);
        this.existingTransaction = transactionToEdit;

        initComponents(defaultType != null ? defaultType : TransactionType.EXPENSE);
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    /**
     * Initializes visual form components, GridBag constraints, radio buttons, and action buttons.
     *
     * @param defaultType initial transaction type
     */
    private void initComponents(TransactionType defaultType) {
        setLayout(new BorderLayout());
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // 1. Type selection (Income vs Expense)
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblType = new JLabel(I18n.get("transactions.table.type") + ":");
        lblType.setFont(UiUtils.regularFont());
        mainPanel.add(lblType, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        rbIncome = new JRadioButton(I18n.get("transactions.filter.income"));
        rbExpense = new JRadioButton(I18n.get("transactions.filter.expense"));
        rbIncome.setFont(UiUtils.boldFont());
        rbIncome.setForeground(UiUtils.COLOR_INCOME);
        rbExpense.setFont(UiUtils.boldFont());
        rbExpense.setForeground(UiUtils.COLOR_EXPENSE);

        ButtonGroup bg = new ButtonGroup();
        bg.add(rbIncome);
        bg.add(rbExpense);
        typePanel.add(rbIncome);
        typePanel.add(rbExpense);
        mainPanel.add(typePanel, gbc);

        // 2. Date & Expected checkbox (DatePicker + Manual entry)
        lblDateTitle = new JLabel(I18n.get("transactions.table.date") + " (yyyy-MM-dd):");
        lblDateTitle.setFont(UiUtils.regularFont());
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(lblDateTitle, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        JPanel datePanel = new JPanel(new BorderLayout(8, 0));
        txtDate = new DatePickerField(LocalDate.now(), 12);
        chkExpected = new JCheckBox(I18n.get("due.filter.upcoming") + " (Expected)");
        chkExpected.setFont(UiUtils.regularFont());
        datePanel.add(txtDate, BorderLayout.WEST);
        datePanel.add(chkExpected, BorderLayout.CENTER);
        mainPanel.add(datePanel, gbc);

        // 3. Amount
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblAmount = new JLabel(I18n.get("transactions.table.amount") + " (" + AppConfig.getCurrencySymbol() + "):");
        lblAmount.setFont(UiUtils.regularFont());
        mainPanel.add(lblAmount, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtAmount = new JTextField();
        txtAmount.setFont(UiUtils.boldFont());
        mainPanel.add(txtAmount, gbc);

        // 4. Payment Via
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblVia = new JLabel(I18n.get("transactions.table.via") + ":");
        lblVia.setFont(UiUtils.regularFont());
        mainPanel.add(lblVia, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        cmbPaymentVia = new JComboBox<>();
        cmbPaymentVia.setFont(UiUtils.regularFont());
        cmbPaymentVia.setEditable(true); // Allow custom editing
        populatePaymentModes();
        mainPanel.add(cmbPaymentVia, gbc);

        // 5. Category
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblCat = new JLabel(I18n.get("transactions.table.category") + ":");
        lblCat.setFont(UiUtils.regularFont());
        mainPanel.add(lblCat, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        cmbCategory = new JComboBox<>();
        cmbCategory.setFont(UiUtils.regularFont());
        cmbCategory.setEditable(true);
        mainPanel.add(cmbCategory, gbc);

        // 6. Sub Category
        lblSubCategoryTitle = new JLabel(I18n.get("transactions.table.subCategory") + ":");
        lblSubCategoryTitle.setFont(UiUtils.regularFont());
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(lblSubCategoryTitle, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        cmbSubCategory = new JComboBox<>();
        cmbSubCategory.setFont(UiUtils.regularFont());
        cmbSubCategory.setEditable(true);
        mainPanel.add(cmbSubCategory, gbc);

        // 7. Associated Person (Optional / Counterparty / Loan)
        lblPersonTitle = new JLabel(I18n.get("transactions.table.person") + ":");
        lblPersonTitle.setFont(UiUtils.regularFont());
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(lblPersonTitle, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        cmbPerson = new JComboBox<>();
        cmbPerson.setFont(UiUtils.regularFont());
        populatePersons();
        mainPanel.add(cmbPerson, gbc);

        // 8. Linking Section (Give <-> Return received counterpart linking)
        lblLinkingTitle = new JLabel("Linked Settlement:");
        lblLinkingTitle.setFont(UiUtils.regularFont());
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(lblLinkingTitle, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        pnlLinking = new JPanel(new BorderLayout(8, 0));
        pnlLinking.setOpaque(false);
        lblLinkingSummary = new JLabel("Unlinked (0 counterparts linked)");
        lblLinkingSummary.setFont(UiUtils.regularFont());
        lblLinkingSummary.setForeground(UiUtils.COLOR_TEXT_MUTED);

        btnManageLinks = UiUtils.createSecondaryButton("Link Counterparts...", NavIcons.link(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnManageLinks.addActionListener(e -> onOpenLinkingDialog());

        pnlLinking.add(lblLinkingSummary, BorderLayout.CENTER);
        pnlLinking.add(btnManageLinks, BorderLayout.EAST);
        mainPanel.add(pnlLinking, gbc);

        // 9. Notes
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblNotes = new JLabel(I18n.get("transactions.table.notes") + ":");
        lblNotes.setFont(UiUtils.regularFont());
        mainPanel.add(lblNotes, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtNotes = new JTextArea(3, 24);
        txtNotes.setFont(UiUtils.regularFont());
        txtNotes.setLineWrap(true);
        txtNotes.setWrapStyleWord(true);
        mainPanel.add(new JScrollPane(txtNotes), gbc);

        add(mainPanel, BorderLayout.CENTER);

        // 10. Bottom Action Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        btnPanel.setBackground(new Color(248, 250, 252));
        btnPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiUtils.COLOR_BORDER));

        JButton btnCancel = UiUtils.createSecondaryButton(I18n.get("btn.cancel"), NavIcons.cross(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnCancel.setFont(UiUtils.boldFont());
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = UiUtils.createPrimaryButton(existingTransaction == null ? I18n.get("btn.save") : I18n.get("btn.edit"), NavIcons.check(13, Color.WHITE));
        btnSave.setFont(UiUtils.boldFont());
        btnSave.addActionListener(e -> onSave());

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);
        add(btnPanel, BorderLayout.SOUTH);

        // Event listeners
        rbIncome.addActionListener(e -> {
            onTypeChanged(TransactionType.INCOME);
            updateLinkingVisibility();
        });
        rbExpense.addActionListener(e -> {
            onTypeChanged(TransactionType.EXPENSE);
            updateLinkingVisibility();
        });
        cmbCategory.addActionListener(e -> {
            onCategoryChanged();
            updateLinkingVisibility();
        });
        cmbPerson.addActionListener(e -> updateLinkingVisibility());

        // Prepopulate if editing
        if (existingTransaction != null) {
            if (existingTransaction.getType() == TransactionType.INCOME) {
                rbIncome.setSelected(true);
                onTypeChanged(TransactionType.INCOME);
            } else {
                rbExpense.setSelected(true);
                onTypeChanged(TransactionType.EXPENSE);
            }

            if (existingTransaction.getTxDate() != null) {
                txtDate.setDate(existingTransaction.getTxDate());
            }
            chkExpected.setSelected(existingTransaction.isExpected());
            txtAmount.setText(String.valueOf(existingTransaction.getAmount()));
            cmbPaymentVia.setSelectedItem(existingTransaction.getPaymentVia());
            cmbCategory.setSelectedItem(existingTransaction.getCategory());
            cmbSubCategory.setSelectedItem(existingTransaction.getSubCategory());
            txtNotes.setText(existingTransaction.getNotes() != null ? existingTransaction.getNotes() : "");

            if (existingTransaction.getPersonId() != null) {
                for (int i = 0; i < cmbPerson.getItemCount(); i++) {
                    Person p = cmbPerson.getItemAt(i);
                    if (p != null && p.getId() != null && p.getId().equals(existingTransaction.getPersonId())) {
                        cmbPerson.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } else {
            if (defaultType == TransactionType.INCOME) {
                rbIncome.setSelected(true);
                onTypeChanged(TransactionType.INCOME);
            } else {
                rbExpense.setSelected(true);
                onTypeChanged(TransactionType.EXPENSE);
            }
        }

        if (existingTransaction != null && existingTransaction.getId() != null) {
            pendingLinks = financeService.getTransactionLinks(existingTransaction.getId());
        }
        updateLinkingVisibility();
    }

    /**
     * Populates payment modes dropdown from the repository.
     */
    private void populatePaymentModes() {
        cmbPaymentVia.removeAllItems();
        List<String> modes = financeService.getAllPaymentModes();
        for (String m : modes) {
            cmbPaymentVia.addItem(m);
        }
        if (cmbPaymentVia.getItemCount() > 0) {
            cmbPaymentVia.setSelectedIndex(0);
        }
    }

    /**
     * Populates contacts dropdown with person directory entities.
     */
    private void populatePersons() {
        cmbPerson.removeAllItems();
        cmbPerson.addItem(null); // Option for None
        List<Person> persons = financeService.getAllPersons();
        for (Person p : persons) {
            cmbPerson.addItem(p);
        }
    }

    /**
     * Reconfigures category options and labels upon toggling between Income and Expense.
     *
     * @param type active transaction type
     */
    private void onTypeChanged(TransactionType type) {
        cmbCategory.removeAllItems();
        if (type == TransactionType.INCOME) {
            lblDateTitle.setText("Date / Expected Date:");
            chkExpected.setText("Expected (Not Received Yet)");
            // Income Categories requested: Salary, Relative, Return, Loan, Udhaar
            cmbCategory.addItem("Salary");
            cmbCategory.addItem("Relative");
            cmbCategory.addItem("Return");
            cmbCategory.addItem("Loan");
            cmbCategory.addItem("Freelance / Business");
            cmbCategory.addItem("Other Income");
        } else {
            lblDateTitle.setText("Date / Planned Date:");
            chkExpected.setText("Planned / Due (Not Paid Yet)");
            // Expense Categories: EMI Payment, Credit Card, Loan Given, Bill, Special Expense, etc.
            cmbCategory.addItem("EMI Payment");
            cmbCategory.addItem("Credit Card");
            cmbCategory.addItem("Loan Given");
            cmbCategory.addItem("Bill");
            cmbCategory.addItem("Special Expense");
            cmbCategory.addItem("Groceries & Food");
            cmbCategory.addItem("Shopping");
            cmbCategory.addItem("Health & Medical");
            cmbCategory.addItem("Fuel & Travel");
            cmbCategory.addItem("Other Expense");
        }
        onCategoryChanged();
    }

    /**
     * Dynamically filters and populates sub-category options based on the chosen category
     * by querying configured master entities (Bills, Cards, EMIs, Persons).
     */
    private void onCategoryChanged() {
        Object catObj = cmbCategory.getSelectedItem();
        String cat = catObj != null ? catObj.toString().trim() : "";
        cmbSubCategory.removeAllItems();

        if (rbExpense.isSelected()) {
            if ("Bill".equalsIgnoreCase(cat)) {
                // Populate from Bills master (e.g. Airtel Bill, Electricity Khardoni, Noida)
                List<Bill> bills = financeService.getAllBills();
                for (Bill b : bills) {
                    cmbSubCategory.addItem(b.getBillName());
                }
                cmbSubCategory.addItem("Other Bill");
            } else if ("Credit Card".equalsIgnoreCase(cat)) {
                // Populate from Credit Cards master
                List<CreditCard> cards = financeService.getAllCreditCards();
                for (CreditCard c : cards) {
                    cmbSubCategory.addItem(c.toString());
                }
                cmbSubCategory.addItem("Other Card");
            } else if ("EMI Payment".equalsIgnoreCase(cat)) {
                // Populate from EMIs master
                List<Emi> emis = financeService.getAllEmis();
                for (Emi e : emis) {
                    cmbSubCategory.addItem(e.getEmiName());
                }
                cmbSubCategory.addItem("Other Loan EMI");
            } else if ("Loan Given".equalsIgnoreCase(cat)) {
                // Populate with Persons
                List<Person> persons = financeService.getAllPersons();
                for (Person p : persons) {
                    cmbSubCategory.addItem(p.getName());
                }
            } else if ("Special Expense".equalsIgnoreCase(cat)) {
                cmbSubCategory.addItem("Family Gathering / Dinner");
                cmbSubCategory.addItem("Vacation / Trip");
                cmbSubCategory.addItem("Festival / Celebration");
                cmbSubCategory.addItem("Electronics Purchase");
                cmbSubCategory.addItem("Home Renovation");
            }
        } else {
            // Income
            if ("Salary".equalsIgnoreCase(cat)) {
                cmbSubCategory.addItem("Primary Employer");
                cmbSubCategory.addItem("Bonus / Incentive");
                cmbSubCategory.addItem("Arrears / Reimbursement");
            } else if ("Relative".equalsIgnoreCase(cat) || "Return".equalsIgnoreCase(cat)) {
                List<Person> persons = financeService.getAllPersons();
                for (Person p : persons) {
                    cmbSubCategory.addItem(p.getName());
                }
                cmbSubCategory.addItem("Other Person");
            } else if ("Loan".equalsIgnoreCase(cat)) {
                cmbSubCategory.addItem("Personal Loan Disbursal");
                cmbSubCategory.addItem("Gold Loan");
                cmbSubCategory.addItem("Friend Loan");
            }
        }
    }

    /**
     * Validates input parameters, resolves foreign key mappings for masters, persists
     * the transaction, and closes the dialog.
     */
    private void onSave() {
        String strAmount = txtAmount.getText().trim();
        if (strAmount.isEmpty()) {
            UiUtils.showError(this, "Please enter a valid amount.");
            txtAmount.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(strAmount);
            if (amount <= 0) {
                UiUtils.showError(this, "Amount must be greater than zero.");
                return;
            }
        } catch (NumberFormatException e) {
            UiUtils.showError(this, "Invalid amount format. Please enter numbers only.");
            return;
        }

        LocalDate txDate = txtDate.getDate();
        if (txDate == null) {
            UiUtils.showError(this, "Invalid date format. Please use YYYY-MM-DD or select from the calendar picker.");
            txtDate.getTextField().requestFocus();
            return;
        }

        TransactionType type = rbIncome.isSelected() ? TransactionType.INCOME : TransactionType.EXPENSE;
        String category = cmbCategory.getSelectedItem() != null ? cmbCategory.getSelectedItem().toString().trim() : "";
        String subCategory = cmbSubCategory.getSelectedItem() != null ? cmbSubCategory.getSelectedItem().toString().trim() : "";
        String paymentVia = cmbPaymentVia.getSelectedItem() != null ? cmbPaymentVia.getSelectedItem().toString().trim() : "Cash";

        if (paymentVia.isEmpty()) paymentVia = "Cash";

        // Remember new custom payment mode if not already present
        financeService.addPaymentMode(paymentVia);

        Person selectedPerson = (Person) cmbPerson.getSelectedItem();
        Long personId = selectedPerson != null ? selectedPerson.getId() : null;

        // Auto-match person if subCategory matches a person's name
        if (personId == null && !subCategory.isEmpty()) {
            List<Person> persons = financeService.getAllPersons();
            for (Person p : persons) {
                if (p.getName().equalsIgnoreCase(subCategory)) {
                    personId = p.getId();
                    break;
                }
            }
        }

        // Match Bill ID if category is Bill
        Long billId = null;
        if (type == TransactionType.EXPENSE && "Bill".equalsIgnoreCase(category)) {
            List<Bill> bills = financeService.getAllBills();
            for (Bill b : bills) {
                if (b.getBillName().equalsIgnoreCase(subCategory)) {
                    billId = b.getId();
                    break;
                }
            }
        }

        // Match EMI ID if category is EMI Payment
        Long emiId = null;
        if (type == TransactionType.EXPENSE && "EMI Payment".equalsIgnoreCase(category)) {
            List<Emi> emis = financeService.getAllEmis();
            for (Emi e : emis) {
                if (e.getEmiName().equalsIgnoreCase(subCategory)) {
                    emiId = e.getId();
                    break;
                }
            }
        }

        // Match Credit Card ID
        Long creditCardId = null;
        if (type == TransactionType.EXPENSE && "Credit Card".equalsIgnoreCase(category)) {
            List<CreditCard> cards = financeService.getAllCreditCards();
            for (CreditCard c : cards) {
                if (subCategory.contains(c.getCardName())) {
                    creditCardId = c.getId();
                    break;
                }
            }
        }

        Transaction tx = existingTransaction != null ? existingTransaction : new Transaction();
        tx.setType(type);
        tx.setTxDate(txDate);
        tx.setExpected(chkExpected.isSelected());
        tx.setAmount(amount);
        tx.setPaymentVia(paymentVia);
        tx.setCategory(category);
        tx.setSubCategory(subCategory);
        tx.setPersonId(personId);
        tx.setBillId(billId);
        tx.setEmiId(emiId);
        tx.setCreditCardId(creditCardId);
        tx.setNotes(txtNotes.getText().trim());

        financeService.saveTransactionWithLinks(tx, pendingLinks, existingTransaction);

        saved = true;
        dispose();
    }

    /**
     * Determines whether linking is relevant and updates visibility of the linking section.
     */
    private void updateLinkingVisibility() {
        String cat = cmbCategory.getSelectedItem() != null ? cmbCategory.getSelectedItem().toString().trim() : "";
        boolean isIncome = rbIncome.isSelected();
        boolean isExpense = rbExpense.isSelected();

        boolean isReturn = isIncome && ("Return".equalsIgnoreCase(cat) || cmbPerson.getSelectedIndex() > 0);
        boolean isGive = isExpense && ("Loan Given".equalsIgnoreCase(cat) || cmbPerson.getSelectedIndex() > 0);

        boolean eligible = isReturn || isGive;
        lblLinkingTitle.setVisible(eligible);
        pnlLinking.setVisible(eligible);

        if (eligible) {
            lblLinkingTitle.setText(isReturn ? "Link Give Tx(s):" : "Link Return Tx(s):");
            updateLinkingSummary();
        }
    }

    /**
     * Refreshes the label displaying linked amount, transaction count, and settlement status.
     */
    private void updateLinkingSummary() {
        if (pendingLinks == null || pendingLinks.isEmpty()) {
            lblLinkingSummary.setText("Unlinked (0 counterparts linked)");
            lblLinkingSummary.setForeground(UiUtils.COLOR_TEXT_MUTED);
            lblLinkingSummary.setIcon(null);
            return;
        }

        double totalLinked = 0.0;
        for (TransactionLink l : pendingLinks) {
            totalLinked += l.getAmount();
        }

        double targetAmount = 0.0;
        try {
            targetAmount = Double.parseDouble(txtAmount.getText().trim());
        } catch (Exception ignored) {}

        if (targetAmount > 0 && totalLinked >= targetAmount - 0.01) {
            lblLinkingSummary.setText(String.format("✓ Nullified (%s across %d tx)",
                    AppConfig.formatCurrency(totalLinked), pendingLinks.size()));
            lblLinkingSummary.setForeground(UiUtils.COLOR_INCOME);
            lblLinkingSummary.setIcon(NavIcons.checkCircle(13, UiUtils.COLOR_INCOME));
        } else {
            lblLinkingSummary.setText(String.format("⏳ Partial (%s across %d tx)",
                    AppConfig.formatCurrency(totalLinked), pendingLinks.size()));
            lblLinkingSummary.setForeground(UiUtils.COLOR_AMBER);
            lblLinkingSummary.setIcon(NavIcons.statusDot(8, UiUtils.COLOR_AMBER));
        }
    }

    /**
     * Opens the modal counterpart linking dialog to pick transactions and assign allocated amounts.
     */
    private void onOpenLinkingDialog() {
        String strAmount = txtAmount.getText().trim();
        double amount;
        try {
            amount = Double.parseDouble(strAmount);
            if (amount <= 0) throw new NumberFormatException();
        } catch (Exception ex) {
            UiUtils.showError(this, "Please enter a valid amount before configuring counterpart links.");
            txtAmount.requestFocus();
            return;
        }

        Transaction sample = new Transaction();
        sample.setId(existingTransaction != null ? existingTransaction.getId() : null);
        sample.setType(rbIncome.isSelected() ? TransactionType.INCOME : TransactionType.EXPENSE);
        sample.setAmount(amount);
        sample.setCategory(cmbCategory.getSelectedItem() != null ? cmbCategory.getSelectedItem().toString() : "");
        sample.setSubCategory(cmbSubCategory.getSelectedItem() != null ? cmbSubCategory.getSelectedItem().toString() : "");
        sample.setTxDate(txtDate.getDate());

        Person p = (Person) cmbPerson.getSelectedItem();
        if (p != null) {
            sample.setPersonId(p.getId());
            sample.setPersonName(p.getName());
        }

        TransactionLinkDialog dlg = new TransactionLinkDialog(this, sample, pendingLinks);
        dlg.setVisible(true);
        if (dlg.isSaved()) {
            this.pendingLinks = dlg.getResultLinks();
            updateLinkingSummary();
        }
    }

    /**
     * Indicates whether the transaction was saved or updated.
     *
     * @return {@code true} if saved successfully, {@code false} otherwise
     */
    public boolean isSaved() {
        return saved;
    }
}
