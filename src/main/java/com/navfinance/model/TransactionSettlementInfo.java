package com.navfinance.model;

import com.navfinance.config.AppConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates computed settlement information for a transaction,
 * including total settled sum, remaining balance, links, and nullification status.
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public class TransactionSettlementInfo {

    private final Long transactionId;
    private final double totalAmount;
    private double settledAmount = 0.0;
    private SettlementStatus status = SettlementStatus.NONE;
    private final List<TransactionLink> links = new ArrayList<>();
    private boolean isCandidate = false; // true if Give or Return candidate

    public TransactionSettlementInfo(Long transactionId, double totalAmount) {
        this.transactionId = transactionId;
        this.totalAmount = totalAmount;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public double getSettledAmount() {
        return settledAmount;
    }

    public void setSettledAmount(double settledAmount) {
        this.settledAmount = settledAmount;
        recalculateStatus();
    }

    public double getRemainingAmount() {
        return Math.max(0.0, totalAmount - settledAmount);
    }

    public SettlementStatus getStatus() {
        return status;
    }

    public void setStatus(SettlementStatus status) {
        this.status = status;
    }

    public boolean isCandidate() {
        return isCandidate;
    }

    public void setCandidate(boolean candidate) {
        isCandidate = candidate;
        recalculateStatus();
    }

    public List<TransactionLink> getLinks() {
        return links;
    }

    public void addLink(TransactionLink link) {
        this.links.add(link);
        this.settledAmount += link.getAmount();
        recalculateStatus();
    }

    public void recalculateStatus() {
        if (!isCandidate && links.isEmpty() && settledAmount <= 0.001) {
            status = SettlementStatus.NONE;
            return;
        }

        if (settledAmount <= 0.001) {
            status = SettlementStatus.UNLINKED;
        } else if (settledAmount >= (totalAmount - 0.01)) {
            status = SettlementStatus.NULLIFIED;
        } else {
            status = SettlementStatus.PARTIAL;
        }
    }

    public double getSettledPercentage() {
        if (totalAmount <= 0) return 0;
        return Math.min(100.0, (settledAmount / totalAmount) * 100.0);
    }

    public boolean isNullified() {
        return status == SettlementStatus.NULLIFIED;
    }

    /**
     * Generates HTML tooltip describing linked transactions and settlement details.
     */
    public String getHtmlTooltip() {
        if (links.isEmpty()) {
            if (status == SettlementStatus.UNLINKED) {
                return "<html><div style='padding:4px;'><b>Status: Unlinked</b><br/>No counterpart transactions linked yet.<br/>Remaining open: "
                        + AppConfig.formatCurrency(getRemainingAmount()) + "</div></html>";
            }
            return null;
        }

        StringBuilder sb = new StringBuilder("<html><div style='padding:5px; font-family:sans-serif;'>");
        if (isNullified()) {
            sb.append("<b style='color:#059669;'>✓ Nullified (Fully Paid / Received)</b><br/>");
        } else {
            sb.append("<b style='color:#d97706;'>⏳ Partially Settled (").append(String.format("%.1f%%", getSettledPercentage())).append(")</b><br/>");
        }

        sb.append("<hr style='border:0; border-top:1px solid #cbd5e1; margin:4px 0;'/>");
        sb.append("<b>Linked Records (").append(links.size()).append("):</b><br/>");

        for (TransactionLink link : links) {
            sb.append("• ");
            if (link.getGiveTxId().equals(transactionId)) {
                // We are the Give transaction, display counterpart Return
                sb.append("Return #").append(link.getReturnTxId());
                if (link.getReturnTxDate() != null) sb.append(" (").append(link.getReturnTxDate()).append(")");
                if (link.getReturnPersonName() != null && !link.getReturnPersonName().isEmpty()) {
                    sb.append(" - ").append(link.getReturnPersonName());
                }
            } else {
                // We are the Return transaction, display counterpart Give
                sb.append("Give #").append(link.getGiveTxId());
                if (link.getGiveTxDate() != null) sb.append(" (").append(link.getGiveTxDate()).append(")");
                if (link.getGivePersonName() != null && !link.getGivePersonName().isEmpty()) {
                    sb.append(" - ").append(link.getGivePersonName());
                }
            }
            sb.append(": <b>").append(AppConfig.formatCurrency(link.getAmount())).append("</b>");
            if (link.getNotes() != null && !link.getNotes().isEmpty()) {
                sb.append(" [").append(link.getNotes()).append("]");
            }
            sb.append("<br/>");
        }

        sb.append("<hr style='border:0; border-top:1px solid #cbd5e1; margin:4px 0;'/>");
        sb.append("Settled: <b>").append(AppConfig.formatCurrency(settledAmount)).append("</b> / ")
          .append(AppConfig.formatCurrency(totalAmount)).append("<br/>");
        if (!isNullified()) {
            sb.append("Remaining Open: <b style='color:#dc2626;'>").append(AppConfig.formatCurrency(getRemainingAmount())).append("</b><br/>");
        }
        sb.append("</div></html>");
        return sb.toString();
    }
}
