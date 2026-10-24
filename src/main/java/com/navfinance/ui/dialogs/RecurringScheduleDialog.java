package com.navfinance.ui.dialogs;

import com.navfinance.config.AppConfig;
import com.navfinance.i18n.I18n;
import com.navfinance.model.AmountType;
import com.navfinance.model.Bill;
import com.navfinance.model.CreditCard;
import com.navfinance.model.Emi;
import com.navfinance.model.Person;
import com.navfinance.model.RecurringSchedule;
import com.navfinance.model.TransactionType;
import com.navfinance.service.FinanceService;
import com.navfinance.service.RecurringService;
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
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
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
 * Modal dialog for registering and configuring recurring monthly schedules (rules)
 * such as EMIs, utility bills, subscriptions, and recurring salaries.
 * <p>
 * Supports day-of-month scheduling, fixed vs. variable amounts, rolling or bounded date ranges,
 * automated debit (NACH) vs. manual payment flags, and immediate projection of due instances.
 * </p>
 */
public class RecurringScheduleDialog extends JDialog {

    /** The existing recurring schedule entity being modified, or {@code null} if new. */
    private final RecurringSchedule existingSchedule;

    /** Service coordinator for recurring schedules and due payment generation. */
    private final RecurringService recurringService = RecurringService.getInstance();

    /** Service coordinator for category masters and payment channels. */
    private final FinanceService financeService = FinanceService.getInstance();

    /** Text input field for schedule name. */
    private JTextField txtName;

    /** Dropdown for selecting transaction direction (EXPENSE or INCOME). */
    private JComboBox<TransactionType> cmbType;

    /** Dropdown for selecting transaction category. */
    private JComboBox<String> cmbCategory;

    /** Editable dropdown for selecting or entering sub-category item. */
    private JComboBox<String> cmbSubCategory;

    /** Spinner for selecting calendar day of month (1 to 31). */
    private JSpinner spnDayOfMonth;

    /** Radio button selecting fixed installment amount. */
    private JRadioButton rbFixed;

    /** Radio button selecting variable bill amount. */
    private JRadioButton rbVariable;

    /** Text input field for fixed default amount or estimated variable amount. */
    private JTextField txtAmount;

    /** Date picker for inception start date. */
    private DatePickerField dpStartDate;

    /** Date picker for optional expiration end date. */
    private DatePickerField dpEndDate;

    /** Checkbox indicating ongoing schedule without an end date. */
    private JCheckBox chkNoEndDate;

    /** Dropdown for selecting payment instrument channel. */
    private JComboBox<String> cmbPaymentVia;

    /** Radio button for automated debit execution. */
    private JRadioButton rbAutoPay;

    /** Radio button for manual user payment execution. */
    private JRadioButton rbManualPay;

    /** Text input field for user remarks or notes. */
    private JTextField txtNotes;

    /** Checkbox indicating active projection state. */
    private JCheckBox chkActive;

    /** Flag indicating whether the schedule configuration was saved successfully. */
    private boolean saved = false;

    /**
     * Constructs a recurring schedule configuration dialog.
     *
     * @param parent owner frame
     * @param schedule existing schedule to edit, or {@code null} to register a new rule
     */
    public RecurringScheduleDialog(JFrame parent, RecurringSchedule schedule) {
        super(parent, schedule == null ? I18n.get("dialog.recurring.addTitle") : I18n.get("dialog.recurring.editTitle"), true);
        this.existingSchedule = schedule;

        setPreferredSize(new Dimension(640, 680));
        setMinimumSize(new Dimension(580, 600));

        initComponents();
        populateData();
        pack();
        setLocationRelativeTo(parent);
    }

    /**
     * Initializes the form fields, layouts, radio button groups, and action buttons.
     */
    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        root.setBackground(new Color(248, 250, 252));

