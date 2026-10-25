package com.navfinance.ui.components;

import com.navfinance.config.AppConfig;
import com.navfinance.ui.util.NavIcons;
import com.navfinance.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reusable composite Date Picker component combining a manual text input field with an
 * interactive modal popup calendar widget.
 * <p>
 * Supports Indian/Standard formatted text entry (dd-MM-yyyy) with lenient parsing fallbacks
 * (slashes, dots, compact digits, 2-digit years, and ISO format), automatic normalization upon focus loss,
 * and comprehensive month/year visual navigation.
 * </p>
 */
public class DatePickerField extends JPanel {

    /** Internal text field displaying the formatted date string. */
    private final JTextField txtDate;

    /** Calendar trigger button displaying the calendar emoji/icon. */
    private final JButton btnPicker;

    /** Observers registered for date modification events. */
    private final List<ActionListener> actionListeners = new ArrayList<>();

    /**
     * Supported date parsing patterns ordered by precedence:
     * Primary is dd-MM-yyyy / d-M-yyyy, followed by slash/dot separators, compact 8-digit, 2-digit year, and ISO fallback.
     */
    private static final DateTimeFormatter[] PARSE_FORMATTERS = new DateTimeFormatter[] {
            DateTimeFormatter.ofPattern("d-M-uuuu"),
            DateTimeFormatter.ofPattern("d/M/uuuu"),
            DateTimeFormatter.ofPattern("d.M.uuuu"),
            DateTimeFormatter.ofPattern("ddMMuuuu"),
            new DateTimeFormatterBuilder()
                    .appendPattern("d-M-")
                    .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
                    .toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder()
                    .appendPattern("d/M/")
                    .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
                    .toFormatter(Locale.ENGLISH),
            DateTimeFormatter.ofPattern("uuuu-M-d"),
            DateTimeFormatter.ofPattern("uuuu/M/d")
    };

    /**
     * Constructs a DatePickerField initialized to the current system date.
     */
    public DatePickerField() {
        this(LocalDate.now());
    }

    /**
     * Constructs a DatePickerField initialized to the specified date with default column width.
     *
     * @param initialDate starting date value
     */
    public DatePickerField(LocalDate initialDate) {
        this(initialDate, 10);
    }

