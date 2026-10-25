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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central database controller managing multi-database connections (embedded H2, external MySQL,
 * PostgreSQL, MariaDB, SQLite), schema migrations, table indexing, initial sample data population,
 * live connection switching, and full CSV table backups.
 * <p>
 * Database connection details are configured via {@link DatabaseConfig} and persisted across restarts.
 * By default, the application runs on embedded H2 database located at:
 * {@code ~user/nav-finance/navfinance_db.mv.db}.
 * </p>
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public class DatabaseManager {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());
    private static DatabaseManager instance;

    /** Observers notified whenever the active database connection or configuration changes. */
    private final List<Runnable> databaseChangeListeners = new ArrayList<>();

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
     * Establishes and returns a live connection to the currently active database.
     *
     * @return A valid {@link Connection} instance.
     * @throws SQLException If a connection error occurs.
     */
    public Connection getConnection() throws SQLException {
        DatabaseConfig config = AppConfig.getActiveDatabaseConfig();
        return getConnection(config);
    }

    /**
     * Establishes and returns a connection using the supplied configuration.
     *
     * @param config database configuration parameters
     * @return A valid {@link Connection} instance
     * @throws SQLException If connection fails
     */
    public Connection getConnection(DatabaseConfig config) throws SQLException {
        if (config == null) {
            config = AppConfig.getActiveDatabaseConfig();
        }

        String driverClass = config.getType().getDriverClassName();
        if (driverClass != null && !driverClass.isBlank()) {
            try {
                Class.forName(driverClass);
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.WARNING, "JDBC Driver class not found: {0}", driverClass);
            }
        }

        String url = config.getEffectiveJdbcUrl();
        return DriverManager.getConnection(url, config.getUsername(), config.getPassword());
    }

    /**
     * Registers a callback listener to be triggered whenever the active database changes.
     *
     * @param listener callback task to execute
     */
    public void addDatabaseChangeListener(Runnable listener) {
        if (listener != null) {
            databaseChangeListeners.add(listener);
        }
    }

    /**
     * Notifies all registered database change observers.
     */
    private void fireDatabaseChanged() {
        for (Runnable r : databaseChangeListeners) {
            try {
                r.run();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in database change listener: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Initializes all relational database tables, foreign keys, and indexes if they do not exist.
     */
    public void initDatabase() {
        DatabaseConfig config = AppConfig.getActiveDatabaseConfig();

        // Ensure ~/nav-finance directory exists if using local file storage
        if (config.getType().isEmbeddedOrLocal()) {
            if (!AppConfig.APP_DATA_DIR.exists()) {
                boolean created = AppConfig.APP_DATA_DIR.mkdirs();
                LOGGER.log(Level.INFO, "Created app directory at: {0} (success: {1})",
                        new Object[]{AppConfig.APP_DATA_DIR.getAbsolutePath(), created});
            }
        }

        try (Connection conn = getConnection(config)) {
            DbSchemaManager.initializeSchema(conn, config.getType());
            LOGGER.log(Level.INFO, "Initialized relational database schema for: {0}", config.getType().getDisplayName());

            // If H2 and empty, seed initial data
            if (config.getType() == DatabaseType.H2) {
                seedInitialDataIfH2(conn);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database schema: " + e.getMessage(), e);
            throw new RuntimeException("Database initialization error: " + e.getMessage(), e);
        }
    }

    /**
     * Switches the active application database to a new configuration.
     * Tests connectivity first, creates tables if absent, persists the configuration, and fires listeners.
     *
     * @param newConfig targeted database connection settings
     * @return {@link DatabaseConfig.ConnectionTestResult} indicating result
     */
    public DatabaseConfig.ConnectionTestResult switchDatabase(DatabaseConfig newConfig) {
        if (newConfig == null) {
            return new DatabaseConfig.ConnectionTestResult(false, "Configuration cannot be null.", 0, "");
        }

        // 1. Diagnostic test connection
        DatabaseConfig.ConnectionTestResult testResult = newConfig.testConnection();
        if (!testResult.success()) {
            return testResult;
        }

        // 2. Initialize schema on target database
        try (Connection conn = getConnection(newConfig)) {
            DbSchemaManager.initializeSchema(conn, newConfig.getType());
            LOGGER.log(Level.INFO, "Schema successfully validated/initialized on: {0}", newConfig.getType().getDisplayName());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize schema on new database: " + e.getMessage(), e);
            return new DatabaseConfig.ConnectionTestResult(false, "Connected, but schema setup failed: " + e.getMessage(), testResult.latencyMs(), testResult.serverInfo());
        }

        // 3. Persist new configuration to disk
        AppConfig.setActiveDatabaseConfig(newConfig);

        // 4. Notify UI components
        fireDatabaseChanged();

        return new DatabaseConfig.ConnectionTestResult(true, "Successfully switched to "
                + newConfig.getType().getDisplayName() + ". All tables verified.", testResult.latencyMs(), testResult.serverInfo());
    }

    /**
     * Reverts the database configuration back to the default embedded H2 engine.
     *
     * @return test result of switching back to H2
     */
    public DatabaseConfig.ConnectionTestResult resetToDefaultH2() {
        DatabaseConfig h2Config = DatabaseConfig.createDefaultH2(AppConfig.DB_PATH);
        return switchDatabase(h2Config);
    }

    private void seedInitialDataIfH2(Connection conn) throws SQLException {
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
     * Exports all tables in the active database as individual CSV files into a target directory.
     */
    public void exportAllTablesToCsv(File targetDirectory) throws Exception {
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs();
        }

        String[] tables = {"transactions", "persons", "bills", "emis", "credit_cards", "payment_modes", "categories", "sub_categories", "recurring_schedules", "due_payments", "audit_logs", "transaction_links"};

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
