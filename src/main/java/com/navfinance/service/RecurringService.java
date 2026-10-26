package com.navfinance.service;

import com.navfinance.config.AppConfig;
import com.navfinance.dao.AuditDao;
import com.navfinance.dao.DuePaymentDao;
import com.navfinance.dao.RecurringScheduleDao;
import com.navfinance.model.AmountType;
import com.navfinance.model.AuditLog;
import com.navfinance.model.DuePayment;
import com.navfinance.model.DueStatus;
import com.navfinance.model.RecurringSchedule;
import com.navfinance.model.Transaction;
import com.navfinance.model.TransactionType;

import java.time.LocalDate;
import java.util.List;

/**
 * Service orchestrating recurring payment schedules, due dates generation,
 * settlement/unsettlement workflows, linked transaction creation, and overdue tracking.
 * <p>
 * This singleton coordinates the automated projection of bills and EMIs across calendar
 * horizons, handles variable and fixed installment amounts, and manages transitions between
 * PENDING, OVERDUE, and SETTLED statuses.
 * </p>
 */
public class RecurringService {

    /** Singleton instance holder. */
    private static RecurringService instance;

    /** Data Access Object for recurring schedule master configurations. */
    private final RecurringScheduleDao scheduleDao = new RecurringScheduleDao();

    /** Data Access Object for monthly payment instances and dues. */
    private final DuePaymentDao duePaymentDao = new DuePaymentDao();

    /** Service layer for financial transactions ledger interaction. */
    private final FinanceService financeService = FinanceService.getInstance();

    /** Data Access Object for recording system audit trails. */
    private final AuditDao auditDao = new AuditDao();

    /**
     * Data Transfer Object aggregating metrics across overdue, pending, and settled payments.
     */
    public static class DueSummary {
        /** Count of overdue payment items. */
        private int overdueCount = 0;
        /** Cumulative financial liability of overdue payments. */
        private double overdueTotal = 0.0;
        /** Count of upcoming pending payment items. */
        private int duePendingCount = 0;
        /** Cumulative financial liability of upcoming pending payments. */
        private double duePendingTotal = 0.0;
        /** Count of settled payment items. */
        private int settledCount = 0;
        /** Cumulative amount paid for settled items. */
        private double settledTotal = 0.0;
        /** Count of payments configured for automated debit. */
        private int autoPayCount = 0;
        /** Count of payments requiring manual remittance. */
        private int manualPayCount = 0;

        /**
         * Returns the count of overdue payments.
         *
         * @return number of overdue records
         */
        public int getOverdueCount() { return overdueCount; }

        /**
         * Returns the total monetary sum of overdue payments.
         *
         * @return overdue sum in default currency
         */
        public double getOverdueTotal() { return overdueTotal; }

        /**
         * Returns the count of pending upcoming payments.
         *
         * @return number of pending records
         */
        public int getDuePendingCount() { return duePendingCount; }

        /**
         * Returns the total monetary sum of pending upcoming payments.
         *
         * @return pending sum in default currency
         */
        public double getDuePendingTotal() { return duePendingTotal; }

        /**
         * Returns the count of settled payments.
         *
         * @return number of settled records
         */
        public int getSettledCount() { return settledCount; }

        /**
         * Returns the total monetary sum settled across the period.
         *
         * @return settled sum in default currency
         */
        public double getSettledTotal() { return settledTotal; }

        /**
         * Returns the count of recurring payments configured for auto-debit.
         *
         * @return auto-pay count
         */
        public int getAutoPayCount() { return autoPayCount; }

        /**
         * Returns the count of recurring payments configured for manual payment.
         *
         * @return manual pay count
         */
        public int getManualPayCount() { return manualPayCount; }
    }

    /**
     * Data Transfer Object aggregating recurring monthly commitment totals
     * across EMIs, Fixed Utility Bills, Variable Utility Bills, and other obligations.
     */
    public static class ScheduleCommitmentSummary {
        private double emiTotal = 0.0;
        private int emiCount = 0;
        private double fixedBillsTotal = 0.0;
        private int fixedBillsCount = 0;
        private double variableBillsTotal = 0.0;
        private int variableBillsCount = 0;
        private double totalBillsCommitment = 0.0;
        private int totalBillsCount = 0;
        private double creditCardTotal = 0.0;
        private int creditCardCount = 0;
        private double otherExpenseTotal = 0.0;
        private int otherExpenseCount = 0;
        private double totalCommitment = 0.0;
        private int totalActiveRulesCount = 0;

