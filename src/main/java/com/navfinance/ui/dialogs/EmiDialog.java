package com.navfinance.ui.dialogs;

import com.navfinance.config.AppConfig;
import com.navfinance.i18n.I18n;
import com.navfinance.model.CreditCard;
import com.navfinance.model.Emi;
import com.navfinance.service.FinanceService;
import com.navfinance.ui.util.NavIcons;
import com.navfinance.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

/**
 * Modal dialog for creating and editing Equated Monthly Installment (EMI) master records
 * for Bank Loans (Home Loan, Auto Loan, Personal Loan) as well as Credit Card EMIs (converted purchases).
 * <p>
 * Captures loan titles, total sanctioned limit / loan principal, current outstanding balances,
 * monthly installment amounts, due amounts, and due day of month.
 * </p>
 */
public class EmiDialog extends JDialog {

    /** The existing EMI entity being modified, or {@code null} if creating a new EMI record. */
    private final Emi emi;

    /** Flag indicating whether the EMI master record was successfully saved. */
    private boolean saved = false;

    /** Radio button for standalone Bank Loans. */
    private JRadioButton rbBankLoan;

    /** Radio button for EMIs running on Credit Cards. */
    private JRadioButton rbCcEmi;

    /** Text input field for the EMI loan title/name. */
    private JTextField txtEmiName;

    /** Container panel for bank lender field (shown for Bank Loans). */
    private JPanel pnlBankContainer;

    /** Text input field for the lending bank or financial institution. */
    private JTextField txtBank;

    /** Container panel for credit card selector (shown for CC EMIs). */
    private JPanel pnlCardContainer;

    /** Dropdown for selecting which Credit Card this EMI runs on. */
    private JComboBox<CreditCard> cmbCreditCard;

    /** Text input field for the total sanctioned loan amount / principal or purchase price. */
    private JTextField txtTotalAmount;

    /** Text input field for remaining principal outstanding balance. */
    private JTextField txtOutstanding;

    /** Text input field for recurring monthly debit installment amount. */
    private JTextField txtMonthlyAmount;

    /** Text input field for current due / overdue payment amount. */
    private JTextField txtDueAmount;

    /** Spinner for monthly payment due day (1-31). */
    private JSpinner spnDueDay;

    /** Informational hint label explaining CC EMI billing integration. */
    private JLabel lblCcInfoHint;

    /** Multi-line text area for loan account numbers, interest rates, or tenure notes. */
    private JTextArea txtNotes;

    /**
     * Constructs an EMI master configuration dialog.
     *
     * @param parent owner frame
     * @param emi existing EMI entity to edit, or {@code null} to create a new one
     */
    public EmiDialog(JFrame parent, Emi emi) {
        super(parent, emi == null ? I18n.get("masters.btn.addEmi") : I18n.get("btn.edit") + " " + I18n.get("masters.table.name"), true);
        this.emi = emi;

        initComponents();
        pack();
        UiUtils.clampDialogToScreen(this);
        setLocationRelativeTo(parent);
    }

