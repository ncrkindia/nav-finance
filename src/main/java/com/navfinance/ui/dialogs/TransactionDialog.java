package com.navfinance.ui.dialogs;

import com.navfinance.config.AppConfig;
import com.navfinance.i18n.I18n;
import com.navfinance.model.AmountType;
import com.navfinance.model.Bill;
import com.navfinance.model.CreditCard;
import com.navfinance.model.Emi;
import com.navfinance.model.Person;
import com.navfinance.model.RecurringSchedule;
import com.navfinance.model.Transaction;
import com.navfinance.model.TransactionLink;
import com.navfinance.model.TransactionType;
import com.navfinance.service.FinanceService;
import com.navfinance.service.RecurringService;
import com.navfinance.ui.components.DatePickerField;
import com.navfinance.ui.util.NavIcons;
import com.navfinance.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Primary modal dialog for adding and editing financial income and expense ledger entries.
 * <p>
 * Supports distinct transaction natures:
 * <ul>
 *   <li>One-Time Expense (Realized Done vs Planned Expected)</li>
 *   <li>One-Time Received / Income (Realized Done vs Planned Expected)</li>
 *   <li>EMI Payment (Fixed monthly installments linked to an EMI master; optionally managed in upcoming/due schedules)</li>
 *   <li>Credit Card Bill (Variable monthly statements linked to a Credit Card master; managed in upcoming/due schedules)</li>
 *   <li>Utility / Other Bills (Outright expense payments - never considered balance expected back like given money)</li>
 *   <li>Personal Loan / Counterparty transfers (Eligible for Give &lt;-&gt; Return settlement linking)</li>
 * </ul>
 * </p>
 *
 * @author Nav Finance Team
 * @version 2.0.0
 */
public class TransactionDialog extends JDialog {

    public enum TransactionNature {
        ONE_TIME_EXPENSE("One-Time Expense (Done / Expected)"),
        ONE_TIME_INCOME("One-Time Received / Income"),
        EMI_PAYMENT("EMI Payment (Monthly Installment)"),
        CREDIT_CARD_BILL("Credit Card Bill (Variable Monthly)"),
        UTILITY_BILL("Utility / Other Bill (Electricity, Wi-Fi, etc.)"),
        PERSONAL_LOAN("Personal Loan / Transfer (Lent / Borrowed / Return)");

