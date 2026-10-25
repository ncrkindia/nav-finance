package com.navfinance.ui.dialogs;

import com.navfinance.config.AppConfig;
import com.navfinance.i18n.I18n;
import com.navfinance.model.Emi;
import com.navfinance.service.FinanceService;
import com.navfinance.ui.util.NavIcons;
import com.navfinance.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.Color;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Modal dialog for creating and editing Equated Monthly Installment (EMI) master records
 * (e.g. Home Loan EMI, Car Loan EMI, Personal Loan EMI).
 * <p>
 * Captures loan titles, financing bank/lender names, fixed monthly installment figures,
 * and operational tenure or account remarks before persisting through {@link FinanceService}.
 * </p>
 */
public class EmiDialog extends JDialog {

    /** The existing EMI entity being modified, or {@code null} if creating a new EMI record. */
    private final Emi emi;

    /** Flag indicating whether the EMI master record was successfully saved. */
    private boolean saved = false;

    /** Text input field for the EMI loan title/name. */
    private JTextField txtEmiName;

    /** Text input field for the lending bank or financial institution. */
    private JTextField txtBank;

    /** Text input field for the recurring monthly debit amount. */
    private JTextField txtMonthlyAmount;

    /** Multi-line text area for loan account numbers or tenure notes. */
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
     * Initializes visual form controls, layout constraints, and action buttons.
     */
    private void initComponents() {
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // EMI Name (e.g. Home Loan EMI, Car Loan EMI, Laptop EMI)
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblName = new JLabel(I18n.get("masters.table.name") + ":*");
        lblName.setFont(UiUtils.regularFont());
        mainPanel.add(lblName, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtEmiName = new JTextField(20);
        txtEmiName.setFont(UiUtils.regularFont());
        mainPanel.add(txtEmiName, gbc);

        // Bank / Lender
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblBank = new JLabel(I18n.get("masters.table.bank") + ":");
        lblBank.setFont(UiUtils.regularFont());
        mainPanel.add(lblBank, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtBank = new JTextField(20);
        txtBank.setFont(UiUtils.regularFont());
        mainPanel.add(txtBank, gbc);

        // Monthly Amount
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblAmt = new JLabel(I18n.get("masters.table.monthlyAmount") + " (" + AppConfig.getCurrencySymbol() + "):");
        lblAmt.setFont(UiUtils.regularFont());
        mainPanel.add(lblAmt, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtMonthlyAmount = new JTextField(20);
        txtMonthlyAmount.setFont(UiUtils.regularFont());
        mainPanel.add(txtMonthlyAmount, gbc);

        // Notes / Tenure
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblNotes = new JLabel(I18n.get("masters.table.notes") + ":");
        lblNotes.setFont(UiUtils.regularFont());
        mainPanel.add(lblNotes, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtNotes = new JTextArea(3, 20);
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


        // Prepopulate fields when editing
        if (emi != null) {
            txtEmiName.setText(emi.getEmiName());
            txtBank.setText(emi.getBankLender());
            if (emi.getMonthlyAmount() != null) {
                txtMonthlyAmount.setText(String.valueOf(emi.getMonthlyAmount()));
            }
            txtNotes.setText(emi.getNotes());
        }
    }

    /**
     * Validates required inputs, parses monthly amount, saves via {@link FinanceService}, and closes.
     */
    private void onSave() {
        String name = txtEmiName.getText().trim();
        if (name.isEmpty()) {
            UiUtils.showError(this, "Please enter EMI name (e.g. Home Loan EMI, Car Loan EMI).");
            txtEmiName.requestFocus();
            return;
        }

        Double amt = null;
        String strAmt = txtMonthlyAmount.getText().trim();
        if (!strAmt.isEmpty()) {
            try {
                amt = Double.valueOf(strAmt);
            } catch (NumberFormatException e) {
                UiUtils.showError(this, "Invalid amount format.");
                txtMonthlyAmount.requestFocus();
                return;
            }
        }

        Emi e = emi != null ? emi : new Emi();
        e.setEmiName(name);
        e.setBankLender(txtBank.getText().trim());
        e.setMonthlyAmount(amt);
        e.setNotes(txtNotes.getText().trim());

        FinanceService.getInstance().saveEmi(e);
        saved = true;
        dispose();
    }

    /**
     * Indicates whether the EMI record was persisted.
     *
     * @return {@code true} if saved successfully, {@code false} otherwise
     */
    public boolean isSaved() {
        return saved;
    }
}
