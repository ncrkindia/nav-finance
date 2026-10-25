package com.navfinance.database;

import com.navfinance.config.AppConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit and integration tests covering multi-database configurations,
 * dialect SQL DDL generation, AppConfig persistence, and connection testing.
 */
public class DatabaseConfigurationTest {

    private DatabaseConfig originalConfig;

    @BeforeEach
    public void setUp() {
        originalConfig = AppConfig.getActiveDatabaseConfig();
    }

    @AfterEach
    public void tearDown() {
        // Restore whatever original configuration was active
        if (originalConfig != null) {
            AppConfig.setActiveDatabaseConfig(originalConfig);
        }
    }

    @Test
    public void testDatabaseTypeDefaultsAndUrlBuilding() {
        // 1. MySQL
        assertEquals(3306, DatabaseType.MYSQL.getDefaultPort());
        assertEquals("com.mysql.cj.jdbc.Driver", DatabaseType.MYSQL.getDriverClassName());
        String mysqlUrl = DatabaseType.MYSQL.buildJdbcUrl("192.168.1.100", 3307, "my_finance", null);
        assertTrue(mysqlUrl.startsWith("jdbc:mysql://192.168.1.100:3307/my_finance"));
        assertTrue(mysqlUrl.contains("serverTimezone=UTC"));

        // 2. PostgreSQL
        assertEquals(5432, DatabaseType.POSTGRESQL.getDefaultPort());
        assertEquals("org.postgresql.Driver", DatabaseType.POSTGRESQL.getDriverClassName());
        String pgUrl = DatabaseType.POSTGRESQL.buildJdbcUrl("localhost", 5432, "pg_nav", null);
        assertEquals("jdbc:postgresql://localhost:5432/pg_nav", pgUrl);

        // 3. MariaDB
        assertEquals(3306, DatabaseType.MARIADB.getDefaultPort());
        assertEquals("org.mariadb.jdbc.Driver", DatabaseType.MARIADB.getDriverClassName());
        String mariaUrl = DatabaseType.MARIADB.buildJdbcUrl("db.example.com", 3306, "fin_prod", null);
        assertTrue(mariaUrl.startsWith("jdbc:mariadb://db.example.com:3306/fin_prod"));
        assertTrue(mariaUrl.contains("createDatabaseIfNotExist=true"));

        // 4. SQLite
        assertEquals(0, DatabaseType.SQLITE.getDefaultPort());
        assertEquals("org.sqlite.JDBC", DatabaseType.SQLITE.getDriverClassName());
        assertTrue(DatabaseType.SQLITE.isEmbeddedOrLocal());
        String sqliteUrl = DatabaseType.SQLITE.buildJdbcUrl(null, 0, null, "C:/data/nav.sqlite");
        assertEquals("jdbc:sqlite:C:/data/nav.sqlite", sqliteUrl);

        // 5. H2 (Default)
        assertEquals(0, DatabaseType.H2.getDefaultPort());
        assertEquals("org.h2.Driver", DatabaseType.H2.getDriverClassName());
        assertTrue(DatabaseType.H2.isEmbeddedOrLocal());
        String h2Url = DatabaseType.H2.buildJdbcUrl(null, 0, null, AppConfig.DB_PATH);
        assertTrue(h2Url.startsWith("jdbc:h2:"));
        assertTrue(h2Url.contains("AUTO_SERVER=TRUE"));
    }

    @Test
    public void testDatabaseConfigEffectiveUrlAndCopy() {
        DatabaseConfig config = new DatabaseConfig(
                DatabaseType.MYSQL,
                "mysql.internal",
                3308,
                "nav_company",
                "finuser",
                "secretPass123",
                null,
                null,
                null
        );

        assertTrue(config.getEffectiveJdbcUrl().startsWith("jdbc:mysql://mysql.internal:3308/nav_company"));
        assertTrue(config.getEffectiveJdbcUrl().contains("createDatabaseIfNotExist=true"));
        assertEquals("finuser", config.getUsername());
        assertEquals("secretPass123", config.getPassword());

        DatabaseConfig copy = config.copy();
        assertEquals(config.getType(), copy.getType());
        assertEquals(config.getHost(), copy.getHost());
        assertEquals(config.getPort(), copy.getPort());
        assertEquals(config.getDatabaseName(), copy.getDatabaseName());
        assertEquals(config.getEffectiveJdbcUrl(), copy.getEffectiveJdbcUrl());

        // Custom URL override
        DatabaseConfig customConfig = new DatabaseConfig(
                DatabaseType.CUSTOM,
                null,
                0,
                null,
                "admin",
                "admin",
                null,
                "jdbc:oracle:thin:@//localhost:1521/XEPDB1",
                "oracle.jdbc.OracleDriver"
        );
        assertEquals("jdbc:oracle:thin:@//localhost:1521/XEPDB1", customConfig.getEffectiveJdbcUrl());
        assertEquals("oracle.jdbc.OracleDriver", customConfig.getEffectiveDriverClassName());
    }

