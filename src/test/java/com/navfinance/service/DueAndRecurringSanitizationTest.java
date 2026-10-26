package com.navfinance.service;

import com.navfinance.config.AppConfig;
import com.navfinance.dao.DuePaymentDao;
import com.navfinance.database.DatabaseManager;
import com.navfinance.model.DuePayment;
import com.navfinance.model.DueStatus;
import com.navfinance.model.RecurringSchedule;
import com.navfinance.model.TransactionType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests verifying that:
 * 1. Income schedules do NOT generate due payments (due payments are liabilities only).
 * 2. Invalid income due payments are purged by sanitation routines.
 * 3. computeSummary ignores non-expense records.
 * 4. settlePaymentOnTime and settleAllForMonthOnTime mark dues settled on due_date.
 */
public class DueAndRecurringSanitizationTest {

    private static RecurringService recurringService;
    private static DuePaymentDao duePaymentDao;

    @BeforeAll
    static void setUp() {
        System.setProperty("java.awt.headless", "true");
        AppConfig.setTestMode(true);
        DatabaseManager.getInstance().initDatabase();
        recurringService = RecurringService.getInstance();
        duePaymentDao = new DuePaymentDao();
    }

    @AfterAll
    static void tearDown() {
        AppConfig.setTestMode(false);
    }

    @Test
    @DisplayName("generateDueForSchedule skips income schedules completely")
    void testIncomeScheduleDoesNotGenerateDues() {
        RecurringSchedule salary = new RecurringSchedule();
        salary.setId(99999L);
        salary.setName("Test Monthly Salary");
        salary.setType(TransactionType.INCOME);
        salary.setCategory("Salary");
        salary.setSubCategory("Employment");
        salary.setDefaultAmount(209000.0);
        salary.setDayOfMonth(1);
        salary.setStartDate(LocalDate.of(2026, 9, 1));
        salary.setActive(true);

        // Before generation, clean any existing
        recurringService.generateDueForSchedule(salary, 2);

        // Verify no due payment was created for this income schedule
        assertFalse(duePaymentDao.existsForScheduleAndDate(99999L, LocalDate.of(2026, 9, 1)));
    }

    @Test
    @DisplayName("cleanupInvalidDuePayments purges records with type = INCOME")
    void testCleanupInvalidDuePayments() {
        // Artificially insert an invalid income due payment
        DuePayment invalidIncome = new DuePayment();
        invalidIncome.setTitle("Erroneous Income Due");
        invalidIncome.setType(TransactionType.INCOME);
        invalidIncome.setCategory("Salary");
        invalidIncome.setDueDate(LocalDate.of(2026, 9, 1));
        invalidIncome.setExpectedAmount(50000.0);
        invalidIncome.setStatus(DueStatus.OVERDUE);
        DuePayment inserted = duePaymentDao.insert(invalidIncome);
        assertNotNull(inserted.getId());

        // Run cleanup
        int purged = recurringService.cleanupInvalidDuePayments();
        assertTrue(purged >= 1);

        // Verify it no longer exists
        DuePayment fetched = duePaymentDao.findById(inserted.getId());
        assertEquals(null, fetched);
    }

    @Test
    @DisplayName("computeSummary excludes income items from overdue liabilities")
    void testComputeSummaryExcludesIncome() {
        List<DuePayment> list = new ArrayList<>();

        DuePayment bill = new DuePayment();
        bill.setTitle("Electricity Bill");
        bill.setType(TransactionType.EXPENSE);
        bill.setDueDate(LocalDate.now().minusDays(5));
        bill.setExpectedAmount(4500.0);
        bill.setStatus(DueStatus.OVERDUE);
        list.add(bill);

        // Erroneous income in list
        DuePayment salary = new DuePayment();
        salary.setTitle("Salary");
        salary.setType(TransactionType.INCOME);
        salary.setDueDate(LocalDate.now().minusDays(5));
        salary.setExpectedAmount(209000.0);
        salary.setStatus(DueStatus.OVERDUE);
        list.add(salary);

        RecurringService.DueSummary summary = recurringService.computeSummary(list);
        assertEquals(1, summary.getOverdueCount());
        assertEquals(4500.0, summary.getOverdueTotal(), 0.001);
    }

    @Test
    @DisplayName("settlePaymentOnTime marks payment settled with settled_date equal to due_date")
    void testSettlePaymentOnTime() {
        DuePayment due = new DuePayment();
        due.setTitle("Test BroadBand Bill");
        due.setType(TransactionType.EXPENSE);
        due.setCategory("Utility");
        LocalDate targetDueDate = LocalDate.of(2026, 9, 15);
        due.setDueDate(targetDueDate);
        due.setExpectedAmount(999.0);
        due.setStatus(DueStatus.OVERDUE);
        DuePayment saved = duePaymentDao.insert(due);
        assertNotNull(saved.getId());

        boolean ok = recurringService.settlePaymentOnTime(saved.getId(), true);
        assertTrue(ok);

        DuePayment refreshed = duePaymentDao.findById(saved.getId());
        assertNotNull(refreshed);
        assertTrue(refreshed.isSettled());
        assertEquals(targetDueDate, refreshed.getSettledDate());
        assertEquals(999.0, refreshed.getSettledAmount(), 0.001);
        assertNotNull(refreshed.getTransactionId());

        // Cleanup
        recurringService.deleteDuePayment(saved.getId());
    }

    @Test
    @DisplayName("settleAllForMonthOnTime settles all due payments in target month")
    void testSettleAllForMonthOnTime() {
        DuePayment d1 = new DuePayment();
        d1.setTitle("Month Test Bill 1");
        d1.setType(TransactionType.EXPENSE);
        d1.setCategory("Bill");
        d1.setDueDate(LocalDate.of(2026, 9, 5));
        d1.setExpectedAmount(1500.0);
        d1.setStatus(DueStatus.OVERDUE);
        DuePayment s1 = duePaymentDao.insert(d1);

        int count = recurringService.settleAllForMonthOnTime(YearMonth.of(2026, 9), false);
        assertTrue(count >= 1);

        DuePayment r1 = duePaymentDao.findById(s1.getId());
        assertTrue(r1.isSettled());
        assertEquals(LocalDate.of(2026, 9, 5), r1.getSettledDate());

        // Cleanup
        recurringService.deleteDuePayment(s1.getId());
    }
}