        public double getEmiTotal() { return emiTotal; }
        public int getEmiCount() { return emiCount; }
        public double getFixedBillsTotal() { return fixedBillsTotal; }
        public int getFixedBillsCount() { return fixedBillsCount; }
        public double getVariableBillsTotal() { return variableBillsTotal; }
        public int getVariableBillsCount() { return variableBillsCount; }
        public double getTotalBillsCommitment() { return totalBillsCommitment; }
        public int getTotalBillsCount() { return totalBillsCount; }
        public double getCreditCardTotal() { return creditCardTotal; }
        public int getCreditCardCount() { return creditCardCount; }
        public double getOtherExpenseTotal() { return otherExpenseTotal; }
        public int getOtherExpenseCount() { return otherExpenseCount; }
        public double getTotalCommitment() { return totalCommitment; }
        public int getTotalActiveRulesCount() { return totalActiveRulesCount; }
    }

    /**
     * Private constructor initializing the service and triggering default payment generation
     * for active schedules for a rolling 2-month projection window.
     */
    private RecurringService() {
        // Automatically ensure due instances are generated up to next 2 months
        try {
            cleanupInvalidDuePayments();
            generateDuePayments(2);
        } catch (Exception ignored) {
            // Silently continue if database initialization is still concluding
        }
    }

    /**
     * Returns the singleton instance of {@link RecurringService}, lazily initializing if required.
     *
     * @return singleton service instance
     */
    public static synchronized RecurringService getInstance() {
        if (instance == null) {
            instance = new RecurringService();
        }
        return instance;
    }

    // =========================================================================
    // Recurring Schedules
    // =========================================================================

    /**
     * Retrieves all registered recurring schedules from the database.
     *
     * @return list of all {@link RecurringSchedule} configurations
     */
    public List<RecurringSchedule> getAllSchedules() {
        return scheduleDao.findAll();
    }

    /**
     * Retrieves a recurring schedule by its unique database identifier.
     *
     * @param id primary key of the schedule
     * @return the matching {@link RecurringSchedule}, or {@code null} if not found
     */
    public RecurringSchedule getScheduleById(Long id) {
        return scheduleDao.findById(id);
    }

    /**
     * Persists or updates a recurring schedule configuration and generates subsequent due dates.
     *
     * @param s the schedule to save or update
     * @return the persisted {@link RecurringSchedule} instance with updated ID
     */
    public RecurringSchedule saveSchedule(RecurringSchedule s) {
        if (s.getId() == null) {
            // Persist new schedule record
            RecurringSchedule created = scheduleDao.insert(s);
            if (created != null) {
                // Record creation audit trail
                auditDao.log(new AuditLog("RECURRING_SCHEDULE", created.getId(), "CREATE",
                        "Created recurring schedule: " + created.getName() + " on day " + created.getDayOfMonth() +
                                " (" + created.getAmountType() + ", " + created.getAutoPayDisplay() + ")"));
                // Immediately generate due payment instances for the upcoming 2 months
                generateDueForSchedule(created, 2);
            }
            return created;
        } else {
            // Update existing schedule record
            RecurringSchedule existing = scheduleDao.findById(s.getId());
            boolean ok = scheduleDao.update(s);
            if (ok) {
                String diff = (existing != null) ?
                        "Updated schedule '" + s.getName() + "' (Day: " + existing.getDayOfMonth() + " -> " + s.getDayOfMonth() + ")" :
                        "Updated schedule: " + s.getName();
                auditDao.log(new AuditLog("RECURRING_SCHEDULE", s.getId(), "UPDATE", diff));
                // Refresh due payment projections
                generateDueForSchedule(s, 2);
            }
            return s;
        }
    }

    /**
     * Deletes a recurring schedule by its primary key and records an audit log.
     *
     * @param id primary key of the schedule to delete
     * @return {@code true} if deletion was successful, {@code false} otherwise
     */
    public boolean deleteSchedule(Long id) {
        RecurringSchedule existing = scheduleDao.findById(id);
        boolean ok = scheduleDao.delete(id);
        if (ok && existing != null) {
            auditDao.log(new AuditLog("RECURRING_SCHEDULE", id, "DELETE",
                    "Deleted recurring schedule: " + existing.getName()));
        }
        return ok;
    }

    // =========================================================================
    // Due Payments Generation
    // =========================================================================

