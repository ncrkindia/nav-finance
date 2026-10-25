package com.navfinance.ui.dialogs;

import com.navfinance.config.AppConfig;
import com.navfinance.i18n.I18n;
import com.navfinance.model.CreditCard;
import com.navfinance.model.Emi;
import com.navfinance.service.FinanceService;
import com.navfinance.ui.util.NavIcons;
import com.navfinance.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

/**
 * Modal dialog for registering and editing Credit Card master entries (e.g. HDFC Regalia,
 * ICICI Amazon Pay, SBI Cashback).
 * <p>
 * Captures card details, credit limits, outstanding utilized balances, statement due amounts,
 * billing/payment due dates, and displays real-time available credit &amp; utilization insights.
 * </p>
 */
public class CreditCardDialog extends JDialog {

    /** The existing credit card entity being edited, or {@code null} if adding a new card. */
    private final CreditCard card;

    /** Flag indicating whether the credit card was successfully saved. */
    private boolean saved = false;

    /** Text input field for the card branding name. */
    private JTextField txtCardName;

    /** Text input field for the card issuing bank. */
    private JTextField txtBank;

    /** Text input field for the last 4 digits of the card number. */
    private JTextField txtLastDigits;

    /** Text input field for the total sanctioned credit limit. */
    private JTextField txtTotalLimit;

    /** Text input field for the current utilized / outstanding balance. */
    private JTextField txtOutstanding;

    /** Text input field for the current statement bill due amount. */
    private JTextField txtDueAmount;

    /** Spinner for monthly bill payment due day (1-31). */
    private JSpinner spnDueDay;

    /** Spinner for monthly billing cycle / statement generation day (1-31). */
    private JSpinner spnBillingDay;

    /** Label displaying dynamically computed available credit limit. */
    private JLabel lblAvailableLimit;

    /** Label displaying dynamically computed utilization percentage. */
    private JLabel lblUtilization;

    /**
     * Constructs a credit card configuration dialog.
     *
     * @param parent owner frame
     * @param card existing card to edit, or {@code null} to add a new card
     */
    public CreditCardDialog(JFrame parent, CreditCard card) {
        super(parent, card == null ? I18n.get("masters.btn.addCard") : I18n.get("btn.edit") + " " + I18n.get("masters.table.name"), true);
        this.card = card;

        initComponents();
        pack();
        UiUtils.clampDialogToScreen(this);
        setLocationRelativeTo(parent);
    }