        // 1. TOP HEADER
        JPanel header = new JPanel(new BorderLayout(0, 4));
        header.setOpaque(false);
        JLabel lblTitle = new JLabel(existingSchedule == null ? I18n.get("dialog.recurring.addTitle") : I18n.get("dialog.recurring.editTitle"));
        lblTitle.setIcon(existingSchedule == null ? NavIcons.calendar(20, UiUtils.COLOR_PRIMARY) : NavIcons.edit(20, UiUtils.COLOR_PRIMARY));
        lblTitle.setIconTextGap(8);
        lblTitle.setFont(UiUtils.titleFont());
        lblTitle.setForeground(new Color(15, 23, 42));

        JLabel lblSub = new JLabel("<html>Schedule repeating payments (e.g. EMI of ₹20k on 5th from Jan 26 to Jan 27, or monthly electricity bill on 15th with variable amount).</html>");
        lblSub.setFont(UiUtils.subtitleFont());
        lblSub.setForeground(UiUtils.COLOR_TEXT_MUTED);

        header.add(lblTitle, BorderLayout.NORTH);
        header.add(lblSub, BorderLayout.SOUTH);
        root.add(header, BorderLayout.NORTH);

        // 2. FORM FIELDS
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(UiUtils.createCardBorder());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 10, 6, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // Schedule Name
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        JLabel lblName = new JLabel("Schedule Name:*");
        lblName.setFont(UiUtils.FONT_BOLD);
        form.add(lblName, gbc);

        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 0.7;
        txtName = new JTextField(20);
        txtName.setToolTipText("e.g. Home Loan EMI, Electricity Bill (Noida), Car EMI");
        form.add(txtName, gbc);

