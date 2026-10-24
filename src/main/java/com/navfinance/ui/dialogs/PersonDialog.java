package com.navfinance.ui.dialogs;

import com.navfinance.model.Person;
import com.navfinance.service.FinanceService;
import com.navfinance.ui.util.NavIcons;
import com.navfinance.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
 * Modal dialog for registering and managing Person contact directory profiles.
 * <p>
 * Captures full names, phone numbers or contacts, interpersonal relationships (Friend,
 * Relative, Colleague, Landlord, etc.), and custom notes for Udhar / Loan tracking.
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
        super(parent, person == null ? "Add Person" : "Edit Person", true);
        this.person = person;

        initComponents();
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
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
        mainPanel.add(new JLabel("Full Name:*"), gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtName = new JTextField(20);
        mainPanel.add(txtName, gbc);

        // Contact
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Contact / Phone:"), gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtContact = new JTextField(20);
        mainPanel.add(txtContact, gbc);

        // Relation
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Relation:"), gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        cmbRelation = new JComboBox<>(new String[]{"Friend", "Relative", "Colleague", "Family", "Landlord", "Neighbor", "Business Associate", "Other"});
        cmbRelation.setEditable(true);
        mainPanel.add(cmbRelation, gbc);

        // Notes
        gbc.gridx = 0; gbc.gridy = row;
        mainPanel.add(new JLabel("Notes:"), gbc);

        gbc.gridx = 1; gbc.gridy = row++;
        txtNotes = new JTextArea(3, 20);
        mainPanel.add(new JScrollPane(txtNotes), gbc);

        add(mainPanel, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        JButton btnCancel = UiUtils.createSecondaryButton("Cancel", NavIcons.cross(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = UiUtils.createPrimaryButton("Save Person", NavIcons.check(13, Color.WHITE));
        btnSave.addActionListener(e -> onSave());

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);
        add(btnPanel, BorderLayout.SOUTH);

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
            UiUtils.showError(this, "Please enter person's name.");
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
