package com.navfinance.database;

import com.navfinance.config.AppConfig;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central database controller managing embedded H2 connections, schema migrations,
 * table indexing, initial sample data population, and full CSV table exports.
 * <p>
 * Database files are stored locally under the user's home directory:
 * {@code ~user/nav-finance/navfinance_db.mv.db}.
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public class DatabaseManager {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static DatabaseManager instance;

    /**
     * Private constructor initializing database schema and seed data.
     */
    private DatabaseManager() {
        initDatabase();
    }

    /**
     * Returns the singleton instance of the DatabaseManager.
     *
     * @return The active {@link DatabaseManager} instance.
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Establishes and returns a live connection to the embedded H2 database.
     *
     * @return A valid {@link Connection} instance.
     * @throws SQLException If a connection error occurs.
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(AppConfig.JDBC_URL, AppConfig.DB_USER, AppConfig.DB_PASSWORD);
    }

    /**
     * Initializes all relational database tables, foreign keys, and indexes if they do not exist.
     */
    private void initDatabase() {
        // Ensure ~/nav-finance directory exists
        if (!AppConfig.APP_DATA_DIR.exists()) {
            boolean created = AppConfig.APP_DATA_DIR.mkdirs();
            LOGGER.log(Level.INFO, "Created app directory at: {0} (success: {1})",
                    new Object[]{AppConfig.APP_DATA_DIR.getAbsolutePath(), created});
        }

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Persons Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS persons (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(150) NOT NULL,
                    contact VARCHAR(50),
                    relation VARCHAR(100),
                    notes VARCHAR(500),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);

            // Bills Table (e.g. Airtel Bill, Electricity Khardoni, Noida)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bills (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    bill_name VARCHAR(150) NOT NULL,
                    provider VARCHAR(100),
                    default_amount DECIMAL(15,2),
                    notes VARCHAR(500),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);

            // EMI Master Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS emis (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    emi_name VARCHAR(150) NOT NULL,
                    bank_lender VARCHAR(100),
                    monthly_amount DECIMAL(15,2),
                    notes VARCHAR(500),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);

            // Credit Cards Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS credit_cards (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    card_name VARCHAR(100) NOT NULL,
                    bank VARCHAR(100),
                    last_digits VARCHAR(10),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);

            // Payment Modes Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS payment_modes (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    mode_name VARCHAR(50) NOT NULL UNIQUE,
                    is_system BOOLEAN DEFAULT FALSE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);

            // Transactions Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    type VARCHAR(10) NOT NULL, -- INCOME / EXPENSE
                    tx_date DATE NOT NULL,
                    is_expected BOOLEAN DEFAULT FALSE,
                    amount DECIMAL(15,2) NOT NULL,
                    payment_via VARCHAR(50),
                    category VARCHAR(100) NOT NULL,
                    sub_category VARCHAR(150),
                    person_id BIGINT,
                    bill_id BIGINT,
                    emi_id BIGINT,
                    credit_card_id BIGINT,
                    notes VARCHAR(1000),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE SET NULL,
                    FOREIGN KEY (bill_id) REFERENCES bills(id) ON DELETE SET NULL,
                    FOREIGN KEY (emi_id) REFERENCES emis(id) ON DELETE SET NULL,
                    FOREIGN KEY (credit_card_id) REFERENCES credit_cards(id) ON DELETE SET NULL
                );
            """);

            // Audit Logs Table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS audit_logs (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    entity_type VARCHAR(50) NOT NULL,
                    entity_id BIGINT,
                    action VARCHAR(20) NOT NULL, -- CREATE, UPDATE, DELETE
                    details VARCHAR(2000),
                    log_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);

            // Recurring Schedules Table (e.g. EMI on 5th of every month, Electricity bill on 15th)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS recurring_schedules (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(150) NOT NULL,
                    type VARCHAR(20) NOT NULL,
                    category VARCHAR(100) NOT NULL,
                    sub_category VARCHAR(150),
                    day_of_month INT NOT NULL,
                    amount_type VARCHAR(20) NOT NULL, -- FIXED or VARIABLE
                    default_amount DECIMAL(15,2) DEFAULT 0.0,
                    start_date DATE NOT NULL,
                    end_date DATE,
                    payment_via VARCHAR(50) DEFAULT 'Net Banking',
                    is_auto_pay BOOLEAN DEFAULT FALSE,
                    notes VARCHAR(500),
                    active BOOLEAN DEFAULT TRUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);

            // Due Payments Table (individual monthly due instances)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS due_payments (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    schedule_id BIGINT,
                    title VARCHAR(150) NOT NULL,
                    type VARCHAR(20) NOT NULL,
                    category VARCHAR(100) NOT NULL,
                    sub_category VARCHAR(150),
                    due_date DATE NOT NULL,
                    expected_amount DECIMAL(15,2) DEFAULT 0.0,
                    is_variable BOOLEAN DEFAULT FALSE,
                    is_auto_pay BOOLEAN DEFAULT FALSE,
                    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, OVERDUE, SETTLED
                    settled_amount DECIMAL(15,2) DEFAULT 0.0,
                    settled_date DATE,
                    settled_via VARCHAR(50),
                    transaction_id BIGINT,
                    notes VARCHAR(500),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (schedule_id) REFERENCES recurring_schedules(id) ON DELETE SET NULL,
                    FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE SET NULL
                );
            """);

            // Create helpful indices
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_tx_date ON transactions(tx_date);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_tx_type ON transactions(type);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_tx_category ON transactions(category);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_due_date ON due_payments(due_date);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_due_status ON due_payments(status);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_audit_time ON audit_logs(log_timestamp);");

            // Seed master data if tables are empty
            seedInitialData(conn);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize H2 database schema", e);
            throw new RuntimeException("Database initialization error: " + e.getMessage(), e);
        }
    }

    private void seedInitialData(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Seed Payment Modes
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM payment_modes");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO payment_modes (mode_name, is_system) VALUES ('Cash', TRUE)");
                stmt.execute("INSERT INTO payment_modes (mode_name, is_system) VALUES ('Paytm', TRUE)");
                stmt.execute("INSERT INTO payment_modes (mode_name, is_system) VALUES ('PhonePe', TRUE)");
                stmt.execute("INSERT INTO payment_modes (mode_name, is_system) VALUES ('UPI', TRUE)");
                stmt.execute("INSERT INTO payment_modes (mode_name, is_system) VALUES ('Google Pay', TRUE)");
                stmt.execute("INSERT INTO payment_modes (mode_name, is_system) VALUES ('Net Banking', TRUE)");
                stmt.execute("INSERT INTO payment_modes (mode_name, is_system) VALUES ('Credit Card', TRUE)");
                stmt.execute("INSERT INTO payment_modes (mode_name, is_system) VALUES ('Debit Card', TRUE)");
                stmt.execute("INSERT INTO payment_modes (mode_name, is_system) VALUES ('Cheque', TRUE)");
                stmt.execute("INSERT INTO payment_modes (mode_name, is_system) VALUES ('Other', TRUE)");
            }

            // Seed Persons
            rs = stmt.executeQuery("SELECT COUNT(*) FROM persons");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO persons (name, contact, relation, notes) VALUES ('Rahul Sharma', '+91 9876543210', 'Friend', 'College friend')");
                stmt.execute("INSERT INTO persons (name, contact, relation, notes) VALUES ('Amit Verma', '+91 9811223344', 'Colleague', 'Office colleague')");
                stmt.execute("INSERT INTO persons (name, contact, relation, notes) VALUES ('Suresh Kumar', '+91 9922334455', 'Relative', 'Maternal Uncle')");
                stmt.execute("INSERT INTO persons (name, contact, relation, notes) VALUES ('Deepak Singh', '+91 9899001122', 'Brother', 'Family')");
                stmt.execute("INSERT INTO persons (name, contact, relation, notes) VALUES ('Manoj Gupta', '+91 9711224466', 'Landlord', 'House owner')");
            }

            // Seed Bills (including user requested examples: Airtel Bill, Electricity Khardoni, Noida)
            rs = stmt.executeQuery("SELECT COUNT(*) FROM bills");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO bills (bill_name, provider, default_amount, notes) VALUES ('Airtel Bill', 'Airtel Broadband & Mobile', 1199.00, 'Monthly postpaid & fiber')");
                stmt.execute("INSERT INTO bills (bill_name, provider, default_amount, notes) VALUES ('Electricity (Khardoni)', 'PVVNL Meerut', 2450.00, 'Village home electricity')");
                stmt.execute("INSERT INTO bills (bill_name, provider, default_amount, notes) VALUES ('Electricity (Noida)', 'NPCL Noida', 3200.00, 'Flat electricity')");
                stmt.execute("INSERT INTO bills (bill_name, provider, default_amount, notes) VALUES ('Piped Gas (IGL)', 'Indraprastha Gas Ltd', 850.00, 'Kitchen gas PNG')");
                stmt.execute("INSERT INTO bills (bill_name, provider, default_amount, notes) VALUES ('Water Bill', 'Noida Authority', 400.00, 'Quarterly water supply')");
            }

            // Seed EMIs
            rs = stmt.executeQuery("SELECT COUNT(*) FROM emis");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO emis (emi_name, bank_lender, monthly_amount, notes) VALUES ('Home Loan EMI', 'SBI', 28500.00, 'Flat loan EMI')");
                stmt.execute("INSERT INTO emis (emi_name, bank_lender, monthly_amount, notes) VALUES ('Car Loan EMI', 'HDFC Bank', 14200.00, 'Automobile loan')");
                stmt.execute("INSERT INTO emis (emi_name, bank_lender, monthly_amount, notes) VALUES ('Personal Loan', 'ICICI Bank', 8500.00, 'Home renovation')");
                stmt.execute("INSERT INTO emis (emi_name, bank_lender, monthly_amount, notes) VALUES ('Laptop EMI', 'Bajaj Finserv', 4500.00, 'No-cost EMI 6 months')");
            }

            // Seed Credit Cards
            rs = stmt.executeQuery("SELECT COUNT(*) FROM credit_cards");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO credit_cards (card_name, bank, last_digits) VALUES ('HDFC Millennia', 'HDFC Bank', '4128')");
                stmt.execute("INSERT INTO credit_cards (card_name, bank, last_digits) VALUES ('ICICI Amazon Pay', 'ICICI Bank', '9051')");
                stmt.execute("INSERT INTO credit_cards (card_name, bank, last_digits) VALUES ('SBI SimplyCLICK', 'SBI Cards', '6733')");
            }

            // Seed realistic Transactions if empty
            rs = stmt.executeQuery("SELECT COUNT(*) FROM transactions");
            if (rs.next() && rs.getInt(1) == 0) {
                LocalDate now = LocalDate.now();
                LocalDate m1 = now.minusMonths(1);
                LocalDate m2 = now.minusMonths(2);

                // Income entries
                stmt.execute(String.format("INSERT INTO transactions (type, tx_date, is_expected, amount, payment_via, category, sub_category, notes) " +
                        "VALUES ('INCOME', '%s', FALSE, 95000.00, 'Net Banking', 'Salary', 'Tech Corp India', 'Monthly Salary')", now.withDayOfMonth(1)));
                stmt.execute(String.format("INSERT INTO transactions (type, tx_date, is_expected, amount, payment_via, category, sub_category, notes) " +
                        "VALUES ('INCOME', '%s', FALSE, 95000.00, 'Net Banking', 'Salary', 'Tech Corp India', 'Monthly Salary')", m1.withDayOfMonth(1)));
                stmt.execute(String.format("INSERT INTO transactions (type, tx_date, is_expected, amount, payment_via, category, sub_category, notes) " +
                        "VALUES ('INCOME', '%s', FALSE, 95000.00, 'Net Banking', 'Salary', 'Tech Corp India', 'Monthly Salary')", m2.withDayOfMonth(1)));

                stmt.execute(String.format("INSERT INTO transactions (type, tx_date, is_expected, amount, payment_via, category, sub_category, person_id, notes) " +
                        "VALUES ('INCOME', '%s', FALSE, 15000.00, 'UPI', 'Return', 'Rahul Sharma', 1, 'Return of loan given last year')", now.minusDays(10)));
                stmt.execute(String.format("INSERT INTO transactions (type, tx_date, is_expected, amount, payment_via, category, sub_category, notes) " +
                        "VALUES ('INCOME', '%s', FALSE, 12000.00, 'Net Banking', 'Return', 'Mutual Fund Dividend', 'Investment return')", now.minusDays(15)));
                stmt.execute(String.format("INSERT INTO transactions (type, tx_date, is_expected, amount, payment_via, category, sub_category, person_id, notes) " +
                        "VALUES ('INCOME', '%s', FALSE, 25000.00, 'PhonePe', 'Loan', 'Suresh Kumar', 3, 'Personal loan borrowed for temporary medical need')", now.minusDays(20)));

                // Expense entries
                stmt.execute(String.format("INSERT INTO transactions (type, tx_date, is_expected, amount, payment_via, category, sub_category, emi_id, notes) " +
                        "VALUES ('EXPENSE', '%s', FALSE, 28500.00, 'Net Banking', 'EMI Payment', 'Home Loan EMI', 1, 'Auto-debit for Home loan')", now.withDayOfMonth(5)));
                stmt.execute(String.format("INSERT INTO transactions (type, tx_date, is_expected, amount, payment_via, category, sub_category, emi_id, notes) " +
                        "VALUES ('EXPENSE', '%s', FALSE, 14200.00, 'Net Banking', 'EMI Payment', 'Car Loan EMI', 2, 'Auto-debit for Car loan')", now.withDayOfMonth(10)));

                stmt.execute(String.format("INSERT INTO transactions (type, tx_date, is_expected, amount, payment_via, category, sub_category, bill_id, notes) " +
                        "VALUES ('EXPENSE', '%s', FALSE, 1199.00, 'Paytm', 'Bill', 'Airtel Bill', 1, 'Monthly Fiber bill paid via Paytm')", now.minusDays(4)));
                stmt.execute(String.format("INSERT INTO transactions (type, tx_date, is_expected, amount, payment_via, category, sub_category, bill_id, notes) " +
                        "VALUES ('EXPENSE', '%s', FALSE, 3200.00, 'UPI', 'Bill', 'Electricity (Noida)', 3, 'Noida power bill')", now.minusDays(8)));
                stmt.execute(String.format("INSERT INTO transactions (type, tx_date, is_expected, amount, payment_via, category, sub_category, bill_id, notes) " +
                        "VALUES ('EXPENSE', '%s', FALSE, 2450.00, 'PhonePe', 'Bill', 'Electricity (Khardoni)', 2, 'Khardoni power bill paid')", m1.withDayOfMonth(12)));

                stmt.execute(String.format("INSERT INTO transactions (type, tx_date, is_expected, amount, payment_via, category, sub_category, credit_card_id, notes) " +
                        "VALUES ('EXPENSE', '%s', FALSE, 18450.00, 'Credit Card', 'Credit Card', 'HDFC Millennia', 1, 'Monthly Credit Card statement settlement')", now.minusDays(12)));
                stmt.execute(String.format("INSERT INTO transactions (type, tx_date, is_expected, amount, payment_via, category, sub_category, person_id, notes) " +
                        "VALUES ('EXPENSE', '%s', FALSE, 20000.00, 'UPI', 'Loan Given', 'Amit Verma', 2, 'Personal loan lent to friend for urgent assistance')", now.minusDays(18)));
                stmt.execute(String.format("INSERT INTO transactions (type, tx_date, is_expected, amount, payment_via, category, sub_category, notes) " +
                        "VALUES ('EXPENSE', '%s', FALSE, 8500.00, 'Cash', 'Special Expense', 'Family Dinner & Outing', 'Weekend celebration')", now.minusDays(6)));

                // Add audit entry for initial seed
                stmt.execute("INSERT INTO audit_logs (entity_type, entity_id, action, details) " +
                        "VALUES ('SYSTEM', 0, 'INITIALIZE', 'Initial sample data seeded successfully for demonstration')");
            }

            // Seed Recurring Schedules (Home Loan EMI on 5th, Electricity on 15th, etc.)
            rs = stmt.executeQuery("SELECT COUNT(*) FROM recurring_schedules");
            if (rs.next() && rs.getInt(1) == 0) {
                // Fixed EMI on 5th of every month from Jan 2026 to Jan 2027 (Auto-Debit)
                stmt.execute("""
                    INSERT INTO recurring_schedules (name, type, category, sub_category, day_of_month, amount_type, default_amount, start_date, end_date, payment_via, is_auto_pay, notes)
                    VALUES ('Home Loan EMI', 'EXPENSE', 'EMI Payment', 'Home Loan EMI', 5, 'FIXED', 28500.00, '2026-01-01', '2027-01-31', 'Net Banking', TRUE, 'Monthly auto-debit on 5th')
                """);
                // Fixed EMI on 10th of every month (Auto-Debit)
                stmt.execute("""
                    INSERT INTO recurring_schedules (name, type, category, sub_category, day_of_month, amount_type, default_amount, start_date, end_date, payment_via, is_auto_pay, notes)
                    VALUES ('Car Loan EMI', 'EXPENSE', 'EMI Payment', 'Car Loan EMI', 10, 'FIXED', 14200.00, '2026-01-01', '2027-06-30', 'Net Banking', TRUE, 'Auto-debit for Car loan')
                """);
                // Variable Bill on 15th of every month (Electricity Bill - ask to enter amount!)
                stmt.execute("""
                    INSERT INTO recurring_schedules (name, type, category, sub_category, day_of_month, amount_type, default_amount, start_date, end_date, payment_via, is_auto_pay, notes)
                    VALUES ('Electricity Bill (Noida)', 'EXPENSE', 'Bill', 'Electricity (Noida)', 15, 'VARIABLE', 3200.00, '2026-01-01', NULL, 'UPI', FALSE, 'Variable bill on 15th - prompt for amount')
                """);
                // Fixed Bill on 4th of every month (Airtel Fiber - Auto-Payment)
                stmt.execute("""
                    INSERT INTO recurring_schedules (name, type, category, sub_category, day_of_month, amount_type, default_amount, start_date, end_date, payment_via, is_auto_pay, notes)
                    VALUES ('Airtel Fiber Bill', 'EXPENSE', 'Bill', 'Airtel Bill', 4, 'FIXED', 1199.00, '2026-01-01', NULL, 'Paytm', TRUE, 'Auto-pay on 4th of month')
                """);
                // Variable Credit Card Bill on 20th of every month (Manual Payment)
                stmt.execute("""
                    INSERT INTO recurring_schedules (name, type, category, sub_category, day_of_month, amount_type, default_amount, start_date, end_date, payment_via, is_auto_pay, notes)
                    VALUES ('HDFC Millennia Credit Card', 'EXPENSE', 'Credit Card', 'HDFC Millennia', 20, 'VARIABLE', 0.00, '2026-01-01', NULL, 'Net Banking', FALSE, 'Bill generated on 20th - enter statement amount')
                """);
            }
        }
    }

    /**
     * Exports all tables in the H2 database as individual CSV files into a target directory.
     */
    public void exportAllTablesToCsv(File targetDirectory) throws Exception {
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs();
        }

        String[] tables = {"transactions", "persons", "bills", "emis", "credit_cards", "payment_modes", "recurring_schedules", "due_payments", "audit_logs"};

        try (Connection conn = getConnection()) {
            for (String tableName : tables) {
                File csvFile = new File(targetDirectory, tableName + ".csv");
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName);
                     PrintWriter writer = new PrintWriter(new FileWriter(csvFile, false))) {

                    ResultSetMetaData meta = rs.getMetaData();
                    int columnCount = meta.getColumnCount();

                    // Header row
                    for (int i = 1; i <= columnCount; i++) {
                        writer.print(escapeCsv(meta.getColumnLabel(i)));
                        if (i < columnCount) writer.print(",");
                    }
                    writer.println();

                    // Data rows
                    while (rs.next()) {
                        for (int i = 1; i <= columnCount; i++) {
                            Object val = rs.getObject(i);
                            writer.print(escapeCsv(val != null ? val.toString() : ""));
                            if (i < columnCount) writer.print(",");
                        }
                        writer.println();
                    }
                }
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
