package com.navfinance.ui;

import com.navfinance.config.AppConfig;
import com.navfinance.service.FinanceService;
import com.navfinance.ui.panels.MastersPanel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests verifying double click and right click context menu configuration
 * across all master catalog tables in {@link MastersPanel}, as well as payment mode CRUD.
 */
public class MastersPanelInteractionsTest {

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
    @DisplayName("MastersPanel tables must all have context menus and mouse listeners attached")
    void testTablesHaveContextMenusAndMouseListeners() throws Exception {
        AtomicReference<MastersPanel> ref = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            MastersPanel panel = new MastersPanel();
            ref.set(panel);
        });

        MastersPanel panel = ref.get();
        assertNotNull(panel, "MastersPanel must instantiate successfully");

        JTable[] tables = {
                panel.getTblPersons(),
                panel.getTblBills(),
                panel.getTblEmis(),
                panel.getTblCards(),
                panel.getTblModes()
        };

        for (JTable table : tables) {
            assertNotNull(table, "Master table must not be null");

            // Verify MouseListener is attached (double-click and right-click row selection)
            MouseListener[] listeners = table.getMouseListeners();
            assertTrue(listeners.length > 0, "Master table must have MouseListener attached");

            // Verify ComponentPopupMenu is attached
            JPopupMenu popup = table.getComponentPopupMenu();
            assertNotNull(popup, "Master table must have JPopupMenu attached for right-click");

            // Verify popup contains an Edit item
            List<String> itemTexts = new ArrayList<>();
            for (Component comp : popup.getComponents()) {
                if (comp instanceof JMenuItem) {
                    itemTexts.add(((JMenuItem) comp).getText());
                }
            }

            assertTrue(itemTexts.stream().anyMatch(t -> t.toLowerCase().contains("edit")),
                    "Context menu must contain an Edit option, found: " + itemTexts);
            assertTrue(itemTexts.stream().anyMatch(t -> t.toLowerCase().contains("delete")),
                    "Context menu must contain a Delete option, found: " + itemTexts);
        }

        // Verify Persons, Bills, EMIs, and Cards have Track & Graph option
        JTable[] trackableTables = {
                panel.getTblPersons(),
                panel.getTblBills(),
                panel.getTblEmis(),
                panel.getTblCards()
        };

        for (JTable table : trackableTables) {
            JPopupMenu popup = table.getComponentPopupMenu();
            List<String> itemTexts = new ArrayList<>();
            for (Component comp : popup.getComponents()) {
                if (comp instanceof JMenuItem) {
                    itemTexts.add(((JMenuItem) comp).getText());
                }
            }
            assertTrue(itemTexts.stream().anyMatch(t -> t.toLowerCase().contains("track")),
                    "Trackable table context menu must contain Track option, found: " + itemTexts);
        }
    }

    @Test
    @DisplayName("FinanceService should allow adding, editing/renaming, and deleting payment modes")
    void testPaymentModeCrud() {
        FinanceService service = FinanceService.getInstance();
        String initialMode = "TestMode_Original";
        String updatedMode = "TestMode_Renamed";

        // Add
        service.addPaymentMode(initialMode);
        assertTrue(service.getAllPaymentModes().contains(initialMode),
                "Payment modes must contain newly added mode");

        // Rename
        boolean updated = service.updatePaymentMode(initialMode, updatedMode);
        assertTrue(updated, "Payment mode update must return true");
        assertTrue(service.getAllPaymentModes().contains(updatedMode),
                "Payment modes must reflect updated name");
        assertFalse(service.getAllPaymentModes().contains(initialMode),
                "Old payment mode name must no longer be present");

        // Delete
        boolean deleted = service.deletePaymentMode(updatedMode);
        assertTrue(deleted, "Payment mode delete must return true");
        assertFalse(service.getAllPaymentModes().contains(updatedMode),
                "Deleted payment mode name must no longer be present");
    }
}
