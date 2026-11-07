package com.centsible.backend;

import com.centsible.backend.config.DataSeeder;
import com.centsible.backend.model.Category;
import com.centsible.backend.model.Transaction;
import com.centsible.backend.model.User;
import com.centsible.backend.repository.CategoryRepository;
import com.centsible.backend.repository.TransactionRepository;
import com.centsible.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The health check must be reachable without signing in (the sign-in page calls it to wake the
 * server), and the demo account's sample data must stay recent on a long-lived database.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class HealthAndDemoRefreshTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSeeder dataSeeder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void healthCheckNeedsNoSignIn() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void healthCheckDoesNotOpenUpAnythingElse() throws Exception {
        mockMvc.perform(get("/api/transactions")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/health")).andExpect(status().is4xxClientError());
    }

    // @Transactional: the test runs in a transaction that is rolled back afterwards, which is
    // also what the bulk delete in the seeder needs, and leaves the shared demo data as it was.
    @Test
    @Transactional
    void staleDemoDataIsReplacedWithFreshSamples() {
        dataSeeder.run();
        User demo = userRepository.findByEmail(DataSeeder.DEMO_EMAIL).orElseThrow();

        // Pretend the database was seeded months ago: only old rows remain
        transactionRepository.deleteAllByUserId(demo.getId());
        Category category = categoryRepository.findByUserIdAndName(demo.getId(), "Groceries").orElseThrow();
        transactionRepository.save(new Transaction(LocalDate.now().minusDays(120), "OLD SAMPLE ROW",
                new BigDecimal("-100.00"), category, demo));

        dataSeeder.run();

        LocalDate newest = transactionRepository.findMaxDateByUserId(demo.getId());
        assertFalse(newest.isBefore(LocalDate.now().minusDays(7)), "newest sample should be recent");
        List<Transaction> rows = transactionRepository.findByUserId(demo.getId());
        assertTrue(rows.size() > 5, "a full set of samples should be present");
        assertTrue(rows.stream().noneMatch(t -> t.getDescription().equals("OLD SAMPLE ROW")));
    }

    @Test
    @Transactional
    void recentDemoDataIsLeftAlone() {
        dataSeeder.run();
        User demo = userRepository.findByEmail(DataSeeder.DEMO_EMAIL).orElseThrow();
        Set<Long> before = transactionRepository.findByUserId(demo.getId()).stream()
                .map(Transaction::getId).collect(Collectors.toSet());

        dataSeeder.run();

        Set<Long> after = transactionRepository.findByUserId(demo.getId()).stream()
                .map(Transaction::getId).collect(Collectors.toSet());
        assertEquals(before, after, "fresh samples must not be re-created on every start");
    }
}
