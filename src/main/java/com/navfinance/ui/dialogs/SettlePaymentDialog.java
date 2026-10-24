package com.navfinance.ui.dialogs;

import com.navfinance.config.AppConfig;
import com.navfinance.model.DuePayment;
import com.navfinance.service.FinanceService;
import com.navfinance.service.RecurringService;
import com.navfinance.ui.components.DatePickerField;
import com.navfinance.ui.util.NavIcons;
import com.navfinance.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.List;

/**
 * Modal dialog for settling scheduled due payments and recurring bills.
 * <p>
 * Handles final billing amounts (particularly for variable electricity and credit card bills),
 * clearance dates, payment instruments (UPI, Net Banking, Cash), and optional automatic
 * creation of linked ledger transactions.
 * </p>
 */
public class SettlePaymentDialog extends JDialog {

    /** The due payment instance being settled. */
    private final DuePayment payment;

    /** Service coordinator for recurring payments and settlements. */
    private final RecurringService recurringService = RecurringService.getInstance();

    /** Service layer for financial transactions and payment modes. */
    private final FinanceService financeService = FinanceService.getInstance();

    /** Text input field for the actual cleared monetary amount. */
    private JTextField txtAmount;

    /** Date picker for selecting the payment settlement date. */
    private DatePickerField dpSettleDate;

    /** Dropdown list for selecting the payment channel or instrument. */
    private JComboBox<String> cmbPaymentVia;

    /** Checkbox indicating whether to automatically record a linked transaction in the ledger. */
    private JCheckBox chkCreateTx;

    /** Text input field for remarks, transaction IDs, or settlement notes. */
    private JTextField txtNotes;

    /** Flag indicating whether the payment was successfully marked as settled. */
    private boolean saved = false;

    /**
     * Constructs a settlement dialog for a specific due payment.
     *
     * @param parent owner window frame
     * @param payment due payment record to settle
     */
    public SettlePaymentDialog(JFrame parent, DuePayment payment) {
        super(parent, "Settle Payment - " + payment.getTitle(), true);
        this.payment = payment;

        setPreferredSize(new Dimension(520, 480));
        setMinimumSize(new Dimension(480, 440));
        initComponents();
        pack();
        setLocationRelativeTo(parent);
    }

    /**
     * Initializes header banner, settlement parameter form controls, and confirmation buttons.
     */
    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        root.setBackground(new Color(248, 250, 252));

        // 1. TOP BANNER
        JPanel banner = new JPanel(new BorderLayout(0, 6));
        banner.setBackground(Color.WHITE);
        banner.setBorder(UiUtils.createCardBorder());

        JLabel lblTitle = new JLabel("Settle Due Payment", NavIcons.check(18, UiUtils.COLOR_INCOME), JLabel.LEFT);
        lblTitle.setFont(UiUtils.FONT_TITLE);
        lblTitle.setForeground(UiUtils.COLOR_INCOME);

        String modeBadge = payment.isAutoPay() ? "[Auto-Debit]" : "[Manual]";
        String dueInfo = String.format("<html><b>%s</b> | Due: <b>%s</b> | %s | %s</html>",
                payment.getTitle(),
                payment.getDueDate() != null ? payment.getDueDate().format(AppConfig.DATE_FORMATTER) : "N/A",
                payment.getCategory(),
                modeBadge);
        JLabel lblSub = new JLabel(dueInfo);
        lblSub.setFont(UiUtils.FONT_SUBTITLE);
        lblSub.setForeground(new Color(51, 65, 85));

        banner.add(lblTitle, BorderLayout.NORTH);
        banner.add(lblSub, BorderLayout.SOUTH);
        root.add(banner, BorderLayout.NORTH);

        // 2. FORM FIELDS
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(UiUtils.createCardBorder());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // If Variable, show prominent prompt
        if (payment.isVariable()) {
            gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
            JLabel lblVarNotice = new JLabel("<html><b style='color:#0284c7;'>Variable Amount Bill:</b> Please enter the exact billed amount for this period:</html>");
            lblVarNotice.setIcon(NavIcons.bill(16, new Color(2, 132, 199)));
            lblVarNotice.setFont(UiUtils.FONT_REGULAR);
            form.add(lblVarNotice, gbc);
            gbc.gridwidth = 1;
        }

