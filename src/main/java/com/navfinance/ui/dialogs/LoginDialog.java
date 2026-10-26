package com.navfinance.ui.dialogs;

import com.navfinance.config.AppConfig;
import com.navfinance.i18n.I18n;
import com.navfinance.security.SecurityService;
import com.navfinance.ui.util.NavIcons;
import com.navfinance.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Modern, high-clarity authentication dialog presented on application startup or manual session lock.
 * <p>
 * Features salted SHA-256 password verification, show/hide password toggle,
 * Caps Lock detection, keyboard shortcut bindings (Enter to submit, Esc to exit),
 * and multi-language support.
 * </p>
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public class LoginDialog extends JDialog {

    private final SecurityService securityService = SecurityService.getInstance();

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JCheckBox chkShowPassword;
    private JLabel lblCapsLock;
    private JLabel lblError;
    private JButton btnLogin;
    private JButton btnExit;

    private boolean authenticated = false;

    /**
     * Constructs a modal {@code LoginDialog}.
     *
     * @param parent owner frame (may be {@code null} during cold startup)
     */
    public LoginDialog(JFrame parent) {
        super(parent, AppConfig.APP_NAME + " - " + I18n.get("login.title"), true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        initComponents();
        pack();
        setLocationRelativeTo(parent);
    }

    /**
     * Initializes dialog visual components, layout, and listeners.
     */
    private void initComponents() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        // 1. TOP: Application Branding Header
        JPanel headerPanel = new JPanel(new BorderLayout(14, 0));
        headerPanel.setOpaque(false);

        JLabel lblLogo = new JLabel(NavIcons.appLogo(44));
        headerPanel.add(lblLogo, BorderLayout.WEST);

        JPanel titleBox = new JPanel(new GridBagLayout());
        titleBox.setOpaque(false);
        GridBagConstraints gbcT = new GridBagConstraints();
        gbcT.gridx = 0; gbcT.gridy = 0;
        gbcT.anchor = GridBagConstraints.WEST;
        gbcT.fill = GridBagConstraints.HORIZONTAL;
        gbcT.weightx = 1.0;

        JLabel lblApp = new JLabel(AppConfig.APP_NAME);
        lblApp.setFont(UiUtils.titleFont());
        lblApp.setForeground(new Color(15, 23, 42));
        titleBox.add(lblApp, gbcT);

        gbcT.gridy = 1;
        JLabel lblSubtitle = new JLabel(I18n.get("login.subtitle"));
        lblSubtitle.setFont(UiUtils.subtitleFont());
        lblSubtitle.setForeground(UiUtils.COLOR_TEXT_MUTED);
        titleBox.add(lblSubtitle, gbcT);

        gbcT.gridy = 2;
        JLabel lblSecurityBadge = new JLabel(" " + I18n.get("login.badge.secure"), NavIcons.lock(12, UiUtils.COLOR_PRIMARY), SwingConstants.LEFT);
        lblSecurityBadge.setFont(UiUtils.regularFont());
        lblSecurityBadge.setForeground(UiUtils.COLOR_PRIMARY);
        titleBox.add(lblSecurityBadge, gbcT);

        headerPanel.add(titleBox, BorderLayout.CENTER);
        root.add(headerPanel, BorderLayout.NORTH);

        // 2. CENTER: Credentials Input Form
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 4, 6, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // Username
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2;
        JLabel lblUser = new JLabel(I18n.get("login.username"), NavIcons.person(14, UiUtils.COLOR_TEXT_MUTED), SwingConstants.LEFT);
        lblUser.setFont(UiUtils.boldFont());
        lblUser.setForeground(new Color(51, 65, 85));
        formPanel.add(lblUser, gbc);

        gbc.gridy = row++;
        txtUsername = new JTextField(20);
        txtUsername.setFont(UiUtils.regularFont());
        txtUsername.setPreferredSize(new Dimension(300, 36));
        txtUsername.setText(securityService.getUsername());
        txtUsername.addActionListener(e -> txtPassword.requestFocusInWindow());
        formPanel.add(txtUsername, gbc);

        // Password
        gbc.gridy = row++;
        JLabel lblPass = new JLabel(I18n.get("login.password"), NavIcons.key(14, UiUtils.COLOR_TEXT_MUTED), SwingConstants.LEFT);
        lblPass.setFont(UiUtils.boldFont());
        lblPass.setForeground(new Color(51, 65, 85));
        formPanel.add(lblPass, gbc);

        gbc.gridy = row++;
        txtPassword = new JPasswordField(20);
        txtPassword.setFont(UiUtils.regularFont());
        txtPassword.setPreferredSize(new Dimension(300, 36));
        txtPassword.addActionListener(e -> onLogin());
        txtPassword.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                checkCapsLock();
            }
            @Override
            public void keyReleased(KeyEvent e) {
                checkCapsLock();
            }
        });
        formPanel.add(txtPassword, gbc);

        // Show Password & Caps Lock notification row
        gbc.gridy = row++; gbc.gridwidth = 1; gbc.weightx = 0.5;
        chkShowPassword = new JCheckBox(I18n.get("login.showPassword"));
        chkShowPassword.setFont(UiUtils.regularFont());
        chkShowPassword.setOpaque(false);
        chkShowPassword.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        chkShowPassword.addActionListener(e -> {
            if (chkShowPassword.isSelected()) {
                txtPassword.setEchoChar((char) 0);
            } else {
                txtPassword.setEchoChar('•');
            }
        });
        formPanel.add(chkShowPassword, gbc);

        gbc.gridx = 1;
        lblCapsLock = new JLabel("");
        lblCapsLock.setFont(UiUtils.regularFont());
        lblCapsLock.setForeground(new Color(194, 65, 12));
        formPanel.add(lblCapsLock, gbc);

        // Error Feedback Label
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.weightx = 1.0;
        lblError = new JLabel(" ");
        lblError.setFont(UiUtils.boldFont());
        lblError.setForeground(UiUtils.COLOR_EXPENSE);
        lblError.setHorizontalAlignment(SwingConstants.CENTER);
        formPanel.add(lblError, gbc);

        root.add(formPanel, BorderLayout.CENTER);

        // 3. BOTTOM: Action Buttons
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomBar.setOpaque(false);

        btnExit = UiUtils.createSecondaryButton(I18n.get("login.btn.exit"), NavIcons.cross(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnExit.addActionListener(e -> onExit());

        btnLogin = UiUtils.createPrimaryButton(I18n.get("login.btn.signIn"), NavIcons.check(13, Color.WHITE));
        btnLogin.addActionListener(e -> onLogin());

        bottomBar.add(btnExit);
        bottomBar.add(btnLogin);
        root.add(bottomBar, BorderLayout.SOUTH);

        setContentPane(root);
        UiUtils.configureDialog(this, btnLogin, btnExit);

        // Set initial focus
        if (txtUsername.getText() != null && !txtUsername.getText().isBlank()) {
            SwingUtilities.invokeLater(() -> txtPassword.requestFocusInWindow());
        } else {
            SwingUtilities.invokeLater(() -> txtUsername.requestFocusInWindow());
        }
    }

    /**
     * Checks if Caps Lock is enabled on the host system and updates the UI banner.
     */
    private void checkCapsLock() {
        try {
            boolean caps = Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK);
            if (caps) {
                lblCapsLock.setText(I18n.get("login.capsLockOn"));
            } else {
                lblCapsLock.setText("");
            }
        } catch (Exception ignored) {}
    }

    /**
     * Handles credential authentication attempt.
     */
    private void onLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty()) {
            lblError.setText(I18n.get("login.error.emptyUsername"));
            txtUsername.requestFocusInWindow();
            return;
        }

        if (securityService.authenticate(username, password)) {
            authenticated = true;
            lblError.setText(" ");
            dispose();
        } else {
            authenticated = false;
            lblError.setText(I18n.get("login.error.invalidCredentials"));
            txtPassword.setText("");
            txtPassword.requestFocusInWindow();
        }
    }

    /**
     * Handles cancellation or dialog exit.
     */
    private void onExit() {
        authenticated = false;
        dispose();
    }

    /**
     * Indicates whether the user successfully logged in through this dialog.
     *
     * @return {@code true} if authenticated
     */
    public boolean isAuthenticated() {
        return authenticated;
    }
}
