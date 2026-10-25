package com.navfinance.dao;

import com.navfinance.database.DatabaseManager;
import com.navfinance.model.Category;
import com.navfinance.model.SubCategory;
import com.navfinance.model.TransactionType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object (DAO) for managing Categories and their associated Sub-Categories.
 * Supports CRUD operations, transaction cascade updates, and cross-dialect safety.
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public class CategoryDao {

    private static final Logger LOGGER = Logger.getLogger(CategoryDao.class.getName());

    /**
     * Retrieves all categories ordered by type (EXPENSE first, then INCOME) and category name,
     * with their associated sub-categories eagerly populated.
     *
     * @return List of all Category entities with sub-categories
     */
    public List<Category> findAll() {
        Map<Long, Category> categoryMap = new LinkedHashMap<>();
        String sql = "SELECT id, name, type, is_system, created_at, updated_at "
                + "FROM categories ORDER BY type DESC, name ASC";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Category c = mapCategory(rs);
                categoryMap.put(c.getId(), c);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load categories: {0}", e.getMessage());
        }

        if (!categoryMap.isEmpty()) {
            loadSubCategoriesForCategories(categoryMap);
        }

        return new ArrayList<>(categoryMap.values());
    }

    /**
     * Retrieves all categories of a specific TransactionType (INCOME or EXPENSE)
     * with associated sub-categories.
     *
     * @param type target TransactionType
     * @return List of matching Category entities
     */
    public List<Category> findByType(TransactionType type) {
        Map<Long, Category> categoryMap = new LinkedHashMap<>();
        String sql = "SELECT id, name, type, is_system, created_at, updated_at "
                + "FROM categories WHERE type = ? ORDER BY name ASC";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type != null ? type.name() : TransactionType.EXPENSE.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Category c = mapCategory(rs);
                    categoryMap.put(c.getId(), c);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load categories by type: {0}", e.getMessage());
        }

        if (!categoryMap.isEmpty()) {
            loadSubCategoriesForCategories(categoryMap);
        }

        return new ArrayList<>(categoryMap.values());
    }

    /**
     * Finds a single category by its database identifier.
     *
     * @param id category ID
     * @return Category entity or null if not found
     */
    public Category findById(Long id) {
        if (id == null) return null;
        String sql = "SELECT id, name, type, is_system, created_at, updated_at FROM categories WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Category c = mapCategory(rs);
                    c.setSubCategories(findSubCategoriesByCategoryId(c.getId()));
                    return c;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to find category by id: {0}", e.getMessage());
        }
        return null;
    }

    /**
     * Finds a category by its name and transaction type.
     *
     * @param name category name
     * @param type transaction type
     * @return matching Category or null
     */
    public Category findByNameAndType(String name, TransactionType type) {
        if (name == null || type == null) return null;
        String sql = "SELECT id, name, type, is_system, created_at, updated_at "
                + "FROM categories WHERE LOWER(name) = LOWER(?) AND type = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ps.setString(2, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Category c = mapCategory(rs);
                    c.setSubCategories(findSubCategoriesByCategoryId(c.getId()));
                    return c;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to find category by name: {0}", e.getMessage());
        }
        return null;
    }

    /**
     * Inserts a new Category into the database and persists any initial sub-categories.
     *
     * @param category Category entity to insert
     * @return persisted Category with generated ID
     */
    public Category insert(Category category) {
        if (category == null || category.getName() == null || category.getName().trim().isEmpty()) {
            return null;
        }

        String sql = "INSERT INTO categories (name, type, is_system) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, category.getName().trim());
            ps.setString(2, category.getType() != null ? category.getType().name() : TransactionType.EXPENSE.name());
            ps.setBoolean(3, category.isSystem());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        category.setId(generatedKeys.getLong(1));
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to insert category: {0}", e.getMessage());
            return null;
        }

        if (category.getId() != null && category.getSubCategories() != null) {
            for (SubCategory s : category.getSubCategories()) {
                if (s != null && s.getName() != null && !s.getName().trim().isEmpty()) {
                    addSubCategory(category.getId(), s.getName().trim());
                }
            }
            category.setSubCategories(findSubCategoriesByCategoryId(category.getId()));
        }

        return category;
    }

    /**
     * Updates an existing Category name or type and cascades rename mutations to transactional tables.
     *
     * @param category updated category entity
     * @param oldName previous name of the category (for cascade update)
     * @return true if updated successfully
     */
    public boolean update(Category category, String oldName) {
        if (category == null || category.getId() == null) return false;
        String sql = "UPDATE categories SET name = ?, type = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, category.getName().trim());
                    ps.setString(2, category.getType() != null ? category.getType().name() : TransactionType.EXPENSE.name());
                    ps.setLong(3, category.getId());
                    ps.executeUpdate();
                }

                // If renamed, cascade to transactions, recurring schedules, and due payments
                if (oldName != null && !oldName.trim().equalsIgnoreCase(category.getName().trim())) {
                    String trimmedOld = oldName.trim();
                    String trimmedNew = category.getName().trim();

                    try (PreparedStatement psTx = conn.prepareStatement(
                            "UPDATE transactions SET category = ? WHERE category = ? AND type = ?")) {
                        psTx.setString(1, trimmedNew);
                        psTx.setString(2, trimmedOld);
                        psTx.setString(3, category.getType().name());
                        psTx.executeUpdate();
                    }

                    try (PreparedStatement psSched = conn.prepareStatement(
                            "UPDATE recurring_schedules SET category = ? WHERE category = ? AND type = ?")) {
                        psSched.setString(1, trimmedNew);
                        psSched.setString(2, trimmedOld);
                        psSched.setString(3, category.getType().name());
                        psSched.executeUpdate();
                    }

                    try (PreparedStatement psDue = conn.prepareStatement(
                            "UPDATE due_payments SET category = ? WHERE category = ? AND type = ?")) {
                        psDue.setString(1, trimmedNew);
                        psDue.setString(2, trimmedOld);
                        psDue.setString(3, category.getType().name());
                        psDue.executeUpdate();
                    }
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to update category: {0}", e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a category and its associated sub-categories.
     *
     * @param id category ID
     * @return true if deleted
     */
    public boolean delete(Long id) {
        if (id == null) return false;
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Delete child sub-categories explicitly
                try (PreparedStatement psSub = conn.prepareStatement("DELETE FROM sub_categories WHERE category_id = ?")) {
                    psSub.setLong(1, id);
                    psSub.executeUpdate();
                }

                // Delete category
                int affected;
                try (PreparedStatement psCat = conn.prepareStatement("DELETE FROM categories WHERE id = ?")) {
                    psCat.setLong(1, id);
                    affected = psCat.executeUpdate();
                }

                conn.commit();
                return affected > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to delete category: {0}", e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // Sub-Categories (Associated Categories)
    // -------------------------------------------------------------------------

    /**
     * Retrieves all associated sub-categories belonging to a category.
     *
     * @param categoryId parent category ID
     * @return list of SubCategory entities
     */
    public List<SubCategory> findSubCategoriesByCategoryId(Long categoryId) {
        List<SubCategory> list = new ArrayList<>();
        if (categoryId == null) return list;
        String sql = "SELECT id, category_id, name, is_system, created_at, updated_at "
                + "FROM sub_categories WHERE category_id = ? ORDER BY name ASC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapSubCategory(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load sub-categories: {0}", e.getMessage());
        }
        return list;
    }

    /**
     * Adds an associated sub-category under a parent category.
     *
     * @param categoryId parent category ID
     * @param name sub-category name
     * @return persisted SubCategory or null on failure
     */
    public SubCategory addSubCategory(Long categoryId, String name) {
        if (categoryId == null || name == null || name.trim().isEmpty()) return null;
        String sql = "INSERT INTO sub_categories (category_id, name, is_system) VALUES (?, ?, FALSE)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, categoryId);
            ps.setString(2, name.trim());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return new SubCategory(rs.getLong(1), categoryId, name.trim(), false);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to add sub-category: {0}", e.getMessage());
        }
        return null;
    }

    /**
     * Updates/renames an associated sub-category and cascades to transactions.
     *
     * @param subCategoryId ID of the sub-category
     * @param newName new name
     * @param oldName previous name (for cascade)
     * @param categoryName parent category name (for scoping cascade)
     * @return true if updated
     */
    public boolean updateSubCategory(Long subCategoryId, String newName, String oldName, String categoryName) {
        if (subCategoryId == null || newName == null || newName.trim().isEmpty()) return false;
        String sql = "UPDATE sub_categories SET name = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, newName.trim());
                    ps.setLong(2, subCategoryId);
                    ps.executeUpdate();
                }

                if (oldName != null && !oldName.trim().equalsIgnoreCase(newName.trim()) && categoryName != null) {
                    try (PreparedStatement psTx = conn.prepareStatement(
                            "UPDATE transactions SET sub_category = ? WHERE sub_category = ? AND category = ?")) {
                        psTx.setString(1, newName.trim());
                        psTx.setString(2, oldName.trim());
                        psTx.setString(3, categoryName.trim());
                        psTx.executeUpdate();
                    }
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to update sub-category: {0}", e.getMessage());
            return false;
        }
    }

    /**
     * Deletes an associated sub-category.
     *
     * @param subCategoryId ID of the sub-category
     * @return true if deleted
     */
    public boolean deleteSubCategory(Long subCategoryId) {
        if (subCategoryId == null) return false;
        String sql = "DELETE FROM sub_categories WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, subCategoryId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to delete sub-category: {0}", e.getMessage());
            return false;
        }
    }

    /**
     * Counts how many transactions currently reference the given category name.
     *
     * @param categoryName category name to check
     * @return number of referencing transactions
     */
    public int countTransactionsForCategory(String categoryName) {
        if (categoryName == null) return 0;
        String sql = "SELECT COUNT(*) FROM transactions WHERE LOWER(category) = LOWER(?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, categoryName.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to count transactions for category: {0}", e.getMessage());
        }
        return 0;
    }

    // -------------------------------------------------------------------------
    // Private Helpers
    // -------------------------------------------------------------------------

    private void loadSubCategoriesForCategories(Map<Long, Category> categoryMap) {
        String sql = "SELECT id, category_id, name, is_system, created_at, updated_at "
                + "FROM sub_categories ORDER BY category_id, name ASC";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                SubCategory sub = mapSubCategory(rs);
                Category parent = categoryMap.get(sub.getCategoryId());
                if (parent != null) {
                    parent.getSubCategories().add(sub);
                    sub.setCategoryName(parent.getName());
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to batch load sub-categories: {0}", e.getMessage());
        }
    }

    private Category mapCategory(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setId(rs.getLong("id"));
        c.setName(rs.getString("name"));
        String typeStr = rs.getString("type");
        c.setType(typeStr != null && typeStr.equalsIgnoreCase("INCOME") ? TransactionType.INCOME : TransactionType.EXPENSE);
        c.setSystem(rs.getBoolean("is_system"));
        java.sql.Timestamp cAt = rs.getTimestamp("created_at");
        if (cAt != null) c.setCreatedAt(cAt.toLocalDateTime());
        java.sql.Timestamp uAt = rs.getTimestamp("updated_at");
        if (uAt != null) c.setUpdatedAt(uAt.toLocalDateTime());
        return c;
    }

    private SubCategory mapSubCategory(ResultSet rs) throws SQLException {
        SubCategory s = new SubCategory();
        s.setId(rs.getLong("id"));
        s.setCategoryId(rs.getLong("category_id"));
        s.setName(rs.getString("name"));
        s.setSystem(rs.getBoolean("is_system"));
        java.sql.Timestamp cAt = rs.getTimestamp("created_at");
        if (cAt != null) s.setCreatedAt(cAt.toLocalDateTime());
        java.sql.Timestamp uAt = rs.getTimestamp("updated_at");
        if (uAt != null) s.setUpdatedAt(uAt.toLocalDateTime());
        return s;
    }
}