    /**
     * Scans all active recurring schedules and generates due payments for each month
     * from their startDate up to (current month + monthsAhead).
     *
     * @param monthsAhead number of months ahead of the current month to project
     */
    public final synchronized void generateDuePayments(int monthsAhead) {
        cleanupInvalidDuePayments();
        List<RecurringSchedule> activeSchedules = scheduleDao.findActive();
        for (RecurringSchedule s : activeSchedules) {
            generateDueForSchedule(s, monthsAhead);
        }
    }

    /**
     * Generates concrete {@link DuePayment} records for a specific schedule across the calendar
     * horizon up to {@code monthsAhead}.
     * <p>
     * Clamps the target calendar day to the maximum number of days in the respective month
     * (e.g., day 31 becomes February 28 or 29). Existing payments for the same date are preserved.
     * </p>
     *
     * @param s recurring schedule definition
     * @param monthsAhead projection horizon in months from today
     */
    public final synchronized void generateDueForSchedule(RecurringSchedule s, int monthsAhead) {
        if (s == null || !s.isActive() || s.getStartDate() == null || s.getType() != TransactionType.EXPENSE) return;

        LocalDate today = LocalDate.now();
        // Compute the outer projection boundary at the end of the future month
        LocalDate horizonLimit = today.plusMonths(monthsAhead).withDayOfMonth(today.plusMonths(monthsAhead).lengthOfMonth());

        // Start from the beginning of the schedule's inception month
        LocalDate startMonth = s.getStartDate().withDayOfMonth(1);
        LocalDate endLimit = (s.getEndDate() != null && s.getEndDate().isBefore(horizonLimit)) ?
                s.getEndDate() : horizonLimit;

        LocalDate current = startMonth;
        while (!current.isAfter(endLimit)) {
            int maxDaysInMonth = current.lengthOfMonth();
            // Handle day clamping for shorter months (e.g. 31st on a 30-day month)
            int targetDay = Math.min(s.getDayOfMonth(), maxDaysInMonth);
            LocalDate dueDate = current.withDayOfMonth(targetDay);

            // Verify that calculated dueDate falls squarely within [startDate, endDate]
            if (!dueDate.isBefore(s.getStartDate()) && (s.getEndDate() == null || !dueDate.isAfter(s.getEndDate()))) {
                // Ensure idempotence: insert only if no due record exists for this schedule and date
                if (!duePaymentDao.existsForScheduleAndDate(s.getId(), dueDate)) {
                    DuePayment p = new DuePayment();
                    p.setScheduleId(s.getId());
                    p.setTitle(s.getName());
                    p.setType(s.getType());
                    p.setCategory(s.getCategory());
                    p.setSubCategory(s.getSubCategory());
                    p.setDueDate(dueDate);
                    p.setExpectedAmount(s.getDefaultAmount());
                    p.setVariable(s.getAmountType() == AmountType.VARIABLE);
                    p.setAutoPay(s.isAutoPay());
                    p.setNotes(s.getNotes());

                    // Determine initial pending vs overdue state relative to today's date
                    if (dueDate.isBefore(today)) {
                        p.setStatus(DueStatus.OVERDUE);
                    } else {
                        p.setStatus(DueStatus.PENDING);
                    }

                    duePaymentDao.insert(p);
                }
            }

            // Advance cursor to subsequent month
            current = current.plusMonths(1);
        }
    }

    // =========================================================================
    // Due Payments Management & Settlement
    // =========================================================================

    /**
     * Retrieves all recorded due payments from the database.
     *
     * @return list of all due payment records
     */
    public List<DuePayment> getAllDuePayments() {
        return duePaymentDao.findAll();
    }

    /**
     * Retrieves due payments falling within a specified date window.
     *
     * @param from inclusive start date
     * @param to inclusive end date
     * @return list of matching {@link DuePayment} records
     */
    public List<DuePayment> getDuePaymentsByPeriod(LocalDate from, LocalDate to) {
        return duePaymentDao.findByPeriod(from, to);
    }

    /**
     * Retrieves a single due payment record by its unique database identifier.
     *
     * @param id primary key of the payment record
     * @return the matching {@link DuePayment}, or {@code null} if not found
     */
    public DuePayment getDuePaymentById(Long id) {
        return duePaymentDao.findById(id);
    }

