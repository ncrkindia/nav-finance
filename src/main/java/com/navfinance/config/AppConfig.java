package com.navfinance.config;

import java.io.File;
import java.text.DecimalFormat;
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
    public static final String APP_SUBTITLE = "Personal Finance & Udhar Management System";

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

    /**
     * Gets the active currency symbol.
     *
     * @return The active currency symbol string (e.g. ₹, $, €).
     */
    public static String getCurrencySymbol() {
        return currencySymbol;
    }

    /**
     * Sets the active currency symbol.
     *
     * @param symbol The new currency symbol to apply across the application.
     */
    public static void setCurrencySymbol(String symbol) {
        if (symbol != null && !symbol.trim().isEmpty()) {
            currencySymbol = symbol.trim();
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