        private final String label;
        TransactionNature(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    private final FinanceService financeService = FinanceService.getInstance();
    private final RecurringService recurringService = RecurringService.getInstance();
    private final Transaction existingTransaction;
    private boolean saved = false;

    // Visual Controls
    private JComboBox<TransactionNature> cmbNature;
    private JRadioButton rbIncome;
    private JRadioButton rbExpense;
    private JPanel pnlTypeSelector;

    // Dynamic Master Container
    private JLabel lblMasterTitle;
    private JPanel pnlMasterCard;
    private CardLayout masterCardLayout;
    private JComboBox<Person> cmbPerson;
    private JComboBox<Emi> cmbEmi;
    private JComboBox<CreditCard> cmbCreditCard;
    private JComboBox<Bill> cmbBill;
    private JLabel lblMasterHint;

    // Recurring / Due Sync Checkbox
    private JCheckBox chkManageRecurring;

    // Standard Fields
    private DatePickerField txtDate;
    private JCheckBox chkExpected;
    private JTextField txtAmount;
    private JLabel lblAmountPrompt;
    private JComboBox<String> cmbPaymentVia;
    private JComboBox<String> cmbCategory;
    private JComboBox<String> cmbSubCategory;

    // Linking Section (Personal loans only)
    private JPanel pnlLinking;
    private JLabel lblLinkingTitle;
    private JLabel lblLinkingSummary;
    private JButton btnManageLinks;
    private List<TransactionLink> pendingLinks = new ArrayList<>();

    private JTextArea txtNotes;
    private JLabel lblDateTitle;
    private JLabel lblSubCategoryTitle;

    public TransactionDialog(JFrame parent, Transaction transactionToEdit, TransactionType defaultType) {
        super(parent, transactionToEdit == null ?
                (defaultType == TransactionType.INCOME ? I18n.get("dialog.income.addTitle") : I18n.get("dialog.expense.addTitle")) :
                (transactionToEdit.getType() == TransactionType.INCOME ? I18n.get("dialog.income.editTitle") : I18n.get("dialog.expense.editTitle")) + " #" + transactionToEdit.getId(), true);
        this.existingTransaction = transactionToEdit;

        initComponents(defaultType != null ? defaultType : TransactionType.EXPENSE);
        pack();
        UiUtils.clampDialogToScreen(this);
        setLocationRelativeTo(parent);
    }

    private void initComponents(TransactionType defaultType) {
        setLayout(new BorderLayout());
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // 1. Transaction Nature Selector (Prominent at top)
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        JLabel lblNature = new JLabel("Transaction Nature:*");
        lblNature.setFont(UiUtils.boldFont());
        mainPanel.add(lblNature, gbc);

        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 0.7;
        cmbNature = new JComboBox<>(TransactionNature.values());
        cmbNature.setFont(UiUtils.boldFont());
        cmbNature.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof TransactionNature nat) {
                    setText(nat.toString());
                    switch (nat) {
                        case ONE_TIME_EXPENSE -> setIcon(NavIcons.minus(14, UiUtils.COLOR_EXPENSE));
                        case ONE_TIME_INCOME -> setIcon(NavIcons.plus(14, UiUtils.COLOR_INCOME));
                        case EMI_PAYMENT -> setIcon(NavIcons.emi(14, UiUtils.COLOR_PRIMARY));
                        case CREDIT_CARD_BILL -> setIcon(NavIcons.card(14, UiUtils.COLOR_PRIMARY));
                        case UTILITY_BILL -> setIcon(NavIcons.bill(14, UiUtils.COLOR_PRIMARY));
                        case PERSONAL_LOAN -> setIcon(NavIcons.person(14, UiUtils.COLOR_AMBER));
                    }
                }
                return this;
            }
        });
        cmbNature.addActionListener(e -> onNatureChanged());
        mainPanel.add(cmbNature, gbc);

        // 2. Type selection (Income vs Expense)
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblType = new JLabel(I18n.get("transactions.table.type") + ":");
        lblType.setFont(UiUtils.regularFont());
        mainPanel.add(lblType, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        pnlTypeSelector = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        rbIncome = new JRadioButton(I18n.get("transactions.filter.income"));
        rbExpense = new JRadioButton(I18n.get("transactions.filter.expense"));
        rbIncome.setFont(UiUtils.boldFont());
        rbIncome.setForeground(UiUtils.COLOR_INCOME);
        rbExpense.setFont(UiUtils.boldFont());
        rbExpense.setForeground(UiUtils.COLOR_EXPENSE);

        ButtonGroup bg = new ButtonGroup();
        bg.add(rbIncome);
        bg.add(rbExpense);
        pnlTypeSelector.add(rbIncome);
        pnlTypeSelector.add(rbExpense);
        mainPanel.add(pnlTypeSelector, gbc);

        // 3. Dynamic Master Association Card
        lblMasterTitle = new JLabel("Associated Master:");
        lblMasterTitle.setFont(UiUtils.boldFont());
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(lblMasterTitle, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        masterCardLayout = new CardLayout();
        pnlMasterCard = new JPanel(masterCardLayout);
        pnlMasterCard.setOpaque(false);

        // Master 1: Person
        cmbPerson = new JComboBox<>();
        cmbPerson.setFont(UiUtils.regularFont());
        cmbPerson.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Person p) {
                    setText(p.getName() + (p.getRelation() != null && !p.getRelation().isBlank() ? " (" + p.getRelation() + ")" : ""));
                    setIcon(NavIcons.person(14, UiUtils.COLOR_PRIMARY));
                } else if (value == null) {
                    setText("(None / No Master Person Associated)");
                    setIcon(NavIcons.person(14, UiUtils.COLOR_TEXT_MUTED));
                }
                return this;
            }
        });
        populatePersons();
        cmbPerson.addActionListener(e -> updateLinkingVisibility());
        pnlMasterCard.add(cmbPerson, "PERSON");

        // Master 2: EMI
        cmbEmi = new JComboBox<>();
        cmbEmi.setFont(UiUtils.regularFont());
        cmbEmi.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Emi e) {
                    double amt = e.getMonthlyAmount() != null ? e.getMonthlyAmount() : 0.0;
                    setText(e.getEmiName() + " (" + AppConfig.formatCurrency(amt) + "/mo" + (e.getBankLender() != null ? " - " + e.getBankLender() : "") + ")");
                    setIcon(NavIcons.emi(14, UiUtils.COLOR_PRIMARY));
                }
                return this;
            }
        });
        populateEmis();
        cmbEmi.addActionListener(e -> onEmiSelected());
        pnlMasterCard.add(cmbEmi, "EMI");

        // Master 3: Credit Card
        cmbCreditCard = new JComboBox<>();
        cmbCreditCard.setFont(UiUtils.regularFont());
        cmbCreditCard.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof CreditCard c) {
                    setText(c.getCardName() + (c.getBank() != null && !c.getBank().isBlank() ? " (" + c.getBank() + ")" : ""));
                    setIcon(NavIcons.card(14, UiUtils.COLOR_PRIMARY));
                }
                return this;
            }
        });
        populateCards();
        cmbCreditCard.addActionListener(e -> onCreditCardSelected());
        pnlMasterCard.add(cmbCreditCard, "CARD");

        // Master 4: Bill
        cmbBill = new JComboBox<>();
        cmbBill.setFont(UiUtils.regularFont());
        cmbBill.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Bill b) {
                    setText(b.getBillName() + (b.getDefaultAmount() > 0 ? " (" + AppConfig.formatCurrency(b.getDefaultAmount()) + ")" : ""));
                    setIcon(NavIcons.bill(14, UiUtils.COLOR_PRIMARY));
                }
                return this;
            }
        });
        populateBills();
        cmbBill.addActionListener(e -> onBillSelected());
        pnlMasterCard.add(cmbBill, "BILL");

        // Empty card for simple one-off without master
        JPanel pnlEmpty = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlEmpty.setOpaque(false);
        JLabel lblNone = new JLabel("(No specific master entity required)");
        lblNone.setFont(UiUtils.regularFont());
        lblNone.setForeground(UiUtils.COLOR_TEXT_MUTED);
        pnlEmpty.add(lblNone);
        pnlMasterCard.add(pnlEmpty, "NONE");

        JPanel pnlMasterWrapper = new JPanel(new BorderLayout(0, 3));
        pnlMasterWrapper.setOpaque(false);
        pnlMasterWrapper.add(pnlMasterCard, BorderLayout.CENTER);

        lblMasterHint = new JLabel();
        lblMasterHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblMasterHint.setForeground(UiUtils.COLOR_TEXT_MUTED);
        pnlMasterWrapper.add(lblMasterHint, BorderLayout.SOUTH);

        mainPanel.add(pnlMasterWrapper, gbc);

        // 4. Recurring / Upcoming Schedule Management Checkbox
        gbc.gridx = 1; gbc.gridy = row++;
        chkManageRecurring = new JCheckBox("Also manage in Due / Upcoming payments schedule");
        chkManageRecurring.setFont(UiUtils.FONT_REGULAR);
        chkManageRecurring.setForeground(new Color(15, 118, 110)); // Teal
        mainPanel.add(chkManageRecurring, gbc);

        // 5. Date & Expected (Done vs Expected Unrealized)
        lblDateTitle = new JLabel(I18n.get("transactions.table.date") + ":");
        lblDateTitle.setFont(UiUtils.regularFont());
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(lblDateTitle, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        JPanel datePanel = new JPanel(new BorderLayout(8, 0));
        txtDate = new DatePickerField(LocalDate.now(), 12);
        chkExpected = new JCheckBox("Upcoming / Expected (Not Settled Yet)");
        chkExpected.setFont(UiUtils.regularFont());
        datePanel.add(txtDate, BorderLayout.WEST);
        datePanel.add(chkExpected, BorderLayout.CENTER);
        mainPanel.add(datePanel, gbc);

        // 6. Amount
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblAmount = new JLabel(I18n.get("transactions.table.amount") + " (" + AppConfig.getCurrencySymbol() + "):*");
        lblAmount.setFont(UiUtils.boldFont());
        mainPanel.add(lblAmount, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        JPanel pnlAmountGroup = new JPanel(new BorderLayout(6, 0));
        txtAmount = new JTextField();
        txtAmount.setFont(UiUtils.boldFont());
        lblAmountPrompt = new JLabel("");
        lblAmountPrompt.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblAmountPrompt.setForeground(UiUtils.COLOR_TEXT_MUTED);

        pnlAmountGroup.add(txtAmount, BorderLayout.CENTER);
        pnlAmountGroup.add(lblAmountPrompt, BorderLayout.EAST);
        mainPanel.add(pnlAmountGroup, gbc);

        // 7. Payment Via
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblVia = new JLabel(I18n.get("transactions.table.via") + ":");
        lblVia.setFont(UiUtils.regularFont());
        mainPanel.add(lblVia, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        cmbPaymentVia = new JComboBox<>();
        cmbPaymentVia.setFont(UiUtils.regularFont());
        cmbPaymentVia.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null && !value.toString().isBlank()) {
                    setIcon(NavIcons.paymentMode(14, UiUtils.COLOR_PRIMARY));
                }
                return this;
            }
        });
        cmbPaymentVia.setEditable(true);
        populatePaymentModes();
        mainPanel.add(cmbPaymentVia, gbc);

        // 8. Category
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblCat = new JLabel(I18n.get("transactions.table.category") + ":");
        lblCat.setFont(UiUtils.regularFont());
        mainPanel.add(lblCat, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        cmbCategory = new JComboBox<>();
        cmbCategory.setFont(UiUtils.regularFont());
        cmbCategory.setEditable(true);
        mainPanel.add(cmbCategory, gbc);

        // 9. Sub Category
        lblSubCategoryTitle = new JLabel(I18n.get("transactions.table.subCategory") + ":");
        lblSubCategoryTitle.setFont(UiUtils.regularFont());
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(lblSubCategoryTitle, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        cmbSubCategory = new JComboBox<>();
        cmbSubCategory.setFont(UiUtils.regularFont());
        cmbSubCategory.setEditable(true);
        mainPanel.add(cmbSubCategory, gbc);

        // 10. Give <-> Return settlement linking (personal loans only)
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

        // 11. Notes
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

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // 12. Bottom Action Buttons
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

        UiUtils.configureDialog(this, btnSave, btnCancel);

        // Wire event listeners
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

        // Initialize state
        if (existingTransaction != null) {
            initFromExistingTransaction();
        } else {
            if (defaultType == TransactionType.INCOME) {
                cmbNature.setSelectedItem(TransactionNature.ONE_TIME_INCOME);
            } else {
                cmbNature.setSelectedItem(TransactionNature.ONE_TIME_EXPENSE);
            }
            onNatureChanged();
        }
    }

    private void initFromExistingTransaction() {
        if (existingTransaction.getEmiId() != null) {
            cmbNature.setSelectedItem(TransactionNature.EMI_PAYMENT);
            selectEmiById(existingTransaction.getEmiId());
        } else if (existingTransaction.getCreditCardId() != null) {
            cmbNature.setSelectedItem(TransactionNature.CREDIT_CARD_BILL);
            selectCreditCardById(existingTransaction.getCreditCardId());
        } else if (existingTransaction.getBillId() != null) {
            cmbNature.setSelectedItem(TransactionNature.UTILITY_BILL);
            selectBillById(existingTransaction.getBillId());
        } else if (existingTransaction.getPersonId() != null) {
            cmbNature.setSelectedItem(TransactionNature.PERSONAL_LOAN);
            selectPersonById(existingTransaction.getPersonId());
        } else if (existingTransaction.getType() == TransactionType.INCOME) {
            cmbNature.setSelectedItem(TransactionNature.ONE_TIME_INCOME);
        } else {
            cmbNature.setSelectedItem(TransactionNature.ONE_TIME_EXPENSE);
        }

        onNatureChanged();

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

        if (existingTransaction.getId() != null) {
            pendingLinks = financeService.getTransactionLinks(existingTransaction.getId());
        }
        updateLinkingVisibility();
    }

    private void onNatureChanged() {
        TransactionNature nature = (TransactionNature) cmbNature.getSelectedItem();
        if (nature == null) nature = TransactionNature.ONE_TIME_EXPENSE;

        switch (nature) {
            case ONE_TIME_EXPENSE -> {
                rbExpense.setSelected(true);
                rbExpense.setEnabled(true);
                rbIncome.setEnabled(false);
                pnlTypeSelector.setVisible(false);
                lblMasterTitle.setText("Associated Person:");
                masterCardLayout.show(pnlMasterCard, "PERSON");
                lblMasterHint.setText("Optional counterparty or personal reference");
                chkManageRecurring.setVisible(false);
                chkExpected.setText("Planned / Due (Not Paid Yet)");
                lblAmountPrompt.setText("");
                onTypeChanged(TransactionType.EXPENSE);
            }
            case ONE_TIME_INCOME -> {
                rbIncome.setSelected(true);
                rbIncome.setEnabled(true);
                rbExpense.setEnabled(false);
                pnlTypeSelector.setVisible(false);
                lblMasterTitle.setText("Associated Person:");
                masterCardLayout.show(pnlMasterCard, "PERSON");
                lblMasterHint.setText("Optional counterparty or source reference");
                chkManageRecurring.setVisible(false);
                chkExpected.setText("Expected (Not Received Yet)");
                lblAmountPrompt.setText("");
                onTypeChanged(TransactionType.INCOME);
            }
            case EMI_PAYMENT -> {
                rbExpense.setSelected(true);
                rbExpense.setEnabled(false);
                rbIncome.setEnabled(false);
                pnlTypeSelector.setVisible(false);
                lblMasterTitle.setText("Associated EMI Master:*");
                masterCardLayout.show(pnlMasterCard, "EMI");
                chkManageRecurring.setVisible(true);
                chkManageRecurring.setSelected(true);
                chkExpected.setText("Upcoming Due (Not Paid Yet)");
                lblAmountPrompt.setText("Monthly installment");
                onTypeChanged(TransactionType.EXPENSE);
                cmbCategory.setSelectedItem("EMI Payment");
                onEmiSelected();
            }
            case CREDIT_CARD_BILL -> {
                rbExpense.setSelected(true);
                rbExpense.setEnabled(false);
                rbIncome.setEnabled(false);
                pnlTypeSelector.setVisible(false);
                lblMasterTitle.setText("Associated Credit Card:*");
                masterCardLayout.show(pnlMasterCard, "CARD");
                chkManageRecurring.setVisible(true);
                chkManageRecurring.setSelected(true);
                chkExpected.setText("Upcoming Statement (Not Paid Yet)");
                lblAmountPrompt.setText("Statement bill varies monthly");
                onTypeChanged(TransactionType.EXPENSE);
                cmbCategory.setSelectedItem("Credit Card");
                onCreditCardSelected();
            }
            case UTILITY_BILL -> {
                rbExpense.setSelected(true);
                rbExpense.setEnabled(false);
                rbIncome.setEnabled(false);
                pnlTypeSelector.setVisible(false);
                lblMasterTitle.setText("Associated Bill Master:*");
                masterCardLayout.show(pnlMasterCard, "BILL");
                chkManageRecurring.setVisible(true);
                chkManageRecurring.setSelected(true);
                chkExpected.setText("Upcoming Bill (Not Paid Yet)");
                lblAmountPrompt.setText("");
                onTypeChanged(TransactionType.EXPENSE);
                cmbCategory.setSelectedItem("Bill");
                onBillSelected();
            }
            case PERSONAL_LOAN -> {
                rbIncome.setEnabled(true);
                rbExpense.setEnabled(true);
                pnlTypeSelector.setVisible(true);
                lblMasterTitle.setText("Associated Person / Contact:*");
                masterCardLayout.show(pnlMasterCard, "PERSON");
                lblMasterHint.setText("Peer loan or counterparty balance (Settlement trackable)");
                chkManageRecurring.setVisible(false);
                lblAmountPrompt.setText("");
                onTypeChanged(rbIncome.isSelected() ? TransactionType.INCOME : TransactionType.EXPENSE);
            }
        }
        updateLinkingVisibility();
    }

    private void onEmiSelected() {
        Emi emi = (Emi) cmbEmi.getSelectedItem();
        if (emi != null) {
            double amt = emi.getMonthlyAmount() != null ? emi.getMonthlyAmount() : 0.0;
            if (txtAmount.getText().trim().isEmpty() || "0.0".equals(txtAmount.getText().trim())) {
                txtAmount.setText(String.valueOf(amt));
            }
            cmbSubCategory.setSelectedItem(emi.getEmiName());
            if (txtNotes.getText().trim().isEmpty()) {
                txtNotes.setText("Monthly EMI for " + emi.getEmiName());
            }
            lblMasterHint.setText("Outright payment: " + AppConfig.formatCurrency(amt) + "/mo | Bank: " + (emi.getBankLender() != null ? emi.getBankLender() : "-"));
        }
    }

    private void onCreditCardSelected() {
        CreditCard card = (CreditCard) cmbCreditCard.getSelectedItem();
        if (card != null) {
            cmbSubCategory.setSelectedItem(card.getCardName());
            if (txtNotes.getText().trim().isEmpty()) {
                txtNotes.setText("Credit Card Bill for " + card.getCardName());
            }
            lblMasterHint.setText("Bank: " + (card.getBank() != null ? card.getBank() : "-") + " | Billing cycle variable statement payment");
        }
    }

    private void onBillSelected() {
        Bill bill = (Bill) cmbBill.getSelectedItem();
        if (bill != null) {
            if (bill.getDefaultAmount() > 0 && (txtAmount.getText().trim().isEmpty() || "0.0".equals(txtAmount.getText().trim()))) {
                txtAmount.setText(String.valueOf(bill.getDefaultAmount()));
            }
            cmbSubCategory.setSelectedItem(bill.getBillName());
            if (txtNotes.getText().trim().isEmpty()) {
                txtNotes.setText("Bill payment for " + bill.getBillName());
            }
            lblMasterHint.setText("Outright expense: " + bill.getBillName() + " (Not expected back like loan money)");
        }
    }

    private void populatePaymentModes() {
        cmbPaymentVia.removeAllItems();
        List<String> modes = financeService.getAllPaymentModes();
        for (String m : modes) cmbPaymentVia.addItem(m);
        if (cmbPaymentVia.getItemCount() > 0) cmbPaymentVia.setSelectedIndex(0);
    }

    private void populatePersons() {
        cmbPerson.removeAllItems();
        cmbPerson.addItem(null);
        for (Person p : financeService.getAllPersons()) cmbPerson.addItem(p);
    }

    private void populateEmis() {
        cmbEmi.removeAllItems();
        for (Emi e : financeService.getAllEmis()) cmbEmi.addItem(e);
    }

    private void populateCards() {
        cmbCreditCard.removeAllItems();
        for (CreditCard c : financeService.getAllCreditCards()) cmbCreditCard.addItem(c);
    }

    private void populateBills() {
        cmbBill.removeAllItems();
        for (Bill b : financeService.getAllBills()) cmbBill.addItem(b);
    }

    private void selectPersonById(Long id) {
        if (id == null) return;
        for (int i = 0; i < cmbPerson.getItemCount(); i++) {
            Person p = cmbPerson.getItemAt(i);
            if (p != null && id.equals(p.getId())) {
                cmbPerson.setSelectedIndex(i);
                break;
            }
        }
    }

    private void selectEmiById(Long id) {
        if (id == null) return;
        for (int i = 0; i < cmbEmi.getItemCount(); i++) {
            Emi e = cmbEmi.getItemAt(i);
            if (e != null && id.equals(e.getId())) {
                cmbEmi.setSelectedIndex(i);
                break;
            }
        }
    }

    private void selectCreditCardById(Long id) {
        if (id == null) return;
        for (int i = 0; i < cmbCreditCard.getItemCount(); i++) {
            CreditCard c = cmbCreditCard.getItemAt(i);
            if (c != null && id.equals(c.getId())) {
                cmbCreditCard.setSelectedIndex(i);
                break;
            }
        }
    }

    private void selectBillById(Long id) {
        if (id == null) return;
        for (int i = 0; i < cmbBill.getItemCount(); i++) {
            Bill b = cmbBill.getItemAt(i);
            if (b != null && id.equals(b.getId())) {
                cmbBill.setSelectedIndex(i);
                break;
            }
        }
    }

    private void onTypeChanged(TransactionType type) {
        cmbCategory.removeAllItems();
        if (type == TransactionType.INCOME) {
            lblDateTitle.setText("Date / Expected Date:");
            chkExpected.setText("Expected (Not Received Yet)");
            cmbCategory.addItem("Salary");
            cmbCategory.addItem("Relative");
            cmbCategory.addItem("Return");
            cmbCategory.addItem("Loan");
            cmbCategory.addItem("Freelance / Business");
            cmbCategory.addItem("Other Income");
        } else {
            lblDateTitle.setText("Date / Planned Date:");
            chkExpected.setText("Planned / Due (Not Paid Yet)");
            cmbCategory.addItem("EMI Payment");
            cmbCategory.addItem("Credit Card");
            cmbCategory.addItem("Bill");
            cmbCategory.addItem("Loan Given");
            cmbCategory.addItem("Special Expense");
            cmbCategory.addItem("Groceries & Food");
            cmbCategory.addItem("Shopping");
            cmbCategory.addItem("Health & Medical");
            cmbCategory.addItem("Fuel & Travel");
            cmbCategory.addItem("Other Expense");
        }
        onCategoryChanged();
    }

    private void onCategoryChanged() {
        Object catObj = cmbCategory.getSelectedItem();
        String cat = catObj != null ? catObj.toString().trim() : "";
        cmbSubCategory.removeAllItems();

        if (rbExpense.isSelected()) {
            if ("Bill".equalsIgnoreCase(cat)) {
                for (Bill b : financeService.getAllBills()) cmbSubCategory.addItem(b.getBillName());
                cmbSubCategory.addItem("Other Bill");
            } else if ("Credit Card".equalsIgnoreCase(cat)) {
                for (CreditCard c : financeService.getAllCreditCards()) cmbSubCategory.addItem(c.getCardName());
                cmbSubCategory.addItem("Other Card");
            } else if ("EMI Payment".equalsIgnoreCase(cat)) {
                for (Emi e : financeService.getAllEmis()) cmbSubCategory.addItem(e.getEmiName());
                cmbSubCategory.addItem("Other Loan EMI");
            } else if ("Loan Given".equalsIgnoreCase(cat)) {
                for (Person p : financeService.getAllPersons()) cmbSubCategory.addItem(p.getName());
            } else if ("Special Expense".equalsIgnoreCase(cat)) {
                cmbSubCategory.addItem("Family Gathering / Dinner");
                cmbSubCategory.addItem("Vacation / Trip");
                cmbSubCategory.addItem("Festival / Celebration");
                cmbSubCategory.addItem("Electronics Purchase");
                cmbSubCategory.addItem("Home Renovation");
            }
        } else {
            if ("Salary".equalsIgnoreCase(cat)) {
                cmbSubCategory.addItem("Primary Employer");
                cmbSubCategory.addItem("Bonus / Incentive");
                cmbSubCategory.addItem("Arrears / Reimbursement");
            } else if ("Relative".equalsIgnoreCase(cat) || "Return".equalsIgnoreCase(cat)) {
                for (Person p : financeService.getAllPersons()) cmbSubCategory.addItem(p.getName());
                cmbSubCategory.addItem("Other Person");
            } else if ("Loan".equalsIgnoreCase(cat)) {
                cmbSubCategory.addItem("Personal Loan Disbursal");
                cmbSubCategory.addItem("Gold Loan");
                cmbSubCategory.addItem("Friend Loan");
            }
        }
    }

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

        TransactionNature nature = (TransactionNature) cmbNature.getSelectedItem();
        if (nature == null) nature = TransactionNature.ONE_TIME_EXPENSE;

        TransactionType type = rbIncome.isSelected() ? TransactionType.INCOME : TransactionType.EXPENSE;
        String category = cmbCategory.getSelectedItem() != null ? cmbCategory.getSelectedItem().toString().trim() : "";
        String subCategory = cmbSubCategory.getSelectedItem() != null ? cmbSubCategory.getSelectedItem().toString().trim() : "";
        String paymentVia = cmbPaymentVia.getSelectedItem() != null ? cmbPaymentVia.getSelectedItem().toString().trim() : "Cash";
        if (paymentVia.isEmpty()) paymentVia = "Cash";

        financeService.addPaymentMode(paymentVia);

        Long personId = null;
        Long billId = null;
        Long emiId = null;
        Long creditCardId = null;

        switch (nature) {
            case EMI_PAYMENT -> {
                Emi e = (Emi) cmbEmi.getSelectedItem();
                if (e != null) {
                    emiId = e.getId();
                    if (subCategory.isEmpty()) subCategory = e.getEmiName();
                }
            }
            case CREDIT_CARD_BILL -> {
                CreditCard c = (CreditCard) cmbCreditCard.getSelectedItem();
                if (c != null) {
                    creditCardId = c.getId();
                    if (subCategory.isEmpty()) subCategory = c.getCardName();
                }
            }
            case UTILITY_BILL -> {
                Bill b = (Bill) cmbBill.getSelectedItem();
                if (b != null) {
                    billId = b.getId();
                    if (subCategory.isEmpty()) subCategory = b.getBillName();
                }
            }
            case PERSONAL_LOAN -> {
                Person p = (Person) cmbPerson.getSelectedItem();
                if (p != null) {
                    personId = p.getId();
                    if (subCategory.isEmpty()) subCategory = p.getName();
                }
            }
            case ONE_TIME_EXPENSE, ONE_TIME_INCOME -> {
                Person p = (Person) cmbPerson.getSelectedItem();
                if (p != null) personId = p.getId();
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

        // Optionally manage recurring/upcoming schedule
        if (chkManageRecurring.isVisible() && chkManageRecurring.isSelected()) {
            manageRecurringSchedule(tx, nature);
        }

        saved = true;
        dispose();
    }

    private void manageRecurringSchedule(Transaction tx, TransactionNature nature) {
        try {
            String schedName = (tx.getSubCategory() != null && !tx.getSubCategory().isBlank()) ?
                    tx.getSubCategory() : (tx.getCategory() + " Schedule");

            // Check if existing schedule with this name exists
            boolean exists = false;
            for (RecurringSchedule s : recurringService.getAllSchedules()) {
                if (s.getName() != null && s.getName().equalsIgnoreCase(schedName)) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                RecurringSchedule s = new RecurringSchedule();
                s.setName(schedName);
                s.setType(TransactionType.EXPENSE);
                s.setCategory(tx.getCategory());
                s.setSubCategory(tx.getSubCategory());
                s.setDayOfMonth(tx.getTxDate() != null ? tx.getTxDate().getDayOfMonth() : 5);
                s.setDefaultAmount(tx.getAmount());
                s.setAmountType(nature == TransactionNature.CREDIT_CARD_BILL ? AmountType.VARIABLE : AmountType.FIXED);
                s.setStartDate(tx.getTxDate() != null ? tx.getTxDate().withDayOfMonth(1) : LocalDate.now().withDayOfMonth(1));
                s.setPaymentVia(tx.getPaymentVia());
                s.setActive(true);
                s.setNotes("Auto-registered from " + nature);

                recurringService.saveSchedule(s);
                recurringService.generateDueForSchedule(s, 2);
            }
        } catch (Exception ex) {
            System.err.println("Could not auto-register recurring schedule: " + ex.getMessage());
        }
    }

    private void updateLinkingVisibility() {
        TransactionNature nature = (TransactionNature) cmbNature.getSelectedItem();
        boolean isPersonalLoan = nature == TransactionNature.PERSONAL_LOAN;

        String cat = cmbCategory.getSelectedItem() != null ? cmbCategory.getSelectedItem().toString().trim() : "";
        boolean isIncome = rbIncome.isSelected();
        boolean isExpense = rbExpense.isSelected();

        boolean isReturn = isIncome && ("Return".equalsIgnoreCase(cat) || cmbPerson.getSelectedIndex() > 0);
        boolean isGive = isExpense && ("Loan Given".equalsIgnoreCase(cat) || cmbPerson.getSelectedIndex() > 0);

        boolean eligible = isPersonalLoan && (isReturn || isGive);
        lblLinkingTitle.setVisible(eligible);
        pnlLinking.setVisible(eligible);

        if (eligible) {
            lblLinkingTitle.setText(isReturn ? "Link Give Tx(s):" : "Link Return Tx(s):");
            updateLinkingSummary();
        }
    }

    private void updateLinkingSummary() {
        if (pendingLinks == null || pendingLinks.isEmpty()) {
            lblLinkingSummary.setText("Unlinked (0 counterparts linked)");
            lblLinkingSummary.setForeground(UiUtils.COLOR_TEXT_MUTED);
            lblLinkingSummary.setIcon(null);
            return;
        }

        double totalLinked = 0.0;
        for (TransactionLink l : pendingLinks) totalLinked += l.getAmount();

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

    public boolean isSaved() {
        return saved;
    }
}
