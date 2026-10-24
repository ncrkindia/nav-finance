package com.navfinance.ui.panels;

import com.navfinance.config.AppConfig;
import com.navfinance.i18n.AppLanguage;
import com.navfinance.i18n.I18n;
import com.navfinance.i18n.LanguageChangeListener;
import com.navfinance.model.Person;
import com.navfinance.model.Transaction;
import com.navfinance.model.TransactionType;
import com.navfinance.service.FinanceService;
import com.navfinance.ui.components.KpiCard;
import com.navfinance.ui.dialogs.PersonDialog;
import com.navfinance.ui.dialogs.TransactionDialog;
import com.navfinance.ui.util.NavIcons;
import com.navfinance.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.List;

/**
 * Specialized ledger panel for tracking personal loans, debts, and informal credit (Udhaar).
 * <p>
 * This dashboard provides:
 * <ul>
 *   <li>Top-level KPI metrics summarizing Total Udhar Given, Total Udhar Taken, and Net Receivable/Payable position.</li>
 *   <li>Person-wise breakdown table displaying Total Given, Total Returned, Total Taken, Total Repaid,
 *       Net Position (+/-), and Status badge ("Owes You", "You Owe", "Settled").</li>
 *   <li>Convenient shortcuts to register new counterparties ({@link PersonDialog}) and record loan
 *       disbursements or repayments directly ({@link TransactionDialog}).</li>
 * </ul>
 */
public class UdharTrackerPanel extends JPanel implements LanguageChangeListener {

    /** Business service managing transaction ledgers and Person master catalogs. */
    private final FinanceService financeService = FinanceService.getInstance();

    /** KPI card displaying aggregated Udhar Given (money lent). */
    private KpiCard kpiGiven;

    /** KPI card displaying aggregated Udhar Taken (money borrowed). */
    private KpiCard kpiTaken;

    /** KPI card displaying net balance position across all personal loans. */
    private KpiCard kpiNet;

    /** Swing table displaying person-wise Udhaar balances and settlement statuses. */
    private JTable tblUdhar;

    /** Table model backing the Udhaar ledger grid. */
    private DefaultTableModel tableModel;

    /**
     * Constructs a new {@code UdharTrackerPanel}, setting up header controls,
     * KPI indicators, ledger table formatting, and loading initial loan aggregates.
     */
    public UdharTrackerPanel() {
        setLayout(new BorderLayout(0, 14));
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        initComponents();
        I18n.addListener(this);
        refreshData();
    }

