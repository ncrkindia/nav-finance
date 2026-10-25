package com.navfinance.service;

import com.navfinance.config.AppConfig;
import com.navfinance.dao.AuditDao;
import com.navfinance.dao.BillDao;
import com.navfinance.dao.CategoryDao;
import com.navfinance.dao.CreditCardDao;
import com.navfinance.dao.EmiDao;
import com.navfinance.dao.PaymentModeDao;
import com.navfinance.dao.PersonDao;
import com.navfinance.dao.TransactionDao;
import com.navfinance.dao.TransactionLinkDao;
import com.navfinance.model.AuditLog;
import com.navfinance.model.Bill;
import com.navfinance.model.Category;
import com.navfinance.model.CreditCard;
import com.navfinance.model.Emi;
import com.navfinance.model.EntityTrackerSummary;
import com.navfinance.model.FilterCriteria;
import com.navfinance.model.Person;
import com.navfinance.model.SubCategory;
import com.navfinance.model.Transaction;
import com.navfinance.model.TransactionLink;
import com.navfinance.model.TransactionSettlementInfo;
import com.navfinance.model.TransactionType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Core business orchestration service managing ledger transactions, master entities (Persons,
 * Bills, EMIs, Credit Cards, Categories, Payment Channels), audit tracking, and loans and dues counterparty balance calculations.
 * <p>
 * Ensures audit logging is automatically triggered on every creation, update, and deletion.
 * </p>
 */
public class FinanceService {

    private static FinanceService instance;

    private final TransactionDao transactionDao = new TransactionDao();
    private final TransactionLinkDao transactionLinkDao = new TransactionLinkDao();
    private final PersonDao personDao = new PersonDao();
    private final BillDao billDao = new BillDao();
    private final EmiDao emiDao = new EmiDao();
    private final CreditCardDao creditCardDao = new CreditCardDao();
    private final PaymentModeDao paymentModeDao = new PaymentModeDao();
    private final CategoryDao categoryDao = new CategoryDao();
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

    /**
     * Updates/renames a payment mode and cascades the change to transactions.
     *
     * @param oldName Existing payment mode name
     * @param newName New payment mode name
     * @return {@code true} if updated successfully
     */
    public boolean updatePaymentMode(String oldName, String newName) {
        if (paymentModeDao.updateMode(oldName, newName)) {
            auditDao.log("PAYMENT_MODE", null, "UPDATE", "Renamed payment mode from '" + oldName + "' to '" + newName + "'");
            return true;
        }
        return false;
    }

    /**
     * Deletes a payment mode.
     *
     * @param mode Payment mode name to delete
     * @return {@code true} if deleted successfully
     */
    public boolean deletePaymentMode(String mode) {
        if (paymentModeDao.deleteMode(mode)) {
            auditDao.log("PAYMENT_MODE", null, "DELETE", "Deleted payment mode: " + mode);
            return true;
        }
        return false;
    }

    // --- Masters: Categories & Associated Sub-Categories ---

    /**
     * Retrieves all configured categories with associated sub-categories.
     *
     * @return List of Category entities
     */
    public List<Category> getAllCategories() {
        return categoryDao.findAll();
    }

    /**
     * Retrieves all categories of a given TransactionType (INCOME or EXPENSE).
     *
     * @param type target TransactionType
     * @return List of matching Category entities
     */
    public List<Category> getCategoriesByType(TransactionType type) {
        return categoryDao.findByType(type);
    }

    /**
     * Retrieves distinct category names for the specified TransactionType.
     *
     * @param type target TransactionType (or null for all)
     * @return List of category name strings
     */
    public List<String> getCategoryNames(TransactionType type) {
        List<Category> list = type != null ? categoryDao.findByType(type) : categoryDao.findAll();
        List<String> names = new ArrayList<>();
        for (Category c : list) {
            if (c != null && c.getName() != null && !c.getName().trim().isEmpty()) {
                if (!names.contains(c.getName().trim())) {
                    names.add(c.getName().trim());
                }
            }
        }
        return names;
    }

