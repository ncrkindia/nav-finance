package com.navfinance.ui.dialogs;

import com.navfinance.i18n.I18n;
import com.navfinance.model.CreditCard;
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
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Modal dialog for registering and editing Credit Card master entries (e.g. HDFC Regalia,
 * ICICI Amazon Pay, SBI Cashback).
 * <p>
 * Allows capturing card labels, issuing financial institutions, and the last 4 card digits
 * for sub-category transaction attribution and payment tracking.
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
     * Initializes visual form components, layout constraints, and buttons.
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

        // Card Name
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblName = new JLabel(I18n.get("masters.table.name") + ":*");
        lblName.setFont(UiUtils.regularFont());
        mainPanel.add(lblName, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtCardName = new JTextField(20);
        txtCardName.setFont(UiUtils.regularFont());
        mainPanel.add(txtCardName, gbc);

        // Bank
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
        if (card != null) {
            txtCardName.setText(card.getCardName());
            txtBank.setText(card.getBank());
            txtLastDigits.setText(card.getLastDigits());
        }
    }

    /**
     * Validates card name, updates the model, saves via {@link FinanceService}, and closes dialog.
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
