package com.navfinance;

import com.formdev.flatlaf.FlatLightLaf;
import com.navfinance.database.DatabaseManager;
import com.navfinance.ui.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main application launcher and bootstrap class for <b>Nav Finance</b>.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Configures the modern FlatLaf Light Look and Feel with standardized typography.</li>
 *   <li>Initializes the embedded H2 database engine at {@code ~user/nav-finance/}.</li>
 *   <li>Dispatches the main application window ({@link com.navfinance.ui.MainFrame}) onto the Swing Event Dispatch Thread (EDT).</li>
 * </ul>
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    /**
     * Application entry point.
     *
     * @param args Command-line arguments passed to the program.
     */
    public static void main(String[] args) {
        // Step 1: Set Look and Feel to Modern FlatLaf Light
        try {
            FlatLightLaf.setup();
            com.navfinance.i18n.I18n.applyGlobalFontDefaults();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to initialize FlatLaf look and feel, falling back to system L&F", e);
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                com.navfinance.i18n.I18n.applyGlobalFontDefaults();
            } catch (Exception ignored) {
                // Fallback to basic Swing look and feel
            }
        }

        // Step 2: Initialize embedded H2 database and schema
        try {
            DatabaseManager.getInstance();
            LOGGER.info("Database initialized successfully at ~user/nav-finance");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fatal: Database initialization failed", e);
            System.err.println("Fatal: Database initialization failed: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Step 3: Launch UI on Swing Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            try {
                if (com.navfinance.security.SecurityService.getInstance().isLoginEnabled()) {
                    com.navfinance.ui.dialogs.LoginDialog loginDialog = new com.navfinance.ui.dialogs.LoginDialog(null);
                    loginDialog.setVisible(true);
                    if (!loginDialog.isAuthenticated()) {
                        System.exit(0);
                        return;
                    }
                }
                MainFrame frame = new MainFrame();
                frame.setVisible(true);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to launch main frame", e);
                e.printStackTrace();
            }
        });
    }
}