    /**
     * Retrieves distinct associated sub-category names for a specific category name and transaction type.
     *
     * @param categoryName category name
     * @param type transaction type
     * @return List of sub-category name strings
     */
    public List<String> getSubCategoryNames(String categoryName, TransactionType type) {
        List<String> names = new ArrayList<>();
        if (categoryName == null || categoryName.trim().isEmpty()) return names;

        Category c = categoryDao.findByNameAndType(categoryName.trim(), type);
        if (c != null && c.getSubCategories() != null) {
            for (SubCategory s : c.getSubCategories()) {
                if (s != null && s.getName() != null && !s.getName().trim().isEmpty()) {
                    names.add(s.getName().trim());
                }
            }
        }
        return names;
    }

    /**
     * Saves a new or updated Category.
     *
     * @param category Category entity to save
     * @return persisted Category
     */
    public Category saveCategory(Category category) {
        return saveCategory(category, null);
    }

    /**
     * Saves or updates a Category (and its initial sub-categories) with audit logging.
     *
     * @param category Category entity to save
     * @param oldName previous name if updating
     * @return persisted Category
     */
    public Category saveCategory(Category category, String oldName) {
        if (category == null) return null;
        if (category.getId() == null) {
            Category created = categoryDao.insert(category);
            if (created != null) {
                auditDao.log("CATEGORY", created.getId(), "CREATE", "Created Category: " + created.getName() + " (" + created.getType() + ")");
            }
            return created;
        } else {
            boolean ok = categoryDao.update(category, oldName);
            if (ok) {
                auditDao.log("CATEGORY", category.getId(), "UPDATE", "Updated Category: " + category.getName() + " (" + category.getType() + ")");
            }
            return category;
        }
    }

    /**
     * Deletes a Category and its associated sub-categories with audit logging.
     *
     * @param categoryId ID of category to delete
     * @return true if deleted
     */
    public boolean deleteCategory(Long categoryId) {
        Category c = categoryDao.findById(categoryId);
        boolean ok = categoryDao.delete(categoryId);
        if (ok) {
            auditDao.log("CATEGORY", categoryId, "DELETE", "Deleted Category: " + (c != null ? c.getName() : categoryId));
        }
        return ok;
    }

    /**
     * Adds an associated sub-category under a parent category with audit logging.
     *
     * @param categoryId parent category ID
     * @param name sub-category name
     * @return created SubCategory
     */
    public SubCategory addSubCategory(Long categoryId, String name) {
        SubCategory sub = categoryDao.addSubCategory(categoryId, name);
        if (sub != null) {
            auditDao.log("SUB_CATEGORY", sub.getId(), "CREATE", "Added Associated Category: '" + name + "' to Category #" + categoryId);
        }
        return sub;
    }

    /**
     * Updates an associated sub-category name with audit logging and transaction cascade.
     *
     * @param subCategoryId sub-category ID
     * @param newName new name
     * @param oldName previous name
     * @param categoryName parent category name
     * @return true if updated
     */
    public boolean updateSubCategory(Long subCategoryId, String newName, String oldName, String categoryName) {
        boolean ok = categoryDao.updateSubCategory(subCategoryId, newName, oldName, categoryName);
        if (ok) {
            auditDao.log("SUB_CATEGORY", subCategoryId, "UPDATE", "Renamed Associated Category from '" + oldName + "' to '" + newName + "'");
        }
        return ok;
    }

    /**
     * Deletes an associated sub-category with audit logging.
     *
     * @param subCategoryId sub-category ID
     * @param name sub-category name for audit logging
     * @return true if deleted
     */
    public boolean deleteSubCategory(Long subCategoryId, String name) {
        boolean ok = categoryDao.deleteSubCategory(subCategoryId);
        if (ok) {
            auditDao.log("SUB_CATEGORY", subCategoryId, "DELETE", "Deleted Associated Category: " + (name != null ? name : subCategoryId));
        }
        return ok;
    }

