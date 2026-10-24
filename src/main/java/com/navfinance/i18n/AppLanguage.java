package com.navfinance.i18n;

/**
 * Enumeration of supported application user interface languages.
 * <p>
 * Supports Standard English, native Devanagari Hindi, and Hinglish
 * (Hindi terminology written phonetically in Latin script).
 * </p>
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public enum AppLanguage {

    /** Standard International English. */
    ENGLISH("en", "English", "English"),

    /** Proper Devanagari Hindi. */
    HINDI("hi", "हिन्दी", "Hindi"),

    /** Hinglish: Hindi financial terminology in Roman script. */
    HINGLISH("hinglish", "Hinglish", "Hinglish (Hindi in English)");

    private final String code;
    private final String nativeName;
    private final String displayName;

    AppLanguage(String code, String nativeName, String displayName) {
        this.code = code;
        this.nativeName = nativeName;
        this.displayName = displayName;
    }

    /**
     * Unique language identifier code (e.g. "en", "hi", "hinglish").
     *
     * @return language code
     */
    public String getCode() {
        return code;
    }

    /**
     * Native language name representation.
     *
     * @return native name
     */
    public String getNativeName() {
        return nativeName;
    }

    /**
     * Human-readable display label for selector dropdowns.
     *
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Resolves an {@link AppLanguage} by its language code, defaulting to English.
     *
     * @param code language code string
     * @return resolved {@link AppLanguage}
     */
    public static AppLanguage fromCode(String code) {
        if (code == null) return ENGLISH;
        String trimmed = code.trim().toLowerCase();
        for (AppLanguage lang : values()) {
            if (lang.code.equalsIgnoreCase(trimmed)) {
                return lang;
            }
        }
        return ENGLISH;
    }
}
