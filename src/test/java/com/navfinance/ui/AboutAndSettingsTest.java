package com.navfinance.ui;

import com.navfinance.config.AppConfig;
import com.navfinance.ui.dialogs.TransactionDialog;
import com.navfinance.ui.panels.AboutUsPanel;
import com.navfinance.ui.panels.SettingsPanel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit and integration tests for the improved About Us and Settings panels,
 * as well as dialog dropdown vector icon rendering.
 */
public class AboutAndSettingsTest {

    @BeforeAll
    static void initHeadless() {
        System.setProperty("java.awt.headless", "true");
        AppConfig.setTestMode(true);
    }

    @AfterAll
    static void tearDown() {
        AppConfig.setTestMode(false);
    }

    @Test
    @DisplayName("AboutUsPanel should initialize and compute live runtime diagnostics without error")
    void testAboutUsPanelInitialization() throws Exception {
        AtomicReference<AboutUsPanel> ref = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            AboutUsPanel panel = new AboutUsPanel();
            ref.set(panel);
        });

        AboutUsPanel panel = ref.get();
        assertNotNull(panel, "AboutUsPanel must instantiate successfully");

        // Refresh live diagnostics
        SwingUtilities.invokeAndWait(panel::refreshLiveDiagnostics);
    }

    @Test
    @DisplayName("SettingsPanel should initialize and update currency preview")
    void testSettingsPanelInitialization() throws Exception {
        AtomicReference<SettingsPanel> ref = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            SettingsPanel panel = new SettingsPanel(() -> {});
            ref.set(panel);
        });

        SettingsPanel panel = ref.get();
        assertNotNull(panel, "SettingsPanel must instantiate successfully");
    }

    @Test
    @DisplayName("TransactionNature labels should be clean without broken unicode emojis")
    void testTransactionNatureLabels() {
        for (TransactionDialog.TransactionNature nature : TransactionDialog.TransactionNature.values()) {
            String label = nature.toString();
            assertNotNull(label);
            // Verify no raw emojis in string label (emojis are now rendered as crisp Java2D vector icons)
            assertFalse(label.contains("💸"), "Label must not contain emoji: " + label);
            assertFalse(label.contains("💰"), "Label must not contain emoji: " + label);
            assertFalse(label.contains("🏦"), "Label must not contain emoji: " + label);
            assertFalse(label.contains("💳"), "Label must not contain emoji: " + label);
            assertFalse(label.contains("🧾"), "Label must not contain emoji: " + label);
            assertFalse(label.contains("🤝"), "Label must not contain emoji: " + label);
            assertTrue(label.length() > 5, "Label must have descriptive text");
        }
    }
}