    /**
     * Settles a due payment. If requested, automatically creates and records a corresponding
     * actual transaction entry in the transactions ledger and links it to the due record.
     *
     * @param duePaymentId ID of the due payment to mark settled
     * @param actualAmount final amount paid or cleared
     * @param settleDate date when payment cleared
     * @param settleVia payment instrument used (Cash, UPI, etc.)
     * @param createTransaction whether to insert a matching transaction in the ledger
     * @param userNotes optional remarks entered during settlement
     * @return {@code true} if settlement completed successfully, {@code false} otherwise
     */
    public boolean settlePayment(Long duePaymentId, double actualAmount, LocalDate settleDate,
                                 String settleVia, boolean createTransaction, String userNotes) {
        DuePayment payment = duePaymentDao.findById(duePaymentId);
        if (payment == null) return false;

        Long txId = null;
        if (createTransaction) {
            // Instantiate ledger transaction matching payment particulars
            Transaction tx = new Transaction();
            tx.setType(payment.getType());
            tx.setTxDate(settleDate != null ? settleDate : LocalDate.now());
            tx.setAmount(actualAmount);
            tx.setPaymentVia(settleVia != null && !settleVia.isEmpty() ? settleVia : "Net Banking");
            tx.setCategory(payment.getCategory());
            tx.setSubCategory(payment.getSubCategory());
            tx.setExpected(false);

            // Construct descriptive ledger notes
            String tag = payment.isAutoPay() ? "[Auto-Payment]" : "[Manual Payment]";
            String noteCombined = tag + " Settled for " + payment.getTitle();
            if (userNotes != null && !userNotes.trim().isEmpty()) {
                noteCombined += ": " + userNotes.trim();
            }
            tx.setNotes(noteCombined);

            // Auto-link matching master entity (EMI, Credit Card, or Bill)
            String pTitle = payment.getTitle() != null ? payment.getTitle().toLowerCase().trim() : "";
            String pSubCat = payment.getSubCategory() != null ? payment.getSubCategory().toLowerCase().trim() : "";
            for (com.navfinance.model.Emi emi : financeService.getAllEmis()) {
                if (emi.getEmiName() != null && !emi.getEmiName().isBlank()) {
                    String eName = emi.getEmiName().toLowerCase().trim();
                    if (pTitle.contains(eName) || (!pSubCat.isEmpty() && pSubCat.contains(eName))) {
                        tx.setEmiId(emi.getId());
                        break;
                    }
                }
            }
            if (tx.getEmiId() == null) {
                for (com.navfinance.model.CreditCard cc : financeService.getAllCreditCards()) {
                    if (cc.getCardName() != null && !cc.getCardName().isBlank()) {
                        String cName = cc.getCardName().toLowerCase().trim();
                        if (pTitle.contains(cName) || (!pSubCat.isEmpty() && pSubCat.contains(cName))) {
                            tx.setCreditCardId(cc.getId());
                            break;
                        }
                    }
                }
            }
            if (tx.getEmiId() == null && tx.getCreditCardId() == null) {
                for (com.navfinance.model.Bill bill : financeService.getAllBills()) {
                    if (bill.getBillName() != null && !bill.getBillName().isBlank()) {
                        String bName = bill.getBillName().toLowerCase().trim();
                        if (pTitle.contains(bName) || (!pSubCat.isEmpty() && pSubCat.contains(bName))) {
                            tx.setBillId(bill.getId());
                            break;
                        }
                    }
                }
            }

            // Persist the transaction via FinanceService
            Transaction savedTx = financeService.createTransaction(tx);
            if (savedTx != null) {
                txId = savedTx.getId();
            }
        }

        // Mark the due payment record as SETTLED in the database
        boolean ok = duePaymentDao.markSettled(duePaymentId, actualAmount, settleDate, settleVia, txId);
        if (ok) {
            String details = String.format("Settled payment '%s' for %s via %s on %s (%s)",
                    payment.getTitle(), AppConfig.formatCurrency(actualAmount), settleVia,
                    settleDate, payment.getAutoPayDisplay());
            auditDao.log(new AuditLog("DUE_PAYMENT", duePaymentId, "SETTLE", details));
        }
        return ok;
    }

