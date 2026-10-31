package com.centsible.backend;

import com.centsible.backend.config.DataSeeder;
import com.centsible.backend.dto.SignUpRequestDTO;
import com.centsible.backend.dto.UpdateCategoryRequestDTO;
import com.centsible.backend.model.Category;
import com.centsible.backend.model.Transaction;
import com.centsible.backend.model.User;
import com.centsible.backend.repository.CategoryRepository;
import com.centsible.backend.repository.TransactionRepository;
import com.centsible.backend.repository.UserRepository;
import com.centsible.backend.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The shared demo account must be strictly read-only, enforced on the server: it can look at
 * everything but cannot import, recategorise, learn rules or delete anything, no matter what
 * the frontend does. Ordinary accounts must be unaffected.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class DemoReadOnlyTest {

    private static final String PASSWORD = "CorrectHorse99!";
    private static final String CSV = "date,description,amount,category\n2026-01-05,TEST ROW,-100.00,Groceries\n";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User demoUser() {
        return userRepository.findByEmail(DataSeeder.DEMO_EMAIL)
                .orElseGet(() -> userRepository.save(
                        new User(DataSeeder.DEMO_EMAIL, passwordEncoder.encode("Password123!"), "Demo Portfolio")));
    }

    private Transaction transactionFor(User user, String categoryName) {
        Category category = categoryRepository.findByUserIdAndName(user.getId(), categoryName)
                .orElseGet(() -> categoryRepository.save(new Category(categoryName, user)));
        return transactionRepository.save(new Transaction(
                LocalDate.now(), "READ ONLY TEST " + System.nanoTime(), new BigDecimal("-10.00"), category, user));
    }

    private MockMultipartFile csvFile() {
        return new MockMultipartFile("file", "statement.csv", "text/csv", CSV.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testDemoUserCanStillReadEverything() throws Exception {
        String token = jwtService.generateToken(demoUser());

        mockMvc.perform(get("/api/transactions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/categories").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", org.hamcrest.Matchers.is(DataSeeder.DEMO_EMAIL)));
    }

    @Test
    public void testDemoUserCannotUpload() throws Exception {
        User demo = demoUser();
        long before = transactionRepository.countByUserId(demo.getId());
        String token = jwtService.generateToken(demo);

        mockMvc.perform(multipart("/api/transactions/upload")
                        .file(csvFile())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("read-only")));

        assertEquals(before, transactionRepository.countByUserId(demo.getId()), "Demo data must not change");
    }

    @Test
    public void testDemoUserCannotRecategoriseOrLearnRules() throws Exception {
        User demo = demoUser();
        Transaction tx = transactionFor(demo, "Demo Original " + System.nanoTime());
        String originalCategory = tx.getCategory().getName();
        String token = jwtService.generateToken(demo);

        UpdateCategoryRequestDTO body = new UpdateCategoryRequestDTO("Hijacked", true, "READ");

        mockMvc.perform(put("/api/transactions/{id}/category", tx.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("read-only")));

        Transaction reloaded = transactionRepository.findByIdAndUserId(tx.getId(), demo.getId()).orElseThrow();
        assertEquals(originalCategory, reloaded.getCategory().getName());
        assertTrue(categoryRepository.findByUserIdAndName(demo.getId(), "Hijacked").isEmpty(),
                "A rejected request must not create categories");
    }

    @Test
    public void testAnonymousWritesAreStillUnauthorized() throws Exception {
        mockMvc.perform(multipart("/api/transactions/upload").file(csvFile()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testRegularUserCanStillEditTheirOwnData() throws Exception {
        String email = "editor_" + System.nanoTime() + "@centsible.local";
        User user = userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), "Editor"));
        Transaction tx = transactionFor(user, "Original " + System.nanoTime());
        String token = jwtService.generateToken(user);

        mockMvc.perform(put("/api/transactions/{id}/category", tx.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCategoryRequestDTO("Recategorised", false, null))))
                .andExpect(status().isOk());

        Transaction reloaded = transactionRepository.findByIdAndUserId(tx.getId(), user.getId()).orElseThrow();
        assertEquals("Recategorised", reloaded.getCategory().getName());
    }

    @Test
    public void testDemoEmailCannotBeRegistered() throws Exception {
        SignUpRequestDTO signUp = new SignUpRequestDTO(DataSeeder.DEMO_EMAIL.toUpperCase(), PASSWORD, "Impostor");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUp)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("reserved")));
    }
}
