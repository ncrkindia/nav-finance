package com.navfinance.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Model representing a financial category (e.g. Groceries, EMI Payment, Salary, Fuel).
 * Can be of type INCOME or EXPENSE and maintains associated sub-categories.
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public class Category {

    private Long id;
    private String name;
    private TransactionType type;
    private boolean isSystem;
    private List<SubCategory> subCategories = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Default constructor. */
    public Category() {}

    /**
     * Parameterized constructor.
     *
     * @param id Database identifier
     * @param name Category name
     * @param type Transaction type (INCOME or EXPENSE)
     * @param isSystem Whether this is a default system-seeded category
     */
    public Category(Long id, String name, TransactionType type, boolean isSystem) {
        this.id = id;
        this.name = name;
        this.type = type != null ? type : TransactionType.EXPENSE;
        this.isSystem = isSystem;
    }

    /**
     * Convenience constructor without ID.
     *
     * @param name Category name
     * @param type Transaction type (INCOME or EXPENSE)
     */
    public Category(String name, TransactionType type) {
        this(null, name, type, false);
    }

    /** @return Database ID */
    public Long getId() { return id; }
    /** @param id Database ID */
    public void setId(Long id) { this.id = id; }

    /** @return Category name */
    public String getName() { return name; }
    /** @param name Category name */
    public void setName(String name) { this.name = name; }

    /** @return Transaction type (INCOME or EXPENSE) */
    public TransactionType getType() { return type; }
    /** @param type Transaction type */
    public void setType(TransactionType type) { this.type = type; }

    /** @return Whether category is a system default */
    public boolean isSystem() { return isSystem; }
    /** @param system Whether category is a system default */
    public void setSystem(boolean system) { isSystem = system; }

    /** @return List of associated sub-categories */
    public List<SubCategory> getSubCategories() { return subCategories; }
    /** @param subCategories List of associated sub-categories */
    public void setSubCategories(List<SubCategory> subCategories) {
        this.subCategories = subCategories != null ? subCategories : new ArrayList<>();
    }

    /**
     * Convenience method returning names of associated sub-categories.
     *
     * @return List of sub-category name strings
     */
    public List<String> getSubCategoryNames() {
        List<String> names = new ArrayList<>();
        if (subCategories != null) {
            for (SubCategory s : subCategories) {
                if (s != null && s.getName() != null && !s.getName().trim().isEmpty()) {
                    names.add(s.getName().trim());
                }
            }
        }
        return names;
    }

    /** @return Timestamp when record was created */
    public LocalDateTime getCreatedAt() { return createdAt; }
    /** @param createdAt Creation timestamp */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** @return Timestamp when record was last updated */
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    /** @param updatedAt Update timestamp */
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return name;
    }
}
