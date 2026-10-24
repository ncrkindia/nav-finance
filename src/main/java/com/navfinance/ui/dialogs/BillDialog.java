package com.navfinance.ui.dialogs;

import com.navfinance.config.AppConfig;
import com.navfinance.i18n.I18n;
import com.navfinance.model.Bill;
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
 * Modal dialog for creating and modifying Bill master records (e.g., Airtel Broadband,
 * Electricity Khardoni, Noida Power).
 * <p>
 * Captures bill titles, service providers, estimated recurring amounts, and user remarks,
 * validating required inputs before delegating persistence to {@link FinanceService}.
 * </p>
 */
public class BillDialog extends JDialog {

    /** The existing bill entity being modified, or {@code null} if creating a new bill. */
    private final Bill bill;

    /** Flag indicating whether the bill was successfully saved. */
    private boolean saved = false;

    /** Text input field for the bill identifier/name. */
    private JTextField txtBillName;

    /** Text input field for the utility company or provider agency. */
    private JTextField txtProvider;

    /** Text input field for typical expected monthly charge. */
    private JTextField txtDefaultAmount;

    /** Multi-line text area for operational account numbers or notes. */
    private JTextArea txtNotes;

    /**
     * Constructs a Bill management dialog.
     *
     * @param parent owner frame
     * @param bill existing bill entity to edit, or {@code null} to create a new one
     */
    public BillDialog(JFrame parent, Bill bill) {
        super(parent, bill == null ? I18n.get("masters.btn.addBill") : I18n.get("btn.edit") + " " + I18n.get("masters.table.name"), true);
        this.bill = bill;

        initComponents();
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    /**
     * Initializes visual form components, layout constraints, and action buttons.
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

        // Bill Name (e.g. Airtel Bill, Electricity (Khardoni), Electricity (Noida))
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblName = new JLabel(I18n.get("masters.table.name") + ":*");
        lblName.setFont(UiUtils.regularFont());
        mainPanel.add(lblName, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtBillName = new JTextField(20);
        txtBillName.setFont(UiUtils.regularFont());
        mainPanel.add(txtBillName, gbc);

        // Provider / Agency
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblProvider = new JLabel(I18n.get("masters.table.provider") + ":");
        lblProvider.setFont(UiUtils.regularFont());
        mainPanel.add(lblProvider, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtProvider = new JTextField(20);
        txtProvider.setFont(UiUtils.regularFont());
        mainPanel.add(txtProvider, gbc);

        // Typical / Default Amount
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblAmt = new JLabel(I18n.get("masters.table.defaultAmount") + " (" + AppConfig.getCurrencySymbol() + "):");
        lblAmt.setFont(UiUtils.regularFont());
        mainPanel.add(lblAmt, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtDefaultAmount = new JTextField(20);
        txtDefaultAmount.setFont(UiUtils.regularFont());
        mainPanel.add(txtDefaultAmount, gbc);

        // Notes / Remarks
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


        // Pre-populate fields when in edit mode
        if (bill != null) {
            txtBillName.setText(bill.getBillName());
            txtProvider.setText(bill.getProvider());
            if (bill.getDefaultAmount() != null) {
                txtDefaultAmount.setText(String.valueOf(bill.getDefaultAmount()));
            }
            txtNotes.setText(bill.getNotes());
        }
    }

    /**
     * Validates user inputs, builds or updates the {@link Bill} entity, persists it
     * via {@link FinanceService}, and dismisses the dialog.
     */
    private void onSave() {
        String name = txtBillName.getText().trim();
        if (name.isEmpty()) {
            UiUtils.showError(this, "Please enter bill name (e.g. Airtel Bill, Electricity (Khardoni)).");
            txtBillName.requestFocus();
            return;
        }

        Double amt = null;
        String strAmt = txtDefaultAmount.getText().trim();
        if (!strAmt.isEmpty()) {
            try {
                amt = Double.valueOf(strAmt);
            } catch (NumberFormatException e) {
                UiUtils.showError(this, "Invalid amount format.");
                txtDefaultAmount.requestFocus();
                return;
            }
        }

        Bill b = bill != null ? bill : new Bill();
        b.setBillName(name);
        b.setProvider(txtProvider.getText().trim());
        b.setDefaultAmount(amt);
        b.setNotes(txtNotes.getText().trim());

        FinanceService.getInstance().saveBill(b);
        saved = true;
        dispose();
    }

    /**
     * Indicates whether the bill record was persisted.
     *
     * @return {@code true} if saved successfully, {@code false} otherwise
     */
    public boolean isSaved() {
        return saved;
    }
}
