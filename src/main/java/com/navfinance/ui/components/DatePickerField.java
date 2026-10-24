package com.navfinance.ui.components;

import com.navfinance.config.AppConfig;
import com.navfinance.ui.util.NavIcons;
import com.navfinance.ui.util.UiUtils;

import javax.swing.BorderFactory;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Reusable composite Date Picker component combining a manual text input field with an
 * interactive modal popup calendar widget.
 * <p>
 * Supports ISO-8601 formatted text entry (YYYY-MM-DD) with lenient parsing fallbacks,
 * automatic normalization upon focus loss, and month/year visual navigation.
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
        if (initialDate != null) {
            txtDate.setText(initialDate.format(AppConfig.ISO_DATE_FORMATTER));
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
                // If valid date was typed, keep normalized in standard ISO format
                LocalDate d = getDate();
                if (d != null) {
                    txtDate.setText(d.format(AppConfig.ISO_DATE_FORMATTER));
                }
            }
        });

        add(txtDate, BorderLayout.CENTER);
        add(btnPicker, BorderLayout.EAST);
    }

    /**
     * Parses the current text content into a {@link LocalDate}.
     * Supports ISO (yyyy-MM-dd), Indian format (dd-MM-yyyy), and slash-separated (dd/MM/yyyy).
     *
     * @return parsed {@link LocalDate}, or {@code null} if the field is blank or unparseable
     */
    public LocalDate getDate() {
        String val = txtDate.getText().trim();
        if (val.isEmpty()) return null;
        try {
            return LocalDate.parse(val, AppConfig.ISO_DATE_FORMATTER);
        } catch (Exception e1) {
            // Lenient fallbacks: try dd-MM-yyyy or dd/MM/yyyy
            try {
                return LocalDate.parse(val, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            } catch (Exception e2) {
                try {
                    return LocalDate.parse(val, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                } catch (Exception ignored) {
                    return null;
                }
            }
        }
    }

    /**
     * Sets the displayed date value in ISO format and notifies action listeners.
     *
     * @param date date value to display, or {@code null} to clear
     */
    public void setDate(LocalDate date) {
        if (date != null) {
            txtDate.setText(date.format(AppConfig.ISO_DATE_FORMATTER));
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
        } catch (Exception ex) {
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
        /** Year selection combo box. */
        private JComboBox<Integer> cmbYear;
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
            setPreferredSize(new Dimension(280, 270));

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

            JButton btnPrev = new JButton(NavIcons.chevronLeft(10, UiUtils.COLOR_TEXT_PRIMARY));
            btnPrev.setMargin(new Insets(2, 4, 2, 4));
            btnPrev.setFocusPainted(false);
            btnPrev.addActionListener(e -> {
                currentYearMonth = currentYearMonth.minusMonths(1);
                syncCombos();
                renderDays();
            });

            JButton btnNext = new JButton(NavIcons.chevronRight(10, UiUtils.COLOR_TEXT_PRIMARY));
            btnNext.setMargin(new Insets(2, 4, 2, 4));
            btnNext.setFocusPainted(false);
            btnNext.addActionListener(e -> {
                currentYearMonth = currentYearMonth.plusMonths(1);
                syncCombos();
                renderDays();
            });

            JPanel selectors = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
            selectors.setOpaque(false);

            cmbMonth = new JComboBox<>(MONTH_NAMES);
            cmbMonth.setFont(UiUtils.FONT_REGULAR);
            cmbMonth.setSelectedIndex(currentYearMonth.getMonthValue() - 1);
            cmbMonth.addActionListener(e -> {
                int m = cmbMonth.getSelectedIndex() + 1;
                currentYearMonth = YearMonth.of(currentYearMonth.getYear(), m);
                renderDays();
            });

            int currentYear = LocalDate.now().getYear();
            Integer[] years = new Integer[41];
            for (int i = 0; i <= 40; i++) {
                years[i] = (currentYear - 20) + i;
            }
            cmbYear = new JComboBox<>(years);
            cmbYear.setFont(UiUtils.FONT_REGULAR);
            cmbYear.setSelectedItem(currentYearMonth.getYear());
            cmbYear.addActionListener(e -> {
                Object yr = cmbYear.getSelectedItem();
                if (yr instanceof Integer y) {
                    currentYearMonth = YearMonth.of(y, currentYearMonth.getMonthValue());
                    renderDays();
                }
            });

            JButton btnClose = new JButton(NavIcons.cross(10, UiUtils.COLOR_TEXT_MUTED));
            btnClose.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            btnClose.setContentAreaFilled(false);
            btnClose.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnClose.addActionListener(e -> onClose.run());

            selectors.add(btnPrev);
            selectors.add(cmbMonth);
            selectors.add(cmbYear);
            selectors.add(btnNext);
            selectors.add(btnClose);

            headerPanel.add(selectors, BorderLayout.CENTER);
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
         * Synchronizes combo box selections with the active year and month.
         */
        private void syncCombos() {
            cmbMonth.setSelectedIndex(currentYearMonth.getMonthValue() - 1);
            cmbYear.setSelectedItem(currentYearMonth.getYear());
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
