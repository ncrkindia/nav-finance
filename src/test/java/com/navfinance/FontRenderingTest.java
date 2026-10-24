package com.navfinance;

import com.formdev.flatlaf.FlatLightLaf;
import com.navfinance.i18n.AppLanguage;
import com.navfinance.i18n.I18n;
import org.junit.jupiter.api.Test;

import java.awt.Font;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that Devanagari script glyphs (Hindi) render cleanly without tofu
 * or replacement rectangles across titles, KPI labels, and dashboard metrics.
 */
public class FontRenderingTest {

    @Test
    void testHindiGlyphCoverage() {
        FlatLightLaf.setup();
        I18n.setLanguage(AppLanguage.HINDI);
        Font font = I18n.getRegularFont();

        String hindiTitle = I18n.get("app.name") + " - " + I18n.get("app.subtitle");
        String hindiKpi = I18n.get("dashboard.kpi.totalIncome");

        for (char c : hindiTitle.toCharArray()) {
            if (c != ' ' && c != '-') {
                assertTrue(font.canDisplay(c), "Font " + font.getFamily() + " should display '" + c + "'");
            }
        }
        for (char c : hindiKpi.toCharArray()) {
            if (c != ' ' && c != '(' && c != ')') {
                assertTrue(font.canDisplay(c), "Font " + font.getFamily() + " should display '" + c + "'");
            }
        }
    }
}
