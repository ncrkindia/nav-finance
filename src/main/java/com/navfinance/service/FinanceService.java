package com.navfinance.service;

import com.navfinance.config.AppConfig;
import com.navfinance.dao.AuditDao;
import com.navfinance.dao.BillDao;
import com.navfinance.dao.CreditCardDao;
import com.navfinance.dao.EmiDao;
import com.navfinance.dao.PaymentModeDao;
import com.navfinance.dao.PersonDao;
import com.navfinance.dao.TransactionDao;
import com.navfinance.model.AuditLog;
import com.navfinance.model.Bill;
import com.navfinance.model.CreditCard;
import com.navfinance.model.Emi;
import com.navfinance.model.FilterCriteria;
import com.navfinance.model.Person;
import com.navfinance.model.Transaction;
import com.navfinance.model.TransactionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Core business orchestration service managing ledger transactions, master entities (Persons,
 * Bills, EMIs, Credit Cards, Payment Channels), audit tracking, and Udhar counterparty balance calculations.
 * <p>
 * Ensures audit logging is automatically triggered on every creation, update, and deletion.
 * </p>
 */
public class FinanceService {

    private static FinanceService instance;

    private final TransactionDao transactionDao = new TransactionDao();
    private final PersonDao personDao = new PersonDao();
    private final BillDao billDao = new BillDao();
    private final EmiDao emiDao = new EmiDao();
    private final CreditCardDao creditCardDao = new CreditCardDao();
    private final PaymentModeDao paymentModeDao = new PaymentModeDao();
    private final AuditDao auditDao = new AuditDao();

    /**
     * Private constructor for singleton pattern.
     */
    private FinanceService() {}

    /**
     * Returns the singleton instance of {@code FinanceService}.
     *
     * @return singleton service instance
     */
    public static synchronized FinanceService getInstance() {
        if (instance == null) {
            instance = new FinanceService();
        }
        return instance;
    }

    // --- Transactions ---

    /**
     * Creates a new financial transaction, persists it in the database, and records an audit log.
     *
     * @param tx Transaction entity to create
     * @return Persisted transaction with generated ID
     */
    public Transaction createTransaction(Transaction tx) {
        Transaction created = transactionDao.insert(tx);
        String details = String.format("Created %s of %s on %s | Category: %s (%s) | Via: %s%s",
                tx.getType().getDisplayName(),
                AppConfig.formatCurrency(tx.getAmount()),
                tx.getTxDate() != null ? tx.getTxDate().format(AppConfig.DATE_FORMATTER) : "N/A",
                tx.getCategory(),
                tx.getSubCategory() != null ? tx.getSubCategory() : "-",
                tx.getPaymentVia() != null ? tx.getPaymentVia() : "N/A",
                tx.getNotes() != null && !tx.getNotes().isEmpty() ? " | Notes: " + tx.getNotes() : ""
        );
        auditDao.log("TRANSACTION", created.getId(), "CREATE", details);
        return created;
    }

    /**
     * Updates an existing transaction in the ledger, computes field-level differences,
     * and logs the mutation details to the audit table.
     *
     * @param updated Transaction entity with new values
     * @param old Original transaction state for diff logging
     * @return {@code true} if updated successfully
     */
    public boolean updateTransaction(Transaction updated, Transaction old) {
        boolean ok = transactionDao.update(updated);
        if (ok) {
            StringBuilder diff = new StringBuilder(String.format("Updated Transaction #%d:", updated.getId()));
            if (old != null) {
                if (old.getAmount() != updated.getAmount()) {
                    diff.append(String.format(" Amount changed from %s to %s;", AppConfig.formatCurrency(old.getAmount()), AppConfig.formatCurrency(updated.getAmount())));
                }
                if (!old.getType().equals(updated.getType())) {
                    diff.append(String.format(" Type: %s -> %s;", old.getType(), updated.getType()));
                }
                if (old.getTxDate() != null && !old.getTxDate().equals(updated.getTxDate())) {
                    diff.append(String.format(" Date: %s -> %s;", old.getTxDate(), updated.getTxDate()));
                }
                if (!String.valueOf(old.getCategory()).equals(String.valueOf(updated.getCategory()))) {
                    diff.append(String.format(" Category: %s -> %s;", old.getCategory(), updated.getCategory()));
                }
                if (!String.valueOf(old.getSubCategory()).equals(String.valueOf(updated.getSubCategory()))) {
                    diff.append(String.format(" Sub-Category: %s -> %s;", old.getSubCategory(), updated.getSubCategory()));
                }
                if (!String.valueOf(old.getPaymentVia()).equals(String.valueOf(updated.getPaymentVia()))) {
                    diff.append(String.format(" Via: %s -> %s;", old.getPaymentVia(), updated.getPaymentVia()));
                }
            } else {
                diff.append(" Changes applied.");
            }
            auditDao.log("TRANSACTION", updated.getId(), "UPDATE", diff.toString());
        }
        return ok;
    }

