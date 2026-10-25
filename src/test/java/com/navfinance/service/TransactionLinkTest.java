package com.navfinance.service;

import com.navfinance.database.DatabaseManager;
import com.navfinance.model.Person;
import com.navfinance.model.SettlementStatus;
import com.navfinance.model.Transaction;
import com.navfinance.model.TransactionLink;
import com.navfinance.model.TransactionSettlementInfo;
import com.navfinance.model.TransactionType;
import com.navfinance.config.AppConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionLinkTest {

    private static FinanceService financeService;

    @BeforeAll
    static void setUp() {
        AppConfig.setTestMode(true);
        DatabaseManager.getInstance().initDatabase();
        financeService = FinanceService.getInstance();
    }

    @AfterAll
    static void tearDown() {
        AppConfig.setTestMode(false);
    }

    @Test
    void testIsGiveAndReturnTransactionIdentification() {
        Transaction giveTx1 = new Transaction();
        giveTx1.setType(TransactionType.EXPENSE);
        giveTx1.setCategory("Loan Given");
        assertTrue(financeService.isGiveTransaction(giveTx1));
        assertFalse(financeService.isReturnTransaction(giveTx1));

        Transaction giveTx2 = new Transaction();
        giveTx2.setType(TransactionType.EXPENSE);
        giveTx2.setCategory("Personal");
        giveTx2.setPersonId(1L);
        assertTrue(financeService.isGiveTransaction(giveTx2));

        Transaction returnTx1 = new Transaction();
        returnTx1.setType(TransactionType.INCOME);
        returnTx1.setCategory("Return");
        assertTrue(financeService.isReturnTransaction(returnTx1));
        assertFalse(financeService.isGiveTransaction(returnTx1));

        Transaction returnTx2 = new Transaction();
        returnTx2.setType(TransactionType.INCOME);
        returnTx2.setCategory("Settlement");
        returnTx2.setPersonId(1L);
        assertTrue(financeService.isReturnTransaction(returnTx2));
    }

    @Test
    void testFullAndPartialSettlementLinking() {
        // Create test counterparty person
        Person person = new Person();
        person.setName("Vikram Test Contact");
        person.setRelation("Friend");
        person.setContact("9999999999");
        Person savedPerson = financeService.savePerson(person);
        assertNotNull(savedPerson);
        Long personId = savedPerson.getId();

        // 1. Create a Give transaction: 10,000 INR
        Transaction giveTx = new Transaction();
        giveTx.setType(TransactionType.EXPENSE);
        giveTx.setTxDate(LocalDate.now());
        giveTx.setAmount(10000.0);
        giveTx.setCategory("Loan Given");
        giveTx.setPersonId(personId);
        giveTx.setPaymentVia("Bank Transfer");
        giveTx.setNotes("Loan given for project");
        Transaction createdGive = financeService.createTransaction(giveTx);
        assertNotNull(createdGive);
        Long giveId = createdGive.getId();

        // Initially giveTx should be UNLINKED
        TransactionSettlementInfo initialGiveInfo = financeService.getSettlementInfo(createdGive);
        assertEquals(SettlementStatus.UNLINKED, initialGiveInfo.getStatus());
        assertEquals(0.0, initialGiveInfo.getSettledAmount(), 0.001);
        assertEquals(10000.0, initialGiveInfo.getRemainingAmount(), 0.001);

        // 2. Create partial Return transaction: 4,000 INR
        Transaction returnTx1 = new Transaction();
        returnTx1.setType(TransactionType.INCOME);
        returnTx1.setTxDate(LocalDate.now());
        returnTx1.setAmount(4000.0);
        returnTx1.setCategory("Return");
        returnTx1.setPersonId(personId);
        returnTx1.setPaymentVia("UPI");
        returnTx1.setNotes("First partial installment return");
        Transaction createdReturn1 = financeService.createTransaction(returnTx1);
        assertNotNull(createdReturn1);
        Long returnId1 = createdReturn1.getId();

        // Link Give (10,000) and Return1 (4,000)
        TransactionLink link1 = financeService.linkTransactions(giveId, returnId1, 4000.0, "Installment 1");
        assertNotNull(link1);

        // Check giveTx status: should be PARTIAL (40% settled, remaining 6,000)
        TransactionSettlementInfo partialGiveInfo = financeService.getSettlementInfo(createdGive);
        assertEquals(SettlementStatus.PARTIAL, partialGiveInfo.getStatus());
        assertEquals(4000.0, partialGiveInfo.getSettledAmount(), 0.001);
        assertEquals(6000.0, partialGiveInfo.getRemainingAmount(), 0.001);
        assertEquals(40.0, partialGiveInfo.getSettledPercentage(), 0.01);
        assertFalse(partialGiveInfo.isNullified());

        // Check returnTx1 status: 4000/4000 settled -> NULLIFIED (100% settled)
        TransactionSettlementInfo return1Info = financeService.getSettlementInfo(createdReturn1);
        assertEquals(SettlementStatus.NULLIFIED, return1Info.getStatus());
        assertTrue(return1Info.isNullified());
        assertEquals(4000.0, return1Info.getSettledAmount(), 0.001);
        assertEquals(0.0, return1Info.getRemainingAmount(), 0.001);

        // 3. Create second Return transaction: 6,000 INR to complete the settlement
        Transaction returnTx2 = new Transaction();
        returnTx2.setType(TransactionType.INCOME);
        returnTx2.setTxDate(LocalDate.now());
        returnTx2.setAmount(6000.0);
        returnTx2.setCategory("Return");
        returnTx2.setPersonId(personId);
        returnTx2.setPaymentVia("Cash");
        returnTx2.setNotes("Final settlement return");
        Transaction createdReturn2 = financeService.createTransaction(returnTx2);
        assertNotNull(createdReturn2);
        Long returnId2 = createdReturn2.getId();

        // Link Give with Return2
        TransactionLink link2 = financeService.linkTransactions(giveId, returnId2, 6000.0, "Installment 2");
        assertNotNull(link2);

        // Now giveTx is 10,000/10,000 -> NULLIFIED!
        TransactionSettlementInfo finalGiveInfo = financeService.getSettlementInfo(createdGive);
        assertEquals(SettlementStatus.NULLIFIED, finalGiveInfo.getStatus());
        assertTrue(finalGiveInfo.isNullified());
        assertEquals(10000.0, finalGiveInfo.getSettledAmount(), 0.001);
        assertEquals(0.0, finalGiveInfo.getRemainingAmount(), 0.001);
        assertEquals(100.0, finalGiveInfo.getSettledPercentage(), 0.01);
        assertEquals(2, finalGiveInfo.getLinks().size());

        // Test Batch Map retrieval
        Map<Long, TransactionSettlementInfo> map = financeService.getSettlementInfoMap(List.of(createdGive, createdReturn1, createdReturn2));
        assertEquals(SettlementStatus.NULLIFIED, map.get(giveId).getStatus());
        assertEquals(SettlementStatus.NULLIFIED, map.get(returnId1).getStatus());
        assertEquals(SettlementStatus.NULLIFIED, map.get(returnId2).getStatus());

        // 4. Test delete links
        int deleted = financeService.deleteLinksForTransaction(giveId);
        assertTrue(deleted >= 2);

        // Status reverts to UNLINKED
        TransactionSettlementInfo unlinkedInfo = financeService.getSettlementInfo(createdGive);
        assertEquals(SettlementStatus.UNLINKED, unlinkedInfo.getStatus());
        assertFalse(unlinkedInfo.isNullified());
        assertEquals(0.0, unlinkedInfo.getSettledAmount(), 0.001);
    }
}
