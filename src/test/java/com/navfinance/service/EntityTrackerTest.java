package com.navfinance.service;

import com.navfinance.database.DatabaseManager;
import com.navfinance.model.DashboardSummary;
import com.navfinance.model.Person;
import com.navfinance.model.Transaction;
import com.navfinance.model.TransactionType;
import com.navfinance.config.AppConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EntityTrackerTest {

    private static FinanceService financeService;
    private static AnalyticsService analyticsService;

    @BeforeAll
    static void setUp() {
        AppConfig.setTestMode(true);
        DatabaseManager.getInstance().initDatabase();
        financeService = FinanceService.getInstance();
        analyticsService = AnalyticsService.getInstance();
    }

    @AfterAll
    static void tearDown() {
        AppConfig.setTestMode(false);
    }

    @Test
    void testBillsAndEmisNotConsideredAsLoansGiven() {
        // Outright bill payment should NOT be considered a 'Give' transaction (money expected back)
        Transaction billTx = new Transaction();
        billTx.setType(TransactionType.EXPENSE);
        billTx.setCategory("Electricity Bill");
        billTx.setBillId(10L);
        billTx.setPersonId(1L); // even if a vendor person is tagged
        assertFalse(financeService.isGiveTransaction(billTx),
                "Bill expense must NOT be considered as loan balance expected back");

        // EMI payment should NOT be considered a 'Give' transaction
        Transaction emiTx = new Transaction();
        emiTx.setType(TransactionType.EXPENSE);
        emiTx.setCategory("Home Loan EMI");
        emiTx.setEmiId(5L);
        emiTx.setPersonId(1L);
        assertFalse(financeService.isGiveTransaction(emiTx),
                "EMI payment must NOT be considered as loan balance expected back");

        // Credit Card bill payment should NOT be considered a 'Give' transaction
        Transaction ccTx = new Transaction();
        ccTx.setType(TransactionType.EXPENSE);
        ccTx.setCategory("Credit Card Bill Payment");
        ccTx.setCreditCardId(2L);
        assertFalse(financeService.isGiveTransaction(ccTx),
                "Credit Card bill payment must NOT be considered as loan balance expected back");

        // Pure Personal loan given without bill/emi/card SHOULD be considered a Give transaction
        Transaction loanTx = new Transaction();
        loanTx.setType(TransactionType.EXPENSE);
        loanTx.setCategory("Personal Loan");
        loanTx.setPersonId(99L);
        assertTrue(financeService.isGiveTransaction(loanTx),
                "Personal loan without bill/emi/card should be identified as Give transaction");
    }

    @Test
    void testEntityTrackerAggregationAcrossGranularities() {
        List<Transaction> testTxs = new ArrayList<>();

        // Day 1: 2026-01-15 (Q1 2026) - Outflow 1200
        Transaction t1 = new Transaction();
        t1.setId(101L);
        t1.setTxDate(LocalDate.of(2026, 1, 15));
        t1.setAmount(1200.0);
        t1.setType(TransactionType.EXPENSE);
        t1.setCategory("Broadband");
        t1.setBillId(1L);
        t1.setNotes("Airtel Fiber Jan");
        testTxs.add(t1);

        // Day 2: 2026-02-15 (Q1 2026) - Outflow 1200
        Transaction t2 = new Transaction();
        t2.setId(102L);
        t2.setTxDate(LocalDate.of(2026, 2, 15));
        t2.setAmount(1200.0);
        t2.setType(TransactionType.EXPENSE);
        t2.setCategory("Broadband");
        t2.setBillId(1L);
        t2.setNotes("Airtel Fiber Feb");
        testTxs.add(t2);

        // Day 3: 2026-04-10 (Q2 2026) - Outflow 1500
        Transaction t3 = new Transaction();
        t3.setId(103L);
        t3.setTxDate(LocalDate.of(2026, 4, 10));
        t3.setAmount(1500.0);
        t3.setType(TransactionType.EXPENSE);
        t3.setCategory("Broadband");
        t3.setBillId(1L);
        t3.setNotes("Airtel Fiber Apr Upgrade");
        testTxs.add(t3);

        // Test MONTHS granularity
        List<DashboardSummary.TrendPoint> monthlyTrend = analyticsService.buildTrendPoints(testTxs, "MONTHS");
        assertNotNull(monthlyTrend);
        assertFalse(monthlyTrend.isEmpty());
        // Should have points for Jan 2026, Feb 2026, Apr 2026
        boolean hasJan = monthlyTrend.stream().anyMatch(p -> p.getLabel().contains("Jan") && p.getExpense() == 1200.0);
        boolean hasFeb = monthlyTrend.stream().anyMatch(p -> p.getLabel().contains("Feb") && p.getExpense() == 1200.0);
        boolean hasApr = monthlyTrend.stream().anyMatch(p -> p.getLabel().contains("Apr") && p.getExpense() == 1500.0);
        assertTrue(hasJan, "Monthly trend should contain Jan with 1200 outflow");
        assertTrue(hasFeb, "Monthly trend should contain Feb with 1200 outflow");
        assertTrue(hasApr, "Monthly trend should contain Apr with 1500 outflow");

        // Test QUARTER granularity
        List<DashboardSummary.TrendPoint> quarterlyTrend = analyticsService.buildTrendPoints(testTxs, "QUARTER");
        assertNotNull(quarterlyTrend);
        // Q1 (Jan + Feb) = 2400, Q2 (Apr) = 1500
        boolean hasQ1 = quarterlyTrend.stream().anyMatch(p -> p.getLabel().contains("Q1") && p.getExpense() == 2400.0);
        boolean hasQ2 = quarterlyTrend.stream().anyMatch(p -> p.getLabel().contains("Q2") && p.getExpense() == 1500.0);
        assertTrue(hasQ1, "Quarterly trend should aggregate Q1 to 2400 outflow");
        assertTrue(hasQ2, "Quarterly trend should aggregate Q2 to 1500 outflow");

        // Test YEARS granularity
        List<DashboardSummary.TrendPoint> yearlyTrend = analyticsService.buildTrendPoints(testTxs, "YEARS");
        assertNotNull(yearlyTrend);
        assertEquals(1, yearlyTrend.size(), "All transactions in 2026 should aggregate to 1 yearly point");
        assertEquals(3900.0, yearlyTrend.get(0).getExpense(), 0.001);
        assertEquals(0.0, yearlyTrend.get(0).getIncome(), 0.001);

        // Test DAYS granularity
        List<DashboardSummary.TrendPoint> dailyTrend = analyticsService.buildTrendPoints(testTxs, "DAYS");
        assertEquals(3, dailyTrend.size(), "Three distinct days should yield 3 daily points");

        // Test WEEKS granularity
        List<DashboardSummary.TrendPoint> weeklyTrend = analyticsService.buildTrendPoints(testTxs, "WEEK");
        assertTrue(weeklyTrend.size() >= 2, "Weekly trend should contain distinct weeks");
    }

    @Test
    void testEntityTrackerSummaryInflowOutflowNetBalance() {
        List<Transaction> mixedTxs = new ArrayList<>();

        // Expense (Outflow) 5000
        Transaction exp = new Transaction();
        exp.setId(201L);
        exp.setTxDate(LocalDate.of(2026, 3, 1));
        exp.setAmount(5000.0);
        exp.setType(TransactionType.EXPENSE);
        exp.setCategory("Project Freelance");
        exp.setPersonId(5L);
        mixedTxs.add(exp);

        // Income (Inflow) 8000
        Transaction inc = new Transaction();
        inc.setId(202L);
        inc.setTxDate(LocalDate.of(2026, 3, 15));
        inc.setAmount(8000.0);
        inc.setType(TransactionType.INCOME);
        inc.setCategory("Project Freelance");
        inc.setPersonId(5L);
        mixedTxs.add(inc);

        List<DashboardSummary.TrendPoint> points = analyticsService.buildTrendPoints(mixedTxs, "MONTHS");
        assertEquals(1, points.size());
        assertEquals(8000.0, points.get(0).getIncome(), 0.001);
        assertEquals(5000.0, points.get(0).getExpense(), 0.001);
        assertEquals(3000.0, points.get(0).getIncome() - points.get(0).getExpense(), 0.001);
    }

    @Test
    void testPersonLoanSummaryExcludesBillsAndCards() {
        // Create an isolated counterparty test person
        Person vendor = new Person();
        vendor.setName("Test Vendor Power Corp");
        vendor.setRelation("Utility Vendor");
        final Person savedVendor = financeService.savePerson(vendor);

        // Create a valid bill in the database
        com.navfinance.model.Bill bill = new com.navfinance.model.Bill();
        bill.setBillName("Electricity Master Bill");
        bill.setProvider("State Power");
        bill.setDefaultAmount(3500.0);
        com.navfinance.model.Bill savedBill = new com.navfinance.dao.BillDao().insert(bill);
        assertNotNull(savedBill.getId());

        // Create a bill transaction for this vendor
        Transaction billTx = new Transaction();
        billTx.setTxDate(LocalDate.now());
        billTx.setAmount(3500.0);
        billTx.setType(TransactionType.EXPENSE);
        billTx.setCategory("Electricity Bill");
        billTx.setPaymentVia("UPI");
        billTx.setPersonId(savedVendor.getId());
        billTx.setBillId(savedBill.getId());
        billTx.setNotes("Monthly electricity bill payment");
        Transaction created = financeService.createTransaction(billTx);
        assertNotNull(created);
        assertNotNull(created.getId());

        // Fetch loan summaries
        List<FinanceService.PersonLoanSummary> summaries = financeService.getPersonLoanSummaries();
        FinanceService.PersonLoanSummary vendorSummary = summaries.stream()
                .filter(s -> s.getPerson().getId().equals(savedVendor.getId()))
                .findFirst()
                .orElse(null);

        // Because it was a bill transaction, it should NOT count as loan given (net balance should be 0)
        if (vendorSummary != null) {
            assertEquals(0.0, vendorSummary.getTotalGiven(), 0.001,
                    "Bill payments to vendor must NOT be counted as loans given");
            assertEquals(0.0, vendorSummary.getNetBalance(), 0.001,
                    "Bill payments to vendor must NOT result in an outstanding loan balance expected back");
        }
    }
}
