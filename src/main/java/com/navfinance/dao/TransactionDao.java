package com.navfinance.dao;

import com.navfinance.database.DatabaseManager;
import com.navfinance.model.FilterCriteria;
import com.navfinance.model.Transaction;
import com.navfinance.model.TransactionType;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object (DAO) for financial transactions in the cashbook ledger.
 * <p>
 * Supports inserting, updating, deleting, multi-criteria filtering,
 * aggregate sums, category breakdown rollups, distinct category querying,
 * and Udhar Given / Udhar Taken summaries.
 * </p>
 */
public class TransactionDao {

    /**
     * Inserts a new financial transaction into the ledger.
     *
     * @param tx Transaction entity to persist
     * @return Persisted transaction with generated ID and timestamps
     */
    public Transaction insert(Transaction tx) {
        String sql = """
            INSERT INTO transactions (
                type, tx_date, is_expected, amount, payment_via,
                category, sub_category, person_id, bill_id, emi_id,
                credit_card_id, notes, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            LocalDateTime now = LocalDateTime.now();
            ps.setString(1, tx.getType().name());
            ps.setDate(2, Date.valueOf(tx.getTxDate() != null ? tx.getTxDate() : LocalDate.now()));
            ps.setBoolean(3, tx.isExpected());
            ps.setDouble(4, tx.getAmount());
            ps.setString(5, tx.getPaymentVia());
            ps.setString(6, tx.getCategory());
            ps.setString(7, tx.getSubCategory());

            if (tx.getPersonId() != null) ps.setLong(8, tx.getPersonId());
            else ps.setNull(8, java.sql.Types.BIGINT);

            if (tx.getBillId() != null) ps.setLong(9, tx.getBillId());
            else ps.setNull(9, java.sql.Types.BIGINT);

            if (tx.getEmiId() != null) ps.setLong(10, tx.getEmiId());
            else ps.setNull(10, java.sql.Types.BIGINT);

            if (tx.getCreditCardId() != null) ps.setLong(11, tx.getCreditCardId());
            else ps.setNull(11, java.sql.Types.BIGINT);

            ps.setString(12, tx.getNotes());
            ps.setTimestamp(13, Timestamp.valueOf(now));
            ps.setTimestamp(14, Timestamp.valueOf(now));

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    tx.setId(keys.getLong(1));
                    tx.setCreatedAt(now);
                    tx.setUpdatedAt(now);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to insert transaction: " + e.getMessage());
        }
        return tx;
    }


    /**
     * Updates an existing transaction in the ledger.
     *
     * @param tx Transaction entity containing updated fields and valid ID
     * @return {@code true} if updated successfully, {@code false} otherwise
     */
    public boolean update(Transaction tx) {
        String sql = """
            UPDATE transactions SET
                type = ?, tx_date = ?, is_expected = ?, amount = ?, payment_via = ?,
                category = ?, sub_category = ?, person_id = ?, bill_id = ?, emi_id = ?,
                credit_card_id = ?, notes = ?, updated_at = ?
            WHERE id = ?
        """;

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            LocalDateTime now = LocalDateTime.now();
            ps.setString(1, tx.getType().name());
            ps.setDate(2, Date.valueOf(tx.getTxDate() != null ? tx.getTxDate() : LocalDate.now()));
            ps.setBoolean(3, tx.isExpected());
            ps.setDouble(4, tx.getAmount());
            ps.setString(5, tx.getPaymentVia());
            ps.setString(6, tx.getCategory());
            ps.setString(7, tx.getSubCategory());

            if (tx.getPersonId() != null) ps.setLong(8, tx.getPersonId());
            else ps.setNull(8, java.sql.Types.BIGINT);

            if (tx.getBillId() != null) ps.setLong(9, tx.getBillId());
            else ps.setNull(9, java.sql.Types.BIGINT);

            if (tx.getEmiId() != null) ps.setLong(10, tx.getEmiId());
            else ps.setNull(10, java.sql.Types.BIGINT);

            if (tx.getCreditCardId() != null) ps.setLong(11, tx.getCreditCardId());
            else ps.setNull(11, java.sql.Types.BIGINT);

            ps.setString(12, tx.getNotes());
            ps.setTimestamp(13, Timestamp.valueOf(now));
            ps.setLong(14, tx.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to update transaction: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a transaction from the ledger by its ID.
     *
     * @param id Primary key ID to delete
     * @return {@code true} if deleted successfully, {@code false} otherwise
     */
    public boolean delete(Long id) {
        String sql = "DELETE FROM transactions WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to delete transaction: " + e.getMessage());
            return false;
        }
    }

    /**
     * Finds a single transaction by primary key ID, populating linked person details.
     *
     * @param id Transaction ID
     * @return {@link Transaction} entity or {@code null} if not found
     */
    public Transaction findById(Long id) {
        if (id == null) return null;
        String sql = """
            SELECT t.*, p.name AS person_name
            FROM transactions t
            LEFT JOIN persons p ON t.person_id = p.id
            WHERE t.id = ?
        """;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to find transaction by id: " + e.getMessage());
        }
        return null;
    }

    /**
     * Executes a dynamic SQL query across multiple filtering dimensions including dates,
     * amounts, categories, payment modes, and search text with sorting.
     *
     * @param criteria Filter criteria encapsulation
     * @return List of matching {@link Transaction} entities
     */
    public List<Transaction> findByCriteria(FilterCriteria criteria) {
        List<Transaction> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT t.*, p.name AS person_name
            FROM transactions t
            LEFT JOIN persons p ON t.person_id = p.id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();

        if (criteria != null) {
            if (criteria.getType() != null) {
                sql.append(" AND t.type = ?");
                params.add(criteria.getType().name());
            }
            if (criteria.getCategory() != null && !criteria.getCategory().trim().isEmpty() && !criteria.getCategory().equalsIgnoreCase("All")) {
                sql.append(" AND t.category = ?");
                params.add(criteria.getCategory().trim());
            }
            if (criteria.getSubCategory() != null && !criteria.getSubCategory().trim().isEmpty() && !criteria.getSubCategory().equalsIgnoreCase("All")) {
                sql.append(" AND t.sub_category = ?");
                params.add(criteria.getSubCategory().trim());
            }
            if (criteria.getPaymentVia() != null && !criteria.getPaymentVia().trim().isEmpty() && !criteria.getPaymentVia().equalsIgnoreCase("All")) {
                sql.append(" AND t.payment_via = ?");
                params.add(criteria.getPaymentVia().trim());
            }
            if (criteria.getPersonId() != null) {
                sql.append(" AND t.person_id = ?");
                params.add(criteria.getPersonId());
            }
            if (criteria.getStartDate() != null) {
                sql.append(" AND t.tx_date >= ?");
                params.add(Date.valueOf(criteria.getStartDate()));
            }
            if (criteria.getEndDate() != null) {
                sql.append(" AND t.tx_date <= ?");
                params.add(Date.valueOf(criteria.getEndDate()));
            }
            if (criteria.getMinAmount() != null) {
                sql.append(" AND t.amount >= ?");
                params.add(criteria.getMinAmount());
            }
            if (criteria.getMaxAmount() != null) {
                sql.append(" AND t.amount <= ?");
                params.add(criteria.getMaxAmount());
            }
            if (criteria.getExpected() != null) {
                sql.append(" AND t.is_expected = ?");
                params.add(criteria.getExpected());
            }
            if (criteria.getSearchKeyword() != null && !criteria.getSearchKeyword().trim().isEmpty()) {
                String kw = "%" + criteria.getSearchKeyword().trim().toLowerCase() + "%";
                sql.append(" AND (LOWER(t.notes) LIKE ? OR LOWER(t.category) LIKE ? OR LOWER(t.sub_category) LIKE ? OR LOWER(t.payment_via) LIKE ? OR LOWER(p.name) LIKE ?)");
                params.add(kw);
                params.add(kw);
                params.add(kw);
                params.add(kw);
                params.add(kw);
            }

            // Sorting
            String sortCol = "t.tx_date";
            if ("type".equalsIgnoreCase(criteria.getSortBy())) {
                sortCol = "t.type";
            } else if ("category".equalsIgnoreCase(criteria.getSortBy())) {
                sortCol = "t.category";
            } else if ("sub_category".equalsIgnoreCase(criteria.getSortBy())) {
                sortCol = "t.sub_category";
            } else if ("amount".equalsIgnoreCase(criteria.getSortBy())) {
                sortCol = "t.amount";
            } else if ("tx_date".equalsIgnoreCase(criteria.getSortBy())) {
                sortCol = "t.tx_date";
            }

            String sortDir = criteria.isSortAscending() ? "ASC" : "DESC";
            sql.append(" ORDER BY ").append(sortCol).append(" ").append(sortDir).append(", t.id DESC");
        } else {
            sql.append(" ORDER BY t.tx_date DESC, t.id DESC");
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Aggregates total transaction amount for a given transaction type within an optional date range.
     *
     * @param type Transaction type (INCOME or EXPENSE)
     * @param start Optional start date (inclusive)
     * @param end Optional end date (inclusive)
     * @return Summed monetary amount in INR
     */
    public double sumAmountByTypeAndDateRange(TransactionType type, LocalDate start, LocalDate end) {
        StringBuilder sql = new StringBuilder("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE type = ?");
        List<Object> params = new ArrayList<>();
        params.add(type.name());

        if (start != null) {
            sql.append(" AND tx_date >= ?");
            params.add(Date.valueOf(start));
        }
        if (end != null) {
            sql.append(" AND tx_date <= ?");
            params.add(Date.valueOf(end));
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to sum transactions: " + e.getMessage());
        }
        return 0.0;
    }

    /**
     * Computes spending or earning totals grouped by category for donut/pie chart visual breakdown.
     *
     * @param type Transaction type (INCOME or EXPENSE)
     * @param start Optional start date (inclusive)
     * @param end Optional end date (inclusive)
     * @return Ordered map of Category Name to Aggregated Amount (highest first)
     */
    public Map<String, Double> getCategoryBreakdown(TransactionType type, LocalDate start, LocalDate end) {
        Map<String, Double> map = new LinkedHashMap<>();
        StringBuilder sql = new StringBuilder("""
            SELECT category, SUM(amount) AS total
            FROM transactions
            WHERE type = ?
        """);
        List<Object> params = new ArrayList<>();
        params.add(type.name());

        if (start != null) {
            sql.append(" AND tx_date >= ?");
            params.add(Date.valueOf(start));
        }
        if (end != null) {
            sql.append(" AND tx_date <= ?");
            params.add(Date.valueOf(end));
        }

        sql.append(" GROUP BY category ORDER BY total DESC");

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("category"), rs.getDouble("total"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to get category breakdown: " + e.getMessage());
        }
        return map;
    }

    /**
     * Retrieves unique category names currently in use for auto-complete combo boxes.
     *
     * @param type Transaction type filter, or {@code null} for both
     * @return Alphabetical list of distinct category names
     */
    public List<String> getDistinctCategories(TransactionType type) {
        List<String> list = new ArrayList<>();
        String sql = type != null ?
                "SELECT DISTINCT category FROM transactions WHERE type = ? ORDER BY category" :
                "SELECT DISTINCT category FROM transactions ORDER BY category";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (type != null) {
                ps.setString(1, type.name());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("category"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch distinct categories: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves unique sub-category names currently associated with a specific category.
     *
     * @param category Parent category name
     * @return Alphabetical list of distinct sub-category names
     */
    public List<String> getDistinctSubCategories(String category) {
        List<String> list = new ArrayList<>();
        String sql = (category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("All")) ?
                "SELECT DISTINCT sub_category FROM transactions WHERE category = ? AND sub_category IS NOT NULL AND sub_category != '' ORDER BY sub_category" :
                "SELECT DISTINCT sub_category FROM transactions WHERE sub_category IS NOT NULL AND sub_category != '' ORDER BY sub_category";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (category != null && !category.trim().isEmpty() && !category.equalsIgnoreCase("All")) {
                ps.setString(1, category.trim());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("sub_category"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch distinct sub-categories: " + e.getMessage());
        }
        return list;
    }

    /**
     * Calculates the cumulative total of money loaned out / given to relatives, friends, or debtors.
     *
     * @return Total Udhar Given in INR
     */
    public double getTotalUdharGiven() {
        String sql = "SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE category IN ('Udhar Given', 'Loan Given')";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            System.err.println("Failed to calculate total udhar given: " + e.getMessage());
        }
        return 0.0;
    }

    /**
     * Calculates the cumulative total of money borrowed / taken from lenders or relatives.
     *
     * @return Total Udhar Taken in INR
     */
    public double getTotalUdharTaken() {
        String sql = "SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE category IN ('Udhaar', 'Udhar', 'Loan')";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            System.err.println("Failed to calculate total udhar taken: " + e.getMessage());
        }
        return 0.0;
    }

    /**
     * Maps an active result set row to a {@link Transaction} domain entity.
     *
     * @param rs Active result set positioned at a valid row
     * @return Mapped Transaction entity
     * @throws SQLException on database column access error
     */
    private Transaction mapRow(ResultSet rs) throws SQLException {
        Transaction tx = new Transaction();
        tx.setId(rs.getLong("id"));
        tx.setType(TransactionType.valueOf(rs.getString("type")));
        Date d = rs.getDate("tx_date");
        if (d != null) tx.setTxDate(d.toLocalDate());
        tx.setExpected(rs.getBoolean("is_expected"));
        tx.setAmount(rs.getDouble("amount"));
        tx.setPaymentVia(rs.getString("payment_via"));
        tx.setCategory(rs.getString("category"));
        tx.setSubCategory(rs.getString("sub_category"));

        long pid = rs.getLong("person_id");
        if (!rs.wasNull()) tx.setPersonId(pid);

        long bid = rs.getLong("bill_id");
        if (!rs.wasNull()) tx.setBillId(bid);

        long eid = rs.getLong("emi_id");
        if (!rs.wasNull()) tx.setEmiId(eid);

        long cid = rs.getLong("credit_card_id");
        if (!rs.wasNull()) tx.setCreditCardId(cid);

        try {
            tx.setPersonName(rs.getString("person_name"));
        } catch (SQLException ignored) {}

        tx.setNotes(rs.getString("notes"));
        Timestamp cat = rs.getTimestamp("created_at");
        if (cat != null) tx.setCreatedAt(cat.toLocalDateTime());
        Timestamp uat = rs.getTimestamp("updated_at");
        if (uat != null) tx.setUpdatedAt(uat.toLocalDateTime());

        return tx;
    }
}

