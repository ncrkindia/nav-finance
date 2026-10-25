package com.navfinance.ui.dialogs;

import com.navfinance.i18n.I18n;
import com.navfinance.model.Person;
import com.navfinance.service.FinanceService;
import com.navfinance.ui.util.NavIcons;
import com.navfinance.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import java.awt.Color;
import java.awt.Component;
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
 * Modal dialog for registering and managing Person contact directory profiles.
 * <p>
 * Captures full names, phone numbers or contacts, interpersonal relationships (Friend,
 * Relative, Colleague, Landlord, etc.), and custom notes for loan and dues tracking.
 * </p>
 */
public class PersonDialog extends JDialog {

    /** The existing person contact being edited, or {@code null} if adding a new contact. */
    private final Person person;

    /** Flag indicating whether the person was successfully saved. */
    private boolean saved = false;

    /** Text input field for the contact's full name. */
    private JTextField txtName;

    /** Text input field for the contact's telephone or phone number. */
    private JTextField txtContact;

    /** Editable dropdown list for specifying the personal or professional relationship. */
    private JComboBox<String> cmbRelation;

    /** Multi-line text area for address or lending history remarks. */
    private JTextArea txtNotes;

    /**
     * Constructs a person directory editor dialog.
     *
     * @param parent owner frame
     * @param person existing person entity to edit, or {@code null} to add a new person
     */
    public PersonDialog(JFrame parent, Person person) {
        super(parent, person == null ? I18n.get("masters.btn.addPerson") : I18n.get("btn.edit") + " " + I18n.get("masters.table.name"), true);
        this.person = person;

        initComponents();
        pack();
        UiUtils.clampDialogToScreen(this);
        setLocationRelativeTo(parent);
    }

    /**
     * Initializes visual form components, relationship choices, and action buttons.
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

        // Name
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblName = new JLabel(I18n.get("masters.table.name") + ":*");
        lblName.setFont(UiUtils.regularFont());
        mainPanel.add(lblName, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtName = new JTextField(20);
        txtName.setFont(UiUtils.regularFont());
        mainPanel.add(txtName, gbc);

        // Contact
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblContact = new JLabel(I18n.get("masters.table.contact") + ":");
        lblContact.setFont(UiUtils.regularFont());
        mainPanel.add(lblContact, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtContact = new JTextField(20);
        txtContact.setFont(UiUtils.regularFont());
        mainPanel.add(txtContact, gbc);

        // Relation
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblRel = new JLabel(I18n.get("masters.table.relation") + ":");
        lblRel.setFont(UiUtils.regularFont());
        mainPanel.add(lblRel, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        cmbRelation = new JComboBox<>(new String[]{"Friend", "Relative", "Colleague", "Family", "Landlord", "Neighbor", "Business Associate", "Other"});
        cmbRelation.setFont(UiUtils.regularFont());
        cmbRelation.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null && !value.toString().isBlank()) {
                    setIcon(NavIcons.person(14, UiUtils.COLOR_PRIMARY));
                }
                return this;
            }
        });
        cmbRelation.setEditable(true);
        mainPanel.add(cmbRelation, gbc);

        // Notes
        gbc.gridx = 0; gbc.gridy = row;
        JLabel lblNotes = new JLabel(I18n.get("masters.table.notes") + ":");
        lblNotes.setFont(UiUtils.regularFont());
        mainPanel.add(lblNotes, gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtNotes = new JTextArea(3, 20);
        txtNotes.setFont(UiUtils.regularFont());
        mainPanel.add(new JScrollPane(txtNotes), gbc);

        add(mainPanel, BorderLayout.CENTER);

        // Buttons
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

        // Prefill
        if (person != null) {
            txtName.setText(person.getName());
            txtContact.setText(person.getContact());
            cmbRelation.setSelectedItem(person.getRelation());
            txtNotes.setText(person.getNotes());
        }
    }

    /**
     * Validates contact name, builds or updates the {@link Person} entity, persists via
     * {@link FinanceService}, and closes the dialog.
     */
    private void onSave() {
        String name = txtName.getText().trim();
        if (name.isEmpty()) {
            UiUtils.showError(this, I18n.get("dialog.validation.requiredName"));
            txtName.requestFocus();
            return;
        }

        Person p = person != null ? person : new Person();
        p.setName(name);
        p.setContact(txtContact.getText().trim());
        p.setRelation(cmbRelation.getSelectedItem() != null ? cmbRelation.getSelectedItem().toString().trim() : "");
        p.setNotes(txtNotes.getText().trim());

        FinanceService.getInstance().savePerson(p);
        saved = true;
        dispose();
    }


    /**
     * Indicates whether the person record was persisted.
     *
     * @return {@code true} if saved successfully, {@code false} otherwise
     */
    public boolean isSaved() {
        return saved;
    }
}