    /**
     * Initializes visual form components, layout constraints, live computation listeners, and buttons.
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

        // Card Name
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblName = new JLabel(I18n.get("masters.table.name") + ":*");
        lblName.setFont(UiUtils.regularFont());
        mainPanel.add(lblName, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtCardName = new JTextField(20);
        txtCardName.setFont(UiUtils.regularFont());
        mainPanel.add(txtCardName, gbc);

        // Bank / Provider
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblBank = new JLabel(I18n.get("masters.table.bank") + ":");
        lblBank.setFont(UiUtils.regularFont());
        mainPanel.add(lblBank, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtBank = new JTextField(20);
        txtBank.setFont(UiUtils.regularFont());
        mainPanel.add(txtBank, gbc);

        // Last 4 Digits
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblDigits = new JLabel(I18n.get("masters.table.lastDigits") + ":");
        lblDigits.setFont(UiUtils.regularFont());
        mainPanel.add(lblDigits, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtLastDigits = new JTextField(6);
        txtLastDigits.setFont(UiUtils.regularFont());
        mainPanel.add(txtLastDigits, gbc);

        // Total Credit Limit
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblLimit = new JLabel(I18n.get("masters.table.totalLimit") + " (" + AppConfig.getCurrencySymbol() + "):");
        lblLimit.setFont(UiUtils.boldFont());
        mainPanel.add(lblLimit, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtTotalLimit = new JTextField(20);
        txtTotalLimit.setFont(UiUtils.boldFont());
        mainPanel.add(txtTotalLimit, gbc);

        // Outstanding / Utilized Amount
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblOut = new JLabel(I18n.get("masters.table.outstanding") + " (" + AppConfig.getCurrencySymbol() + "):");
        lblOut.setFont(UiUtils.boldFont());
        mainPanel.add(lblOut, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtOutstanding = new JTextField(20);
        txtOutstanding.setFont(UiUtils.boldFont());
        mainPanel.add(txtOutstanding, gbc);

        // Current Due / Bill Amount
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblDue = new JLabel(I18n.get("masters.table.dueAmount") + " (" + AppConfig.getCurrencySymbol() + "):");
        lblDue.setFont(UiUtils.regularFont());
        mainPanel.add(lblDue, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtDueAmount = new JTextField(20);
        txtDueAmount.setFont(UiUtils.regularFont());
        mainPanel.add(txtDueAmount, gbc);

        // Billing Cycle & Payment Due Days (in a single row)
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblDays = new JLabel(I18n.get("masters.table.dueDay") + " / " + I18n.get("masters.table.billingDay") + ":");
        lblDays.setFont(UiUtils.regularFont());
        mainPanel.add(lblDays, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        JPanel daysPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        daysPanel.setOpaque(false);

        daysPanel.add(new JLabel("Due:"));
        spnDueDay = new JSpinner(new SpinnerNumberModel(5, 1, 31, 1));
        spnDueDay.setFont(UiUtils.regularFont());
        daysPanel.add(spnDueDay);

        daysPanel.add(new JLabel("Bill Cycle:"));
        spnBillingDay = new JSpinner(new SpinnerNumberModel(15, 1, 31, 1));
        spnBillingDay.setFont(UiUtils.regularFont());
        daysPanel.add(spnBillingDay);
        mainPanel.add(daysPanel, gbc);

        // Dynamic Calculations Banner (Available Credit & Utilization)
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        JPanel summaryCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        summaryCard.setBackground(new Color(241, 245, 249));
        summaryCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        lblAvailableLimit = new JLabel("Available: " + AppConfig.formatCurrency(0.0));
        lblAvailableLimit.setFont(UiUtils.boldFont());
        lblAvailableLimit.setForeground(UiUtils.COLOR_INCOME);

        lblUtilization = new JLabel("Utilization: 0.0%");
        lblUtilization.setFont(UiUtils.boldFont());
        lblUtilization.setForeground(UiUtils.COLOR_TEXT_PRIMARY);

        summaryCard.add(lblAvailableLimit);
        summaryCard.add(lblUtilization);
        mainPanel.add(summaryCard, gbc);
        gbc.gridwidth = 1;
        row++;

        // If card exists, display active CC EMIs overview
        if (card != null && card.getId() != null) {
            List<Emi> cardEmis = FinanceService.getInstance().getEmisByCreditCard(card.getId());
            if (!cardEmis.isEmpty()) {
                gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
                double totalEmiMonthly = cardEmis.stream().mapToDouble(e -> e.getMonthlyAmount() != null ? e.getMonthlyAmount() : 0.0).sum();
                double totalEmiOut = cardEmis.stream().mapToDouble(e -> e.getOutstandingAmount() != null ? e.getOutstandingAmount() : 0.0).sum();

                JLabel lblEmiInfo = new JLabel("<html>💳 <b>" + cardEmis.size() + " Active CC EMIs running:</b> "
                        + "Monthly commitment: <b>" + AppConfig.formatCurrency(totalEmiMonthly) + "</b> | "
                        + "Outstanding EMI: <b>" + AppConfig.formatCurrency(totalEmiOut) + "</b><br>"
                        + "<span style='color:#64748b;font-size:11px'>Note: CC EMI installments are billed inside this card's monthly statement.</span></html>");
                lblEmiInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                lblEmiInfo.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
                mainPanel.add(lblEmiInfo, gbc);
                gbc.gridwidth = 1;
            }
        }

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

        // Live calculation listener
        DocumentListener docListener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateLiveMetrics(); }
            @Override public void removeUpdate(DocumentEvent e) { updateLiveMetrics(); }
            @Override public void changedUpdate(DocumentEvent e) { updateLiveMetrics(); }
        };
        txtTotalLimit.getDocument().addDocumentListener(docListener);
        txtOutstanding.getDocument().addDocumentListener(docListener);

        // Prepopulate fields when editing
        if (card != null) {
            txtCardName.setText(card.getCardName());
            txtBank.setText(card.getBank());
            txtLastDigits.setText(card.getLastDigits());
            if (card.getTotalLimit() != null && card.getTotalLimit() > 0) {
                txtTotalLimit.setText(String.valueOf(card.getTotalLimit()));
            }
            if (card.getOutstandingAmount() != null && card.getOutstandingAmount() > 0) {
                txtOutstanding.setText(String.valueOf(card.getOutstandingAmount()));
            }
            if (card.getDueAmount() != null && card.getDueAmount() > 0) {
                txtDueAmount.setText(String.valueOf(card.getDueAmount()));
            }
            if (card.getDueDay() != null && card.getDueDay() >= 1 && card.getDueDay() <= 31) {
                spnDueDay.setValue(card.getDueDay());
            }
            if (card.getBillingDay() != null && card.getBillingDay() >= 1 && card.getBillingDay() <= 31) {
                spnBillingDay.setValue(card.getBillingDay());
            }
        }

        updateLiveMetrics();
    }

    /**
     * Dynamically recalibrates available credit balance and card utilization percentage.
     */
    private void updateLiveMetrics() {
        double limit = parseDoubleSafe(txtTotalLimit.getText().trim());
        double out = parseDoubleSafe(txtOutstanding.getText().trim());
        double avail = Math.max(0.0, limit - out);
        lblAvailableLimit.setText("Available: " + AppConfig.formatCurrency(avail));

        if (limit > 0.0) {
            double pct = (out / limit) * 100.0;
            lblUtilization.setText(String.format("Utilization: %.1f%%", pct));
            if (pct <= 30.0) {
                lblUtilization.setForeground(new Color(22, 101, 52)); // Green
            } else if (pct <= 70.0) {
                lblUtilization.setForeground(new Color(180, 83, 9)); // Amber
            } else {
                lblUtilization.setForeground(UiUtils.COLOR_EXPENSE); // Red
            }
        } else {
            lblUtilization.setText("Utilization: 0.0%");
            lblUtilization.setForeground(UiUtils.COLOR_TEXT_PRIMARY);
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
        String name = txtCardName.getText().trim();
        if (name.isEmpty()) {
            UiUtils.showError(this, "Please enter card name (e.g. HDFC Millennia).");
            txtCardName.requestFocus();
            return;
        }

        CreditCard c = card != null ? card : new CreditCard();
        c.setCardName(name);
        c.setBank(txtBank.getText().trim());
        c.setLastDigits(txtLastDigits.getText().trim());
        c.setTotalLimit(parseDoubleSafe(txtTotalLimit.getText().trim()));
        c.setOutstandingAmount(parseDoubleSafe(txtOutstanding.getText().trim()));
        c.setDueAmount(parseDoubleSafe(txtDueAmount.getText().trim()));
        c.setDueDay((Integer) spnDueDay.getValue());
        c.setBillingDay((Integer) spnBillingDay.getValue());

        FinanceService.getInstance().saveCreditCard(c);
        saved = true;
        dispose();
    }

    /**
     * Indicates whether the credit card record was persisted.
     *
     * @return {@code true} if saved successfully, {@code false} otherwise
     */
    public boolean isSaved() {
        return saved;
    }
}
