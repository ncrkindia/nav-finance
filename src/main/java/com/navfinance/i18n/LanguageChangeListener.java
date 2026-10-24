package com.navfinance.i18n;

/**
 * Functional callback interface invoked whenever the active application language is changed.
 * <p>
 * Registered UI panels and dialogs implement this listener to dynamically re-translate
 * and update their labels, titles, table headers, and layout typography without restarting.
 * </p>
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
@FunctionalInterface
public interface LanguageChangeListener {

    /**
     * Notification triggered when the language has switched.
     *
     * @param newLanguage the newly selected {@link AppLanguage}
     */
    void onLanguageChanged(AppLanguage newLanguage);
}
