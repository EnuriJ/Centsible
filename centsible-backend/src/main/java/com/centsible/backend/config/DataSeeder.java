package com.centsible.backend.config;

import com.centsible.backend.model.Category;
import com.centsible.backend.model.CategoryRule;
import com.centsible.backend.model.Transaction;
import com.centsible.backend.repository.CategoryRepository;
import com.centsible.backend.repository.CategoryRuleRepository;
import com.centsible.backend.repository.TransactionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Seeds default categories, keyword auto-categorization rules, and sample data.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final CategoryRuleRepository categoryRuleRepository;
    private final TransactionRepository transactionRepository;

    public DataSeeder(CategoryRepository categoryRepository,
                      CategoryRuleRepository categoryRuleRepository,
                      TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.categoryRuleRepository = categoryRuleRepository;
        this.transactionRepository = transactionRepository;
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

        // Seed sample transactions if empty (values in LKR)
        if (transactionRepository.count() == 0) {
            Object[][] sample = {
                    {"2026-08-01", "PURCHASE BARISTA NAWAM MAWAT", -2629.00, "Dining Out"},
                    {"2026-08-10", "PURCHASE PICKME RIDE", -792.87, "Transport"},
                    {"2026-08-13", "PURCHASE PICKME RIDE", -881.98, "Transport"},
                    {"2026-08-14", "PURCHASE PICKME RIDE", -763.60, "Transport"},
                    {"2026-08-14", "TRANSFER TO 8019457594", -3600.00, "Shopping"},
                    {"2026-08-24", "TRANSFER TO 8019457594", -6373.00, "Shopping"},
                    {"2026-08-25", "PURCHASE PICKME RIDE", -1547.19, "Transport"},
                    {"2026-08-31", "PURCHASE SATHSUKI JAPANESE R", -2100.00, "Dining Out"},
                    {"2026-08-31", "INTEREST", 207.71, "Bank Interest"},
                    {"2026-08-31", "ADVANCE INCOME TAX", -20.77, "Bank Taxes & Fees"},
                    {"2026-08-05", "Monthly Salary", 185000.00, "Income"},
                    {"2026-08-02", "Monthly Rent", -75000.00, "Rent"},
                    {"2026-08-08", "Keells Supermarket", -12450.00, "Groceries"},
                    {"2026-08-18", "Electricity CEB Bill", -8920.00, "Utilities"},
                    {"2026-08-20", "Dialog Broadband Bill", -4500.00, "Utilities"}
            };

            for (Object[] row : sample) {
                LocalDate date = LocalDate.parse((String) row[0]);
                String description = (String) row[1];
                BigDecimal amount = BigDecimal.valueOf((Double) row[2]);
                Category category = categories.get((String) row[3]);
                transactionRepository.save(new Transaction(date, description, amount, category));
            }
        }
    }
}