    @Test
    public void testAppConfigPersistence() {
        // Set MySQL config in AppConfig
        DatabaseConfig mysqlConfig = new DatabaseConfig(
                DatabaseType.MYSQL,
                "remote.database.org",
                3306,
                "nav_test_db",
                "db_user",
                "db_pass_xyz",
                null,
                null,
                null
        );
        AppConfig.setActiveDatabaseConfig(mysqlConfig);

        // Verify retrieval
        DatabaseConfig retrieved = AppConfig.getActiveDatabaseConfig();
        assertEquals(DatabaseType.MYSQL, retrieved.getType());
        assertEquals("remote.database.org", retrieved.getHost());
        assertEquals(3306, retrieved.getPort());
        assertEquals("nav_test_db", retrieved.getDatabaseName());
        assertEquals("db_user", retrieved.getUsername());
        assertEquals("db_pass_xyz", retrieved.getPassword());

        // Revert to H2 default
        DatabaseConfig h2Default = DatabaseConfig.createDefaultH2(AppConfig.DB_PATH);
        AppConfig.setActiveDatabaseConfig(h2Default);

        DatabaseConfig retrievedH2 = AppConfig.getActiveDatabaseConfig();
        assertEquals(DatabaseType.H2, retrievedH2.getType());
        assertTrue(retrievedH2.getType().isEmbeddedOrLocal());
    }

    @Test
    public void testDbSchemaDialectDifferences() {
        // 1. H2 DDL
        List<String> h2Statements = DbSchemaManager.getTableCreationStatements(DatabaseType.H2);
        assertEquals(10, h2Statements.size());
        assertTrue(h2Statements.get(0).contains("AUTO_INCREMENT PRIMARY KEY"));

        // 2. MySQL DDL
        List<String> mysqlStatements = DbSchemaManager.getTableCreationStatements(DatabaseType.MYSQL);
        assertEquals(10, mysqlStatements.size());
        assertTrue(mysqlStatements.get(0).contains("AUTO_INCREMENT PRIMARY KEY"));

        // 3. PostgreSQL DDL
        List<String> pgStatements = DbSchemaManager.getTableCreationStatements(DatabaseType.POSTGRESQL);
        assertEquals(10, pgStatements.size());
        assertTrue(pgStatements.get(0).contains("BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY"));

        // 4. SQLite DDL
        List<String> sqliteStatements = DbSchemaManager.getTableCreationStatements(DatabaseType.SQLITE);
        assertEquals(10, sqliteStatements.size());
        assertTrue(sqliteStatements.get(0).contains("INTEGER PRIMARY KEY AUTOINCREMENT"));
    }

    @Test
    public void testInMemoryH2SchemaInitializationAndOperations() throws Exception {
        String memUrl = "jdbc:h2:mem:schematestdb;DB_CLOSE_DELAY=-1";
        try (Connection conn = DriverManager.getConnection(memUrl, "sa", "")) {
            DbSchemaManager.initializeSchema(conn, DatabaseType.H2);

            // Test inserting into transactions table
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO transactions (type, category, amount, tx_date, payment_via, notes) " +
                        "VALUES ('INCOME', 'Salary', 10000.00, '2026-10-01', 'Bank Transfer', 'Initial schema test')");

                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*), SUM(amount) FROM transactions")) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt(1));
                    assertEquals(10000.00, rs.getDouble(2), 0.001);
                }
            }
        }
    }

    @Test
    public void testInMemorySqliteSchemaInitializationAndOperations() throws Exception {
        String sqliteMemUrl = "jdbc:sqlite::memory:";
        try (Connection conn = DriverManager.getConnection(sqliteMemUrl)) {
            DbSchemaManager.initializeSchema(conn, DatabaseType.SQLITE);

            // Test inserting into transactions table in SQLite
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO transactions (type, category, amount, tx_date, payment_via, notes) " +
                        "VALUES ('EXPENSE', 'Groceries', 2500.50, '2026-10-02', 'UPI', 'SQLite schema test')");

                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*), SUM(amount) FROM transactions")) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt(1));
                    assertEquals(2500.50, rs.getDouble(2), 0.001);
                }
            }
        }
    }

    @Test
    public void testConnectionFailureHandling() {
        // Connect to a non-existent port with timeout
        DatabaseConfig unreachableConfig = new DatabaseConfig(
                DatabaseType.MYSQL,
                "127.0.0.1",
                59999,
                "nonexistent_db",
                "root",
                "wrong_pass",
                null,
                null,
                null
        );

        DatabaseConfig.ConnectionTestResult result = unreachableConfig.testConnection();
        assertFalse(result.success());
        assertNotNull(result.message());
        assertFalse(result.message().isEmpty());
    }
}
