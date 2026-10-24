package com.navfinance.ui.dialogs;

import com.navfinance.config.AppConfig;
import com.navfinance.dao.TransactionLinkDao.EligibleCandidate;
import com.navfinance.i18n.I18n;
import com.navfinance.model.Transaction;
import com.navfinance.model.TransactionLink;
import com.navfinance.model.TransactionType;
import com.navfinance.service.FinanceService;
import com.navfinance.ui.util.NavIcons;
import com.navfinance.ui.util.UiUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dedicated modal dialog enabling linking and settlement between
 * Give Transactions (Lent / Outflow) and Return Received Transactions (Inflow).
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public class TransactionLinkDialog extends JDialog {

    private final FinanceService financeService = FinanceService.getInstance();
    private final Transaction targetTx;
    private final boolean isGive;
    private final List<TransactionLink> initialLinks;
    private List<TransactionLink> resultLinks = new ArrayList<>();
    private boolean saved = false;

    private JTable candidateTable;
    private DefaultTableModel tableModel;
    private final List<CandidateRow> candidateRows = new ArrayList<>();

    private JLabel lblStatusBadge;
    private JLabel lblAllocatedSummary;

    public TransactionLinkDialog(Window owner, Transaction targetTx) {
        this(owner, targetTx, null);
    }

    public TransactionLinkDialog(Window owner, Transaction targetTx, List<TransactionLink> initialLinks) {
        super(owner, ModalityType.APPLICATION_MODAL);
        this.targetTx = targetTx;
        this.isGive = targetTx.getType() == TransactionType.EXPENSE;
        this.initialLinks = initialLinks;

        String txIdStr = targetTx.getId() != null ? "#" + targetTx.getId() : "New Draft";
        setTitle(String.format("Link %s [%s]", isGive ? "Give Transaction" : "Return Received Transaction", txIdStr));
        setSize(850, 560);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        initComponents();
        loadCandidates();
    }

    private void initComponents() {
        // TOP: Header banner with target transaction details
        JPanel topBanner = new JPanel(new BorderLayout(15, 0));
        topBanner.setBackground(Color.WHITE);
        topBanner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiUtils.COLOR_BORDER),
                BorderFactory.createEmptyBorder(14, 20, 14, 20)
        ));

        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 4, 2, 16);

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lblTarget = new JLabel(String.format("Transaction #%d [%s]",
                targetTx.getId(), isGive ? "Give / Loan Given" : "Return Received"));
        lblTarget.setFont(UiUtils.headerFont());
        lblTarget.setForeground(isGive ? UiUtils.COLOR_EXPENSE : UiUtils.COLOR_INCOME);
        infoPanel.add(lblTarget, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        String personStr = targetTx.getPersonName() != null ? targetTx.getPersonName() : "General";
        String dateStr = targetTx.getTxDate() != null ? targetTx.getTxDate().format(AppConfig.DATE_FORMATTER) : "-";
        JLabel lblDetails = new JLabel(String.format("Person: %s  |  Date: %s  |  Category: %s",
                personStr, dateStr, targetTx.getCategory()));
        lblDetails.setFont(UiUtils.regularFont());
        lblDetails.setForeground(UiUtils.COLOR_TEXT_MUTED);
        infoPanel.add(lblDetails, gbc);

        topBanner.add(infoPanel, BorderLayout.CENTER);

        // Right side badge and amount
        JPanel rightBadgePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightBadgePanel.setOpaque(false);

        JPanel amtBox = new JPanel(new GridBagLayout());
        amtBox.setOpaque(false);
        GridBagConstraints gbcAmt = new GridBagConstraints();
        gbcAmt.anchor = GridBagConstraints.EAST;
        gbcAmt.gridx = 0; gbcAmt.gridy = 0;

        JLabel lblAmt = new JLabel(AppConfig.formatCurrency(targetTx.getAmount()));
        lblAmt.setFont(UiUtils.headerFont());
        lblAmt.setForeground(isGive ? UiUtils.COLOR_EXPENSE : UiUtils.COLOR_INCOME);
        amtBox.add(lblAmt, gbcAmt);

        gbcAmt.gridy = 1;
        lblStatusBadge = new JLabel("Evaluating Status...");
        lblStatusBadge.setFont(UiUtils.boldFont());
        amtBox.add(lblStatusBadge, gbcAmt);

        rightBadgePanel.add(amtBox);
        topBanner.add(rightBadgePanel, BorderLayout.EAST);
        add(topBanner, BorderLayout.NORTH);

        // CENTER: Candidate counter-parties table
        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));

        JPanel tableHeaderPanel = new JPanel(new BorderLayout());
        tableHeaderPanel.setOpaque(false);

        String counterpartTitle = isGive ?
                "Select Return Received Transactions to Link (Udhaar Wapasi):" :
                "Select Give Transactions to Link (Udhaar Diya):";
        JLabel lblSub = new JLabel(counterpartTitle);
        lblSub.setFont(UiUtils.boldFont());
        tableHeaderPanel.add(lblSub, BorderLayout.WEST);

        JPanel autoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        autoPanel.setOpaque(false);
        JButton btnAutoAllocate = UiUtils.createPrimaryButton("Auto-Allocate", NavIcons.refresh(12, Color.WHITE));
        btnAutoAllocate.addActionListener(e -> autoAllocate());
        autoPanel.add(btnAutoAllocate);
        tableHeaderPanel.add(autoPanel, BorderLayout.EAST);

        centerPanel.add(tableHeaderPanel, BorderLayout.NORTH);

        String[] cols = {"Select", "Tx ID", "Date", "Person", "Category / Note", "Total", "Settled", "Remaining", "Link Amount"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public Class<?> getColumnClass(int col) {
                if (col == 0) return Boolean.class;
                return Object.class;
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 0 || col == 8; // Checkbox and Link Amount are editable
            }
        };

        tableModel.addTableModelListener(e -> {
            if (e.getColumn() == 0 || e.getColumn() == 8) {
                int row = e.getFirstRow();
                if (row >= 0 && row < candidateRows.size()) {
                    onRowModified(row);
                }
            }
        });

        candidateTable = new JTable(tableModel);
        candidateTable.setRowHeight(30);
        candidateTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        candidateTable.getTableHeader().setFont(UiUtils.boldFont());
        candidateTable.setFont(UiUtils.regularFont());
        candidateTable.setShowGrid(false);

        // Column widths
        candidateTable.getColumnModel().getColumn(0).setPreferredWidth(55);
        candidateTable.getColumnModel().getColumn(1).setPreferredWidth(60);
        candidateTable.getColumnModel().getColumn(2).setPreferredWidth(85);
        candidateTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        candidateTable.getColumnModel().getColumn(4).setPreferredWidth(180);
        candidateTable.getColumnModel().getColumn(5).setPreferredWidth(80);
        candidateTable.getColumnModel().getColumn(6).setPreferredWidth(80);
        candidateTable.getColumnModel().getColumn(7).setPreferredWidth(80);
        candidateTable.getColumnModel().getColumn(8).setPreferredWidth(95);

        // Right alignment for numeric columns
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        for (int i = 5; i <= 8; i++) {
            candidateTable.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
        }

        // Color the Link Amount column distinctly
        candidateTable.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                l.setHorizontalAlignment(RIGHT);
                l.setFont(UiUtils.boldFont());
                l.setForeground(UiUtils.COLOR_PRIMARY);
                return l;
            }
        });

        JScrollPane scrollPane = new JScrollPane(candidateTable);
        scrollPane.setBorder(UiUtils.createCardBorder());
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // BOTTOM: Summary and Save buttons
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(new Color(248, 250, 252));
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiUtils.COLOR_BORDER),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        lblAllocatedSummary = new JLabel("Total Allocated: ₹0.00");
        lblAllocatedSummary.setFont(UiUtils.boldFont());
        bottomBar.add(lblAllocatedSummary, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnCancel = UiUtils.createSecondaryButton(I18n.get("btn.cancel"), NavIcons.cross(13, UiUtils.COLOR_TEXT_PRIMARY));
        btnCancel.addActionListener(e -> dispose());

        JButton btnSave = UiUtils.createPrimaryButton("Save Links", NavIcons.check(13, Color.WHITE));
        btnSave.addActionListener(e -> onSaveLinks());

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);
        bottomBar.add(btnPanel, BorderLayout.EAST);

        add(bottomBar, BorderLayout.SOUTH);
    }

    private void loadCandidates() {
        tableModel.setRowCount(0);
        candidateRows.clear();

        // 1. Fetch existing links for this transaction
        List<TransactionLink> existingLinks;
        if (initialLinks != null) {
            existingLinks = initialLinks;
        } else if (targetTx.getId() != null) {
            existingLinks = financeService.getTransactionLinks(targetTx.getId());
        } else {
            existingLinks = new ArrayList<>();
        }

        Map<Long, TransactionLink> existingLinkMap = new HashMap<>();
        for (TransactionLink l : existingLinks) {
            Long counterpartId = isGive ? l.getReturnTxId() : l.getGiveTxId();
            if (counterpartId != null) {
                existingLinkMap.put(counterpartId, l);
            }
        }

        // 2. Fetch candidates from database
        Long personId = targetTx.getPersonId();
        List<EligibleCandidate> candidates = isGive ?
                financeService.getEligibleReturnTransactions(personId, targetTx.getId()) :
                financeService.getEligibleGiveTransactions(personId, targetTx.getId());

        for (EligibleCandidate c : candidates) {
            Transaction tx = c.getTransaction();
            TransactionLink existing = existingLinkMap.get(tx.getId());
            boolean selected = existing != null;
            double linkAmt = existing != null ? existing.getAmount() : 0.0;

            // When previously linked, the candidate's available remaining amount is:
            // c.getRemainingAmount() + (existing != null ? existing.getAmount() : 0.0)
            double maxAvailable = c.getRemainingAmount() + (existing != null ? existing.getAmount() : 0.0);

            CandidateRow rowData = new CandidateRow(tx, maxAvailable, selected, linkAmt);
            candidateRows.add(rowData);

            tableModel.addRow(new Object[]{
                    selected,
                    tx.getId(),
                    tx.getTxDate() != null ? tx.getTxDate().format(AppConfig.DATE_FORMATTER) : "-",
                    tx.getPersonName() != null ? tx.getPersonName() : "-",
                    (tx.getCategory() != null ? tx.getCategory() : "") + (tx.getNotes() != null && !tx.getNotes().isEmpty() ? " - " + tx.getNotes() : ""),
                    AppConfig.formatCurrency(tx.getAmount()),
                    AppConfig.formatCurrency(c.getSettledAmount()),
                    AppConfig.formatCurrency(maxAvailable),
                    String.format("%.2f", linkAmt)
            });
        }

        recalculateAllocation();
    }

    private void onRowModified(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= candidateRows.size()) return;
        CandidateRow row = candidateRows.get(rowIndex);

        Boolean selected = (Boolean) tableModel.getValueAt(rowIndex, 0);
        Object amtObj = tableModel.getValueAt(rowIndex, 8);

        double amt = 0.0;
        try {
            amt = Double.parseDouble(String.valueOf(amtObj).replace(",", "").trim());
        } catch (Exception ignored) {}

        if (Boolean.TRUE.equals(selected) && amt <= 0.001) {
            // Auto fill remaining if amount was zero
            double needed = Math.max(0.0, targetTx.getAmount() - calculateAllocatedExcluding(rowIndex));
            amt = Math.min(row.maxAvailable, needed);
            tableModel.setValueAt(String.format("%.2f", amt), rowIndex, 8);
        } else if (Boolean.FALSE.equals(selected)) {
            amt = 0.0;
            tableModel.setValueAt("0.00", rowIndex, 8);
        }

        row.selected = Boolean.TRUE.equals(selected);
        row.linkAmount = amt;
        recalculateAllocation();
    }

    private double calculateAllocatedExcluding(int excludeRowIndex) {
        double sum = 0.0;
        for (int i = 0; i < candidateRows.size(); i++) {
            if (i != excludeRowIndex && candidateRows.get(i).selected) {
                sum += candidateRows.get(i).linkAmount;
            }
        }
        return sum;
    }

    private void autoAllocate() {
        double remainingToAllocate = targetTx.getAmount();

        for (int i = 0; i < candidateRows.size(); i++) {
            CandidateRow row = candidateRows.get(i);
            if (remainingToAllocate > 0.001 && row.maxAvailable > 0.001) {
                double allocate = Math.min(row.maxAvailable, remainingToAllocate);
                row.selected = true;
                row.linkAmount = allocate;
                remainingToAllocate -= allocate;

                tableModel.setValueAt(true, i, 0);
                tableModel.setValueAt(String.format("%.2f", allocate), i, 8);
            } else {
                row.selected = false;
                row.linkAmount = 0.0;
                tableModel.setValueAt(false, i, 0);
                tableModel.setValueAt("0.00", i, 8);
            }
        }

        recalculateAllocation();
    }

    private void recalculateAllocation() {
        double totalAllocated = 0.0;
        for (CandidateRow r : candidateRows) {
            if (r.selected) {
                totalAllocated += r.linkAmount;
            }
        }

        double remaining = Math.max(0.0, targetTx.getAmount() - totalAllocated);

        lblAllocatedSummary.setText(String.format("Allocated: %s of %s  (Remaining: %s)",
                AppConfig.formatCurrency(totalAllocated),
                AppConfig.formatCurrency(targetTx.getAmount()),
                AppConfig.formatCurrency(remaining)));

        if (totalAllocated >= (targetTx.getAmount() - 0.01)) {
            lblStatusBadge.setText("✓ NULLIFIED (100% Fully Settled)");
            lblStatusBadge.setForeground(new Color(109, 40, 217));
        } else if (totalAllocated > 0.001) {
            lblStatusBadge.setText(String.format("⏳ PARTIAL (%s Settled)", AppConfig.formatCurrency(totalAllocated)));
            lblStatusBadge.setForeground(UiUtils.COLOR_AMBER);
        } else {
            lblStatusBadge.setText("⚠ UNLINKED (0% Settled)");
            lblStatusBadge.setForeground(UiUtils.COLOR_TEXT_MUTED);
        }
    }

    private void onSaveLinks() {
        // Collect links
        List<TransactionLink> linksToSave = new ArrayList<>();
        double total = 0.0;

        for (CandidateRow row : candidateRows) {
            if (row.selected && row.linkAmount > 0.001) {
                total += row.linkAmount;
                Long giveId = isGive ? targetTx.getId() : row.tx.getId();
                Long returnId = isGive ? row.tx.getId() : targetTx.getId();
                linksToSave.add(new TransactionLink(giveId, returnId, row.linkAmount, "Settlement link"));
            }
        }

        if (total > targetTx.getAmount() + 0.01) {
            JOptionPane.showMessageDialog(this,
                    "Total allocated amount exceeds the transaction principal of " + AppConfig.formatCurrency(targetTx.getAmount()),
                    "Allocation Exceeded", JOptionPane.WARNING_MESSAGE);
            return;
        }

        this.resultLinks = linksToSave;

        // If target transaction is already persisted in DB, save links immediately
        if (targetTx.getId() != null) {
            financeService.deleteLinksForTransaction(targetTx.getId());
            for (TransactionLink l : linksToSave) {
                financeService.linkTransactions(l.getGiveTxId(), l.getReturnTxId(), l.getAmount(), l.getNotes());
            }
        }

        saved = true;
        dispose();
    }

    public boolean isSaved() {
        return saved;
    }

    public List<TransactionLink> getResultLinks() {
        return resultLinks;
    }

    private static class CandidateRow {
        final Transaction tx;
        final double maxAvailable;
        boolean selected;
        double linkAmount;

        CandidateRow(Transaction tx, double maxAvailable, boolean selected, double linkAmount) {
            this.tx = tx;
            this.maxAvailable = maxAvailable;
            this.selected = selected;
            this.linkAmount = linkAmount;
        }
    }
}