        // Type: Expense or Income
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblType = new JLabel("Transaction Type:*");
        lblType.setFont(UiUtils.FONT_BOLD);
        form.add(lblType, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        cmbType = new JComboBox<>(new TransactionType[]{TransactionType.EXPENSE, TransactionType.INCOME});
        cmbType.addActionListener(e -> updateCategories());
        form.add(cmbType, gbc);

        // Category & Sub-Category
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblCat = new JLabel("Category:*");
        lblCat.setFont(UiUtils.FONT_BOLD);
        form.add(lblCat, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        cmbCategory = new JComboBox<>();
        cmbCategory.addActionListener(e -> updateSubCategories());
        form.add(cmbCategory, gbc);

        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblSubCat = new JLabel("Sub-Category:");
        lblSubCat.setFont(UiUtils.FONT_BOLD);
        form.add(lblSubCat, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        cmbSubCategory = new JComboBox<>();
        cmbSubCategory.setEditable(true);
        form.add(cmbSubCategory, gbc);

        // Day of Month (1-31)
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblDay = new JLabel("Day of Month (1 - 31):*");
        lblDay.setFont(UiUtils.FONT_BOLD);
        form.add(lblDay, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        JPanel pnlDay = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlDay.setOpaque(false);
        spnDayOfMonth = new JSpinner(new SpinnerNumberModel(5, 1, 31, 1));
        spnDayOfMonth.setPreferredSize(new Dimension(80, 28));
        pnlDay.add(spnDayOfMonth);
        JLabel lblDayHint = new JLabel("(e.g., 5th of every month)");
        lblDayHint.setFont(UiUtils.FONT_REGULAR);
        lblDayHint.setForeground(UiUtils.COLOR_TEXT_MUTED);
        pnlDay.add(lblDayHint);
        form.add(pnlDay, gbc);

        // Amount Type: Fixed vs Variable
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblAmtType = new JLabel("Amount Type:*");
        lblAmtType.setFont(UiUtils.FONT_BOLD);
        form.add(lblAmtType, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        JPanel pnlAmtType = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnlAmtType.setOpaque(false);
        rbFixed = new JRadioButton("Fixed Amount", true);
        rbFixed.setFont(UiUtils.FONT_BOLD);
        rbFixed.setOpaque(false);

        rbVariable = new JRadioButton("Variable (Ask amount when due)", false);
        rbVariable.setFont(UiUtils.FONT_BOLD);
        rbVariable.setOpaque(false);

        ButtonGroup bgAmt = new ButtonGroup();
        bgAmt.add(rbFixed);
        bgAmt.add(rbVariable);

        pnlAmtType.add(rbFixed);
        pnlAmtType.add(rbVariable);
        form.add(pnlAmtType, gbc);

        rbFixed.addActionListener(e -> updateAmountFieldState());
        rbVariable.addActionListener(e -> updateAmountFieldState());

        // Amount Value
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblAmt = new JLabel("Amount (" + AppConfig.getCurrencySymbol() + "):");
        lblAmt.setFont(UiUtils.FONT_BOLD);
        form.add(lblAmt, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtAmount = new JTextField(12);
        txtAmount.setFont(new Font("Segoe UI", Font.BOLD, 13));
        form.add(txtAmount, gbc);

        // Date Range: Start Date to End Date
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblStart = new JLabel("Start Month / Date:*");
        lblStart.setFont(UiUtils.FONT_BOLD);
        form.add(lblStart, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        dpStartDate = new DatePickerField();
        dpStartDate.setDate(LocalDate.now().withDayOfMonth(1));
        form.add(dpStartDate, gbc);

        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblEnd = new JLabel("End Month / Date:");
        lblEnd.setFont(UiUtils.FONT_BOLD);
        form.add(lblEnd, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        JPanel pnlEnd = new JPanel(new BorderLayout(8, 0));
        pnlEnd.setOpaque(false);
        dpEndDate = new DatePickerField();
        dpEndDate.setDate(LocalDate.now().plusYears(1).withDayOfMonth(1));
        chkNoEndDate = new JCheckBox("Ongoing / No End Date", false);
        chkNoEndDate.setFont(UiUtils.FONT_REGULAR);
        chkNoEndDate.setOpaque(false);
        chkNoEndDate.addActionListener(e -> {
            dpEndDate.setEnabled(!chkNoEndDate.isSelected());
        });
        pnlEnd.add(dpEndDate, BorderLayout.CENTER);
        pnlEnd.add(chkNoEndDate, BorderLayout.EAST);
        form.add(pnlEnd, gbc);

        // Payment Channel
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblVia = new JLabel("Payment Channel:*");
        lblVia.setFont(UiUtils.FONT_BOLD);
        form.add(lblVia, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        cmbPaymentVia = new JComboBox<>();
        for (String m : financeService.getAllPaymentModes()) cmbPaymentVia.addItem(m);
        cmbPaymentVia.setSelectedItem("Net Banking");
        form.add(cmbPaymentVia, gbc);

        // Execution Mode: Auto-Payment vs Manual
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblExec = new JLabel("Execution Mode:*");
        lblExec.setFont(UiUtils.FONT_BOLD);
        form.add(lblExec, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        JPanel pnlExec = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnlExec.setOpaque(false);

        rbAutoPay = new JRadioButton("Auto-Payment (Auto-Debit / NACH)", false);
        rbAutoPay.setFont(UiUtils.FONT_BOLD);
        rbAutoPay.setForeground(new Color(14, 116, 144)); // Cyan
        rbAutoPay.setOpaque(false);

        rbManualPay = new JRadioButton("Manual Payment (User pays directly)", true);
        rbManualPay.setFont(UiUtils.FONT_BOLD);
        rbManualPay.setForeground(new Color(180, 83, 9)); // Amber
        rbManualPay.setOpaque(false);

        ButtonGroup bgExec = new ButtonGroup();
        bgExec.add(rbAutoPay);
        bgExec.add(rbManualPay);

        pnlExec.add(rbAutoPay);
        pnlExec.add(rbManualPay);
        form.add(pnlExec, gbc);

        // Remarks / Notes
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblNotes = new JLabel("Notes / Description:");
        lblNotes.setFont(UiUtils.FONT_BOLD);
        form.add(lblNotes, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtNotes = new JTextField(20);
        form.add(txtNotes, gbc);

        // Active status
        gbc.gridx = 1; gbc.gridy = row++;
        chkActive = new JCheckBox("Active Schedule (Auto-generates due entries)", true);
        chkActive.setFont(UiUtils.FONT_BOLD);
        chkActive.setOpaque(false);
        form.add(chkActive, gbc);

        root.add(form, BorderLayout.CENTER);

        // 3. BOTTOM BUTTONS
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomBar.setOpaque(false);

        JButton btnCancel = UiUtils.createSecondaryButton(I18n.get("btn.cancel"), NavIcons.cross(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnCancel.setFont(UiUtils.boldFont());
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = UiUtils.createPrimaryButton(I18n.get("btn.save"), NavIcons.check(13, Color.WHITE));
        btnSave.setFont(UiUtils.boldFont());
        btnSave.addActionListener(e -> onSave());

        bottomBar.add(btnCancel);
        bottomBar.add(btnSave);
        root.add(bottomBar, BorderLayout.SOUTH);

        add(root, BorderLayout.CENTER);

        updateCategories();
    }

    /**
     * Updates category options based on whether EXPENSE or INCOME is currently selected.
     */
    private void updateCategories() {
        TransactionType type = (TransactionType) cmbType.getSelectedItem();
        cmbCategory.removeAllItems();
        if (type == TransactionType.EXPENSE) {
            cmbCategory.addItem("EMI Payment");
            cmbCategory.addItem("Bill");
            cmbCategory.addItem("Credit Card");
            cmbCategory.addItem("Udhar Given");
            cmbCategory.addItem("Special Expense");
            cmbCategory.addItem("Rent & Housing");
            cmbCategory.addItem("Groceries & Food");
            cmbCategory.addItem("Subscription / OTT");
            cmbCategory.addItem("Insurance Premium");
            cmbCategory.addItem("Other Expense");
        } else {
            cmbCategory.addItem("Salary");
            cmbCategory.addItem("Return");
            cmbCategory.addItem("Loan");
            cmbCategory.addItem("Udhaar");
            cmbCategory.addItem("Rental Income");
            cmbCategory.addItem("Freelance");
            cmbCategory.addItem("Other Income");
        }
        updateSubCategories();
    }

    /**
     * Dynamically populates sub-categories from master entities (Bills, EMIs, Cards, Persons).
     */
    private void updateSubCategories() {
        String cat = (String) cmbCategory.getSelectedItem();
        cmbSubCategory.removeAllItems();
        if (cat == null) return;

        if ("Bill".equalsIgnoreCase(cat)) {
            List<Bill> bills = financeService.getAllBills();
            for (Bill b : bills) cmbSubCategory.addItem(b.getBillName());
        } else if ("EMI Payment".equalsIgnoreCase(cat)) {
            List<Emi> emis = financeService.getAllEmis();
            for (Emi em : emis) cmbSubCategory.addItem(em.getEmiName());
        } else if ("Credit Card".equalsIgnoreCase(cat)) {
            List<CreditCard> cards = financeService.getAllCreditCards();
            for (CreditCard cc : cards) cmbSubCategory.addItem(cc.getCardName());
        } else if ("Udhar Given".equalsIgnoreCase(cat) || "Return".equalsIgnoreCase(cat) || "Udhaar".equalsIgnoreCase(cat)) {
            List<Person> persons = financeService.getAllPersons();
            for (Person p : persons) cmbSubCategory.addItem(p.getName());
        }
    }

    /**
     * Updates input tooltip hints based on fixed vs variable amount selection.
     */
    private void updateAmountFieldState() {
        if (rbVariable.isSelected()) {
            txtAmount.setToolTipText("Optional estimate; user will be prompted for actual amount on due date");
        } else {
            txtAmount.setToolTipText("Fixed amount that recurs every month");
        }
    }

    /**
     * Pre-populates all dialog form fields when editing an existing recurring schedule.
     */
    private void populateData() {
        if (existingSchedule == null) return;

        txtName.setText(existingSchedule.getName());
        cmbType.setSelectedItem(existingSchedule.getType());
        updateCategories();
        cmbCategory.setSelectedItem(existingSchedule.getCategory());
        updateSubCategories();
        if (existingSchedule.getSubCategory() != null) {
            cmbSubCategory.setSelectedItem(existingSchedule.getSubCategory());
        }
        spnDayOfMonth.setValue(existingSchedule.getDayOfMonth());

        if (existingSchedule.getAmountType() == AmountType.VARIABLE) {
            rbVariable.setSelected(true);
        } else {
            rbFixed.setSelected(true);
        }
        updateAmountFieldState();

        if (existingSchedule.getDefaultAmount() > 0) {
            txtAmount.setText(String.format("%.2f", existingSchedule.getDefaultAmount()));
        }

        if (existingSchedule.getStartDate() != null) dpStartDate.setDate(existingSchedule.getStartDate());
        if (existingSchedule.getEndDate() != null) {
            dpEndDate.setDate(existingSchedule.getEndDate());
            chkNoEndDate.setSelected(false);
        } else {
            chkNoEndDate.setSelected(true);
            dpEndDate.setEnabled(false);
        }

        if (existingSchedule.getPaymentVia() != null) cmbPaymentVia.setSelectedItem(existingSchedule.getPaymentVia());

        if (existingSchedule.isAutoPay()) rbAutoPay.setSelected(true);
        else rbManualPay.setSelected(true);

        txtNotes.setText(existingSchedule.getNotes());
        chkActive.setSelected(existingSchedule.isActive());
    }

    /**
     * Validates recurring schedule inputs, persists via {@link RecurringService},
     * and dismisses the dialog.
     */
    private void onSave() {
        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            UiUtils.showError(this, "Please enter a schedule name.");
            txtName.requestFocus();
            return;
        }

        TransactionType type = (TransactionType) cmbType.getSelectedItem();
        String category = (String) cmbCategory.getSelectedItem();
        String subCat = cmbSubCategory.getEditor().getItem() != null ?
                cmbSubCategory.getEditor().getItem().toString().trim() : "";

        int day = (Integer) spnDayOfMonth.getValue();
        AmountType amountType = rbVariable.isSelected() ? AmountType.VARIABLE : AmountType.FIXED;

        double amount = 0.0;
        String strAmount = txtAmount.getText().trim();
        if (!strAmount.isEmpty()) {
            try {
                amount = Double.parseDouble(strAmount.replace(",", "").replace(AppConfig.getCurrencySymbol(), "").trim());
            } catch (NumberFormatException e) {
                UiUtils.showError(this, "Invalid amount format.");
                return;
            }
        }

        if (amountType == AmountType.FIXED && amount <= 0) {
            UiUtils.showError(this, "Fixed amount must be greater than zero.");
            txtAmount.requestFocus();
            return;
        }

        LocalDate startDate = dpStartDate.getDate();
        if (startDate == null) {
            UiUtils.showError(this, "Please select a valid start date.");
            return;
        }

        LocalDate endDate = null;
        if (!chkNoEndDate.isSelected()) {
            endDate = dpEndDate.getDate();
            if (endDate != null && endDate.isBefore(startDate)) {
                UiUtils.showError(this, "End date cannot be earlier than start date.");
                return;
            }
        }

        String via = (String) cmbPaymentVia.getSelectedItem();
        boolean autoPay = rbAutoPay.isSelected();
        String notes = txtNotes.getText().trim();
        boolean active = chkActive.isSelected();

        RecurringSchedule s = existingSchedule != null ? existingSchedule : new RecurringSchedule();
        s.setName(name);
        s.setType(type);
        s.setCategory(category);
        s.setSubCategory(subCat);
        s.setDayOfMonth(day);
        s.setAmountType(amountType);
        s.setDefaultAmount(amount);
        s.setStartDate(startDate);
        s.setEndDate(endDate);
        s.setPaymentVia(via);
        s.setAutoPay(autoPay);
        s.setNotes(notes);
        s.setActive(active);

        RecurringSchedule savedSchedule = recurringService.saveSchedule(s);
        if (savedSchedule != null) {
            saved = true;
            UiUtils.showSuccess(this, "Recurring schedule saved successfully!\nDue occurrences generated.");
            dispose();
        } else {
            UiUtils.showError(this, "Failed to save schedule.");
        }
    }

    /**
     * Indicates whether the recurring schedule was persisted.
     *
     * @return {@code true} if saved successfully, {@code false} otherwise
     */
    public boolean isSaved() {
        return saved;
    }
}
