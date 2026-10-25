package com.navfinance;

import com.navfinance.config.AppConfig;
import com.navfinance.model.Transaction;
import com.navfinance.model.TransactionType;
import com.navfinance.ui.dialogs.TransactionLinkDialog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying that TransactionLinkDialog size and location
 * are saved and restored properly, and that opening a new draft transaction
 * with null transaction ID never throws an exception.
 */
public class LinkDialogBoundsTest {

    private Rectangle originalBounds;

    @BeforeEach
    void setUp() {
        originalBounds = AppConfig.getLinkDialogBounds();
    }

    @AfterEach
    void tearDown() {
        if (originalBounds != null) {
            AppConfig.saveLinkDialogBounds(originalBounds);
        }
    }

    @Test
    void testLinkDialogBoundsPersistence() {
        Rectangle testBounds = new Rectangle(150, 120, 1050, 680);
        AppConfig.saveLinkDialogBounds(testBounds);

        Rectangle restored = AppConfig.getLinkDialogBounds();
        assertNotNull(restored, "Saved link dialog bounds must not be null");
        assertEquals(150, restored.x);
        assertEquals(120, restored.y);
        assertEquals(1050, restored.width);
        assertEquals(680, restored.height);
    }

    @Test
    void testOpenLinkDialogWithNullTransactionId() {
        // Prepare a new draft transaction without an ID
        Transaction draftTx = new Transaction();
        draftTx.setType(TransactionType.EXPENSE);
        draftTx.setAmount(5000.0);
        draftTx.setTxDate(LocalDate.now());
        draftTx.setCategory("Loan Given");
        draftTx.setPersonName("Ramesh Kumar");

        assertNull(draftTx.getId(), "Draft transaction ID must be null");

        // Verify that dialog instantiation works without crashing
        if (!GraphicsEnvironment.isHeadless()) {
            TransactionLinkDialog dialog = new TransactionLinkDialog(null, draftTx);
            assertNotNull(dialog, "Dialog should instantiate cleanly without NullPointer or IllegalFormatConversion");
            assertTrue(dialog.getWidth() >= 200, "Dialog width should be initialized");
            assertTrue(dialog.getHeight() >= 150, "Dialog height should be initialized");
            dialog.dispose();
        }
    }

    @Test
    void testRestoreSavedBoundsOnDialogCreation() {
        Rectangle saved = new Rectangle(200, 180, 980, 620);
        AppConfig.saveLinkDialogBounds(saved);

        if (!GraphicsEnvironment.isHeadless()) {
            Transaction tx = new Transaction();
            tx.setId(99L);
            tx.setType(TransactionType.EXPENSE);
            tx.setAmount(1000.0);

            TransactionLinkDialog dialog = new TransactionLinkDialog(null, tx);
            assertEquals(980, dialog.getWidth(), "Dialog should restore saved width");
            assertEquals(620, dialog.getHeight(), "Dialog should restore saved height");
            dialog.dispose();
        }
    }
}
