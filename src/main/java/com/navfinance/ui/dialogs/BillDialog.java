package com.navfinance.ui.dialogs;

import com.navfinance.config.AppConfig;
import com.navfinance.i18n.I18n;
import com.navfinance.model.AmountType;
import com.navfinance.model.Bill;
import com.navfinance.service.FinanceService;
import com.navfinance.ui.util.NavIcons;
import com.navfinance.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Modal dialog for creating and modifying Bill master records (e.g., Airtel Broadband,
 * Electricity Khardoni, Noida Power).
 * <p>
 * Captures bill titles, service providers, amount classification (Fixed vs Variable),
 * estimated recurring amounts, due day of month, and remarks,
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

    /** Radio button selecting Fixed amount type. */
    private JRadioButton rbFixed;

    /** Radio button selecting Variable (usage-based) amount type. */
    private JRadioButton rbVariable;

    /** Text input field for typical expected monthly charge. */
    private JTextField txtDefaultAmount;

    /** Spinner for monthly recurring due day (1-31, 0 if flexible). */
    private JSpinner spnDueDay;

    /** Text input field for current unpaid / pending bill amount. */
    private JTextField txtDueAmount;

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
        UiUtils.clampDialogToScreen(this);
        setLocationRelativeTo(parent);
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

        // Amount Classification (Fixed vs Variable)
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblType = new JLabel(I18n.get("masters.table.billType") + ":*");
        lblType.setFont(UiUtils.regularFont());
        mainPanel.add(lblType, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        JPanel pnlType = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnlType.setOpaque(false);
        rbFixed = new JRadioButton("Fixed Amount", true);
        rbFixed.setFont(UiUtils.regularFont());
        rbFixed.setOpaque(false);
        rbVariable = new JRadioButton("Variable / Usage-based", false);
        rbVariable.setFont(UiUtils.regularFont());
        rbVariable.setOpaque(false);
        ButtonGroup bgType = new ButtonGroup();
        bgType.add(rbFixed);
        bgType.add(rbVariable);
        pnlType.add(rbFixed);
        pnlType.add(rbVariable);
        mainPanel.add(pnlType, gbc);

        // Typical / Default Amount
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblAmt = new JLabel(I18n.get("masters.table.defaultAmount") + " (" + AppConfig.getCurrencySymbol() + "):");
        lblAmt.setFont(UiUtils.regularFont());
        mainPanel.add(lblAmt, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtDefaultAmount = new JTextField(20);
        txtDefaultAmount.setFont(UiUtils.regularFont());
        mainPanel.add(txtDefaultAmount, gbc);

        // Typical Due Day of Month (1 - 31)
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblDueDay = new JLabel(I18n.get("masters.table.dueDay") + ":");
        lblDueDay.setFont(UiUtils.regularFont());
        mainPanel.add(lblDueDay, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        JPanel pnlDueDay = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlDueDay.setOpaque(false);
        spnDueDay = new JSpinner(new SpinnerNumberModel(0, 0, 31, 1));
        spnDueDay.setPreferredSize(new Dimension(80, 28));
        pnlDueDay.add(spnDueDay);
        JLabel lblDueDayHint = new JLabel("(1 - 31, 0 if flexible / on arrival)");
        lblDueDayHint.setFont(UiUtils.regularFont());
        lblDueDayHint.setForeground(UiUtils.COLOR_TEXT_MUTED);
        pnlDueDay.add(lblDueDayHint);
        mainPanel.add(pnlDueDay, gbc);

        // Current Due / Pending Amount
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblDueAmt = new JLabel(I18n.get("masters.table.dueAmount") + " (" + AppConfig.getCurrencySymbol() + "):");
        lblDueAmt.setFont(UiUtils.regularFont());
        mainPanel.add(lblDueAmt, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtDueAmount = new JTextField(20);
        txtDueAmount.setFont(UiUtils.regularFont());
        mainPanel.add(txtDueAmount, gbc);

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

        UiUtils.configureDialog(this, btnSave, btnCancel);


        // Pre-populate fields when in edit mode
        if (bill != null) {
            txtBillName.setText(bill.getBillName());
            txtProvider.setText(bill.getProvider());
            if (bill.getAmountType() == AmountType.VARIABLE) {
                rbVariable.setSelected(true);
            } else {
                rbFixed.setSelected(true);
            }
            if (bill.getDefaultAmount() != null) {
                txtDefaultAmount.setText(String.valueOf(bill.getDefaultAmount()));
            }
            if (bill.getDueDay() != null) {
                spnDueDay.setValue(Math.max(0, Math.min(31, bill.getDueDay())));
            }
            if (bill.getDueAmount() != null && bill.getDueAmount() > 0) {
                txtDueAmount.setText(String.valueOf(bill.getDueAmount()));
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

        Double dueAmt = 0.0;
        String strDueAmt = txtDueAmount.getText().trim();
        if (!strDueAmt.isEmpty()) {
            try {
                dueAmt = Double.valueOf(strDueAmt);
            } catch (NumberFormatException e) {
                UiUtils.showError(this, "Invalid due amount format.");
                txtDueAmount.requestFocus();
                return;
            }
        }

        Bill b = bill != null ? bill : new Bill();
        b.setBillName(name);
        b.setProvider(txtProvider.getText().trim());
        b.setAmountType(rbVariable.isSelected() ? AmountType.VARIABLE : AmountType.FIXED);
        b.setDefaultAmount(amt);
        b.setDueDay(((Number) spnDueDay.getValue()).intValue());
        b.setDueAmount(dueAmt);
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
