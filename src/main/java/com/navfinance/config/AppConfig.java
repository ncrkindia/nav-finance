package com.navfinance.config;

import java.io.File;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Global application constants, database configuration parameters, file paths,
 * localization settings, and currency/date formatting utilities for <b>Nav Finance</b>.
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public final class AppConfig {

    private AppConfig() {}

    /** Display name of the application. */
    public static final String APP_NAME = "Nav Finance";

    /** Secondary branding description shown in window titles and headers. */
    public static final String APP_SUBTITLE = "Personal Finance & Loan Management System";

    /** Semantic version string of the release. */
    public static final String APP_VERSION = "1.0.0";

    /** Current user home directory on host OS. */
    public static final String USER_HOME = System.getProperty("user.home");

    /** Embedded storage root directory located at {@code ~user/nav-finance/}. */
    public static final File APP_DATA_DIR = new File(USER_HOME, "nav-finance");

    /** H2 database file base name. */
    public static final String DB_FILE_NAME = "navfinance_db";

    /** Absolute filesystem path prefix for the H2 database file. */
    public static final String DB_PATH = new File(APP_DATA_DIR, DB_FILE_NAME).getAbsolutePath();

    /** JDBC connection URL enabling multi-process embedded mode (AUTO_SERVER=TRUE). */
    public static final String JDBC_URL = "jdbc:h2:" + DB_PATH + ";AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1;MODE=REGULAR";

    /** Default H2 database username. */
    public static final String DB_USER = "sa";

    /** Default H2 database password. */
    public static final String DB_PASSWORD = "";

    /** Default Indian English locale for currency formatting. */
    public static final Locale DEFAULT_LOCALE = Locale.of("en", "IN");

    /** Standard date formatter: {@code dd-MMM-yyyy} (e.g. 24-Oct-2026). */
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    /** Standard timestamp formatter: {@code dd-MMM-yyyy hh:mm a} (e.g. 24-Oct-2026 06:15 PM). */
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a");

    /** ISO date formatter: {@code yyyy-MM-dd} (e.g. 2026-10-24). */
    public static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** Configurable currency symbol (default: ₹). */
    private static String currencySymbol = "₹";

    /** Configurable active language code (default: "en"). */
    private static String language = "en";

    /** Configurable active zoom index pointer (default: 2 for 100%). */
    private static int zoomIndex = 2;

    /** Persisted window X coordinate (default: 0). */
    private static int windowX = 0;

    /** Persisted window Y coordinate (default: 0). */
    private static int windowY = 0;

    /** Persisted window width (-1 indicates uninitialized default full size). */
    private static int windowWidth = -1;

    /** Persisted window height (-1 indicates uninitialized default full size). */
    private static int windowHeight = -1;

    /** Persisted window maximized state (default: true for full size). */
    private static boolean windowMaximized = true;

    /** Active database configuration instance. */
    private static com.navfinance.database.DatabaseConfig activeDatabaseConfig =
            com.navfinance.database.DatabaseConfig.createDefaultH2(DB_PATH);

    /** Settings file in user data directory. */
    private static final File SETTINGS_FILE = new File(APP_DATA_DIR, "settings.properties");

    static {
        loadSettings();
    }

    /**
     * Loads persisted user configuration settings from settings.properties.
     */
    private static void loadSettings() {
        if (!SETTINGS_FILE.exists()) return;
        try (java.io.FileInputStream fis = new java.io.FileInputStream(SETTINGS_FILE)) {
            java.util.Properties props = new java.util.Properties();
            props.load(fis);
            String curr = props.getProperty("currencySymbol");
            if (curr != null && !curr.trim().isEmpty()) {
                currencySymbol = curr.trim();
            }
            String lang = props.getProperty("language");
            if (lang != null && !lang.trim().isEmpty()) {
                language = lang.trim();
            }
            String zIdx = props.getProperty("zoomIndex");
            if (zIdx != null && !zIdx.trim().isEmpty()) {
                try {
                    int idx = Integer.parseInt(zIdx.trim());
                    if (idx >= 0 && idx <= 6) {
                        zoomIndex = idx;
                    }
                } catch (NumberFormatException ignored) {}
            }
            String wx = props.getProperty("windowX");
            String wy = props.getProperty("windowY");
            String ww = props.getProperty("windowWidth");
            String wh = props.getProperty("windowHeight");
            String wm = props.getProperty("windowMaximized");
            if (wx != null && !wx.trim().isEmpty()) {
                try { windowX = Integer.parseInt(wx.trim()); } catch (NumberFormatException ignored) {}
            }
            if (wy != null && !wy.trim().isEmpty()) {
                try { windowY = Integer.parseInt(wy.trim()); } catch (NumberFormatException ignored) {}
            }
            if (ww != null && !ww.trim().isEmpty()) {
                try { windowWidth = Integer.parseInt(ww.trim()); } catch (NumberFormatException ignored) {}
            }
            if (wh != null && !wh.trim().isEmpty()) {
                try { windowHeight = Integer.parseInt(wh.trim()); } catch (NumberFormatException ignored) {}
            }
            if (wm != null && !wm.trim().isEmpty()) {
                windowMaximized = Boolean.parseBoolean(wm.trim());
            }

            // Database configuration properties
            String dbTypeStr = props.getProperty("db.type");
            com.navfinance.database.DatabaseType dbType = com.navfinance.database.DatabaseType.fromString(dbTypeStr);
            String dbHost = props.getProperty("db.host", "localhost");
            String dbPortStr = props.getProperty("db.port");
            int dbPort = dbType.getDefaultPort();
            if (dbPortStr != null && !dbPortStr.isBlank()) {
                try { dbPort = Integer.parseInt(dbPortStr.trim()); } catch (NumberFormatException ignored) {}
            }
            String dbName = props.getProperty("db.name", "navfinance");
            String dbUser = props.getProperty("db.user", dbType.getDefaultUser());
            String dbPass = props.getProperty("db.password", "");
            String dbLocalPath = props.getProperty("db.localPath", DB_PATH);
            String dbCustomUrl = props.getProperty("db.customUrl", "");

            com.navfinance.database.DatabaseConfig cfg = new com.navfinance.database.DatabaseConfig();
            cfg.setType(dbType);
            cfg.setHost(dbHost);
            cfg.setPort(dbPort);
            cfg.setDatabaseName(dbName);
            cfg.setUsername(dbUser);
            cfg.setPassword(dbPass);
            cfg.setLocalPath(dbLocalPath);
            cfg.setCustomJdbcUrl(dbCustomUrl);
            activeDatabaseConfig = cfg;
        } catch (Exception ignored) {}
    }

    /**
     * Reloads persisted settings from disk.
     */
    public static synchronized void reloadSettings() {
        loadSettings();
    }

    /**
     * Persists current settings to settings.properties.
     */
    public static synchronized void saveSettings() {
        try {
            if (!APP_DATA_DIR.exists()) {
                APP_DATA_DIR.mkdirs();
            }
            java.util.Properties props = new java.util.Properties();
            props.setProperty("currencySymbol", currencySymbol);
            props.setProperty("language", language);
            props.setProperty("zoomIndex", String.valueOf(zoomIndex));
            props.setProperty("windowX", String.valueOf(windowX));
            props.setProperty("windowY", String.valueOf(windowY));
            props.setProperty("windowWidth", String.valueOf(windowWidth));
            props.setProperty("windowHeight", String.valueOf(windowHeight));
            props.setProperty("windowMaximized", String.valueOf(windowMaximized));

            if (activeDatabaseConfig != null) {
                props.setProperty("db.type", activeDatabaseConfig.getType().name());
                props.setProperty("db.host", activeDatabaseConfig.getHost());
                props.setProperty("db.port", String.valueOf(activeDatabaseConfig.getPort()));
                props.setProperty("db.name", activeDatabaseConfig.getDatabaseName());
                props.setProperty("db.user", activeDatabaseConfig.getUsername());
                props.setProperty("db.password", activeDatabaseConfig.getPassword());
                props.setProperty("db.localPath", activeDatabaseConfig.getLocalPath());
                props.setProperty("db.customUrl", activeDatabaseConfig.getCustomJdbcUrl());
            }

            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(SETTINGS_FILE)) {
                props.store(fos, "Nav Finance User Settings");
            }
        } catch (Exception ignored) {}
    }

    /**
     * Gets a copy of the active database configuration.
     */
    public static synchronized com.navfinance.database.DatabaseConfig getActiveDatabaseConfig() {
        return new com.navfinance.database.DatabaseConfig(activeDatabaseConfig);
    }

    /**
     * Updates the active database configuration and persists to settings.
     */
    public static synchronized void setActiveDatabaseConfig(com.navfinance.database.DatabaseConfig config) {
        if (config != null) {
            activeDatabaseConfig = new com.navfinance.database.DatabaseConfig(config);
            saveSettings();
        }
    }

    /**
     * Resolves the current live JDBC connection URL.
     */
    public static String getEffectiveJdbcUrl() {
        return activeDatabaseConfig != null ? activeDatabaseConfig.getEffectiveJdbcUrl() : JDBC_URL;
    }

    /**
     * Resolves the current live database username.
     */
    public static String getEffectiveDbUser() {
        return activeDatabaseConfig != null ? activeDatabaseConfig.getUsername() : DB_USER;
    }

    /**
     * Resolves the current live database password.
     */
    public static String getEffectiveDbPassword() {
        return activeDatabaseConfig != null ? activeDatabaseConfig.getPassword() : DB_PASSWORD;
    }

    /**
     * Resolves the active database type.
     */
    public static com.navfinance.database.DatabaseType getEffectiveDbType() {
        return activeDatabaseConfig != null ? activeDatabaseConfig.getType() : com.navfinance.database.DatabaseType.H2;
    }

    /**
     * Gets the persisted zoom scale index.
     *
     * @return active zoom index (0 to 6)
     */
    public static int getZoomIndex() {
        return zoomIndex;
    }

    /**
     * Sets the active zoom scale index and persists to disk.
     *
     * @param idx target zoom index (0 to 6)
     */
    public static void setZoomIndex(int idx) {
        if (idx >= 0 && idx <= 6) {
            zoomIndex = idx;
            saveSettings();
        }
    }

    public static int getWindowX() { return windowX; }
    public static int getWindowY() { return windowY; }
    public static int getWindowWidth() { return windowWidth; }
    public static int getWindowHeight() { return windowHeight; }
    public static boolean isWindowMaximized() { return windowMaximized; }

    public static void saveWindowBounds(int x, int y, int width, int height, boolean maximized) {
        windowX = x;
        windowY = y;
        windowWidth = width;
        windowHeight = height;
        windowMaximized = maximized;
        saveSettings();
    }

    /**
     * Gets the active language code.
     *
     * @return language code (e.g. "en", "hi", "hinglish")
     */
    public static String getLanguage() {
        return language;
    }

    /**
     * Sets the active language code and persists to disk.
     *
     * @param lang target language code
     */
    public static void setLanguage(String lang) {
        if (lang != null && !lang.trim().isEmpty()) {
            language = lang.trim();
            saveSettings();
        }
    }

    /**
     * Gets the active currency symbol.
     *
     * @return The active currency symbol string (e.g. ₹, $, €).
     */
    public static String getCurrencySymbol() {
        return currencySymbol;
    }

    /**
     * Sets the active currency symbol and persists to disk.
     *
     * @param symbol The new currency symbol to apply across the application.
     */
    public static void setCurrencySymbol(String symbol) {
        if (symbol != null && !symbol.trim().isEmpty()) {
            currencySymbol = symbol.trim();
            saveSettings();
        }
    }

    /**
     * Formats a numerical currency amount with the active currency symbol and Indian grouping (lakhs/crores).
     *
     * @param amount The numerical amount to format.
     * @return Formatted currency string (e.g. {@code ₹ 1,50,000.00}).
     */
    public static String formatCurrency(double amount) {
        DecimalFormat df = new DecimalFormat("#,##,##0.00");
        return currencySymbol + " " + df.format(amount);
    }

    /**
     * Formats a date using the standard application date formatter (dd-MMM-yyyy).
     *
     * @param date Date to format
     * @return Formatted date string, or empty string if null
     */
    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }

    /**
     * Formats a date-time using standard application timestamp formatter (dd-MMM-yyyy hh:mm a).
     *
     * @param dateTime DateTime to format
     * @return Formatted timestamp string, or empty string if null
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMATTER) : "";
    }

    /**
     * Formats a numerical currency amount without the currency symbol.
     *
     * @param amount The numerical amount to format.
     * @return Formatted numeric string (e.g. {@code 1,50,000.00}).
     */
    public static String formatCurrencyPlain(double amount) {
        DecimalFormat df = new DecimalFormat("#,##,##0.00");
        return df.format(amount);
    }
}
