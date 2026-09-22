package com.centsible.backend.config;

import com.centsible.backend.model.Category;
import com.centsible.backend.model.CategoryRule;
import com.centsible.backend.repository.CategoryRepository;
import com.centsible.backend.repository.CategoryRuleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Seeds default categories and keyword auto-categorization rules.
 * No longer seeds sample transactions - real data comes from CSV/PDF import.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final CategoryRuleRepository categoryRuleRepository;

    public DataSeeder(CategoryRepository categoryRepository,
                      CategoryRuleRepository categoryRuleRepository) {
        this.categoryRepository = categoryRepository;
        this.categoryRuleRepository = categoryRuleRepository;
    }

    @Override
    public void run(String... args) {
        // Ensure standard categories exist
        List<String> categoryNames = List.of(
                "Transport", "Dining Out", "Groceries", "Rent",
                "Subscriptions", "Utilities", "Entertainment", "Shopping",
                "Income", "Bank Interest", "Bank Taxes & Fees", "Uncategorized"
        );

        Map<String, Category> categories = new HashMap<>();
        for (String name : categoryNames) {
            Category cat = categoryRepository.findByName(name);
            if (cat == null) {
                cat = categoryRepository.save(new Category(name));
            }
            categories.put(name, cat);
        }

        // Seed default auto-categorization rules if empty
        if (categoryRuleRepository.count() == 0) {
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
                    categoryRuleRepository.save(new CategoryRule(keyword, cat, keyword.length()));
                }
            }
        }
    }
}