    /**
     * Initializes UI layout, action buttons, KPI cards, table models, and custom cell renderers.
     */
    private void initComponents() {
        // TOP: Header & Action Buttons
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel lblTitle = new JLabel(I18n.get("udhar.title"));
        lblTitle.setFont(UiUtils.titleFont());
        lblTitle.setForeground(new Color(15, 23, 42));
        topPanel.add(lblTitle, BorderLayout.WEST);

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnGroup.setOpaque(false);

        JButton btnAddPerson = UiUtils.createSecondaryButton(I18n.get("udhar.btn.addContact"), NavIcons.person(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnAddPerson.addActionListener(e -> onAddPerson());

        JButton btnGiveUdhar = UiUtils.createDangerButton(I18n.get("udhar.btn.giveLoan"), NavIcons.minus(13, Color.WHITE));
        btnGiveUdhar.addActionListener(e -> onRecordUdhar(true));

        JButton btnTakeUdhar = UiUtils.createSuccessButton(I18n.get("udhar.btn.takeLoan"), NavIcons.plus(13, Color.WHITE));
        btnTakeUdhar.addActionListener(e -> onRecordUdhar(false));

        btnGroup.add(btnAddPerson);
        btnGroup.add(btnGiveUdhar);
        btnGroup.add(btnTakeUdhar);
        topPanel.add(btnGroup, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // CENTER: KPI Cards + Ledger Table
        JPanel centerPanel = new JPanel(new BorderLayout(0, 14));
        centerPanel.setOpaque(false);

        // Summary KPI Row
        JPanel kpiRow = new JPanel(new GridLayout(1, 3, 14, 0));
        kpiRow.setOpaque(false);

        kpiGiven = new KpiCard(I18n.get("udhar.kpi.totalLent"), "₹ 0.00", "Money Lent to Others", UiUtils.COLOR_AMBER, NavIcons.minus(22, UiUtils.COLOR_AMBER));
        kpiTaken = new KpiCard(I18n.get("udhar.kpi.totalBorrowed"), "₹ 0.00", "Money Borrowed from Others", UiUtils.COLOR_PRIMARY, NavIcons.plus(22, UiUtils.COLOR_PRIMARY));
        kpiNet = new KpiCard(I18n.get("udhar.kpi.netOutstanding"), "₹ 0.00", "Net Balance", UiUtils.COLOR_INCOME, NavIcons.udhar(22, UiUtils.COLOR_INCOME));

        kpiRow.add(kpiGiven);
        kpiRow.add(kpiTaken);
        kpiRow.add(kpiNet);
        centerPanel.add(kpiRow, BorderLayout.NORTH);

        // Person-wise Udhaar Ledger Table
        String[] cols = {
                I18n.get("udhar.table.personId"),
                I18n.get("udhar.table.person"),
                I18n.get("udhar.table.relation"),
                I18n.get("udhar.table.contact"),
                I18n.get("udhar.table.lent"),
                I18n.get("udhar.table.returned"),
                I18n.get("udhar.table.taken"),
                I18n.get("udhar.table.repaid"),
                I18n.get("udhar.table.netBalance"),
                I18n.get("udhar.table.status")
        };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        tblUdhar = new JTable(tableModel);
        tblUdhar.setRowHeight(32);
        tblUdhar.getTableHeader().setFont(UiUtils.boldFont());
        tblUdhar.setFont(UiUtils.regularFont());
        tblUdhar.setShowGrid(false);
        tblUdhar.setIntercellSpacing(new Dimension(0, 0));

        // Format Net Position column (Col 8: Green if owes you, Red if you owe)
        tblUdhar.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(RIGHT);
                setFont(UiUtils.FONT_BOLD);
                String str = String.valueOf(value);
                if (str.startsWith("+")) {
                    setForeground(UiUtils.COLOR_INCOME); // Owes you
                } else if (str.startsWith("-")) {
                    setForeground(UiUtils.COLOR_EXPENSE); // You owe
                } else {
                    setForeground(UiUtils.COLOR_TEXT_MUTED);
                }
                return c;
            }
        });

        // Format Status column with vector status dot (Col 9: Status badge)
        tblUdhar.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(CENTER);
                l.setFont(UiUtils.boldFont());
                String str = String.valueOf(value);
                Color statusColor;
                if (str.equals(I18n.get("udhar.status.simple.owesYou")) || str.contains("Owes")) {
                    statusColor = UiUtils.COLOR_INCOME;
                } else if (str.equals(I18n.get("udhar.status.simple.youOwe")) || str.contains("Owe")) {
                    statusColor = UiUtils.COLOR_EXPENSE;
                } else {
                    statusColor = UiUtils.COLOR_TEXT_MUTED;
                }
                l.setForeground(statusColor);
                l.setIcon(NavIcons.statusDot(8, statusColor));
                return l;
            }
        });

        JScrollPane sp = new JScrollPane(tblUdhar);
        sp.setBorder(UiUtils.createCardBorder());
        centerPanel.add(sp, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);
    }

    /**
     * Re-queries all Udhar summaries across registered persons from {@link FinanceService},
     * updates table rows, and refreshes the summary KPI metric cards.
     * Declared final to safeguard constructor invocation.
     */
    public final void refreshData() {
        List<FinanceService.PersonUdharSummary> summaries = financeService.getPersonUdharSummaries();
        tableModel.setRowCount(0);

        double sumGiven = 0;
        double sumTaken = 0;

        for (FinanceService.PersonUdharSummary s : summaries) {
            Person p = s.getPerson();
            double net = s.getNetBalance();
            sumGiven += s.getTotalGiven();
            sumTaken += s.getTotalTaken();

            String status;
            String netStr;
            if (net > 0) {
                status = I18n.get("udhar.status.simple.owesYou");
                netStr = "+ " + AppConfig.formatCurrency(net);
            } else if (net < 0) {
                status = I18n.get("udhar.status.simple.youOwe");
                netStr = "- " + AppConfig.formatCurrency(Math.abs(net));
            } else {
                status = I18n.get("udhar.status.simple.settled");
                netStr = AppConfig.formatCurrency(0);
            }

            tableModel.addRow(new Object[]{
                    p.getId(),
                    p.getName(),
                    p.getRelation() != null ? p.getRelation() : "-",
                    p.getContact() != null ? p.getContact() : "-",
                    AppConfig.formatCurrency(s.getTotalGiven()),
                    AppConfig.formatCurrency(s.getTotalReturned()),
                    AppConfig.formatCurrency(s.getTotalTaken()),
                    AppConfig.formatCurrency(s.getTotalRepaid()),
                    netStr,
                    status
            });
        }

        kpiGiven.setValue(AppConfig.formatCurrency(sumGiven));
        kpiTaken.setValue(AppConfig.formatCurrency(sumTaken));

        double netAll = sumGiven - sumTaken;
        kpiNet.setValue(AppConfig.formatCurrency(netAll));
        kpiNet.setSubtitle(netAll >= 0 ? I18n.get("udhar.kpi.netOutstanding") : I18n.get("udhar.kpi.totalRepaid"));
    }

    /**
     * Opens the modal dialog to register a new counterparty Person in the master catalog.
     */
    private void onAddPerson() {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        PersonDialog dialog = new PersonDialog(frame, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshData();
        }
    }

    /**
     * Opens the transaction creation dialog pre-configured for personal loan lending or repayment.
     *
     * @param isGive {@code true} to record Udhar Given (Expense), {@code false} to record Udhar Taken or Returned (Income)
     */
    private void onRecordUdhar(boolean isGive) {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        Transaction sample = new Transaction();
        sample.setTxDate(LocalDate.now());

        if (isGive) {
            sample.setType(TransactionType.EXPENSE);
            sample.setCategory("Udhar Given");
        } else {
            sample.setType(TransactionType.INCOME);
            sample.setCategory("Udhaar");
        }

        TransactionDialog dialog = new TransactionDialog(frame, sample, sample.getType());
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshData();
        }
    }

    /**
     * Responds to language updates by rebuilding localized visual labels, columns, and metric texts.
     *
     * @param newLanguage the newly selected system language
     */
    @Override
    public void onLanguageChanged(AppLanguage newLanguage) {
        removeAll();
        initComponents();
        revalidate();
        repaint();
        refreshData();
    }
}