    /**
     * Reverts a settled payment back to pending (or overdue based on current date).
     * Optionally deletes the linked ledger transaction if one was originally generated.
     *
     * @param duePaymentId ID of the due payment record
     * @param deleteLinkedTransaction {@code true} to delete the associated transaction in ledger
     * @return {@code true} if reverted successfully, {@code false} otherwise
     */
    public boolean markUnsettled(Long duePaymentId, boolean deleteLinkedTransaction) {
        DuePayment payment = duePaymentDao.findById(duePaymentId);
        if (payment == null) return false;

        // Clean up linked ledger transaction if configured
        if (deleteLinkedTransaction && payment.getTransactionId() != null) {
            financeService.deleteTransaction(payment.getTransactionId());
        }

        // Transition status back in database
        boolean ok = duePaymentDao.markUnsettled(duePaymentId);
        if (ok) {
            auditDao.log(new AuditLog("DUE_PAYMENT", duePaymentId, "UNSETTLE",
                    "Reverted payment '" + payment.getTitle() + "' to unsettled (Linked TX deleted: " + deleteLinkedTransaction + ")"));
        }
        return ok;
    }

    /**
     * Deletes a due payment record and logs an audit trail entry.
     *
     * @param id primary key of the due payment
     * @return {@code true} if deletion was successful, {@code false} otherwise
     */
    public boolean deleteDuePayment(Long id) {
        DuePayment payment = duePaymentDao.findById(id);
        boolean ok = duePaymentDao.delete(id);
        if (ok && payment != null) {
            auditDao.log(new AuditLog("DUE_PAYMENT", id, "DELETE",
                    "Deleted due payment record: " + payment.getTitle() + " (Due: " + payment.getDueDate() + ")"));
        }
        return ok;
    }

    /**
     * Purges invalid due payment records with type != 'EXPENSE' (e.g. income schedules erroneously projected).
     *
     * @return Number of purged invalid records
     */
    public final int cleanupInvalidDuePayments() {
        return duePaymentDao.deleteInvalidIncomeDuePayments();
    }

    /**
     * Settles a due payment on its exact scheduled due date ("on time").
     *
     * @param duePaymentId ID of the due payment to settle
     * @param createTransaction {@code true} to record matching ledger transaction
     * @return {@code true} if successfully settled
     */
    public boolean settlePaymentOnTime(Long duePaymentId, boolean createTransaction) {
        DuePayment p = duePaymentDao.findById(duePaymentId);
        if (p == null || p.isSettled()) return false;

        LocalDate onTimeDate = p.getDueDate() != null ? p.getDueDate() : LocalDate.now();
        double amount = p.getExpectedAmount();
        String paymentVia = "Net Banking";
        if (p.getNotes() != null && p.getNotes().toUpperCase().contains("UPI")) {
            paymentVia = "UPI";
        }

        return settlePayment(duePaymentId, amount, onTimeDate, paymentVia, createTransaction, "Settled on time");
    }

    /**
     * Settles all unsettled due payments within the specified calendar month on time.
     *
     * @param ym target YearMonth
     * @param createTransactions {@code true} to generate matching transactions
     * @return count of payments successfully settled
     */
    public int settleAllForMonthOnTime(java.time.YearMonth ym, boolean createTransactions) {
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        List<DuePayment> list = duePaymentDao.findUnsettledByPeriod(start, end);
        int count = 0;
        for (DuePayment p : list) {
            if (settlePaymentOnTime(p.getId(), createTransactions)) {
                count++;
            }
        }
        return count;
    }

    // =========================================================================
    // Summary & Metrics
    // =========================================================================

    /**
     * Computes real-time analytical metrics across a provided collection of due payments.
     *
     * @param list list of {@link DuePayment} records to analyze
     * @return populated {@link DueSummary} metric container
     */
    public DueSummary computeSummary(List<DuePayment> list) {
        DueSummary summary = new DueSummary();
        LocalDate today = LocalDate.now();

        for (DuePayment p : list) {
            // Only expense liabilities count towards dues and overdue totals
            if (p.getType() != null && p.getType() != TransactionType.EXPENSE) {
                continue;
            }

            // Tally payment execution mode
            if (p.isAutoPay()) summary.autoPayCount++;
            else summary.manualPayCount++;

            // Partition by settlement and timeliness
            if (p.isSettled()) {
                summary.settledCount++;
                summary.settledTotal += p.getSettledAmount();
            } else if (p.getDueDate() != null && p.getDueDate().isBefore(today)) {
                summary.overdueCount++;
                summary.overdueTotal += p.getExpectedAmount();
            } else {
                summary.duePendingCount++;
                summary.duePendingTotal += p.getExpectedAmount();
            }
        }
        return summary;
    }

