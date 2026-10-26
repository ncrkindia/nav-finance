package com.navfinance.service;

import com.navfinance.config.AppConfig;
import com.navfinance.database.DatabaseManager;
import com.navfinance.model.AmountType;
import com.navfinance.model.Bill;
import com.navfinance.model.RecurringSchedule;
import com.navfinance.model.TransactionType;
import com.navfinance.ui.panels.DuePaymentsPanel;
import com.navfinance.ui.panels.MastersPanel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying Total Commitments calculation for:
 * 1. Monthly Loan EMIs
 * 2. Fixed Utility Bills
 * 3. Variable Utility Bills (Estimated)
 * 4. Combined Total Bill Commitments (Fixed + Variables)
 * 5. DuePaymentsPanel & MastersPanel KPI updates.
 */
public class RecurringCommitmentsTest {

    private static FinanceService financeService;
    private static RecurringService recurringService;

    @BeforeAll
    static void setUp() {
        System.setProperty("java.awt.headless", "true");
        AppConfig.setTestMode(true);
        DatabaseManager.getInstance().initDatabase();
        financeService = FinanceService.getInstance();
        recurringService = RecurringService.getInstance();
    }

    @AfterAll
    static void tearDown() {
        AppConfig.setTestMode(false);
    }

    @Test
    @DisplayName("computeCommitmentSummary correctly categorizes EMIs, Fixed Bills, and Variable Bills")
    void testComputeCommitmentSummary() {
        List<RecurringSchedule> schedules = new ArrayList<>();

        // Loan EMIs
        RecurringSchedule emi1 = new RecurringSchedule();
        emi1.setName("ICICI Loan EMI");
        emi1.setType(TransactionType.EXPENSE);
        emi1.setCategory("EMI Payment");
        emi1.setSubCategory("ICICI Bank Loan");
        emi1.setAmountType(AmountType.FIXED);
        emi1.setDefaultAmount(28500.0);
        emi1.setActive(true);
        schedules.add(emi1);

        RecurringSchedule emi2 = new RecurringSchedule();
        emi2.setName("Bajaj Loan EMI");
        emi2.setType(TransactionType.EXPENSE);
        emi2.setCategory("Loan");
        emi2.setSubCategory("Bajaj Finserv");
        emi2.setAmountType(AmountType.VARIABLE);
        emi2.setDefaultAmount(5479.0);
        emi2.setActive(true);
        schedules.add(emi2);

        // Fixed Utility Bills
        RecurringSchedule billFixed1 = new RecurringSchedule();
        billFixed1.setName("House Rent");
        billFixed1.setType(TransactionType.EXPENSE);
        billFixed1.setCategory("Bill");
        billFixed1.setSubCategory("House Rent");
        billFixed1.setAmountType(AmountType.FIXED);
        billFixed1.setDefaultAmount(10000.0);
        billFixed1.setActive(true);
        schedules.add(billFixed1);

        RecurringSchedule billFixed2 = new RecurringSchedule();
        billFixed2.setName("Airtel Postpaid Bill");
        billFixed2.setType(TransactionType.EXPENSE);
        billFixed2.setCategory("Utility");
        billFixed2.setSubCategory("Airtel Postpaid Bill");
        billFixed2.setAmountType(AmountType.FIXED);
        billFixed2.setDefaultAmount(1200.0);
        billFixed2.setActive(true);
        schedules.add(billFixed2);

        // Variable Utility Bill
        RecurringSchedule billVar = new RecurringSchedule();
        billVar.setName("Electricity Bill");
        billVar.setType(TransactionType.EXPENSE);
        billVar.setCategory("Bill");
        billVar.setSubCategory("Electricity Bill");
        billVar.setAmountType(AmountType.VARIABLE);
        billVar.setDefaultAmount(3500.0);
        billVar.setActive(true);
        schedules.add(billVar);

        // Income schedule (should NOT be added to expense commitments)
        RecurringSchedule salary = new RecurringSchedule();
        salary.setName("Monthly Salary");
        salary.setType(TransactionType.INCOME);
        salary.setCategory("Salary");
        salary.setDefaultAmount(209000.0);
        salary.setActive(true);
        schedules.add(salary);

        // Compute summary
        RecurringService.ScheduleCommitmentSummary summary = recurringService.computeCommitmentSummary(schedules);
        assertNotNull(summary);

        // EMIs: 28500 + 5479 = 33979
        assertEquals(33979.0, summary.getEmiTotal(), 0.001);
        assertEquals(2, summary.getEmiCount());

        // Fixed Bills: 10000 + 1200 = 11200
        assertEquals(11200.0, summary.getFixedBillsTotal(), 0.001);
        assertEquals(2, summary.getFixedBillsCount());

        // Variable Bills: 3500
        assertEquals(3500.0, summary.getVariableBillsTotal(), 0.001);
        assertEquals(1, summary.getVariableBillsCount());

        // Total Bill Commitments: Fixed (11200) + Variable (3500) = 14700
        assertEquals(14700.0, summary.getTotalBillsCommitment(), 0.001);

        // Combined Commitments: 33979 + 14700 = 48679
        assertEquals(48679.0, summary.getTotalCommitment(), 0.001);
    }

