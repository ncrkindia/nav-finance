package com.navfinance.ui;

import com.navfinance.ui.components.DatePickerField;
import com.navfinance.ui.util.NavIcons;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.Icon;
import java.awt.Color;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class DatePickerFieldTest {

    @BeforeAll
    public static void setUpHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    public void testParseDateLenientStandardDdMmYyyy() {
        LocalDate date = DatePickerField.parseDateLenient("25-10-2026");
        assertNotNull(date);
        assertEquals(LocalDate.of(2026, 10, 25), date);

        LocalDate singleDigits = DatePickerField.parseDateLenient("5-3-2026");
        assertNotNull(singleDigits);
        assertEquals(LocalDate.of(2026, 3, 5), singleDigits);
    }

    @Test
    public void testParseDateLenientSlashAndDotSeparators() {
        LocalDate slashDate = DatePickerField.parseDateLenient("25/10/2026");
        assertNotNull(slashDate);
        assertEquals(LocalDate.of(2026, 10, 25), slashDate);

        LocalDate dotDate = DatePickerField.parseDateLenient("25.10.2026");
        assertNotNull(dotDate);
        assertEquals(LocalDate.of(2026, 10, 25), dotDate);

        LocalDate singleDot = DatePickerField.parseDateLenient("5.3.2026");
        assertNotNull(singleDot);
        assertEquals(LocalDate.of(2026, 3, 5), singleDot);
    }

    @Test
    public void testParseDateLenientCompactAndTwoDigitYear() {
        LocalDate compact = DatePickerField.parseDateLenient("25102026");
        assertNotNull(compact);
        assertEquals(LocalDate.of(2026, 10, 25), compact);

        LocalDate twoDigit = DatePickerField.parseDateLenient("25-10-26");
        assertNotNull(twoDigit);
        assertEquals(LocalDate.of(2026, 10, 25), twoDigit);
    }

    @Test
    public void testParseDateLenientIsoFallback() {
        LocalDate isoDate = DatePickerField.parseDateLenient("2026-10-25");
        assertNotNull(isoDate);
        assertEquals(LocalDate.of(2026, 10, 25), isoDate);
    }

    @Test
    public void testParseDateLenientInvalidInputs() {
        assertNull(DatePickerField.parseDateLenient(null));
        assertNull(DatePickerField.parseDateLenient(""));
        assertNull(DatePickerField.parseDateLenient("   "));
        assertNull(DatePickerField.parseDateLenient("invalid-date"));
        assertNull(DatePickerField.parseDateLenient("32-01-2026"));
    }

    @Test
    public void testDatePickerFieldDisplayAndNormalization() {
        LocalDate initial = LocalDate.of(2026, 10, 25);
        DatePickerField dp = new DatePickerField(initial);
        assertEquals("25-10-2026", dp.getText());
        assertEquals(initial, dp.getDate());

        dp.setDate(LocalDate.of(2025, 1, 5));
        assertEquals("05-01-2025", dp.getText());
        assertEquals(LocalDate.of(2025, 1, 5), dp.getDate());

        dp.setText("15/08/1947");
        assertEquals(LocalDate.of(1947, 8, 15), dp.getDate());

        dp.setDate(null);
        assertEquals("", dp.getText());
        assertNull(dp.getDate());
    }

    @Test
    public void testYearNavigationIcons() {
        Icon doubleLeft = NavIcons.chevronDoubleLeft(14, Color.BLACK);
        assertNotNull(doubleLeft);
        assertEquals(14, doubleLeft.getIconWidth());
        assertEquals(14, doubleLeft.getIconHeight());

        Icon doubleRight = NavIcons.chevronDoubleRight(14, Color.BLACK);
        assertNotNull(doubleRight);
        assertEquals(14, doubleRight.getIconWidth());
        assertEquals(14, doubleRight.getIconHeight());
    }
}