    /**
     * Deletes a transaction from the ledger by its ID and records an audit log.
     *
     * @param id Transaction ID to delete
     * @return {@code true} if deleted successfully
     */
    public boolean deleteTransaction(Long id) {
        Transaction tx = transactionDao.findById(id);
        boolean ok = transactionDao.delete(id);
        if (ok) {
            String details = tx != null ?
                    String.format("Deleted %s #%d of %s on %s | Category: %s",
                            tx.getType(), id, AppConfig.formatCurrency(tx.getAmount()),
                            tx.getTxDate(), tx.getCategory()) :
                    "Deleted Transaction #" + id;
            auditDao.log("TRANSACTION", id, "DELETE", details);
        }
        return ok;
    }

    /**
     * Retrieves a single transaction by primary key ID.
     *
     * @param id Transaction ID
     * @return Transaction entity or {@code null} if not found
     */
    public Transaction getTransactionById(Long id) {
        return transactionDao.findById(id);
    }

    /**
     * Queries transactions matching specified search and filter criteria.
     *
     * @param criteria Filter criteria object
     * @return List of matching transactions
     */
    public List<Transaction> filterTransactions(FilterCriteria criteria) {
        return transactionDao.findByCriteria(criteria);
    }

    /**
     * Retrieves unique category names for a given flow direction.
     *
     * @param type Transaction type filter
     * @return Distinct category names
     */
    public List<String> getCategories(TransactionType type) {
        return transactionDao.getDistinctCategories(type);
    }

    /**
     * Retrieves distinct sub-categories registered under a category.
     *
     * @param category Parent category name
     * @return Distinct sub-category names
     */
    public List<String> getSubCategories(String category) {
        return transactionDao.getDistinctSubCategories(category);
    }

    // --- Masters: Persons ---

    /**
     * Retrieves all contacts registered in the system.
     *
     * @return List of all {@link Person} records
     */
    public List<Person> getAllPersons() {
        return personDao.findAll();
    }

    /**
     * Saves a person (inserts if ID is null, updates otherwise) and records audit log.
     *
     * @param person Person entity
     * @return Persisted person entity
     */
    public Person savePerson(Person person) {
        if (person.getId() == null) {
            Person p = personDao.insert(person);
            auditDao.log("PERSON", p.getId(), "CREATE", "Added Person: " + p.getName() + " (" + p.getRelation() + ")");
            return p;
        } else {
            personDao.update(person);
            auditDao.log("PERSON", person.getId(), "UPDATE", "Updated Person: " + person.getName() + " (" + person.getRelation() + ")");
            return person;
        }
    }

    /**
     * Deletes a person contact and logs the event to audit table.
     *
     * @param id Person ID
     * @return {@code true} if deleted successfully
     */
    public boolean deletePerson(Long id) {
        Person p = personDao.findById(id);
        boolean ok = personDao.delete(id);
        if (ok) {
            auditDao.log("PERSON", id, "DELETE", "Deleted Person: " + (p != null ? p.getName() : id));
        }
        return ok;
    }

    // --- Masters: Bills ---

