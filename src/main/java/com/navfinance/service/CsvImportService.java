package com.navfinance.service;

import com.navfinance.config.AppConfig;
import com.navfinance.model.Person;
import com.navfinance.model.Transaction;
import com.navfinance.model.TransactionType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to parse, validate, preview, and execute bulk CSV imports for CREATE, UPDATE, and DELETE operations.
 * <p>
 * Ensures pre-flight validation against database foreign keys and formats, providing
 * clear error diagnosis before executing ledger mutations.
 * </p>
 */
public class CsvImportService {

    private static CsvImportService instance;
    private final FinanceService financeService = FinanceService.getInstance();

    /**
     * Private constructor for singleton pattern.
     */
    private CsvImportService() {}

    /**
     * Returns the singleton instance of {@code CsvImportService}.
     *
     * @return singleton service instance
     */
    public static synchronized CsvImportService getInstance() {
        if (instance == null) {
            instance = new CsvImportService();
        }
        return instance;
    }

    /**
     * Data Transfer Object (DTO) holding a parsed CSV row with its validation status and error notes.
     */
    public static class CsvImportRecord {
        private int rowNumber;
        private String action; // CREATE, UPDATE, DELETE
        private Long id;
        private TransactionType type;
        private LocalDate date;
        private boolean expected;
        private double amount;
        private String paymentVia;
        private String category;
        private String subCategory;
        private String personName;
        private String notes;

        private boolean valid = true;
        private String statusMessage = "Valid";

        public int getRowNumber() { return rowNumber; }
        public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public TransactionType getType() { return type; }
        public void setType(TransactionType type) { this.type = type; }

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }

        public boolean isExpected() { return expected; }
        public void setExpected(boolean expected) { this.expected = expected; }

        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }

        public String getPaymentVia() { return paymentVia; }
        public void setPaymentVia(String paymentVia) { this.paymentVia = paymentVia; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getSubCategory() { return subCategory; }
        public void setSubCategory(String subCategory) { this.subCategory = subCategory; }

        public String getPersonName() { return personName; }
        public void setPersonName(String personName) { this.personName = personName; }

        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }

        public String getStatusMessage() { return statusMessage; }
        public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }
    }

    /**
     * Summary outcome of a batch CSV execution containing count metrics and error details.
     */
    public static class ImportSummary {
        private int createdCount = 0;
        private int updatedCount = 0;
        private int deletedCount = 0;
        private int failedCount = 0;
        private final List<String> errorMessages = new ArrayList<>();

        public int getCreatedCount() { return createdCount; }
        public int getUpdatedCount() { return updatedCount; }
        public int getDeletedCount() { return deletedCount; }
        public int getFailedCount() { return failedCount; }
        public List<String> getErrorMessages() { return errorMessages; }

        /**
         * Gets the total count of successfully processed entries.
         *
         * @return sum of created, updated, and deleted rows
         */
        public int getTotalProcessed() {
            return createdCount + updatedCount + deletedCount;
        }
    }

    /**
     * Downloads/exports a ready-to-use CSV template pre-populated with clear format comments
     * and illustrative samples for CREATE, UPDATE, and DELETE operations.
     *
     * @param targetFile File to write the template to
     * @throws Exception on file I/O error
     */
    public void generateCsvTemplate(File targetFile) throws Exception {
        try (PrintWriter writer = new PrintWriter(new FileWriter(targetFile, false))) {
            writer.println("# Nav Finance - Transaction Import/Update/Delete Template");
            writer.println("# Instructions:");
            writer.println("# 1. Action: CREATE (for new entry), UPDATE (modifies existing ID), DELETE (deletes existing ID)");
            writer.println("# 2. ID: Leave blank for CREATE; Required for UPDATE and DELETE.");
            writer.println("# 3. Type: INCOME or EXPENSE");
            writer.println("# 4. Date format: YYYY-MM-DD (e.g. 2026-10-24)");
            writer.println("# 5. Expected: true or false");
            writer.println("# -------------------------------------------------------------");
            writer.println("Action,ID,Type,Date,Expected,Amount,Via,Category,SubCategory,Person,Notes");

            // Sample 1: Create new Income
            writer.println("CREATE,,INCOME,2026-10-01,false,95000.00,Net Banking,Salary,Tech Corp India,,Monthly salary credit");
            // Sample 2: Create new Bill Expense
            writer.println("CREATE,,EXPENSE,2026-10-04,false,1199.00,Paytm,Bill,Airtel Bill,,Home fiber bill");
            // Sample 3: Create new EMI Expense
            writer.println("CREATE,,EXPENSE,2026-10-05,false,28500.00,Net Banking,EMI Payment,Home Loan EMI,,Auto-debit for Home loan");
            // Sample 4: Create new Udhar Given
            writer.println("CREATE,,EXPENSE,2026-10-15,false,5000.00,UPI,Udhar Given,Rahul Sharma,Rahul Sharma,Urgent loan to friend");
            // Sample 5: Update existing entry (replace 1 with your actual ID)
            writer.println("UPDATE,1,INCOME,2026-10-01,false,98500.00,Net Banking,Salary,Tech Corp India,,Salary with festival bonus");
            // Sample 6: Delete entry by ID (replace 999 with target ID)
            writer.println("DELETE,999,,,,,,,,,,Deleting unwanted entry");
        }
    }

    /**
     * Reads a CSV file line-by-line, validating syntax, IDs, categories, dates, and amounts,
     * and produces a list of {@link CsvImportRecord} objects for user preview.
     *
     * @param csvFile Uploaded or selected CSV file
     * @return List of parsed and validated records
     * @throws Exception on file reading error
     */
    public List<CsvImportRecord> parseAndValidateCsv(File csvFile) throws Exception {
        List<CsvImportRecord> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line;
            int lineNum = 0;
            boolean headerFound = false;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();

                // Skip comment lines and empty lines
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Check for header
                if (!headerFound) {
                    if (line.toLowerCase().contains("action") && line.toLowerCase().contains("type")) {
                        headerFound = true;
                        continue;
                    }
                }

                List<String> tokens = parseCsvLine(line);
                if (tokens.isEmpty()) continue;

                CsvImportRecord record = new CsvImportRecord();
                record.setRowNumber(lineNum);

                // Action
                String action = tokens.size() > 0 ? tokens.get(0).trim().toUpperCase() : "CREATE";
                if (action.isEmpty() || action.equals("ADD")) action = "CREATE";
                record.setAction(action);

                // ID
                String strId = tokens.size() > 1 ? tokens.get(1).trim() : "";
                if (!strId.isEmpty()) {
                    try {
                        record.setId(Long.parseLong(strId));
                    } catch (NumberFormatException e) {
                        record.setValid(false);
                        record.setStatusMessage("Invalid ID format: " + strId);
                    }
                }

                // Validation for Action & ID
                if ("UPDATE".equals(action) || "DELETE".equals(action)) {
                    if (record.getId() == null) {
                        record.setValid(false);
                        record.setStatusMessage(action + " requires a valid Transaction ID.");
                    } else {
                        Transaction existing = financeService.getTransactionById(record.getId());
                        if (existing == null) {
                            record.setValid(false);
                            record.setStatusMessage("Transaction ID #" + record.getId() + " not found in database.");
                        }
                    }
                }

                // If DELETE, further fields are not strictly required
                if ("DELETE".equals(action)) {
                    if (tokens.size() > 10) record.setNotes(tokens.get(10));
                    records.add(record);
                    continue;
                }

                // Type
                String strType = tokens.size() > 2 ? tokens.get(2).trim().toUpperCase() : "";
                if ("INCOME".equals(strType) || "IN".equals(strType)) {
                    record.setType(TransactionType.INCOME);
                } else if ("EXPENSE".equals(strType) || "OUT".equals(strType)) {
                    record.setType(TransactionType.EXPENSE);
                } else {
                    record.setValid(false);
                    record.setStatusMessage("Type must be INCOME or EXPENSE (got: " + strType + ")");
                }

                // Date
                String strDate = tokens.size() > 3 ? tokens.get(3).trim() : "";
                if (!strDate.isEmpty()) {
                    LocalDate d = parseDateLenient(strDate);
                    if (d != null) {
                        record.setDate(d);
                    } else {
                        record.setValid(false);
                        record.setStatusMessage("Invalid date format: " + strDate + " (use YYYY-MM-DD)");
                    }
                } else {
                    record.setDate(LocalDate.now());
                }

                // Expected
                String strExp = tokens.size() > 4 ? tokens.get(4).trim().toLowerCase() : "false";
                record.setExpected("true".equals(strExp) || "yes".equals(strExp) || "1".equals(strExp));

                // Amount
                String strAmount = tokens.size() > 5 ? tokens.get(5).trim() : "0";
                try {
                    double amt = Double.parseDouble(strAmount.replace(",", "").replace(AppConfig.getCurrencySymbol(), "").trim());
                    if (amt <= 0) {
                        record.setValid(false);
                        record.setStatusMessage("Amount must be greater than zero.");
                    } else {
                        record.setAmount(amt);
                    }
                } catch (NumberFormatException e) {
                    record.setValid(false);
                    record.setStatusMessage("Invalid numerical amount: " + strAmount);
                }

                // Via
                record.setPaymentVia(tokens.size() > 6 && !tokens.get(6).trim().isEmpty() ? tokens.get(6).trim() : "Cash");

                // Category
                String cat = tokens.size() > 7 ? tokens.get(7).trim() : "";
                if (cat.isEmpty()) {
                    record.setValid(false);
                    record.setStatusMessage("Category cannot be empty.");
                } else {
                    record.setCategory(cat);
                }

                // SubCategory
                record.setSubCategory(tokens.size() > 8 ? tokens.get(8).trim() : "");

                // Person
                record.setPersonName(tokens.size() > 9 ? tokens.get(9).trim() : "");

                // Notes
                record.setNotes(tokens.size() > 10 ? tokens.get(10).trim() : "");

                records.add(record);
            }
        }

        return records;
    }

    /**
     * Executes validated CSV import operations (CREATE, UPDATE, DELETE) against the database ledger,
     * maintaining audit logs and returning aggregate outcome statistics.
     *
     * @param records List of parsed {@link CsvImportRecord} entries
     * @return Execution summary containing counts of created, updated, deleted, and failed operations
     */
    public ImportSummary executeImport(List<CsvImportRecord> records) {
        ImportSummary summary = new ImportSummary();
        List<Person> allPersons = financeService.getAllPersons();

        for (CsvImportRecord rec : records) {
            if (!rec.isValid()) {
                summary.failedCount++;
                summary.errorMessages.add("Row " + rec.getRowNumber() + ": " + rec.getStatusMessage());
                continue;
            }

            try {
                if ("DELETE".equalsIgnoreCase(rec.getAction())) {
                    boolean ok = financeService.deleteTransaction(rec.getId());
                    if (ok) {
                        summary.deletedCount++;
                    } else {
                        summary.failedCount++;
                        summary.errorMessages.add("Row " + rec.getRowNumber() + ": Failed to delete ID #" + rec.getId());
                    }

                } else if ("UPDATE".equalsIgnoreCase(rec.getAction())) {
                    Transaction existing = financeService.getTransactionById(rec.getId());
                    if (existing != null) {
                        Transaction updated = buildTransactionFromRecord(rec, allPersons);
                        updated.setId(rec.getId());
                        boolean ok = financeService.updateTransaction(updated, existing);
                        if (ok) {
                            summary.updatedCount++;
                        } else {
                            summary.failedCount++;
                            summary.errorMessages.add("Row " + rec.getRowNumber() + ": Failed to update ID #" + rec.getId());
                        }
                    } else {
                        summary.failedCount++;
                        summary.errorMessages.add("Row " + rec.getRowNumber() + ": ID #" + rec.getId() + " not found.");
                    }

                } else {
                    // CREATE
                    Transaction newTx = buildTransactionFromRecord(rec, allPersons);
                    financeService.createTransaction(newTx);
                    summary.createdCount++;
                }
            } catch (Exception ex) {
                summary.failedCount++;
                summary.errorMessages.add("Row " + rec.getRowNumber() + ": " + ex.getMessage());
            }
        }

        return summary;
    }

    private Transaction buildTransactionFromRecord(CsvImportRecord rec, List<Person> allPersons) {
        Transaction tx = new Transaction();
        tx.setType(rec.getType());
        tx.setTxDate(rec.getDate() != null ? rec.getDate() : LocalDate.now());
        tx.setExpected(rec.isExpected());
        tx.setAmount(rec.getAmount());
        tx.setPaymentVia(rec.getPaymentVia());
        tx.setCategory(rec.getCategory());
        tx.setSubCategory(rec.getSubCategory());
        tx.setNotes(rec.getNotes());

        // Match person ID if personName matches or subcategory matches person
        Long personId = null;
        String matchName = rec.getPersonName() != null && !rec.getPersonName().isEmpty() ?
                rec.getPersonName() : rec.getSubCategory();

        if (matchName != null && !matchName.isEmpty()) {
            for (Person p : allPersons) {
                if (p.getName().equalsIgnoreCase(matchName)) {
                    personId = p.getId();
                    break;
                }
            }
        }
        tx.setPersonId(personId);

        // Ensure payment mode is saved
        if (rec.getPaymentVia() != null && !rec.getPaymentVia().isEmpty()) {
            financeService.addPaymentMode(rec.getPaymentVia());
        }

        return tx;
    }

    private List<String> parseCsvLine(String line) {
        List<String> list = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                list.add(cur.toString().trim());
                cur = new StringBuilder();
            } else {
                cur.append(c);
            }
        }
        list.add(cur.toString().trim());
        return list;
    }

    private LocalDate parseDateLenient(String str) {
        try {
            return LocalDate.parse(str, AppConfig.ISO_DATE_FORMATTER);
        } catch (Exception ignored) {}
        try {
            return LocalDate.parse(str, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        } catch (Exception ignored) {}
        try {
            return LocalDate.parse(str, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception ignored) {}
        return null;
    }
}
