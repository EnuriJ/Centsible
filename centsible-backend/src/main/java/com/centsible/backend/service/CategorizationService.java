package com.centsible.backend.service;

import com.centsible.backend.model.Category;
import com.centsible.backend.model.CategoryRule;
import com.centsible.backend.model.Transaction;
import com.centsible.backend.repository.CategoryRepository;
import com.centsible.backend.repository.CategoryRuleRepository;
import com.centsible.backend.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategorizationService {

    public static final String UNCATEGORIZED = "Uncategorized";

    private final CategoryRuleRepository categoryRuleRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    private volatile List<CategoryRule> cachedRules = null;

    public CategorizationService(CategoryRuleRepository categoryRuleRepository,
                                  CategoryRepository categoryRepository,
                                  TransactionRepository transactionRepository) {
        this.categoryRuleRepository = categoryRuleRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    public synchronized void invalidateCache() {
        this.cachedRules = null;
    }

    private List<CategoryRule> getRules() {
        if (cachedRules == null) {
            synchronized (this) {
                if (cachedRules == null) {
                    cachedRules = categoryRuleRepository.findAllByOrderByPriorityDesc();
                }
            }
        }
        return cachedRules;
    }

    public Category getOrCreateCategory(String name) {
        if (name == null || name.isBlank()) {
            name = UNCATEGORIZED;
        }
        String cleanName = name.trim();
        Category existing = categoryRepository.findByName(cleanName);
        if (existing != null) {
            return existing;
        }
        return categoryRepository.save(new Category(cleanName));
    }

    /**
     * Auto-categorizes a raw merchant/particulars description using keyword rules.
     * Defaults to "Uncategorized" if no keyword matches.
     */
    public Category categorize(String rawDescription) {
        if (rawDescription == null || rawDescription.isBlank()) {
            return getOrCreateCategory(UNCATEGORIZED);
        }

        String normalized = rawDescription.trim().toUpperCase();

        for (CategoryRule rule : getRules()) {
            if (normalized.contains(rule.getKeyword())) {
                return rule.getCategory();
            }
        }

        return getOrCreateCategory(UNCATEGORIZED);
    }

    /**
     * Learns a new rule (or updates existing) from manual user categorization.
     * Optionally backfills all existing transactions containing the keyword.
     */
    @Transactional
    public CategoryRule learnRule(String keyword, String categoryName, boolean backfillExisting) {
        if (keyword == null || keyword.isBlank() || categoryName == null || categoryName.isBlank()) {
            throw new IllegalArgumentException("Keyword and categoryName cannot be empty");
        }

        String cleanKeyword = keyword.trim().toUpperCase();
        Category category = getOrCreateCategory(categoryName);

        Optional<CategoryRule> existingRuleOpt = categoryRuleRepository.findByKeyword(cleanKeyword);
        CategoryRule rule;
        if (existingRuleOpt.isPresent()) {
            rule = existingRuleOpt.get();
            rule.setCategory(category);
            rule.setPriority(cleanKeyword.length());
        } else {
            rule = new CategoryRule(cleanKeyword, category, cleanKeyword.length());
        }

        CategoryRule savedRule = categoryRuleRepository.save(rule);
        invalidateCache();

        if (backfillExisting) {
            List<Transaction> allTransactions = transactionRepository.findAll();
            for (Transaction t : allTransactions) {
                if (t.getDescription() != null && t.getDescription().toUpperCase().contains(cleanKeyword)) {
                    t.setCategory(category);
                    transactionRepository.save(t);
                }
            }
        }

        return savedRule;
    }

    public List<CategoryRule> getAllRules() {
        return categoryRuleRepository.findAllByOrderByPriorityDesc();
    }
}