    /**
     * Counts transactions currently referencing a category name.
     *
     * @param categoryName category name
     * @return count
     */
    public int countTransactionsForCategory(String categoryName) {
        return categoryDao.countTransactionsForCategory(categoryName);
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

    // --- Loans & Dues Counterparty Breakdown ---

    /**
     * Encapsulates cumulative loan accounting balances for a specific person.
     * Computes money lent, money returned, money borrowed, money repaid, and net balance.
     */
    public static class PersonLoanSummary {
        private final Person person;
        private double totalGiven = 0; // Money we gave/lent to person
        private double totalReturned = 0; // Money person returned to us
        private double totalTaken = 0; // Money we borrowed from person
        private double totalRepaid = 0; // Money we repaid to person

        public PersonLoanSummary(Person person) {
            this.person = person;
        }

        public Person getPerson() {
            return person;
        }

        public double getTotalGiven() {
            return totalGiven;
        }

        public double getTotalLent() {
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

        public double getTotalBorrowed() {
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
         * @return Net balance in active currency
         */
        public double getNetBalance() {
            double weAreOwed = totalGiven - totalReturned;
            double weOwe = totalTaken - totalRepaid;
            return weAreOwed - weOwe;
        }
    }


    /**
     * Computes consolidated loan and dues positions for all contacts across all historical transactions.
     *
     * @return List of {@link PersonLoanSummary} objects
     */
    public List<PersonLoanSummary> getPersonLoanSummaries() {
        List<Person> persons = getAllPersons();
        Map<Long, PersonLoanSummary> map = new LinkedHashMap<>();
        for (Person p : persons) {
            map.put(p.getId(), new PersonLoanSummary(p));
        }

        FilterCriteria criteria = new FilterCriteria();
        criteria.setSortBy("tx_date");
        List<Transaction> transactions = transactionDao.findByCriteria(criteria);

        for (Transaction tx : transactions) {
            // Outright bills, EMIs, and credit cards are NEVER peer-to-peer loan balances
            if (tx.getBillId() != null || tx.getEmiId() != null || tx.getCreditCardId() != null) {
                continue;
            }
            if (tx.getPersonId() != null && map.containsKey(tx.getPersonId())) {
                PersonLoanSummary summary = map.get(tx.getPersonId());
                String cat = tx.getCategory() != null ? tx.getCategory().toLowerCase() : "";

                if (tx.getType() == TransactionType.EXPENSE) {
                    if (cat.contains("loan given") || cat.contains("lent")) {
                        summary.addGiven(tx.getAmount());
                    } else if (cat.contains("repayment") || cat.contains("repaid")) {
                        summary.addRepaid(tx.getAmount());
                    }
                } else if (tx.getType() == TransactionType.INCOME) {
                    if (cat.contains("return") || cat.contains("returned")) {
                        summary.addReturned(tx.getAmount());
                    } else if (cat.contains("loan") || cat.contains("borrowed")) {
                        summary.addTaken(tx.getAmount());
                    }
                }
            }
        }

        return new ArrayList<>(map.values());
    }

    // ==============================================================================
    // TRANSACTION LINKING & SETTLEMENT METHODS
    // ==============================================================================

    /**
     * Determines whether a transaction is considered a "Give" candidate (e.g. Loan Given, Lent, or Expense to a Person).
     * Outright bills, EMIs, and credit card payments are excluded and never treated as Give candidates.
     *
     * @param tx Target transaction
     * @return {@code true} if this transaction qualifies as a Give transaction
     */
    public boolean isGiveTransaction(Transaction tx) {
        if (tx == null || tx.getType() != TransactionType.EXPENSE) return false;
        if (tx.getBillId() != null || tx.getEmiId() != null || tx.getCreditCardId() != null) return false;
        String cat = tx.getCategory() != null ? tx.getCategory().toLowerCase() : "";
        if (cat.contains("bill") || cat.contains("emi") || cat.contains("credit card") || cat.contains("utility")) return false;
        return cat.contains("loan given") || cat.contains("lent") || tx.getPersonId() != null;
    }

    /**
     * Determines whether a transaction is considered a "Return Received" candidate (e.g. Return, Returned, or Income from a Person).
     *
     * @param tx Target transaction
     * @return {@code true} if this transaction qualifies as a Return transaction
     */
    public boolean isReturnTransaction(Transaction tx) {
        if (tx == null || tx.getType() != TransactionType.INCOME) return false;
        if (tx.getBillId() != null || tx.getEmiId() != null || tx.getCreditCardId() != null) return false;
        String cat = tx.getCategory() != null ? tx.getCategory().toLowerCase() : "";
        return cat.contains("return") || cat.contains("returned") || tx.getPersonId() != null;
    }

    /**
     * Retrieves all links where the transaction participates either as Give or Return.
     *
     * @param txId Transaction primary key ID
     * @return List of {@link TransactionLink} entities
     */
    public List<TransactionLink> getTransactionLinks(Long txId) {
        if (txId == null) return new ArrayList<>();
        return transactionLinkDao.findByTransactionId(txId);
    }

    /**
     * Links a Give transaction with a Return transaction for a specific settled amount.
     *
     * @param giveTxId   Give transaction ID (Expense)
     * @param returnTxId Return transaction ID (Income)
     * @param amount     Allocated settlement amount
     * @param notes      Optional link remarks
     * @return Created link entity
     */
    public TransactionLink linkTransactions(Long giveTxId, Long returnTxId, double amount, String notes) {
        TransactionLink link = new TransactionLink(giveTxId, returnTxId, amount, notes);
        TransactionLink saved = transactionLinkDao.insert(link);
        if (saved != null) {
            auditDao.log("TRANSACTION_LINK", saved.getId(), "CREATE",
                    String.format("Linked Give #%d with Return #%d for amount %s",
                            giveTxId, returnTxId, AppConfig.formatCurrency(amount)));
        }
        return saved;
    }

    /**
     * Deletes an existing settlement link.
     *
     * @param linkId Primary key of the link
     * @return {@code true} if deleted successfully
     */
    public boolean removeTransactionLink(Long linkId) {
        boolean ok = transactionLinkDao.delete(linkId);
        if (ok) {
            auditDao.log("TRANSACTION_LINK", linkId, "DELETE", "Removed link #" + linkId);
        }
        return ok;
    }

    /**
     * Removes all links associated with a transaction.
     *
     * @param txId Transaction ID
     * @return Number of links deleted
     */
    public int deleteLinksForTransaction(Long txId) {
        int count = transactionLinkDao.deleteByTransactionId(txId);
        if (count > 0) {
            auditDao.log("TRANSACTION_LINK", txId, "DELETE", "Removed all links (" + count + ") for tx #" + txId);
        }
        return count;
    }

    /**
     * Retrieves eligible open Give transactions (Expense / Lent) for a specific person.
     *
     * @param personId    Optional person filter
     * @param excludeTxId Transaction to exclude (e.g. currently editing tx)
     * @return List of candidates
     */
    public List<TransactionLinkDao.EligibleCandidate> getEligibleGiveTransactions(Long personId, Long excludeTxId) {
        return transactionLinkDao.getEligibleGiveTransactions(personId, excludeTxId);
    }

    /**
     * Retrieves eligible open Return transactions (Income / Repayment) for a specific person.
     *
     * @param personId    Optional person filter
     * @param excludeTxId Transaction to exclude (e.g. currently editing tx)
     * @return List of candidates
     */
    public List<TransactionLinkDao.EligibleCandidate> getEligibleReturnTransactions(Long personId, Long excludeTxId) {
        return transactionLinkDao.getEligibleReturnTransactions(personId, excludeTxId);
    }

    /**
     * Computes the settlement info for a single transaction.
     *
     * @param tx Target transaction
     * @return {@link TransactionSettlementInfo}
     */
    public TransactionSettlementInfo getSettlementInfo(Transaction tx) {
        if (tx == null) return null;
        TransactionSettlementInfo info = new TransactionSettlementInfo(tx.getId(), tx.getAmount());
        info.setCandidate(isGiveTransaction(tx) || isReturnTransaction(tx));

        if (tx.getId() != null) {
            List<TransactionLink> links = transactionLinkDao.findByTransactionId(tx.getId());
            for (TransactionLink link : links) {
                info.addLink(link);
            }
        }
        info.recalculateStatus();
        return info;
    }

    /**
     * Computes settlement info for a batch of transactions in an efficient single query,
     * avoiding N+1 round trips to the database.
     *
     * @param transactions List of transactions
     * @return Map of transaction ID to {@link TransactionSettlementInfo}
     */
    public Map<Long, TransactionSettlementInfo> getSettlementInfoMap(List<Transaction> transactions) {
        Map<Long, TransactionSettlementInfo> result = new HashMap<>();
        if (transactions == null || transactions.isEmpty()) return result;

        for (Transaction tx : transactions) {
            if (tx.getId() != null) {
                TransactionSettlementInfo info = new TransactionSettlementInfo(tx.getId(), tx.getAmount());
                info.setCandidate(isGiveTransaction(tx) || isReturnTransaction(tx));
                result.put(tx.getId(), info);
            }
        }

        List<TransactionLink> allLinks = transactionLinkDao.findAllLinks();
        for (TransactionLink link : allLinks) {
            TransactionSettlementInfo giveInfo = result.get(link.getGiveTxId());
            if (giveInfo != null) {
                giveInfo.addLink(link);
            }

            TransactionSettlementInfo returnInfo = result.get(link.getReturnTxId());
            if (returnInfo != null) {
                returnInfo.addLink(link);
            }
        }

        for (TransactionSettlementInfo info : result.values()) {
            info.recalculateStatus();
        }

        return result;
    }

    /**
     * Persists or updates a transaction along with its associated counterpart links.
     *
     * @param tx    Transaction to save
     * @param links Selected settlement links
     * @param oldTx Old transaction state if editing, or {@code null} if new
     * @return Persisted transaction
     */
    public Transaction saveTransactionWithLinks(Transaction tx, List<TransactionLink> links, Transaction oldTx) {
        Transaction saved;
        if (oldTx == null || tx.getId() == null) {
            saved = createTransaction(tx);
        } else {
            updateTransaction(tx, oldTx);
            saved = tx;
            transactionLinkDao.deleteByTransactionId(saved.getId());
        }

        if (saved != null && links != null) {
            for (TransactionLink link : links) {
                if (saved.getType() == TransactionType.EXPENSE) {
                    link.setGiveTxId(saved.getId());
                } else {
                    link.setReturnTxId(saved.getId());
                }
                if (link.getGiveTxId() != null && link.getReturnTxId() != null && link.getAmount() > 0.001) {
                    transactionLinkDao.insert(link);
                }
            }
        }

        return saved;
    }

    /**
     * Delegates entity tracking analytics computation to {@link AnalyticsService}.
     *
     * @param entityType Master entity type: "PERSON", "BILL", "EMI", "CREDIT_CARD", or "CATEGORY"
     * @param entityId Primary key ID of entity (or null if category)
     * @param categoryName Category string name
     * @param timeGranularity Time bucket: "DAYS", "WEEK", "MONTHS", "QUARTER", or "YEARS"
     * @param startDate Optional start date limit
     * @param endDate Optional end date limit
     * @return Fully populated {@link EntityTrackerSummary}
     */
    public EntityTrackerSummary getEntityTrackerSummary(String entityType, Long entityId, String categoryName,
                                                        String timeGranularity, LocalDate startDate, LocalDate endDate) {
        return AnalyticsService.getInstance().getEntityTrackerSummary(entityType, entityId, categoryName, timeGranularity, startDate, endDate);
    }
}