    /**
     * Initializes visual form controls, layout constraints, type toggle behaviors, and action buttons.
     */
    private void initComponents() {
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 6, 5, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // 1. Loan Category Toggle: Bank Loan vs Credit Card EMI
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblType = new JLabel(I18n.get("masters.table.loanType") + ":*");
        lblType.setFont(UiUtils.boldFont());
        mainPanel.add(lblType, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        typePanel.setOpaque(false);
        rbBankLoan = new JRadioButton(I18n.get("masters.table.bankLoan"), true);
        rbBankLoan.setFont(UiUtils.boldFont());
        rbCcEmi = new JRadioButton(I18n.get("masters.table.ccEmi"));
        rbCcEmi.setFont(UiUtils.boldFont());

        ButtonGroup bg = new ButtonGroup();
        bg.add(rbBankLoan);
        bg.add(rbCcEmi);
        typePanel.add(rbBankLoan);
        typePanel.add(rbCcEmi);
        mainPanel.add(typePanel, gbc);

        // 2. Loan / EMI Name
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblName = new JLabel(I18n.get("masters.table.name") + ":*");
        lblName.setFont(UiUtils.regularFont());
        mainPanel.add(lblName, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtEmiName = new JTextField(22);
        txtEmiName.setFont(UiUtils.regularFont());
        mainPanel.add(txtEmiName, gbc);

        // 3. Bank / Lender (for Bank Loans)
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblBankTitle = new JLabel(I18n.get("masters.table.bank") + ":");
        lblBankTitle.setFont(UiUtils.regularFont());
        mainPanel.add(lblBankTitle, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        pnlBankContainer = new JPanel(new BorderLayout());
        pnlBankContainer.setOpaque(false);
        txtBank = new JTextField(22);
        txtBank.setFont(UiUtils.regularFont());
        pnlBankContainer.add(txtBank, BorderLayout.CENTER);
        mainPanel.add(pnlBankContainer, gbc);

        // 4. Linked Credit Card (for CC EMIs)
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblCardTitle = new JLabel("Credit Card:*");
        lblCardTitle.setFont(UiUtils.regularFont());
        mainPanel.add(lblCardTitle, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        pnlCardContainer = new JPanel(new BorderLayout());
        pnlCardContainer.setOpaque(false);
        cmbCreditCard = new JComboBox<>();
        cmbCreditCard.setFont(UiUtils.regularFont());
        populateCreditCards();
        pnlCardContainer.add(cmbCreditCard, BorderLayout.CENTER);
        mainPanel.add(pnlCardContainer, gbc);

        // 5. Total Sanctioned Loan Limit / Purchase Price
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblTotalAmt = new JLabel(I18n.get("masters.table.totalLimit") + " (" + AppConfig.getCurrencySymbol() + "):");
        lblTotalAmt.setFont(UiUtils.boldFont());
        mainPanel.add(lblTotalAmt, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtTotalAmount = new JTextField(22);
        txtTotalAmount.setFont(UiUtils.boldFont());
        mainPanel.add(txtTotalAmount, gbc);

        // 6. Remaining Principal Outstanding
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblOut = new JLabel(I18n.get("masters.table.outstanding") + " (" + AppConfig.getCurrencySymbol() + "):");
        lblOut.setFont(UiUtils.boldFont());
        mainPanel.add(lblOut, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtOutstanding = new JTextField(22);
        txtOutstanding.setFont(UiUtils.boldFont());
        mainPanel.add(txtOutstanding, gbc);

        // 7. Monthly Installment Amount
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblMonthlyAmt = new JLabel(I18n.get("masters.table.monthlyAmount") + " (" + AppConfig.getCurrencySymbol() + "):*");
        lblMonthlyAmt.setFont(UiUtils.boldFont());
        mainPanel.add(lblMonthlyAmt, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtMonthlyAmount = new JTextField(22);
        txtMonthlyAmount.setFont(UiUtils.boldFont());
        mainPanel.add(txtMonthlyAmount, gbc);

        // 8. Due Amount & Due Day
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblDue = new JLabel(I18n.get("masters.table.dueAmount") + " / " + I18n.get("masters.table.dueDay") + ":");
        lblDue.setFont(UiUtils.regularFont());
        mainPanel.add(lblDue, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        JPanel dueRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        dueRow.setOpaque(false);
        txtDueAmount = new JTextField(10);
        txtDueAmount.setFont(UiUtils.regularFont());
        spnDueDay = new JSpinner(new SpinnerNumberModel(5, 1, 31, 1));
        spnDueDay.setFont(UiUtils.regularFont());

        dueRow.add(new JLabel(AppConfig.getCurrencySymbol()));
        dueRow.add(txtDueAmount);
        dueRow.add(new JLabel("Day:"));
        dueRow.add(spnDueDay);
        mainPanel.add(dueRow, gbc);

        // 9. CC EMI Information Hint Banner
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        lblCcInfoHint = new JLabel("<html><span style='color:#0369a1'>ℹ️ <b>Billed within Credit Card:</b> CC EMI installments are paid as part of your monthly card statement. They are tracked here for balance and tenure without creating duplicate due bills.</span></html>");
        lblCcInfoHint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblCcInfoHint.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(186, 230, 253), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        lblCcInfoHint.setBackground(new Color(240, 249, 255));
        lblCcInfoHint.setOpaque(true);
        mainPanel.add(lblCcInfoHint, gbc);
        gbc.gridwidth = 1;
        row++;

        // 10. Notes / Account / Remarks
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblNotes = new JLabel(I18n.get("masters.table.notes") + ":");
        lblNotes.setFont(UiUtils.regularFont());
        mainPanel.add(lblNotes, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtNotes = new JTextArea(3, 22);
        txtNotes.setFont(UiUtils.regularFont());
        mainPanel.add(new JScrollPane(txtNotes), gbc);

        add(mainPanel, BorderLayout.CENTER);

        // Action Buttons: Cancel and Save
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        JButton btnCancel = UiUtils.createSecondaryButton(I18n.get("btn.cancel"), NavIcons.cross(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnCancel.setFont(UiUtils.boldFont());
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = UiUtils.createPrimaryButton(I18n.get("btn.save"), NavIcons.check(13, Color.WHITE));
        btnSave.setFont(UiUtils.boldFont());
        btnSave.addActionListener(e -> onSave());

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);
        add(btnPanel, BorderLayout.SOUTH);

        UiUtils.configureDialog(this, btnSave, btnCancel);

        // Type toggle handlers
        rbBankLoan.addActionListener(e -> syncTypeVisibility());
        rbCcEmi.addActionListener(e -> syncTypeVisibility());

        // Prepopulate fields when editing
        if (emi != null) {
            txtEmiName.setText(emi.getEmiName());
            txtBank.setText(emi.getBankLender());
            if (emi.getMonthlyAmount() != null && emi.getMonthlyAmount() > 0) {
                txtMonthlyAmount.setText(String.valueOf(emi.getMonthlyAmount()));
            }
            if (emi.getTotalAmount() != null && emi.getTotalAmount() > 0) {
                txtTotalAmount.setText(String.valueOf(emi.getTotalAmount()));
            }
            if (emi.getOutstandingAmount() != null && emi.getOutstandingAmount() > 0) {
                txtOutstanding.setText(String.valueOf(emi.getOutstandingAmount()));
            }
            if (emi.getDueAmount() != null && emi.getDueAmount() > 0) {
                txtDueAmount.setText(String.valueOf(emi.getDueAmount()));
            }
            if (emi.getDueDay() != null && emi.getDueDay() >= 1 && emi.getDueDay() <= 31) {
                spnDueDay.setValue(emi.getDueDay());
            }
            txtNotes.setText(emi.getNotes());

            if (emi.isCreditCardEmi()) {
                rbCcEmi.setSelected(true);
                selectCreditCardById(emi.getCreditCardId());
            } else {
                rbBankLoan.setSelected(true);
            }
        }

        syncTypeVisibility();
    }

    private void syncTypeVisibility() {
        boolean isCc = rbCcEmi.isSelected();
        pnlBankContainer.setVisible(!isCc);
        pnlCardContainer.setVisible(isCc);
        lblCcInfoHint.setVisible(isCc);
        revalidate();
        repaint();
    }

    private void populateCreditCards() {
        cmbCreditCard.removeAllItems();
        List<CreditCard> cards = FinanceService.getInstance().getAllCreditCards();
        for (CreditCard c : cards) {
            cmbCreditCard.addItem(c);
        }
    }

    private void selectCreditCardById(Long cardId) {
        if (cardId == null) return;
        for (int i = 0; i < cmbCreditCard.getItemCount(); i++) {
            CreditCard c = cmbCreditCard.getItemAt(i);
            if (c != null && c.getId() != null && c.getId().equals(cardId)) {
                cmbCreditCard.setSelectedIndex(i);
                break;
            }
        }
    }

    private double parseDoubleSafe(String s) {
        if (s == null || s.isBlank()) return 0.0;
        try {
            return Double.parseDouble(s.replace(",", "").replace(AppConfig.getCurrencySymbol(), "").trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Validates inputs, persists via {@link FinanceService}, and closes dialog.
     */
    private void onSave() {
        String name = txtEmiName.getText().trim();
        if (name.isEmpty()) {
            UiUtils.showError(this, "Please enter loan or EMI name (e.g. Home Loan EMI, MacBook EMI).");
            txtEmiName.requestFocus();
            return;
        }

        boolean isCc = rbCcEmi.isSelected();
        Long selectedCcId = null;
        String lender = txtBank.getText().trim();

        if (isCc) {
            CreditCard selectedCc = (CreditCard) cmbCreditCard.getSelectedItem();
            if (selectedCc == null) {
                UiUtils.showError(this, "Please select the credit card on which this EMI is running.");
                cmbCreditCard.requestFocus();
                return;
            }
            selectedCcId = selectedCc.getId();
            lender = selectedCc.getCardName() + (selectedCc.getLastDigits() != null ? " (.." + selectedCc.getLastDigits() + ")" : "");
        }

        double monthly = parseDoubleSafe(txtMonthlyAmount.getText().trim());

        Emi e = emi != null ? emi : new Emi();
        e.setEmiName(name);
        e.setBankLender(lender);
        e.setMonthlyAmount(monthly);
        e.setTotalAmount(parseDoubleSafe(txtTotalAmount.getText().trim()));
        e.setOutstandingAmount(parseDoubleSafe(txtOutstanding.getText().trim()));
        e.setDueAmount(parseDoubleSafe(txtDueAmount.getText().trim()));
        e.setDueDay((Integer) spnDueDay.getValue());
        e.setCreditCardEmi(isCc);
        e.setCreditCardId(selectedCcId);
        e.setNotes(txtNotes.getText().trim());

        FinanceService.getInstance().saveEmi(e);
        saved = true;
        dispose();
    }

    /**
     * Indicates whether the EMI master record was persisted.
     *
     * @return {@code true} if saved successfully, {@code false} otherwise
     */
    public boolean isSaved() {
        return saved;
    }
}
