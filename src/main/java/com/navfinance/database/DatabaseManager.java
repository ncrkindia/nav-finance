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

            // Transaction Links Table (Linking Give and Return Transactions)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS transaction_links (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    give_tx_id BIGINT NOT NULL,
                    return_tx_id BIGINT NOT NULL,
                    amount DECIMAL(15,2) NOT NULL,
                    notes VARCHAR(500),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (give_tx_id) REFERENCES transactions(id) ON DELETE CASCADE,
                    FOREIGN KEY (return_tx_id) REFERENCES transactions(id) ON DELETE CASCADE,
                    CONSTRAINT uc_give_return UNIQUE (give_tx_id, return_tx_id)
                );
            """);

            // Create helpful indices
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_tx_date ON transactions(tx_date);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_tx_type ON transactions(type);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_tx_category ON transactions(category);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_due_date ON due_payments(due_date);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_due_status ON due_payments(status);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_audit_time ON audit_logs(log_timestamp);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_link_give ON transaction_links(give_tx_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_link_return ON transaction_links(return_tx_id);");

            // Seed master data if tables are empty
            seedInitialData(conn);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize H2 database schema", e);
            throw new RuntimeException("Database initialization error: " + e.getMessage(), e);
        }
    }

    private void seedInitialData(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM transactions");
            if (rs.next() && rs.getInt(1) == 0) {
                java.io.InputStream is = getClass().getResourceAsStream("/sql/cleanup_and_seed_from_excel.sql");
                if (is != null) {
                    try (java.io.InputStreamReader reader = new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8)) {
                        org.h2.tools.RunScript.execute(conn, reader);
                        LOGGER.log(Level.INFO, "Seeded initial financial records from /sql/cleanup_and_seed_from_excel.sql");
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to run resource seed script: " + e.getMessage(), e);
                    }
                }
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
