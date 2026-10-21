package com.centsible.backend.config;

import com.centsible.backend.model.Category;
import com.centsible.backend.model.Transaction;
import com.centsible.backend.model.User;
import com.centsible.backend.repository.CategoryRepository;
import com.centsible.backend.repository.TransactionRepository;
import com.centsible.backend.repository.UserRepository;
import com.centsible.backend.service.CategorizationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Seeds default demo user with synthetic showcase transactions and default categories/rules.
 * Does not touch or reassign any existing data.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    public static final String DEMO_EMAIL = "demo@centsible.com";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategorizationService categorizationService;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public DataSeeder(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      CategorizationService categorizationService,
                      CategoryRepository categoryRepository,
                      TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.categorizationService = categorizationService;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public void run(String... args) {
        // Provision demo user if not present
        User demoUser = userRepository.findByEmail(DEMO_EMAIL)
                .orElseGet(() -> userRepository.save(
                        new User(DEMO_EMAIL, passwordEncoder.encode("Password123!"), "Demo Portfolio")
                ));

        // Seed default categories and categorization rules for demo user
        categorizationService.seedDefaultCategoriesAndRules(demoUser);

        // Seed synthetic showcase transactions strictly for demo user if they have none
        if (transactionRepository.countByUserId(demoUser.getId()) == 0) {
            seedSyntheticDemoTransactions(demoUser);
        }
    }

    private void seedSyntheticDemoTransactions(User user) {
        LocalDate today = LocalDate.now();

        createTx(user, today.minusDays(26), "MONTHLY SALARY CREDIT - TECH CORP", new BigDecimal("350000.00"), "Income");
        createTx(user, today.minusDays(14), "FREELANCE CONSULTING INVOICE #104", new BigDecimal("85000.00"), "Income");
        createTx(user, today.minusDays(2), "SAVINGS ACCOUNT INTEREST", new BigDecimal("1250.00"), "Bank Interest");

        createTx(user, today.minusDays(25), "APARTMENT RENT TRANSFER", new BigDecimal("-95000.00"), "Rent");
        createTx(user, today.minusDays(21), "CARGILLS FOOD CITY COLOMBO", new BigDecimal("-14250.00"), "Groceries");
        createTx(user, today.minusDays(10), "KEELLS SUPERMARKET THIMBIRIGASYAYA", new BigDecimal("-18900.00"), "Groceries");
        createTx(user, today.minusDays(19), "CEB ELECTRICITY BILL PAYMENT", new BigDecimal("-12800.00"), "Utilities");
        createTx(user, today.minusDays(16), "DIALOG BROADBAND & MOBILE", new BigDecimal("-6500.00"), "Utilities");
        createTx(user, today.minusDays(15), "BARISTA COFFEE NAWAM MAWATHA", new BigDecimal("-2450.00"), "Dining Out");
        createTx(user, today.minusDays(8), "RESTAURANT THE GALLERY CAFE", new BigDecimal("-16500.00"), "Dining Out");
        createTx(user, today.minusDays(11), "PICKME RIDE", new BigDecimal("-1250.00"), "Transport");
        createTx(user, today.minusDays(5), "UBER TRIP COLOMBO", new BigDecimal("-1850.00"), "Transport");
        createTx(user, today.minusDays(17), "NETFLIX SUBSCRIPTION", new BigDecimal("-3900.00"), "Subscriptions");
        createTx(user, today.minusDays(9), "SPOTIFY PREMIUM", new BigDecimal("-1499.00"), "Subscriptions");
        createTx(user, today.minusDays(2), "ADVANCE INCOME TAX ON INTEREST", new BigDecimal("-125.00"), "Bank Taxes & Fees");
    }

    private void createTx(User user, LocalDate date, String description, BigDecimal amount, String categoryName) {
        Category category = categoryRepository.findByUserIdAndName(user.getId(), categoryName)
                .orElseGet(() -> categoryRepository.save(new Category(categoryName, user)));
        transactionRepository.save(new Transaction(date, description, amount, category, user));
    }
}
