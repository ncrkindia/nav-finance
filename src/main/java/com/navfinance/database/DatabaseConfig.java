package com.navfinance.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Encapsulates relational database connection properties, JDBC URL generation,
 * and live connectivity diagnostic testing.
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public class DatabaseConfig {

    private DatabaseType type = DatabaseType.H2;
    private String host = "localhost";
    private int port = 3306;
    private String databaseName = "navfinance";
    private String username = "sa";
    private String password = "";
    private String localPath = "";
    private String customJdbcUrl = "";
    private String customDriver = "";

    public DatabaseConfig() {}

    /**
     * Creates a copy of an existing configuration.
     */
    public DatabaseConfig(DatabaseConfig other) {
        if (other != null) {
            this.type = other.type;
            this.host = other.host;
            this.port = other.port;
            this.databaseName = other.databaseName;
            this.username = other.username;
            this.password = other.password;
            this.localPath = other.localPath;
            this.customJdbcUrl = other.customJdbcUrl;
            this.customDriver = other.customDriver;
        }
    }

    /**
     * Parameterized constructor for all database configuration options.
     */
    public DatabaseConfig(DatabaseType type, String host, int port, String databaseName,
                          String username, String password, String localPath,
                          String customJdbcUrl, String customDriver) {
        this.type = type != null ? type : DatabaseType.H2;
        this.host = host != null ? host : "localhost";
        this.port = port;
        this.databaseName = databaseName != null ? databaseName : "navfinance";
        this.username = username != null ? username : "";
        this.password = password != null ? password : "";
        this.localPath = localPath != null ? localPath : "";
        this.customJdbcUrl = customJdbcUrl != null ? customJdbcUrl : "";
        this.customDriver = customDriver != null ? customDriver : "";
    }

    /**
     * Creates a deep copy of this configuration.
     */
    public DatabaseConfig copy() {
        return new DatabaseConfig(this);
    }

    /**
     * Creates a default embedded H2 configuration.
     *
     * @param h2FilePath default absolute filesystem path prefix for H2
     * @return configured DatabaseConfig for H2
     */
    public static DatabaseConfig createDefaultH2(String h2FilePath) {
        DatabaseConfig config = new DatabaseConfig();
        config.setType(DatabaseType.H2);
        config.setUsername("sa");
        config.setPassword("");
        config.setLocalPath(h2FilePath != null ? h2FilePath : "");
        return config;
    }

    public DatabaseType getType() {
        return type != null ? type : DatabaseType.H2;
    }

    public void setType(DatabaseType type) {
        this.type = type != null ? type : DatabaseType.H2;
        if (this.port <= 0 && this.type.getDefaultPort() > 0) {
            this.port = this.type.getDefaultPort();
        }
        if ((this.username == null || this.username.isBlank()) && !this.type.getDefaultUser().isBlank()) {
            this.username = this.type.getDefaultUser();
        }
    }

    public String getHost() {
        return host != null ? host : "localhost";
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port > 0 ? port : (type != null ? type.getDefaultPort() : 3306);
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDatabaseName() {
        return databaseName != null ? databaseName : "navfinance";
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getUsername() {
        return username != null ? username : "";
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password != null ? password : "";
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLocalPath() {
        return localPath != null ? localPath : "";
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getCustomJdbcUrl() {
        return customJdbcUrl != null ? customJdbcUrl : "";
    }

    public void setCustomJdbcUrl(String customJdbcUrl) {
        this.customJdbcUrl = customJdbcUrl;
    }

    public String getCustomUrl() {
        return getCustomJdbcUrl();
    }

    public void setCustomUrl(String customUrl) {
        setCustomJdbcUrl(customUrl);
    }

    public String getCustomDriver() {
        return customDriver != null ? customDriver : "";
    }

    public void setCustomDriver(String customDriver) {
        this.customDriver = customDriver;
    }

    public String getEffectiveDriverClassName() {
        if (type == DatabaseType.CUSTOM && customDriver != null && !customDriver.isBlank()) {
            return customDriver.trim();
        }
        return type != null ? type.getDriverClassName() : "";
    }

    /**
     * Resolves and returns the effective JDBC connection URL for this configuration.
     *
     * @return live JDBC URL string
     */
    public String getEffectiveJdbcUrl() {
        if (type == DatabaseType.CUSTOM) {
            return customJdbcUrl != null ? customJdbcUrl.trim() : "";
        }
        return type.buildJdbcUrl(host, port, databaseName, localPath);
    }

    /**
     * Performs a live diagnostic connection test against the target database.
     *
     * @return {@link ConnectionTestResult} detailing success or error details
     */
    public ConnectionTestResult testConnection() {
        long startTime = System.currentTimeMillis();
        String url = getEffectiveJdbcUrl();

        if (url.isBlank()) {
            return new ConnectionTestResult(false, "JDBC URL cannot be empty.", 0, "");
        }

        // Ensure driver class is loaded if specified
        if (!type.getDriverClassName().isBlank()) {
            try {
                Class.forName(type.getDriverClassName());
            } catch (ClassNotFoundException e) {
                return new ConnectionTestResult(false, "JDBC Driver class not found: " + type.getDriverClassName(), 0, "");
            }
        }

        try {
            DriverManager.setLoginTimeout(8); // 8 seconds timeout
            try (Connection conn = DriverManager.getConnection(url, getUsername(), getPassword())) {
                DatabaseMetaData meta = conn.getMetaData();
                String serverInfo = meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion();

                // Run validation query
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(type.getValidationQuery());
                }

                long elapsed = System.currentTimeMillis() - startTime;
                return new ConnectionTestResult(true, "Successfully connected to " + type.getDisplayName()
                        + " in " + elapsed + " ms.", elapsed, serverInfo);
            }
        } catch (SQLException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            String msg = e.getMessage();
            if (msg == null || msg.isBlank()) {
                msg = e.getClass().getSimpleName();
            }
            return new ConnectionTestResult(false, "Connection failed: " + msg, elapsed, "");
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            return new ConnectionTestResult(false, "Unexpected error: " + e.getMessage(), elapsed, "");
        }
    }

    /**
     * Value record reporting the outcome of a connection test attempt.
     */
    public record ConnectionTestResult(boolean success, String message, long latencyMs, String serverInfo) {}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatabaseConfig that = (DatabaseConfig) o;
        return port == that.port && type == that.type &&
                Objects.equals(host, that.host) &&
                Objects.equals(databaseName, that.databaseName) &&
                Objects.equals(username, that.username) &&
                Objects.equals(password, that.password) &&
                Objects.equals(localPath, that.localPath) &&
                Objects.equals(customJdbcUrl, that.customJdbcUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, host, port, databaseName, username, password, localPath, customJdbcUrl);
    }
}
