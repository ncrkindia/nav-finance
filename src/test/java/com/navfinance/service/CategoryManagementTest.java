package com.navfinance.service;

import com.navfinance.config.AppConfig;
import com.navfinance.database.DatabaseManager;
import com.navfinance.model.Category;
import com.navfinance.model.SubCategory;
import com.navfinance.model.Transaction;
import com.navfinance.model.TransactionType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying Category & Associated Sub-Category management,
 * compatibility with existing transactions, and cascading rename updates.
 */
public class CategoryManagementTest {

    private static FinanceService financeService;

    @BeforeAll
    static void setUp() {
        AppConfig.setTestMode(true);
        DatabaseManager.getInstance().initDatabase();
        financeService = FinanceService.getInstance();
    }

    @AfterAll
    static void tearDown() {
        AppConfig.setTestMode(false);
    }

    @Test
    void testDefaultCategoriesSeeded() {
        List<Category> all = financeService.getAllCategories();
        assertNotNull(all);
        assertFalse(all.isEmpty(), "Default categories should be seeded on startup");

        List<String> expenseCats = financeService.getCategoryNames(TransactionType.EXPENSE);
        assertTrue(expenseCats.contains("Groceries & Food") || expenseCats.contains("Shopping") || expenseCats.contains("Bill"));

        List<String> incomeCats = financeService.getCategoryNames(TransactionType.INCOME);
        assertTrue(incomeCats.contains("Salary") || incomeCats.contains("Freelance / Business"));
    }

    @Test
    void testAddEditDeleteCategoryWithSubCategories() {
        // 1. Add Category
        Category newCat = new Category("Test Lifestyle", TransactionType.EXPENSE);
        Category saved = financeService.saveCategory(newCat);
        assertNotNull(saved.getId(), "Saved category should have generated ID");
        assertEquals("Test Lifestyle", saved.getName());

        // 2. Add Sub-Categories
        SubCategory sub1 = financeService.addSubCategory(saved.getId(), "Gym & Fitness");
        assertNotNull(sub1);
        assertNotNull(sub1.getId());
        assertEquals("Gym & Fitness", sub1.getName());

        SubCategory sub2 = financeService.addSubCategory(saved.getId(), "Books & Learning");
        assertNotNull(sub2);

        // Verify subcategory names query
        List<String> subs = financeService.getSubCategoryNames("Test Lifestyle", TransactionType.EXPENSE);
        assertTrue(subs.contains("Gym & Fitness"));
        assertTrue(subs.contains("Books & Learning"));

        // 3. Edit Sub-Category
        boolean subUpdated = financeService.updateSubCategory(sub1.getId(), "Gym Membership", "Gym & Fitness", "Test Lifestyle");
        assertTrue(subUpdated, "Sub-category should be updated successfully");

        List<String> updatedSubs = financeService.getSubCategoryNames("Test Lifestyle", TransactionType.EXPENSE);
        assertTrue(updatedSubs.contains("Gym Membership"));
        assertFalse(updatedSubs.contains("Gym & Fitness"));

        // 4. Delete Sub-Category
        boolean subDeleted = financeService.deleteSubCategory(sub2.getId(), "Books & Learning");
        assertTrue(subDeleted, "Sub-category should be deleted successfully");

        // 5. Delete Category
        boolean catDeleted = financeService.deleteCategory(saved.getId());
        assertTrue(catDeleted, "Category should be deleted successfully");

        List<String> remaining = financeService.getCategoryNames(TransactionType.EXPENSE);
        assertFalse(remaining.contains("Test Lifestyle"), "Deleted category should no longer exist");
    }

    @Test
    void testCascadeRenameToExistingTransactions() {
        // 1. Add Category
        Category cat = financeService.saveCategory(new Category("Automobile Care", TransactionType.EXPENSE));
        SubCategory sub = financeService.addSubCategory(cat.getId(), "Engine Oil");

        // 2. Create Transaction referencing this category & sub-category
        Transaction tx = new Transaction();
        tx.setType(TransactionType.EXPENSE);
        tx.setAmount(1200.0);
        tx.setTxDate(LocalDate.now());
        tx.setCategory("Automobile Care");
        tx.setSubCategory("Engine Oil");
        tx.setPaymentVia("UPI");
        tx.setExpected(false);

        Transaction createdTx = financeService.createTransaction(tx);
        assertNotNull(createdTx.getId());

        // Verify transaction count for category
        int count = financeService.countTransactionsForCategory("Automobile Care");
        assertEquals(1, count, "Transaction count for category should be 1");

        // 3. Rename Category
        cat.setName("Vehicle Maintenance");
        financeService.saveCategory(cat, "Automobile Care");

        // 4. Verify existing transaction was cascaded to the new category name
        Transaction reloadedTx = financeService.getTransactionById(createdTx.getId());
        assertEquals("Vehicle Maintenance", reloadedTx.getCategory(), "Transaction category should be cascaded on rename");

        // 5. Rename Sub-Category
        financeService.updateSubCategory(sub.getId(), "Synthetic Oil", "Engine Oil", "Vehicle Maintenance");
        reloadedTx = financeService.getTransactionById(createdTx.getId());
        assertEquals("Synthetic Oil", reloadedTx.getSubCategory(), "Transaction sub-category should be cascaded on rename");

        // 6. Clean up
        financeService.deleteTransaction(createdTx.getId());
        financeService.deleteCategory(cat.getId());
    }

    @Test
    void testPreventDeleteCategoryWhenReferenced() {
        Category cat = financeService.saveCategory(new Category("Protected Category", TransactionType.EXPENSE));

        Transaction tx = new Transaction();
        tx.setType(TransactionType.EXPENSE);
        tx.setAmount(500.0);
        tx.setTxDate(LocalDate.now());
        tx.setCategory("Protected Category");
        Transaction created = financeService.createTransaction(tx);

        int count = financeService.countTransactionsForCategory("Protected Category");
        assertTrue(count > 0, "Transactions reference the category");

        // Cleanup
        financeService.deleteTransaction(created.getId());
        int afterCount = financeService.countTransactionsForCategory("Protected Category");
        assertEquals(0, afterCount);
        assertTrue(financeService.deleteCategory(cat.getId()));
    }
}
