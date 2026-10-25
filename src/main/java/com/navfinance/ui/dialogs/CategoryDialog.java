package com.navfinance.ui.dialogs;

import com.navfinance.i18n.I18n;
import com.navfinance.model.Category;
import com.navfinance.model.TransactionType;
import com.navfinance.service.FinanceService;
import com.navfinance.ui.util.NavIcons;
import com.navfinance.ui.util.UiUtils;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

/**
 * Modal dialog for creating and editing Category Master definitions and initial associated sub-categories.
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public class CategoryDialog extends JDialog {

    private final FinanceService financeService = FinanceService.getInstance();
    private final Category categoryToEdit;
    private final String originalName;
    private boolean saved = false;

    private JTextField txtName;
    private JRadioButton rbExpense;
    private JRadioButton rbIncome;
    private JTextField txtInitialSubs;

    public CategoryDialog(JFrame parent, Category category) {
        super(parent, category == null ? I18n.get("masters.dialog.category.addTitle") : I18n.get("masters.dialog.category.editTitle"), true);
        this.categoryToEdit = category;
        this.originalName = category != null ? category.getName() : null;

        initComponents();
        populateFields();
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Color.WHITE);
        root.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // Header Panel
        JPanel pnlHeader = new JPanel(new BorderLayout(8, 4));
        pnlHeader.setOpaque(false);

        JLabel lblTitle = new JLabel(getTitle());
        lblTitle.setFont(UiUtils.titleFont());
        lblTitle.setForeground(new Color(15, 23, 42));

        JLabel lblSubtitle = new JLabel(categoryToEdit == null ?
                "Define a new transaction category and its optional associated sub-categories." :
                "Modify category name, transaction nature, and manage associated sub-categories.");
        lblSubtitle.setFont(UiUtils.subtitleFont());
        lblSubtitle.setForeground(UiUtils.COLOR_TEXT_MUTED);

        pnlHeader.add(lblTitle, BorderLayout.NORTH);
        pnlHeader.add(lblSubtitle, BorderLayout.SOUTH);
        root.add(pnlHeader, BorderLayout.NORTH);

        // Form Panel
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 4, 6, 4);

        int row = 0;

        // 1. Transaction Type (Expense vs Income)
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        JLabel lblType = new JLabel(I18n.get("masters.table.categoryType") + ":*");
        lblType.setFont(UiUtils.boldFont());
        form.add(lblType, gbc);

        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 0.7;
        JPanel pnlType = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        pnlType.setOpaque(false);

        rbExpense = new JRadioButton("Expense", true);
        rbExpense.setFont(UiUtils.boldFont());
        rbExpense.setForeground(UiUtils.COLOR_EXPENSE);
        rbExpense.setOpaque(false);

        rbIncome = new JRadioButton("Income", false);
        rbIncome.setFont(UiUtils.boldFont());
        rbIncome.setForeground(UiUtils.COLOR_INCOME);
        rbIncome.setOpaque(false);

        ButtonGroup bg = new ButtonGroup();
        bg.add(rbExpense);
        bg.add(rbIncome);

        pnlType.add(rbExpense);
        pnlType.add(rbIncome);
        form.add(pnlType, gbc);

        // 2. Category Name
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        JLabel lblName = new JLabel(I18n.get("masters.table.categoryName") + ":*");
        lblName.setFont(UiUtils.boldFont());
        form.add(lblName, gbc);

        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 0.7;
        txtName = new JTextField(20);
        txtName.setFont(UiUtils.regularFont());
        form.add(txtName, gbc);

        // 3. Initial Associated Sub-Categories (Comma separated)
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        JLabel lblSubs = new JLabel("Sub-Categories:");
        lblSubs.setFont(UiUtils.boldFont());
        form.add(lblSubs, gbc);

        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 0.7;
        txtInitialSubs = new JTextField(20);
        txtInitialSubs.setFont(UiUtils.regularFont());
        txtInitialSubs.setToolTipText("Enter initial associated categories separated by commas");
        form.add(txtInitialSubs, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        JLabel lblHint = new JLabel("<html><font color='#64748B' size='2'>Optional: Separate multiple associated categories with commas (e.g. Dining Out, Groceries, Snacks)</font></html>");
        form.add(lblHint, gbc);

        root.add(form, BorderLayout.CENTER);

        // Footer Actions
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

        UiUtils.configureDialog(this, btnSave, btnCancel);
        setContentPane(root);
    }

    private void populateFields() {
        if (categoryToEdit != null) {
            txtName.setText(categoryToEdit.getName());
            if (categoryToEdit.getType() == TransactionType.INCOME) {
                rbIncome.setSelected(true);
            } else {
                rbExpense.setSelected(true);
            }

            List<String> names = categoryToEdit.getSubCategoryNames();
            if (!names.isEmpty()) {
                txtInitialSubs.setText(String.join(", ", names));
            }
        }
    }

    private void onSave() {
        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            UiUtils.showError(this, "Please enter a category name.");
            txtName.requestFocus();
            return;
        }

        TransactionType type = rbIncome.isSelected() ? TransactionType.INCOME : TransactionType.EXPENSE;

        // Check duplicate name within the same type
        if (categoryToEdit == null || !name.equalsIgnoreCase(originalName)) {
            for (Category existing : financeService.getCategoriesByType(type)) {
                if (existing.getName().trim().equalsIgnoreCase(name)) {
                    UiUtils.showError(this, "A category named '" + name + "' already exists for " + type.getDisplayName() + ".");
                    txtName.requestFocus();
                    return;
                }
            }
        }

        Category target = categoryToEdit != null ? categoryToEdit : new Category();
        target.setName(name);
        target.setType(type);

        Category savedCat = financeService.saveCategory(target, originalName);
        if (savedCat != null && savedCat.getId() != null) {
            // Process initial or updated comma-separated sub-categories
            String rawSubs = txtInitialSubs.getText().trim();
            if (!rawSubs.isEmpty()) {
                String[] tokens = rawSubs.split(",");
                List<String> existingNames = savedCat.getSubCategoryNames();
                for (String t : tokens) {
                    String subName = t.trim();
                    if (!subName.isEmpty()) {
                        boolean exists = false;
                        for (String en : existingNames) {
                            if (en.equalsIgnoreCase(subName)) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            financeService.addSubCategory(savedCat.getId(), subName);
                        }
                    }
                }
            }
            saved = true;
            dispose();
        } else {
            UiUtils.showError(this, "Failed to save category. Please check database connectivity.");
        }
    }

    public boolean isSaved() {
        return saved;
    }
}
