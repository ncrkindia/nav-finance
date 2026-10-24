package com.navfinance.i18n;

import com.navfinance.config.AppConfig;

import javax.swing.UIManager;
import java.awt.Font;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized Internationalization (i18n) and Localization (l10n) subsystem.
 * <p>
 * Manages active language state, UTF-8 properties bundle loading, message lookup with
 * fallback hierarchy, typography font switching (e.g. Nirmala UI for Hindi vs Segoe UI
 * for English/Hinglish), and event broadcasting to registered UI observers.
 * </p>
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public final class I18n {

    private static final Logger LOGGER = Logger.getLogger(I18n.class.getName());

    /** Fallback default language. */
    private static final AppLanguage DEFAULT_LANGUAGE = AppLanguage.ENGLISH;

    /** Currently active user interface language. */
    private static AppLanguage currentLanguage = DEFAULT_LANGUAGE;

    /** In-memory cache of loaded language properties bundles. */
    private static final Map<AppLanguage, Properties> BUNDLES = new HashMap<>();

    /** Registered observers to notify upon language switch. */
    private static final List<LanguageChangeListener> LISTENERS = Collections.synchronizedList(new ArrayList<>());

    // Font families for high-clarity premium rendering
    private static final String FONT_NAME_LATIN = "Segoe UI";
    private static final String FONT_NAME_DEVANAGARI = "Nirmala UI";

    static {
        loadAllBundles();
        // Restore persisted user language preference if saved
        String savedCode = AppConfig.getLanguage();
        if (savedCode != null && !savedCode.isEmpty()) {
            currentLanguage = AppLanguage.fromCode(savedCode);
        }
        applyGlobalFontDefaults();
    }

    private I18n() {}

    /**
     * Pre-loads all translation bundles using explicit UTF-8 character encoding.
     */
    private static void loadAllBundles() {
        for (AppLanguage lang : AppLanguage.values()) {
            Properties props = new Properties();
            String resourcePath = "/i18n/messages_" + lang.getCode() + ".properties";
            try (InputStream is = I18n.class.getResourceAsStream(resourcePath)) {
                if (is != null) {
                    try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                        props.load(reader);
                        BUNDLES.put(lang, props);
                        LOGGER.info("Successfully loaded i18n bundle: " + resourcePath + " (" + props.size() + " entries)");
                    }
                } else {
                    LOGGER.warning("Missing i18n resource file: " + resourcePath);
                    BUNDLES.put(lang, props);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to load translation bundle: " + resourcePath, e);
                BUNDLES.put(lang, props);
            }
        }
    }

    /**
     * Gets the currently active application language.
     *
     * @return current {@link AppLanguage}
     */
    public static AppLanguage getCurrentLanguage() {
        return currentLanguage;
    }

    /**
     * Switches the active application language, updates global typography defaults,
     * persists the setting to user preferences, and notifies all registered UI listeners.
     *
     * @param language new target language
     */
    public static void setLanguage(AppLanguage language) {
        if (language == null || language == currentLanguage) {
            return;
        }
        currentLanguage = language;
        AppConfig.setLanguage(language.getCode());
        applyGlobalFontDefaults();

        // Broadcast to listeners on Event Dispatch Thread / caller thread
        List<LanguageChangeListener> copy;
        synchronized (LISTENERS) {
            copy = new ArrayList<>(LISTENERS);
        }
        for (LanguageChangeListener listener : copy) {
            try {
                listener.onLanguageChanged(currentLanguage);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error notifying language change listener", e);
            }
        }
    }

    /**
     * Registers a listener to receive dynamic language change updates.
     *
     * @param listener observer to attach
     */
    public static void addListener(LanguageChangeListener listener) {
        if (listener != null && !LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    /**
     * Unregisters a previously registered language change listener.
     *
     * @param listener observer to remove
     */
    public static void removeListener(LanguageChangeListener listener) {
        LISTENERS.remove(listener);
    }

    /**
     * Retrieves the localized string associated with the specified key in the active language.
     * If not found, falls back gracefully to English, and finally returns the key itself.
     *
     * @param key bundle property key
     * @return localized string message
     */
    public static String get(String key) {
        if (key == null) return "";
        Properties currentProps = BUNDLES.get(currentLanguage);
        if (currentProps != null && currentProps.containsKey(key)) {
            return currentProps.getProperty(key);
        }
        // Fallback to English
        Properties fallbackProps = BUNDLES.get(DEFAULT_LANGUAGE);
        if (fallbackProps != null && fallbackProps.containsKey(key)) {
            return fallbackProps.getProperty(key);
        }
        return key;
    }

    /**
     * Retrieves the localized string and formats it with positional parameters ({0}, {1}, etc.).
     *
     * @param key bundle property key
     * @param args formatting argument values
     * @return formatted localized string
     */
    public static String get(String key, Object... args) {
        String pattern = get(key);
        if (args == null || args.length == 0) {
            return pattern;
        }
        try {
            return MessageFormat.format(pattern, args);
        } catch (Exception e) {
            return pattern;
        }
    }

    // =========================================================================
    // Typography System (Clear & Premium Fonts supporting Devanagari & Latin)
    // =========================================================================

    /**
     * Determines the optimal font family name based on the active language script.
     *
     * @return font family name ("Nirmala UI" for Hindi, "Segoe UI" for English/Hinglish)
     */
    public static String getFontFamily() {
        return currentLanguage == AppLanguage.HINDI ? FONT_NAME_DEVANAGARI : FONT_NAME_LATIN;
    }

    /**
     * Creates a screen title font (18pt Bold) tailored to the active language script.
     *
     * @return title font
     */
    public static Font getTitleFont() {
        return new Font(getFontFamily(), Font.BOLD, 18);
    }

    /**
     * Creates a section header font (14pt Bold).
     *
     * @return header font
     */
    public static Font getHeaderFont() {
        return new Font(getFontFamily(), Font.BOLD, 14);
    }

    /**
     * Creates a regular body text font (12pt Plain).
     *
     * @return body font
     */
    public static Font getRegularFont() {
        return new Font(getFontFamily(), Font.PLAIN, 12);
    }

    /**
     * Creates an emphasized bold text font (12pt Bold).
     *
     * @return bold font
     */
    public static Font getBoldFont() {
        return new Font(getFontFamily(), Font.BOLD, 12);
    }

    /**
     * Creates a secondary subtitle or caption font (11pt Plain).
     *
     * @return subtitle font
     */
    public static Font getSubFont() {
        return new Font(getFontFamily(), Font.PLAIN, 11);
    }

    /**
     * Creates a prominent KPI card value font (22pt Bold).
     *
     * @return KPI font
     */
    public static Font getKpiFont() {
        return new Font(getFontFamily(), Font.BOLD, 22);
    }

    /**
     * Configures Swing / FlatLaf global font mappings for standard UI widgets.
     */
    public static void applyGlobalFontDefaults() {
        Font defaultFont = getRegularFont();
        UIManager.put("defaultFont", defaultFont);
        UIManager.put("Label.font", defaultFont);
        UIManager.put("Button.font", getBoldFont());
        UIManager.put("ToggleButton.font", defaultFont);
        UIManager.put("RadioButton.font", defaultFont);
        UIManager.put("CheckBox.font", defaultFont);
        UIManager.put("ComboBox.font", defaultFont);
        UIManager.put("TextField.font", defaultFont);
        UIManager.put("TextArea.font", defaultFont);
        UIManager.put("Table.font", defaultFont);
        UIManager.put("TableHeader.font", getBoldFont());
        UIManager.put("TabbedPane.font", getBoldFont());
        UIManager.put("TitledBorder.font", getBoldFont());
    }
}