    /**
     * Identifies whether a recurring schedule rule corresponds to an EMI loan installment.
     *
     * @param s Recurring schedule
     * @return {@code true} if categorized as an EMI
     */
    public boolean isEmiSchedule(RecurringSchedule s) {
        if (s == null) return false;
        String cat = s.getCategory() != null ? s.getCategory().toLowerCase().trim() : "";
        String name = s.getName() != null ? s.getName().toLowerCase().trim() : "";
        String sub = s.getSubCategory() != null ? s.getSubCategory().toLowerCase().trim() : "";
        return cat.contains("emi") || cat.contains("loan") || name.contains("emi") || name.contains("loan") || sub.contains("emi");
    }

    /**
     * Identifies whether a recurring schedule rule corresponds to a Credit Card statement payment.
     *
     * @param s Recurring schedule
     * @return {@code true} if categorized as a Credit Card bill
     */
    public boolean isCreditCardSchedule(RecurringSchedule s) {
        if (s == null) return false;
        String cat = s.getCategory() != null ? s.getCategory().toLowerCase().trim() : "";
        String name = s.getName() != null ? s.getName().toLowerCase().trim() : "";
        String sub = s.getSubCategory() != null ? s.getSubCategory().toLowerCase().trim() : "";
        return cat.contains("credit card") || cat.contains("card") || name.contains("credit card") || sub.contains("credit card");
    }

    /**
     * Identifies whether a recurring schedule rule corresponds to a utility or recurring household bill.
     *
     * @param s Recurring schedule
     * @return {@code true} if categorized as a utility bill
     */
    public boolean isBillSchedule(RecurringSchedule s) {
        if (s == null || s.getType() != TransactionType.EXPENSE) return false;
        if (isEmiSchedule(s) || isCreditCardSchedule(s)) return false;
        String cat = s.getCategory() != null ? s.getCategory().toLowerCase().trim() : "";
        String name = s.getName() != null ? s.getName().toLowerCase().trim() : "";
        String sub = s.getSubCategory() != null ? s.getSubCategory().toLowerCase().trim() : "";
        if (cat.contains("bill") || cat.contains("utilit") || cat.contains("rent") || cat.contains("postpaid")
                || cat.contains("broadband") || cat.contains("wifi") || cat.contains("insurance") || cat.contains("subscription")
                || name.contains("bill") || name.contains("rent") || sub.contains("bill") || sub.contains("rent")) {
            return true;
        }
        for (com.navfinance.model.Bill b : financeService.getAllBills()) {
            if (b.getBillName() != null && !b.getBillName().isBlank()) {
                String bName = b.getBillName().toLowerCase().trim();
                if (name.contains(bName) || sub.contains(bName)) {
                    return true;
                }
            }
        }
        return !cat.contains("invest") && !cat.contains("saving");
    }

    /**
     * Computes recurring monthly financial commitments across active schedules, breaking them down
     * into EMIs, Fixed Utility Bills, Variable Utility Bills, Credit Cards, and combined totals.
     *
     * @param schedules List of recurring schedules to analyze
     * @return Populated {@link ScheduleCommitmentSummary}
     */
    public ScheduleCommitmentSummary computeCommitmentSummary(List<RecurringSchedule> schedules) {
        ScheduleCommitmentSummary summary = new ScheduleCommitmentSummary();
        if (schedules == null) return summary;

        for (RecurringSchedule s : schedules) {
            if (s == null || !s.isActive()) continue;
            if (s.getType() != TransactionType.EXPENSE) continue;

            double amt = s.getDefaultAmount() > 0 ? s.getDefaultAmount() : 0.0;
            boolean isVariable = s.getAmountType() == AmountType.VARIABLE;

            if (isEmiSchedule(s)) {
                summary.emiTotal += amt;
                summary.emiCount++;
            } else if (isCreditCardSchedule(s)) {
                summary.creditCardTotal += amt;
                summary.creditCardCount++;
            } else if (isBillSchedule(s)) {
                if (isVariable) {
                    summary.variableBillsTotal += amt;
                    summary.variableBillsCount++;
                } else {
                    summary.fixedBillsTotal += amt;
                    summary.fixedBillsCount++;
                }
                summary.totalBillsCommitment += amt;
                summary.totalBillsCount++;
            } else {
                summary.otherExpenseTotal += amt;
                summary.otherExpenseCount++;
            }

            summary.totalCommitment += amt;
            summary.totalActiveRulesCount++;
        }
        return summary;
    }
}