    /**
     * Retrieves all utility bill master definitions.
     *
     * @return List of all {@link Bill} records
     */
    public List<Bill> getAllBills() {
        return billDao.findAll();
    }

    /**
     * Saves a bill template (inserts or updates) and logs the mutation.
     *
     * @param bill Bill entity
     * @return Persisted bill entity
     */
    public Bill saveBill(Bill bill) {
        if (bill.getId() == null) {
            Bill b = billDao.insert(bill);
            auditDao.log("BILL", b.getId(), "CREATE", "Added Bill Master: " + b.getBillName() + " (" + b.getProvider() + ")");
            return b;
        } else {
            billDao.update(bill);
            auditDao.log("BILL", bill.getId(), "UPDATE", "Updated Bill Master: " + bill.getBillName());
            return bill;
        }
    }

    /**
     * Deletes a bill template and records an audit log.
     *
     * @param id Bill ID
     * @return {@code true} if deleted successfully
     */
    public boolean deleteBill(Long id) {
        Bill b = billDao.findById(id);
        boolean ok = billDao.delete(id);
        if (ok) {
            auditDao.log("BILL", id, "DELETE", "Deleted Bill Master: " + (b != null ? b.getBillName() : id));
        }
        return ok;
    }

    // --- Masters: EMIs ---

    /**
     * Retrieves all EMI loan master templates.
     *
     * @return List of all {@link Emi} records
     */
    public List<Emi> getAllEmis() {
        return emiDao.findAll();
    }

    /**
     * Saves an EMI master loan template and logs the mutation.
     *
     * @param emi EMI entity
     * @return Persisted EMI entity
     */
    public Emi saveEmi(Emi emi) {
        if (emi.getId() == null) {
            Emi e = emiDao.insert(emi);
            auditDao.log("EMI", e.getId(), "CREATE", "Added EMI Master: " + e.getEmiName() + " (" + e.getBankLender() + ")");
            return e;
        } else {
            emiDao.update(emi);
            auditDao.log("EMI", emi.getId(), "UPDATE", "Updated EMI Master: " + emi.getEmiName());
            return emi;
        }
    }

    /**
     * Deletes an EMI master loan template and records an audit log.
     *
     * @param id EMI ID
     * @return {@code true} if deleted successfully
     */
    public boolean deleteEmi(Long id) {
        Emi e = emiDao.findById(id);
        boolean ok = emiDao.delete(id);
        if (ok) {
            auditDao.log("EMI", id, "DELETE", "Deleted EMI Master: " + (e != null ? e.getEmiName() : id));
        }
        return ok;
    }

    // --- Masters: Credit Cards ---

    /**
     * Retrieves all credit card master records.
     *
     * @return List of all {@link CreditCard} records
     */
    public List<CreditCard> getAllCreditCards() {
        return creditCardDao.findAll();
    }

    /**
     * Saves a credit card master record and logs the mutation.
     *
     * @param card CreditCard entity
     * @return Persisted CreditCard entity
     */
    public CreditCard saveCreditCard(CreditCard card) {
        if (card.getId() == null) {
            CreditCard c = creditCardDao.insert(card);
            auditDao.log("CREDIT_CARD", c.getId(), "CREATE", "Added Credit Card: " + c.getCardName());
            return c;
        } else {
            creditCardDao.update(card);
            auditDao.log("CREDIT_CARD", card.getId(), "UPDATE", "Updated Credit Card: " + card.getCardName());
            return card;
        }
    }

    /**
     * Deletes a credit card master record and records an audit log.
     *
     * @param id CreditCard ID
     * @return {@code true} if deleted successfully
     */
    public boolean deleteCreditCard(Long id) {
        CreditCard c = creditCardDao.findById(id);
        boolean ok = creditCardDao.delete(id);
        if (ok) {
            auditDao.log("CREDIT_CARD", id, "DELETE", "Deleted Credit Card: " + (c != null ? c.getCardName() : id));
        }
        return ok;
    }

    // --- Masters: Payment Modes (Via) ---