    @Test
    @DisplayName("Schedule detector helper methods classify EMI, Credit Card, and Utility Bills correctly")
    void testScheduleDetectorHelpers() {
        RecurringSchedule s1 = new RecurringSchedule();
        s1.setCategory("EMI Payment");
        s1.setName("ICICI Loan");
        assertTrue(recurringService.isEmiSchedule(s1));
        assertFalse(recurringService.isBillSchedule(s1));
        assertFalse(recurringService.isCreditCardSchedule(s1));

        RecurringSchedule s2 = new RecurringSchedule();
        s2.setCategory("Credit Card");
        s2.setName("HDFC Rupay Card Bill");
        assertTrue(recurringService.isCreditCardSchedule(s2));
        assertFalse(recurringService.isEmiSchedule(s2));

        RecurringSchedule s3 = new RecurringSchedule();
        s3.setCategory("Bill");
        s3.setName("Broadband Wifi");
        assertTrue(recurringService.isBillSchedule(s3));
        assertFalse(recurringService.isEmiSchedule(s3));
    }

    @Test
    @DisplayName("Bill model and DAO persist amount_type, due_day, and due_amount properly")
    void testBillPersistenceWithAmountTypeAndDueFields() {
        Bill b = new Bill();
        b.setBillName("Unit Test Power Bill");
        b.setProvider("State Power Corp");
        b.setAmountType(AmountType.VARIABLE);
        b.setDefaultAmount(4200.0);
        b.setDueDay(15);
        b.setDueAmount(4200.0);
        b.setNotes("Test variable electricity bill");

        Bill saved = financeService.saveBill(b);
        assertNotNull(saved.getId());

        List<Bill> all = financeService.getAllBills();
        Bill fetched = all.stream().filter(x -> x.getId().equals(saved.getId())).findFirst().orElse(null);
        assertNotNull(fetched);
        assertEquals(AmountType.VARIABLE, fetched.getAmountType());
        assertTrue(fetched.isVariable());
        assertEquals(15, fetched.getDueDay());
        assertEquals(4200.0, fetched.getDueAmount(), 0.001);

        // Clean up
        financeService.deleteBill(saved.getId());
    }

    @Test
    @DisplayName("DuePaymentsPanel and MastersPanel KPI cards instantiate and display commitment metrics")
    void testPanelsCommitmentKpiCards() throws Exception {
        AtomicReference<DuePaymentsPanel> dueRef = new AtomicReference<>();
        AtomicReference<MastersPanel> mastersRef = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            dueRef.set(new DuePaymentsPanel());
            mastersRef.set(new MastersPanel());
        });

        DuePaymentsPanel duePanel = dueRef.get();
        assertNotNull(duePanel);
        assertNotNull(duePanel.getKpiRuleEmi(), "kpiRuleEmi should not be null");
        assertNotNull(duePanel.getKpiRuleTotalBills(), "kpiRuleTotalBills should not be null");
        assertNotNull(duePanel.getKpiRuleFixedBills(), "kpiRuleFixedBills should not be null");
        assertNotNull(duePanel.getKpiRuleVariableBills(), "kpiRuleVariableBills should not be null");
        assertNotNull(duePanel.getLblRulesCommitmentSummary(), "lblRulesCommitmentSummary should not be null");

        MastersPanel mastersPanel = mastersRef.get();
        assertNotNull(mastersPanel);
        assertNotNull(mastersPanel.getKpiBillTotalCommitment(), "kpiBillTotalCommitment should not be null");
        assertNotNull(mastersPanel.getKpiBillFixed(), "kpiBillFixed should not be null");
        assertNotNull(mastersPanel.getKpiBillVariable(), "kpiBillVariable should not be null");
        assertNotNull(mastersPanel.getKpiBillTotal(), "kpiBillTotal should not be null");
    }
}
