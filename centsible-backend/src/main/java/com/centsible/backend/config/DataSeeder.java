package com.centsible.backend.config;

import com.centsible.backend.model.Category;
import com.centsible.backend.model.Transaction;
import com.centsible.backend.repository.CategoryRepository;
import com.centsible.backend.repository.TransactionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Seeds the database with realistic sample data on first startup, so the
 * dashboard has something meaningful to show before CSV upload is built.
 * Skips seeding if data already exists (safe to restart the app repeatedly).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public DataSeeder(CategoryRepository categoryRepository, TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            return;
        }

        List<String> categoryNames = List.of(
                "Groceries", "Rent", "Transport", "Dining Out",
                "Subscriptions", "Utilities", "Entertainment", "Shopping", "Income"
        );

        Map<String, Category> categories = new HashMap<>();
        for (String name : categoryNames) {
            categories.put(name, categoryRepository.save(new Category(name)));
        }

        // date, description, amount (negative = expense, positive = income), category
        Object[][] sample = {
                {"2026-06-01", "Monthly Rent", -1450.00, "Rent"},
                {"2026-06-02", "Whole Foods Market", -84.32, "Groceries"},
                {"2026-06-03", "Uber Trip", -18.50, "Transport"},
                {"2026-06-04", "Netflix", -15.99, "Subscriptions"},
                {"2026-06-05", "Paycheck", 2600.00, "Income"},
                {"2026-06-06", "Chipotle", -12.75, "Dining Out"},
                {"2026-06-07", "Electric Bill", -76.20, "Utilities"},
                {"2026-06-08", "Spotify", -10.99, "Subscriptions"},
                {"2026-06-09", "Trader Joe's", -52.10, "Groceries"},
                {"2026-06-10", "Movie Tickets", -28.00, "Entertainment"},
                {"2026-06-11", "Amazon Order", -64.45, "Shopping"},
                {"2026-06-12", "Gas Station", -40.00, "Transport"},
                {"2026-06-15", "Freelance Payment", 450.00, "Income"},
                {"2026-06-18", "Water Bill", -29.10, "Utilities"},
                {"2026-06-20", "Trader Joe's", -47.60, "Groceries"},
                {"2026-06-22", "Local Diner", -19.80, "Dining Out"},
                {"2026-07-01", "Monthly Rent", -1450.00, "Rent"},
                {"2026-07-02", "Costco", -132.60, "Groceries"},
                {"2026-07-03", "Paycheck", 2600.00, "Income"},
                {"2026-07-05", "Netflix", -15.99, "Subscriptions"},
                {"2026-07-06", "Local Diner", -22.40, "Dining Out"},
                {"2026-07-08", "Water Bill", -31.00, "Utilities"},
                {"2026-07-09", "Spotify", -10.99, "Subscriptions"},
                {"2026-07-10", "Concert Ticket", -55.00, "Entertainment"},
                {"2026-07-12", "Uber Trip", -21.30, "Transport"},
                {"2026-07-14", "Whole Foods Market", -91.05, "Groceries"},
                {"2026-07-16", "Gas Station", -38.20, "Transport"},
                {"2026-07-18", "Amazon Order", -47.99, "Shopping"},
                {"2026-07-20", "Paycheck", 2600.00, "Income"},
                {"2026-07-22", "Sushi Night", -34.50, "Dining Out"},
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
