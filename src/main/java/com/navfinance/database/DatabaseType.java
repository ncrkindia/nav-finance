package com.navfinance.database;

/**
 * Enumeration of supported database management system types.
 * <p>
 * Provides metadata, default ports, driver classes, and JDBC URL builders for
 * embedded H2 (default), external MySQL, PostgreSQL, MariaDB, SQLite, and custom JDBC connections.
 * </p>
 */
public enum DatabaseType {

    H2("H2 (Embedded - Default)", "org.h2.Driver", 0, "sa", true, true),
    MYSQL("MySQL", "com.mysql.cj.jdbc.Driver", 3306, "root", false, false),
    POSTGRESQL("PostgreSQL", "org.postgresql.Driver", 5432, "postgres", false, false),
    MARIADB("MariaDB", "org.mariadb.jdbc.Driver", 3306, "root", false, false),
    SQLITE("SQLite (File-based)", "org.sqlite.JDBC", 0, "", true, false),
    CUSTOM("Custom JDBC", "", 0, "", false, false);

    private final String displayName;
    private final String driverClassName;
    private final int defaultPort;
    private final String defaultUser;
    private final boolean embeddedOrLocal;
    private final boolean defaultEngine;

    DatabaseType(String displayName, String driverClassName, int defaultPort, String defaultUser,
                 boolean embeddedOrLocal, boolean defaultEngine) {
        this.displayName = displayName;
        this.driverClassName = driverClassName;
        this.defaultPort = defaultPort;
        this.defaultUser = defaultUser;
        this.embeddedOrLocal = embeddedOrLocal;
        this.defaultEngine = defaultEngine;
    }

    public static final DatabaseType CUSTOM_JDBC = CUSTOM;

    public String getDisplayName() {
        return displayName;
    }

    public String getTypeName() {
        return displayName;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    public String getDefaultUser() {
        return defaultUser;
    }

    public boolean isEmbeddedOrLocal() {
        return embeddedOrLocal;
    }

    public boolean isDefaultEngine() {
        return defaultEngine;
    }

    /**
     * Constructs a JDBC connection URL for this database type using supplied parameters.
     *
     * @param host         hostname or IP address (e.g. "localhost")
     * @param port         TCP port number
     * @param databaseName target database schema name
     * @param localPath    file path for embedded/local databases (H2, SQLite)
     * @return full JDBC connection URL string
     */
    public String buildJdbcUrl(String host, int port, String databaseName, String localPath) {
        switch (this) {
            case H2:
                String h2Path = (localPath != null && !localPath.isBlank()) ? localPath : "navfinance_db";
                return "jdbc:h2:" + h2Path + ";AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1;MODE=REGULAR";
            case MYSQL:
                int myPort = port > 0 ? port : defaultPort;
                String myHost = (host != null && !host.isBlank()) ? host.trim() : "localhost";
                String myDb = (databaseName != null && !databaseName.isBlank()) ? databaseName.trim() : "navfinance";
                return "jdbc:mysql://" + myHost + ":" + myPort + "/" + myDb
                        + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true&characterEncoding=UTF-8";
            case MARIADB:
                int mariaPort = port > 0 ? port : defaultPort;
                String mariaHost = (host != null && !host.isBlank()) ? host.trim() : "localhost";
                String mariaDb = (databaseName != null && !databaseName.isBlank()) ? databaseName.trim() : "navfinance";
                return "jdbc:mariadb://" + mariaHost + ":" + mariaPort + "/" + mariaDb
                        + "?createDatabaseIfNotExist=true&characterEncoding=UTF-8";
            case POSTGRESQL:
                int pgPort = port > 0 ? port : defaultPort;
                String pgHost = (host != null && !host.isBlank()) ? host.trim() : "localhost";
                String pgDb = (databaseName != null && !databaseName.isBlank()) ? databaseName.trim() : "navfinance";
                return "jdbc:postgresql://" + pgHost + ":" + pgPort + "/" + pgDb;
            case SQLITE:
                String sqPath = (localPath != null && !localPath.isBlank()) ? localPath : "navfinance.db";
                return "jdbc:sqlite:" + sqPath;
            case CUSTOM:
            default:
                return "";
        }
    }

    /**
     * Returns a lightweight validation SQL query to test connectivity.
     */
    public String getValidationQuery() {
        return switch (this) {
            case SQLITE -> "SELECT 1";
            case POSTGRESQL -> "SELECT 1";
            case MYSQL, MARIADB -> "SELECT 1";
            case H2 -> "SELECT 1";
            default -> "SELECT 1";
        };
    }

    /**
     * Parses a DatabaseType from its string name with safe fallback to H2.
     */
    public static DatabaseType fromString(String name) {
        if (name == null || name.isBlank()) {
            return H2;
        }
        for (DatabaseType type : values()) {
            if (type.name().equalsIgnoreCase(name.trim()) || type.displayName.equalsIgnoreCase(name.trim())) {
                return type;
            }
        }
        return H2;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