        // Amount Field
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblAmt = new JLabel("Actual Amount (" + AppConfig.getCurrencySymbol() + "):*");
        lblAmt.setFont(UiUtils.FONT_BOLD);
        form.add(lblAmt, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtAmount = new JTextField(12);
        txtAmount.setFont(new Font("Segoe UI", Font.BOLD, 14));
        if (payment.getExpectedAmount() > 0) {
            txtAmount.setText(String.format("%.2f", payment.getExpectedAmount()));
        }
        form.add(txtAmount, gbc);

        // Settlement Date (Picker + Manual)
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblDate = new JLabel("Settlement Date:*");
        lblDate.setFont(UiUtils.FONT_BOLD);
        form.add(lblDate, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        dpSettleDate = new DatePickerField();
        // default to today or due date if today is before due date
        dpSettleDate.setDate(LocalDate.now());
        form.add(dpSettleDate, gbc);

        // Payment Via
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblVia = new JLabel("Paid Via:*");
        lblVia.setFont(UiUtils.FONT_BOLD);
        form.add(lblVia, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        cmbPaymentVia = new JComboBox<>();
        List<String> modes = financeService.getAllPaymentModes();
        for (String m : modes) cmbPaymentVia.addItem(m);
        cmbPaymentVia.setSelectedItem(payment.isAutoPay() ? "Net Banking" : "UPI");
        form.add(cmbPaymentVia, gbc);

        // Notes
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblNotes = new JLabel("Remarks / Reference:");
        lblNotes.setFont(UiUtils.FONT_BOLD);
        form.add(lblNotes, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtNotes = new JTextField(15);
        txtNotes.setText(payment.isAutoPay() ? "Auto-debited from bank account" : "Paid manually");
        form.add(txtNotes, gbc);

        // Checkbox: Create Transaction in Ledger
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        chkCreateTx = new JCheckBox("Record as transaction in Expenses/Income ledger", true);
        chkCreateTx.setFont(UiUtils.FONT_BOLD);
        chkCreateTx.setForeground(UiUtils.COLOR_PRIMARY);
        chkCreateTx.setOpaque(false);
        form.add(chkCreateTx, gbc);

        root.add(form, BorderLayout.CENTER);

        // 3. BOTTOM BUTTONS
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomBar.setOpaque(false);

        JButton btnCancel = UiUtils.createSecondaryButton("Cancel", NavIcons.cross(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnCancel.addActionListener(e -> dispose());

        JButton btnConfirm = UiUtils.createSuccessButton("Confirm Settlement", NavIcons.check(13, Color.WHITE));
        btnConfirm.addActionListener(e -> onConfirmSettle());

        bottomBar.add(btnCancel);
        bottomBar.add(btnConfirm);
        root.add(bottomBar, BorderLayout.SOUTH);

        add(root, BorderLayout.CENTER);
    }

    /**
     * Validates input values, invokes {@link RecurringService#settlePayment},
     * optionally generates linked ledger entries, and closes the dialog.
     */
    private void onConfirmSettle() {
        String strAmount = txtAmount.getText().trim();
        if (strAmount.isEmpty()) {
            UiUtils.showError(this, "Please enter the settlement amount.");
            txtAmount.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(strAmount.replace(",", "").replace(AppConfig.getCurrencySymbol(), "").trim());
            if (amount <= 0) {
                UiUtils.showError(this, "Settlement amount must be greater than zero.");
                return;
            }
        } catch (NumberFormatException e) {
            UiUtils.showError(this, "Please enter a valid numeric amount.");
            return;
        }

        LocalDate settleDate = dpSettleDate.getDate();
        if (settleDate == null) {
            UiUtils.showError(this, "Please enter a valid settlement date.");
            return;
        }

        String via = (String) cmbPaymentVia.getSelectedItem();
        boolean createTx = chkCreateTx.isSelected();
        String notes = txtNotes.getText().trim();

        boolean ok = recurringService.settlePayment(payment.getId(), amount, settleDate, via, createTx, notes);
        if (ok) {
            saved = true;
            UiUtils.showSuccess(this, "Payment settled successfully!\nAmount: " + AppConfig.formatCurrency(amount));
            dispose();
        } else {
            UiUtils.showError(this, "Failed to settle payment. Please try again.");
        }
    }

    /**
     * Indicates whether the payment was successfully settled.
     *
     * @return {@code true} if settled, {@code false} otherwise
     */
    public boolean isSaved() {
        return saved;
    }
}
