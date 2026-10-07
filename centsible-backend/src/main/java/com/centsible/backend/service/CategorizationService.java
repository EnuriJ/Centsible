package com.centsible.backend.service;

import com.centsible.backend.model.Category;
import com.centsible.backend.model.CategoryRule;
import com.centsible.backend.model.Transaction;
import com.centsible.backend.model.User;
import com.centsible.backend.repository.CategoryRepository;
import com.centsible.backend.repository.CategoryRuleRepository;
import com.centsible.backend.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CategorizationService {

    public static final String UNCATEGORIZED = "Uncategorized";

    private final CategoryRuleRepository categoryRuleRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public CategorizationService(CategoryRuleRepository categoryRuleRepository,
                                  CategoryRepository categoryRepository,
                                  TransactionRepository transactionRepository) {
        this.categoryRuleRepository = categoryRuleRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<CategoryRule> getRules(Long userId) {
        return categoryRuleRepository.findByUserIdOrderByPriorityDesc(userId);
    }

    public Category getOrCreateCategory(String name, User user) {
        if (name == null || name.isBlank()) {
            name = UNCATEGORIZED;
        }
        String cleanName = name.trim();
        Optional<Category> existing = categoryRepository.findByUserIdAndName(user.getId(), cleanName);
        if (existing.isPresent()) {
            return existing.get();
        }
        return categoryRepository.save(new Category(cleanName, user));
    }

    /**
     * Auto-categorizes a raw merchant/particulars description using the user's keyword rules.
     * Defaults to "Uncategorized" if no keyword matches.
     */
    public Category categorize(String rawDescription, User user) {
        if (rawDescription == null || rawDescription.isBlank()) {
            return getOrCreateCategory(UNCATEGORIZED, user);
        }

        String normalized = rawDescription.trim().toUpperCase();

        for (CategoryRule rule : getRules(user.getId())) {
            if (normalized.contains(rule.getKeyword())) {
                return rule.getCategory();
            }
        }

        return getOrCreateCategory(UNCATEGORIZED, user);
    }

    /**
     * Learns a new rule (or updates existing) from manual user categorization for the specific user.
     * Optionally backfills only the user's transactions containing the keyword.
     */
    @Transactional
    public CategoryRule learnRule(String keyword, String categoryName, boolean backfillExisting, User user) {
        if (keyword == null || keyword.isBlank() || categoryName == null || categoryName.isBlank()) {
            throw new IllegalArgumentException("Keyword and categoryName cannot be empty");
        }

        String cleanKeyword = keyword.trim().toUpperCase();
        Category category = getOrCreateCategory(categoryName, user);

        Optional<CategoryRule> existingRuleOpt = categoryRuleRepository.findByUserIdAndKeyword(user.getId(), cleanKeyword);
        CategoryRule rule;
        if (existingRuleOpt.isPresent()) {
            rule = existingRuleOpt.get();
            rule.setCategory(category);
            rule.setPriority(cleanKeyword.length());
        } else {
            rule = new CategoryRule(cleanKeyword, category, cleanKeyword.length(), user);
        }

        CategoryRule savedRule = categoryRuleRepository.save(rule);

        if (backfillExisting) {
            List<Transaction> userTransactions = transactionRepository.findByUserId(user.getId());
            for (Transaction t : userTransactions) {
                if (t.getDescription() != null && t.getDescription().toUpperCase().contains(cleanKeyword)) {
                    t.setCategory(category);
                    transactionRepository.save(t);
                }
            }
        }

        return savedRule;
    }

    /**
     * Seeds standard categories and keyword rules for a newly created or existing user.
     */
    @Transactional
    public void seedDefaultCategoriesAndRules(User user) {
        List<String> defaultCategoryNames = List.of(
                "Transport", "Dining Out", "Groceries", "Rent",
                "Subscriptions", "Utilities", "Entertainment", "Shopping",
                "Income", "Bank Interest", "Bank Taxes & Fees", UNCATEGORIZED
        );

        Map<String, Category> categories = new HashMap<>();
        for (String name : defaultCategoryNames) {
            Category cat = categoryRepository.findByUserIdAndName(user.getId(), name)
                    .orElseGet(() -> categoryRepository.save(new Category(name, user)));
            categories.put(name, cat);
        }

        if (categoryRuleRepository.findByUserIdOrderByPriorityDesc(user.getId()).isEmpty()) {
            Map<String, String> defaultRules = Map.ofEntries(
                    Map.entry("PICKME", "Transport"),
                    Map.entry("UBER", "Transport"),
                    Map.entry("BARISTA", "Dining Out"),
                    Map.entry("SATHSUKI", "Dining Out"),
                    Map.entry("RESTAURANT", "Dining Out"),
                    Map.entry("CAFE", "Dining Out"),
                    Map.entry("CHIPOTLE", "Dining Out"),
                    Map.entry("DINER", "Dining Out"),
                    Map.entry("KEELLS", "Groceries"),
                    Map.entry("CARGILLS", "Groceries"),
                    Map.entry("SUPERMARKET", "Groceries"),
                    Map.entry("WHOLE FOODS", "Groceries"),
                    Map.entry("TRADER JOE", "Groceries"),
                    Map.entry("COSTCO", "Groceries"),
                    Map.entry("NETFLIX", "Subscriptions"),
                    Map.entry("SPOTIFY", "Subscriptions"),
                    Map.entry("ELECTRIC", "Utilities"),
                    Map.entry("WATER", "Utilities"),
                    Map.entry("CEB", "Utilities"),
                    Map.entry("NWSDB", "Utilities"),
                    Map.entry("DIALOG", "Utilities"),
                    Map.entry("SLT", "Utilities"),
                    Map.entry("INTEREST", "Bank Interest"),
                    Map.entry("ADVANCE INCOME TAX", "Bank Taxes & Fees"),
                    Map.entry("TAX", "Bank Taxes & Fees"),
                    Map.entry("PAYCHECK", "Income"),
                    Map.entry("SALARY", "Income"),
                    Map.entry("FREELANCE", "Income"),
                    Map.entry("RENT", "Rent")
            );

            for (Map.Entry<String, String> entry : defaultRules.entrySet()) {
                String keyword = entry.getKey();
                Category cat = categories.get(entry.getValue());
                if (cat != null) {
                    categoryRuleRepository.save(new CategoryRule(keyword, cat, keyword.length(), user));
                }
            }
        }
    }

    public List<CategoryRule> getAllRules(Long userId) {
        return categoryRuleRepository.findByUserIdOrderByPriorityDesc(userId);
    }
}
