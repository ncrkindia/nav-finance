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
 * Loans &amp; Dues directory panel providing comprehensive tracking of informal peer-to-peer
 * loans, borrowings, and outstanding counterparty dues.
 * <p>
 * Key Features:
 * </p>
 * <ul>
 *   <li>Top-level KPI metrics summarizing Total Lent (Money Given), Total Borrowed (Money Taken), and Net Balance position.</li>
 *   <li>Person-wise breakdown table displaying Total Lent, Total Returned, Total Borrowed, Total Repaid,
 *       Net Position (+/-), and Status badge ("Owes You", "You Owe", "Settled").</li>
 *   <li>Quick action buttons to register new contacts ({@link PersonDialog}) and record loans or repayments directly ({@link TransactionDialog}).</li>
 * </ul>
 */
public class LoansDuesPanel extends JPanel implements LanguageChangeListener {

    /** Business service managing transaction ledgers and Person master catalogs. */
    private final FinanceService financeService = FinanceService.getInstance();

    /** KPI card displaying aggregated money lent to others. */
    private KpiCard kpiLent;

    /** KPI card displaying aggregated money borrowed from others. */
    private KpiCard kpiBorrowed;

    /** KPI card displaying net balance position across all personal loans. */
    private KpiCard kpiNet;

    /** Swing table displaying person-wise loan balances and settlement statuses. */
    private JTable tblLoans;

    /** Table model backing the loans and dues grid. */
    private DefaultTableModel tableModel;

    /**
     * Constructs a new {@code LoansDuesPanel}, setting up header controls,
     * KPI indicators, ledger table formatting, and loading initial loan aggregates.
     */
    public LoansDuesPanel() {
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

        JLabel lblTitle = new JLabel(I18n.get("loans.title"));
        lblTitle.setFont(UiUtils.titleFont());
        lblTitle.setForeground(new Color(15, 23, 42));
        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnGroup.setOpaque(false);

        JButton btnAddPerson = UiUtils.createSecondaryButton(I18n.get("loans.btn.addContact"), NavIcons.person(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnAddPerson.setFont(UiUtils.boldFont());
        btnAddPerson.addActionListener(e -> onAddPerson());

        JButton btnLendMoney = UiUtils.createDangerButton(I18n.get("loans.btn.giveLoan"), NavIcons.minus(13, Color.WHITE));
        btnLendMoney.setFont(UiUtils.boldFont());
        btnLendMoney.addActionListener(e -> onRecordLoan(true));

        JButton btnBorrowMoney = UiUtils.createSuccessButton(I18n.get("loans.btn.takeLoan"), NavIcons.plus(13, Color.WHITE));
        btnBorrowMoney.setFont(UiUtils.boldFont());
        btnBorrowMoney.addActionListener(e -> onRecordLoan(false));

        JButton btnTrack = UiUtils.createPrimaryButton("Track History & Graph 📊", NavIcons.dashboard(13, Color.WHITE));
        btnTrack.setFont(UiUtils.boldFont());
        btnTrack.addActionListener(e -> {
            int row = tblLoans.getSelectedRow();
            if (row < 0) {
                UiUtils.showError(this, "Select a person from the ledger to track.");
                return;
            }
            Long personId = (Long) tblLoans.getValueAt(row, 0);
            com.navfinance.ui.MainFrame.getInstance().switchToEntityTracker("PERSON", personId, null, "MONTHS");
        });

        btnGroup.add(btnAddPerson);
        btnGroup.add(btnLendMoney);
        btnGroup.add(btnBorrowMoney);
        btnGroup.add(btnTrack);

        // Responsive header prevents collision between title and buttons
        com.navfinance.ui.components.ResponsiveBarPanel responsiveHeader =
                new com.navfinance.ui.components.ResponsiveBarPanel(lblTitle, btnGroup, 14, 8, true);
        topPanel.add(responsiveHeader, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);

        // CENTER: KPI Cards + Ledger Table
        JPanel centerPanel = new JPanel(new BorderLayout(0, 14));
        centerPanel.setOpaque(false);

        // Summary KPI Row
        JPanel kpiRow = new JPanel(new GridLayout(1, 3, 14, 0));
        kpiRow.setOpaque(false);

        kpiLent = new KpiCard(I18n.get("loans.kpi.totalLent"), "₹ 0.00", "Money Lent to Others", UiUtils.COLOR_AMBER, NavIcons.minus(22, UiUtils.COLOR_AMBER));
        kpiBorrowed = new KpiCard(I18n.get("loans.kpi.totalBorrowed"), "₹ 0.00", "Money Borrowed from Others", UiUtils.COLOR_PRIMARY, NavIcons.plus(22, UiUtils.COLOR_PRIMARY));
        kpiNet = new KpiCard(I18n.get("loans.kpi.netOutstanding"), "₹ 0.00", "Net Balance", UiUtils.COLOR_INCOME, NavIcons.loan(22, UiUtils.COLOR_INCOME));

        kpiRow.add(kpiLent);
        kpiRow.add(kpiBorrowed);
        kpiRow.add(kpiNet);
        centerPanel.add(kpiRow, BorderLayout.NORTH);

        // Ledger Table
        String[] cols = {
                I18n.get("loans.table.personId"),
                I18n.get("loans.table.person"),
                I18n.get("loans.table.relation"),
                I18n.get("loans.table.contact"),
                I18n.get("loans.table.lent"),
                I18n.get("loans.table.returned"),
                I18n.get("loans.table.taken"),
                I18n.get("loans.table.repaid"),
                I18n.get("loans.table.netBalance"),
                I18n.get("loans.table.status")
        };

        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        tblLoans = new JTable(tableModel);
        tblLoans.setRowHeight(32);
        tblLoans.getTableHeader().setFont(UiUtils.boldFont());
        tblLoans.setFont(UiUtils.regularFont());
        tblLoans.setShowGrid(false);
        tblLoans.setIntercellSpacing(new Dimension(0, 0));

        // Format numeric columns with right alignment
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        for (int i = 4; i <= 8; i++) {
            tblLoans.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
        }

        // Color net balance column
        tblLoans.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(RIGHT);
                l.setFont(UiUtils.boldFont());
                String str = String.valueOf(value);
                if (str.startsWith("+")) {
                    l.setForeground(UiUtils.COLOR_INCOME); // Money owed to user
                } else if (str.startsWith("-")) {
                    l.setForeground(UiUtils.COLOR_EXPENSE); // User owes money
                } else {
                    l.setForeground(UiUtils.COLOR_TEXT_MUTED);
                }
                return l;
            }
        });