    /**
     * Retrieves all active payment mode channels.
     *
     * @return List of payment mode names
     */
    public List<String> getAllPaymentModes() {
        return paymentModeDao.findAllModes();
    }

    /**
     * Adds a new custom payment mode and logs the creation.
     *
     * @param mode Custom mode name
     */
    public void addPaymentMode(String mode) {
        if (paymentModeDao.addMode(mode)) {
            auditDao.log("PAYMENT_MODE", null, "CREATE", "Added custom payment mode: " + mode);
        }
    }

    // --- Audit Logs ---

    /**
     * Queries the most recent system audit logs.
     *
     * @param limit Maximum log entries to return
     * @return List of {@link AuditLog} entries
     */
    public List<AuditLog> getAuditLogs(int limit) {
        return auditDao.findAll(limit);
    }

    // --- Udhar Tracker Breakdown ---

    /**
     * Encapsulates cumulative Udhar accounting balances for a specific person.
     * Computes money given, money returned, money borrowed, money repaid, and net balance.
     */
    public static class PersonUdharSummary {
        private final Person person;
        private double totalGiven = 0; // Money we gave to person (Udhar Given)
        private double totalReturned = 0; // Money person returned to us (Return from person)
        private double totalTaken = 0; // Money we borrowed from person (Udhar / Loan taken)
        private double totalRepaid = 0; // Money we repaid to person

        public PersonUdharSummary(Person person) {
            this.person = person;
        }

        public Person getPerson() {
            return person;
        }

        public double getTotalGiven() {
            return totalGiven;
        }

        public void addGiven(double amt) {
            this.totalGiven += amt;
        }

        public double getTotalReturned() {
            return totalReturned;
        }

        public void addReturned(double amt) {
            this.totalReturned += amt;
        }

        public double getTotalTaken() {
            return totalTaken;
        }

        public void addTaken(double amt) {
            this.totalTaken += amt;
        }

        public double getTotalRepaid() {
            return totalRepaid;
        }

        public void addRepaid(double amt) {
            this.totalRepaid += amt;
        }

        /**
         * Calculates net position with this person.
         * Positive means the person owes us money (Asset / Receivable).
         * Negative means we owe the person money (Liability / Payable).
         *
         * @return Net balance in INR
         */
        public double getNetBalance() {
            double weAreOwed = totalGiven - totalReturned;
            double weOwe = totalTaken - totalRepaid;
            return weAreOwed - weOwe;
        }
    }

    /**
     * Computes consolidated Udhar positions for all contacts across all historical transactions.
     *
     * @return List of {@link PersonUdharSummary} objects
     */
    public List<PersonUdharSummary> getPersonUdharSummaries() {
        List<Person> persons = getAllPersons();
        Map<Long, PersonUdharSummary> map = new HashMap<>();
        for (Person p : persons) {
            map.put(p.getId(), new PersonUdharSummary(p));
        }

        FilterCriteria criteria = new FilterCriteria();
        criteria.setSortBy("tx_date");
        List<Transaction> transactions = transactionDao.findByCriteria(criteria);

        for (Transaction tx : transactions) {
            if (tx.getPersonId() != null && map.containsKey(tx.getPersonId())) {
                PersonUdharSummary summary = map.get(tx.getPersonId());
                String cat = tx.getCategory() != null ? tx.getCategory().toLowerCase() : "";

                if (tx.getType() == TransactionType.EXPENSE) {
                    if (cat.contains("udhar given") || cat.contains("loan given")) {
                        summary.addGiven(tx.getAmount());
                    } else if (cat.contains("udhar return") || cat.contains("repayment")) {
                        summary.addRepaid(tx.getAmount());
                    }
                } else if (tx.getType() == TransactionType.INCOME) {
                    if (cat.contains("return")) {
                        summary.addReturned(tx.getAmount());
                    } else if (cat.contains("udhaar") || cat.contains("udhar") || cat.contains("loan")) {
                        summary.addTaken(tx.getAmount());
                    }
                }
            }
        }

        return new ArrayList<>(map.values());
    }
}