    /**
     * Constructs a DatePickerField initialized to the specified date and column width.
     *
     * @param initialDate starting date value
     * @param columns text field width in columns
     */
    public DatePickerField(LocalDate initialDate, int columns) {
        setLayout(new BorderLayout(2, 0));
        setOpaque(false);

        txtDate = new JTextField(columns);
        txtDate.setFont(UiUtils.FONT_REGULAR);
        txtDate.setToolTipText("Enter date in DD-MM-YYYY format");
        if (initialDate != null) {
            txtDate.setText(initialDate.format(AppConfig.INPUT_DATE_FORMATTER));
        }

        btnPicker = new JButton(NavIcons.calendar(14, UiUtils.COLOR_PRIMARY));
        btnPicker.setToolTipText("Open Calendar Picker");
        btnPicker.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPicker.setFocusPainted(false);
        btnPicker.setMargin(new Insets(2, 6, 2, 6));
        btnPicker.setBackground(new Color(241, 245, 249));

        btnPicker.addActionListener(e -> showCalendarPopup());

        // Validate on enter or focus lost
        txtDate.addActionListener(e -> fireActionEvent());
        txtDate.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // If valid date was typed, keep normalized in standard DD-MM-YYYY format
                LocalDate d = getDate();
                if (d != null) {
                    txtDate.setText(d.format(AppConfig.INPUT_DATE_FORMATTER));
                }
            }
        });

        add(txtDate, BorderLayout.CENTER);
        add(btnPicker, BorderLayout.EAST);
    }

    /**
     * Parses the current text content into a {@link LocalDate}.
     * Supports dd-MM-yyyy (primary), slash-separated (dd/MM/yyyy), dot-separated (dd.MM.yyyy),
     * compact (ddMMyyyy), 2-digit years, and ISO (yyyy-MM-dd).
     *
     * @return parsed {@link LocalDate}, or {@code null} if the field is blank or unparseable
     */
    public LocalDate getDate() {
        return parseDateLenient(txtDate.getText());
    }

    /**
     * Parses the given text string leniently into a {@link LocalDate}.
     * Prioritizes dd-MM-yyyy format, then slash/dot separators, compact 8-digit, 2-digit year, and ISO format.
     *
     * @param text date string to parse
     * @return parsed {@link LocalDate}, or {@code null} if unparseable or blank
     */
    public static LocalDate parseDateLenient(String text) {
        if (text == null) return null;
        String val = text.trim();
        if (val.isEmpty()) return null;

        for (DateTimeFormatter fmt : PARSE_FORMATTERS) {
            try {
                return LocalDate.parse(val, fmt);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * Sets the displayed date value in dd-MM-yyyy format and notifies action listeners.
     *
     * @param date date value to display, or {@code null} to clear
     */
    public void setDate(LocalDate date) {
        if (date != null) {
            txtDate.setText(date.format(AppConfig.INPUT_DATE_FORMATTER));
        } else {
            txtDate.setText("");
        }
        fireActionEvent();
    }

    /**
     * Returns the raw trimmed string content of the underlying text field.
     *
     * @return date text string
     */
    public String getText() {
        return txtDate.getText().trim();
    }

    /**
     * Directly sets the string value of the underlying text field.
     *
     * @param text date string
     */
    public void setText(String text) {
        txtDate.setText(text != null ? text.trim() : "");
    }

    /**
     * Returns the internal {@link JTextField} instance for direct listener binding or styling.
     *
     * @return internal text field
     */
    public JTextField getTextField() {
        return txtDate;
    }

    /**
     * Enables or disables both the text field and calendar popup button.
     *
     * @param enabled {@code true} to enable, {@code false} to disable
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        txtDate.setEnabled(enabled);
        btnPicker.setEnabled(enabled);
    }

    /**
     * Adds an {@link ActionListener} that fires whenever the date value is altered.
     *
     * @param l listener callback
     */
    public void addActionListener(ActionListener l) {
        actionListeners.add(l);
    }

    /**
     * Dispatches an action event notification to all registered listeners.
     */
    private void fireActionEvent() {
        ActionEvent evt = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, txtDate.getText());
        for (ActionListener l : actionListeners) {
            l.actionPerformed(evt);
        }
    }

    /**
     * Positions and reveals the modal calendar dialog directly adjacent to the input field.
     */
    private void showCalendarPopup() {
        LocalDate current = getDate();
        if (current == null) {
            current = LocalDate.now();
        }

        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog popup = new JDialog(parentWindow);
        popup.setUndecorated(true);
        popup.setModal(true);

        CalendarPickerPanel pickerPanel = new CalendarPickerPanel(current, selectedDate -> {
            if (selectedDate != null) {
                setDate(selectedDate);
            }
            popup.dispose();
        }, () -> {
            setText("");
            fireActionEvent();
            popup.dispose();
        }, popup::dispose);

        popup.setContentPane(pickerPanel);
        popup.pack();

        // Calculate placement on screen relative to the text field
        try {
            Point loc = txtDate.getLocationOnScreen();
            int x = loc.x;
            int y = loc.y + txtDate.getHeight() + 2;

            // Ensure popup does not overflow viewport dimensions
            Dimension screen = getToolkit().getScreenSize();
            if (x + popup.getWidth() > screen.width) {
                x = screen.width - popup.getWidth() - 10;
            }
            if (y + popup.getHeight() > screen.height) {
                y = loc.y - popup.getHeight() - 2;
            }

            popup.setLocation(x, y);
        } catch (java.awt.IllegalComponentStateException | NullPointerException ex) {
            popup.setLocationRelativeTo(this);
        }

        popup.setVisible(true);
    }

    /**
     * Inner interactive calendar panel featuring month/year dropdowns, previous/next controls,
     * a 7x6 day grid, and 'Today' and 'Clear' quick actions.
     */
    private static class CalendarPickerPanel extends JPanel {

        /** Active year-month shown in calendar view. */
        private YearMonth currentYearMonth;
        /** Initially preselected date highlighted in blue. */
        private final LocalDate initialSelectedDate;
        /** Callback invoked when user clicks a specific day. */
        private final java.util.function.Consumer<LocalDate> onDateSelected;
        /** Callback invoked when clear button is clicked. */
        private final Runnable onClear;
        /** Callback invoked when close cross is clicked. */
        private final Runnable onClose;

        /** Month selection combo box. */
        private JComboBox<String> cmbMonth;
        /** Year selection combo box (editable). */
        private JComboBox<Integer> cmbYear;
        /** Flag to prevent action listener feedback loops while synchronizing combo boxes. */
        private boolean updatingCombos = false;
        /** Day cell button container panel. */
        private JPanel daysGrid;

        /** Month display names. */
        private static final String[] MONTH_NAMES = {
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        };

        /**
         * Constructs a calendar picker popup view.
         *
         * @param selectedDate initial selected date
         * @param onDateSelected handler for selected date
         * @param onClear handler to clear date
         * @param onClose handler to dismiss popup
         */
        public CalendarPickerPanel(LocalDate selectedDate, java.util.function.Consumer<LocalDate> onDateSelected,
                                   Runnable onClear, Runnable onClose) {
            this.initialSelectedDate = selectedDate;
            this.currentYearMonth = YearMonth.from(selectedDate != null ? selectedDate : LocalDate.now());
            this.onDateSelected = onDateSelected;
            this.onClear = onClear;
            this.onClose = onClose;

            setLayout(new BorderLayout(0, 6));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UiUtils.COLOR_PRIMARY, 1, true),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));
            setPreferredSize(new Dimension(330, 285));

            initComponents();
            renderDays();
        }

        /**
         * Builds header selectors, day grid layout, and action footer.
         */
        private void initComponents() {
            // Header: Month & Year Selector + Prev/Next Buttons + Close
            JPanel headerPanel = new JPanel(new BorderLayout(4, 0));
            headerPanel.setOpaque(false);

            // Left Navigation: Previous Year (<<) and Previous Month (<)
            JPanel leftNav = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
            leftNav.setOpaque(false);

            JButton btnPrevYear = new JButton(NavIcons.chevronDoubleLeft(10, UiUtils.COLOR_TEXT_PRIMARY));
            btnPrevYear.setToolTipText("Previous Year");
            btnPrevYear.setMargin(new Insets(2, 4, 2, 4));
            btnPrevYear.setFocusPainted(false);
            btnPrevYear.addActionListener(e -> {
                currentYearMonth = currentYearMonth.minusYears(1);
                syncCombos();
                renderDays();
            });

            JButton btnPrevMonth = new JButton(NavIcons.chevronLeft(10, UiUtils.COLOR_TEXT_PRIMARY));
            btnPrevMonth.setToolTipText("Previous Month");
            btnPrevMonth.setMargin(new Insets(2, 4, 2, 4));
            btnPrevMonth.setFocusPainted(false);
            btnPrevMonth.addActionListener(e -> {
                currentYearMonth = currentYearMonth.minusMonths(1);
                syncCombos();
                renderDays();
            });

            leftNav.add(btnPrevYear);
            leftNav.add(btnPrevMonth);

            // Center: Month & Year Dropdowns
            JPanel centerSelectors = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
            centerSelectors.setOpaque(false);

            cmbMonth = new JComboBox<>(MONTH_NAMES);
            cmbMonth.setFont(UiUtils.FONT_REGULAR);
            cmbMonth.setSelectedIndex(currentYearMonth.getMonthValue() - 1);
            cmbMonth.addActionListener(e -> {
                if (updatingCombos) return;
                int m = cmbMonth.getSelectedIndex() + 1;
                currentYearMonth = YearMonth.of(currentYearMonth.getYear(), m);
                renderDays();
            });

            // Populate Year Model with wide coverage (1950 to 2070)
            DefaultComboBoxModel<Integer> yearModel = new DefaultComboBoxModel<>();
            for (int y = 1950; y <= 2070; y++) {
                yearModel.addElement(y);
            }
            if (yearModel.getIndexOf(currentYearMonth.getYear()) == -1) {
                yearModel.addElement(currentYearMonth.getYear());
            }

            cmbYear = new JComboBox<>(yearModel);
            cmbYear.setFont(UiUtils.FONT_REGULAR);
            cmbYear.setEditable(true);
            cmbYear.setSelectedItem(currentYearMonth.getYear());
            cmbYear.setToolTipText("Select or type any year");
            cmbYear.addActionListener(e -> {
                if (updatingCombos) return;
                int yr = parseYearFromCombo(cmbYear.getSelectedItem());
                if (yr > 0 && yr != currentYearMonth.getYear()) {
                    currentYearMonth = YearMonth.of(yr, currentYearMonth.getMonthValue());
                    syncCombos();
                    renderDays();
                }
            });

            centerSelectors.add(cmbMonth);
            centerSelectors.add(cmbYear);

            // Right Navigation: Next Month (>), Next Year (>>), and Close (X)
            JPanel rightNav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
            rightNav.setOpaque(false);

            JButton btnNextMonth = new JButton(NavIcons.chevronRight(10, UiUtils.COLOR_TEXT_PRIMARY));
            btnNextMonth.setToolTipText("Next Month");
            btnNextMonth.setMargin(new Insets(2, 4, 2, 4));
            btnNextMonth.setFocusPainted(false);
            btnNextMonth.addActionListener(e -> {
                currentYearMonth = currentYearMonth.plusMonths(1);
                syncCombos();
                renderDays();
            });

            JButton btnNextYear = new JButton(NavIcons.chevronDoubleRight(10, UiUtils.COLOR_TEXT_PRIMARY));
            btnNextYear.setToolTipText("Next Year");
            btnNextYear.setMargin(new Insets(2, 4, 2, 4));
            btnNextYear.setFocusPainted(false);
            btnNextYear.addActionListener(e -> {
                currentYearMonth = currentYearMonth.plusYears(1);
                syncCombos();
                renderDays();
            });

            JButton btnClose = new JButton(NavIcons.cross(10, UiUtils.COLOR_TEXT_MUTED));
            btnClose.setToolTipText("Close");
            btnClose.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            btnClose.setContentAreaFilled(false);
            btnClose.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnClose.addActionListener(e -> onClose.run());

            rightNav.add(btnNextMonth);
            rightNav.add(btnNextYear);
            rightNav.add(btnClose);

            headerPanel.add(leftNav, BorderLayout.WEST);
            headerPanel.add(centerSelectors, BorderLayout.CENTER);
            headerPanel.add(rightNav, BorderLayout.EAST);
            add(headerPanel, BorderLayout.NORTH);

            // Center Panel: Weekday Headers + Days Grid
            JPanel centerPanel = new JPanel(new BorderLayout(0, 4));
            centerPanel.setOpaque(false);

            // Day of week headers (Mo - Su)
            JPanel weekDaysHeader = new JPanel(new GridLayout(1, 7, 2, 2));
            weekDaysHeader.setOpaque(false);
            String[] days = {"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"};
            for (String day : days) {
                JLabel lbl = new JLabel(day, SwingConstants.CENTER);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
                lbl.setForeground(day.equals("Su") ? UiUtils.COLOR_EXPENSE : UiUtils.COLOR_TEXT_MUTED);
                weekDaysHeader.add(lbl);
            }
            centerPanel.add(weekDaysHeader, BorderLayout.NORTH);

            // Days Grid (6 weeks x 7 days = 42 cells)
            daysGrid = new JPanel(new GridLayout(6, 7, 2, 2));
            daysGrid.setOpaque(false);
            centerPanel.add(daysGrid, BorderLayout.CENTER);

            add(centerPanel, BorderLayout.CENTER);

            // Footer: Today & Clear Buttons
            JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            footerPanel.setOpaque(false);

            JButton btnClear = new JButton("Clear");
            btnClear.setFont(UiUtils.FONT_SUBTITLE);
            btnClear.setMargin(new Insets(2, 8, 2, 8));
            btnClear.setFocusPainted(false);
            btnClear.addActionListener(e -> onClear.run());

            JButton btnToday = new JButton("Today");
            btnToday.setFont(UiUtils.FONT_BOLD);
            btnToday.setForeground(UiUtils.COLOR_PRIMARY);
            btnToday.setMargin(new Insets(2, 10, 2, 10));
            btnToday.setFocusPainted(false);
            btnToday.addActionListener(e -> onDateSelected.accept(LocalDate.now()));

            footerPanel.add(btnClear);
            footerPanel.add(btnToday);
            add(footerPanel, BorderLayout.SOUTH);
        }

        /**
         * Safely parses a year value from the combo box, handling both Integer objects and manual string typing.
         */
        private int parseYearFromCombo(Object val) {
            if (val instanceof Integer i) {
                return i;
            } else if (val != null) {
                try {
                    int parsed = Integer.parseInt(val.toString().trim());
                    if (parsed >= 1900 && parsed <= 2100) {
                        return parsed;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            return currentYearMonth.getYear();
        }

        /**
         * Synchronizes combo box selections with the active year and month without triggering recursive events.
         */
        private void syncCombos() {
            updatingCombos = true;
            try {
                cmbMonth.setSelectedIndex(currentYearMonth.getMonthValue() - 1);
                int yr = currentYearMonth.getYear();
                DefaultComboBoxModel<Integer> model = (DefaultComboBoxModel<Integer>) cmbYear.getModel();
                if (model.getIndexOf(yr) == -1) {
                    model.addElement(yr);
                }
                cmbYear.setSelectedItem(yr);
            } finally {
                updatingCombos = false;
            }
        }

        /**
         * Populates the 42-cell calendar grid with previous, current, and next month day buttons.
         */
        private void renderDays() {
            daysGrid.removeAll();

            LocalDate firstOfMonth = currentYearMonth.atDay(1);
            int startDayOfWeek = firstOfMonth.getDayOfWeek().getValue(); // 1 = Monday, 7 = Sunday
            int daysInMonth = currentYearMonth.lengthOfMonth();

            LocalDate today = LocalDate.now();

            // Previous month trailing days
            YearMonth prevMonth = currentYearMonth.minusMonths(1);
            int prevMonthDays = prevMonth.lengthOfMonth();
            for (int i = startDayOfWeek - 1; i > 0; i--) {
                int d = prevMonthDays - i + 1;
                LocalDate date = prevMonth.atDay(d);
                daysGrid.add(createDayButton(String.valueOf(d), date, false, false, false));
            }

            // Current month days
            for (int day = 1; day <= daysInMonth; day++) {
                LocalDate date = currentYearMonth.atDay(day);
                boolean isToday = date.equals(today);
                boolean isSelected = initialSelectedDate != null && date.equals(initialSelectedDate);
                daysGrid.add(createDayButton(String.valueOf(day), date, true, isToday, isSelected));
            }

            // Next month leading days to fill 42 cells (6 rows * 7)
            int currentCells = (startDayOfWeek - 1) + daysInMonth;
            int remaining = 42 - currentCells;
            YearMonth nextMonth = currentYearMonth.plusMonths(1);
            for (int day = 1; day <= remaining; day++) {
                LocalDate date = nextMonth.atDay(day);
                daysGrid.add(createDayButton(String.valueOf(day), date, false, false, false));
            }

            daysGrid.revalidate();
            daysGrid.repaint();
        }

        /**
         * Creates an individual day button styled according to whether it is today, selected,
         * or in a neighboring month.
         */
        private JButton createDayButton(String text, LocalDate date, boolean isCurrentMonth, boolean isToday, boolean isSelected) {
            JButton btn = new JButton(text);
            btn.setFont(new Font("Segoe UI", isToday || isSelected ? Font.BOLD : Font.PLAIN, 11));
            btn.setMargin(new Insets(1, 1, 1, 1));
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            if (isSelected) {
                btn.setBackground(UiUtils.COLOR_PRIMARY);
                btn.setForeground(Color.WHITE);
                btn.setBorder(BorderFactory.createLineBorder(UiUtils.COLOR_PRIMARY, 1));
            } else if (isToday) {
                btn.setBackground(new Color(239, 246, 255));
                btn.setForeground(UiUtils.COLOR_PRIMARY);
                btn.setBorder(BorderFactory.createLineBorder(UiUtils.COLOR_PRIMARY, 1));
            } else if (!isCurrentMonth) {
                btn.setBackground(Color.WHITE);
                btn.setForeground(new Color(203, 213, 225));
                btn.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
            } else {
                btn.setBackground(Color.WHITE);
                btn.setForeground(date.getDayOfWeek() == DayOfWeek.SUNDAY ? new Color(239, 68, 68) : new Color(30, 41, 59));
                btn.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
            }

            btn.addActionListener(e -> onDateSelected.accept(date));
            return btn;
        }
    }
}