        // Format Status badge column
        tblLoans.getColumnModel().getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(CENTER);
                l.setFont(UiUtils.boldFont());
                String str = String.valueOf(value);
                if (str.equals(I18n.get("loans.status.simple.owesYou")) || str.contains("Owes")) {
                    l.setForeground(UiUtils.COLOR_INCOME);
                    l.setIcon(NavIcons.statusDot(8, UiUtils.COLOR_INCOME));
                } else if (str.equals(I18n.get("loans.status.simple.youOwe")) || str.contains("Owe")) {
                    l.setForeground(UiUtils.COLOR_EXPENSE);
                    l.setIcon(NavIcons.statusDot(8, UiUtils.COLOR_EXPENSE));
                } else {
                    l.setForeground(UiUtils.COLOR_TEXT_MUTED);
                    l.setIcon(NavIcons.check(10, UiUtils.COLOR_TEXT_MUTED));
                }
                return l;
            }
        });

        JScrollPane sp = new JScrollPane(tblLoans);
        sp.setBorder(UiUtils.createCardBorder());
        centerPanel.add(sp, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);
    }

    /**
     * Re-queries all loan summaries across registered persons from {@link FinanceService},
     * updates table rows, and refreshes the summary KPI metric cards.
     */
    public final void refreshData() {
        List<FinanceService.PersonLoanSummary> summaries = financeService.getPersonLoanSummaries();
        tableModel.setRowCount(0);

        double sumLent = 0;
        double sumBorrowed = 0;

        for (FinanceService.PersonLoanSummary s : summaries) {
            Person p = s.getPerson();
            double net = s.getNetBalance();
            sumLent += s.getTotalGiven();
            sumBorrowed += s.getTotalTaken();

            String status;
            String netStr;
            if (net > 0) {
                status = I18n.get("loans.status.simple.owesYou");
                netStr = "+ " + AppConfig.formatCurrency(net);
            } else if (net < 0) {
                status = I18n.get("loans.status.simple.youOwe");
                netStr = "- " + AppConfig.formatCurrency(Math.abs(net));
            } else {
                status = I18n.get("loans.status.simple.settled");
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

        kpiLent.setValue(AppConfig.formatCurrency(sumLent));
        kpiBorrowed.setValue(AppConfig.formatCurrency(sumBorrowed));

        double netAll = sumLent - sumBorrowed;
        kpiNet.setValue(AppConfig.formatCurrency(netAll));
        kpiNet.setSubtitle(netAll >= 0 ? I18n.get("loans.kpi.netOutstanding") : I18n.get("loans.kpi.totalRepaid"));
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
     * @param isLending {@code true} to record money lent (Expense), {@code false} to record money borrowed or returned (Income)
     */
    private void onRecordLoan(boolean isLending) {
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        Transaction sample = new Transaction();
        sample.setTxDate(LocalDate.now());

        if (isLending) {
            sample.setType(TransactionType.EXPENSE);
            sample.setCategory("Loan Given");
        } else {
            sample.setType(TransactionType.INCOME);
            sample.setCategory("Loan");
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
