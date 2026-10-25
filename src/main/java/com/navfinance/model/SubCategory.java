package com.navfinance.model;

import java.time.LocalDateTime;

/**
 * Model representing an associated category (Sub-Category) under a master Category.
 *
 * @author Nav Finance Team
 * @version 1.0.0
 */
public class SubCategory {

    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private boolean isSystem;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Default constructor. */
    public SubCategory() {}

    /**
     * Parameterized constructor.
     *
     * @param id Database identifier
     * @param categoryId Parent category ID
     * @param name Sub-category name
     * @param isSystem Whether sub-category is system default
     */
    public SubCategory(Long id, Long categoryId, String name, boolean isSystem) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.isSystem = isSystem;
    }

    /**
     * Convenience constructor without ID.
     *
     * @param categoryId Parent category ID
     * @param name Sub-category name
     */
    public SubCategory(Long categoryId, String name) {
        this(null, categoryId, name, false);
    }

    /** @return Database ID */
    public Long getId() { return id; }
    /** @param id Database ID */
    public void setId(Long id) { this.id = id; }

    /** @return Parent category ID */
    public Long getCategoryId() { return categoryId; }
    /** @param categoryId Parent category ID */
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    /** @return Parent category name (optional convenience) */
    public String getCategoryName() { return categoryName; }
    /** @param categoryName Parent category name */
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    /** @return Sub-category name */
    public String getName() { return name; }
    /** @param name Sub-category name */
    public void setName(String name) { this.name = name; }

    /** @return Whether sub-category is system default */
    public boolean isSystem() { return isSystem; }
    /** @param system Whether sub-category is system default */
    public void setSystem(boolean system) { isSystem = system; }

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
