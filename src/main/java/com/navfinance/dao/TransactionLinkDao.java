package com.navfinance.dao;

import com.navfinance.database.DatabaseManager;
import com.navfinance.model.Transaction;
import com.navfinance.model.TransactionLink;
import com.navfinance.model.TransactionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object managing operations for transaction linking
 * between Give Transactions (Lent) and Return Received Transactions (Repaid).
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public class TransactionLinkDao {

    /**
     * Inserts a new link between a Give transaction and a Return transaction.
     *
     * @param link Link entity to save
     * @return Saved link with database ID
     */
    public TransactionLink insert(TransactionLink link) {
        String sql = """
            INSERT INTO transaction_links (give_tx_id, return_tx_id, amount, notes, created_at)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            LocalDateTime now = LocalDateTime.now();
            ps.setLong(1, link.getGiveTxId());
            ps.setLong(2, link.getReturnTxId());
            ps.setDouble(3, link.getAmount());
            ps.setString(4, link.getNotes());
            ps.setTimestamp(5, Timestamp.valueOf(now));

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    link.setId(rs.getLong(1));
                    link.setCreatedAt(now);
                }
            }
            return link;
        } catch (SQLException e) {
            System.err.println("Failed to insert transaction link: " + e.getMessage());
            return null;
        }
    }

    /**
     * Deletes a link by its primary key ID.
     *
     * @param linkId ID of the link to delete
     * @return {@code true} if deleted, {@code false} otherwise
     */
    public boolean delete(Long linkId) {
        String sql = "DELETE FROM transaction_links WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, linkId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to delete transaction link: " + e.getMessage());
            return false;
        }
    }

    /**
     * Removes all links associated with a given transaction (either as Give or as Return).
     *
     * @param txId Transaction primary key ID
     * @return Number of links removed
     */
    public int deleteByTransactionId(Long txId) {
        String sql = "DELETE FROM transaction_links WHERE give_tx_id = ? OR return_tx_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, txId);
            ps.setLong(2, txId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to delete transaction links for tx: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Retrieves all links associated with a given transaction, populating both sides' details.
     *
     * @param txId Target transaction ID
     * @return List of {@link TransactionLink} entities
     */
    public List<TransactionLink> findByTransactionId(Long txId) {
        String sql = """
            SELECT l.id, l.give_tx_id, l.return_tx_id, l.amount, l.notes, l.created_at,
                   g.tx_date AS give_date, g.amount AS give_amount, g.notes AS give_notes, gp.name AS give_person,
                   r.tx_date AS return_date, r.amount AS return_amount, r.notes AS return_notes, rp.name AS return_person
            FROM transaction_links l
            JOIN transactions g ON l.give_tx_id = g.id
            LEFT JOIN persons gp ON g.person_id = gp.id
            JOIN transactions r ON l.return_tx_id = r.id
            LEFT JOIN persons rp ON r.person_id = rp.id
            WHERE l.give_tx_id = ? OR l.return_tx_id = ?
            ORDER BY l.created_at DESC
        """;

        List<TransactionLink> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, txId);
            ps.setLong(2, txId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToLink(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch links for transaction " + txId + ": " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves all transaction links in the database along with associated transaction summaries.
     * Used for batch in-memory calculation of settlement statuses across large views.
     *
     * @return List of all links
     */
    public List<TransactionLink> findAllLinks() {
        String sql = """
            SELECT l.id, l.give_tx_id, l.return_tx_id, l.amount, l.notes, l.created_at,
                   g.tx_date AS give_date, g.amount AS give_amount, g.notes AS give_notes, gp.name AS give_person,
                   r.tx_date AS return_date, r.amount AS return_amount, r.notes AS return_notes, rp.name AS return_person
            FROM transaction_links l
            JOIN transactions g ON l.give_tx_id = g.id
            LEFT JOIN persons gp ON g.person_id = gp.id
            JOIN transactions r ON l.return_tx_id = r.id
            LEFT JOIN persons rp ON r.person_id = rp.id
            ORDER BY l.created_at DESC
        """;

        List<TransactionLink> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToLink(rs));
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch all links: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves eligible open Give Transactions (Expense / Lent) for a specific person
     * or across all persons if {@code personId} is null.
     *
     * @param personId    Optional person filter
     * @param excludeTxId Transaction ID to exclude (e.g. current editing transaction)
     * @return List of Give transactions
     */
    public List<EligibleCandidate> getEligibleGiveTransactions(Long personId, Long excludeTxId) {
        String sql = """
            SELECT t.id, t.type, t.tx_date, t.amount, t.category, t.sub_category, t.person_id,
                   p.name AS person_name, t.notes,
                   COALESCE((SELECT SUM(l.amount) FROM transaction_links l WHERE l.give_tx_id = t.id), 0.0) AS settled_amount
            FROM transactions t
            LEFT JOIN persons p ON t.person_id = p.id
            WHERE t.type = 'EXPENSE'
              AND (t.category = 'Loan Given' OR t.category = 'Lent' OR t.person_id IS NOT NULL)
              AND (? IS NULL OR t.person_id = ?)
              AND (? IS NULL OR t.id <> ?)
            ORDER BY t.tx_date DESC, t.id DESC
        """;

        return queryCandidates(sql, personId, excludeTxId);
    }

    /**
     * Retrieves eligible open Return Transactions (Income / Repayment) for a specific person
     * or across all persons if {@code personId} is null.
     *
     * @param personId    Optional person filter
     * @param excludeTxId Transaction ID to exclude (e.g. current editing transaction)
     * @return List of Return transactions
     */
    public List<EligibleCandidate> getEligibleReturnTransactions(Long personId, Long excludeTxId) {
        String sql = """
            SELECT t.id, t.type, t.tx_date, t.amount, t.category, t.sub_category, t.person_id,
                   p.name AS person_name, t.notes,
                   COALESCE((SELECT SUM(l.amount) FROM transaction_links l WHERE l.return_tx_id = t.id), 0.0) AS settled_amount
            FROM transactions t
            LEFT JOIN persons p ON t.person_id = p.id
            WHERE t.type = 'INCOME'
              AND (t.category = 'Return' OR t.category = 'Returned' OR t.person_id IS NOT NULL)
              AND (? IS NULL OR t.person_id = ?)
              AND (? IS NULL OR t.id <> ?)
            ORDER BY t.tx_date DESC, t.id DESC
        """;

        return queryCandidates(sql, personId, excludeTxId);
    }

    private List<EligibleCandidate> queryCandidates(String sql, Long personId, Long excludeTxId) {
        List<EligibleCandidate> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (personId != null) {
                ps.setLong(1, personId);
                ps.setLong(2, personId);
            } else {
                ps.setNull(1, java.sql.Types.BIGINT);
                ps.setNull(2, java.sql.Types.BIGINT);
            }

            if (excludeTxId != null) {
                ps.setLong(3, excludeTxId);
                ps.setLong(4, excludeTxId);
            } else {
                ps.setNull(3, java.sql.Types.BIGINT);
                ps.setNull(4, java.sql.Types.BIGINT);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Transaction tx = new Transaction();
                    tx.setId(rs.getLong("id"));
                    tx.setType(TransactionType.valueOf(rs.getString("type")));
                    java.sql.Date d = rs.getDate("tx_date");
                    if (d != null) tx.setTxDate(d.toLocalDate());
                    tx.setAmount(rs.getDouble("amount"));
                    tx.setCategory(rs.getString("category"));
                    tx.setSubCategory(rs.getString("sub_category"));
                    long pid = rs.getLong("person_id");
                    if (!rs.wasNull()) tx.setPersonId(pid);
                    tx.setPersonName(rs.getString("person_name"));
                    tx.setNotes(rs.getString("notes"));

                    double settled = rs.getDouble("settled_amount");
                    list.add(new EligibleCandidate(tx, settled));
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to query eligible link candidates: " + e.getMessage());
        }
        return list;
    }

    private TransactionLink mapResultSetToLink(ResultSet rs) throws SQLException {
        TransactionLink link = new TransactionLink();
        link.setId(rs.getLong("id"));
        link.setGiveTxId(rs.getLong("give_tx_id"));
        link.setReturnTxId(rs.getLong("return_tx_id"));
        link.setAmount(rs.getDouble("amount"));
        link.setNotes(rs.getString("notes"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) link.setCreatedAt(ts.toLocalDateTime());

        // Give side
        java.sql.Date gd = rs.getDate("give_date");
        if (gd != null) link.setGiveTxDate(gd.toLocalDate());
        link.setGiveTxAmount(rs.getDouble("give_amount"));
        link.setGiveTxNotes(rs.getString("give_notes"));
        link.setGivePersonName(rs.getString("give_person"));

        // Return side
        java.sql.Date rd = rs.getDate("return_date");
        if (rd != null) link.setReturnTxDate(rd.toLocalDate());
        link.setReturnTxAmount(rs.getDouble("return_amount"));
        link.setReturnTxNotes(rs.getString("return_notes"));
        link.setReturnPersonName(rs.getString("return_person"));

        return link;
    }

    /**
     * Transfer object holding a transaction and its already settled sum.
     */
    public static class EligibleCandidate {
        private final Transaction transaction;
        private final double settledAmount;

        public EligibleCandidate(Transaction transaction, double settledAmount) {
            this.transaction = transaction;
            this.settledAmount = settledAmount;
        }

        public Transaction getTransaction() {
            return transaction;
        }

        public double getSettledAmount() {
            return settledAmount;
        }

        public double getRemainingAmount() {
            return Math.max(0.0, transaction.getAmount() - settledAmount);
        }

        public boolean isFullySettled() {
            return getRemainingAmount() <= 0.01;
        }
    }
}
