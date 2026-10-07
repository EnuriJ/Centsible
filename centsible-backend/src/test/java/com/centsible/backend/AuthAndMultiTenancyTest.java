package com.centsible.backend;

import com.centsible.backend.dto.LoginRequestDTO;
import com.centsible.backend.dto.SignUpRequestDTO;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthAndMultiTenancyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    public void testSignUpAndPasswordHashing() throws Exception {
        String email = "alice_" + System.currentTimeMillis() + "@centsible.local";
        SignUpRequestDTO signUp = new SignUpRequestDTO(email, "SuperSecret123!", "Alice Wonder");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUp)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.user.email", is(email)))
                .andExpect(jsonPath("$.user.displayName", is("Alice Wonder")));

        // Verify user in DB has hashed password, never plaintext
        User user = userRepository.findByEmail(email).orElseThrow();
        assertNotEquals("SuperSecret123!", user.getPassword());
        assertTrue(passwordEncoder.matches("SuperSecret123!", user.getPassword()), "BCrypt should match plain password");

        // Verify default categories were seeded for Alice
        List<Category> userCats = categoryRepository.findByUserIdOrderByNameAsc(user.getId());
        assertFalse(userCats.isEmpty(), "Default categories should be seeded for new user");
    }

    @Test
    public void testDuplicateEmailSignUpConflict() throws Exception {
        String email = "dup_" + System.currentTimeMillis() + "@centsible.local";
        SignUpRequestDTO signUp = new SignUpRequestDTO(email, "Password123!", "First User");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUp)))
                .andExpect(status().isCreated());

        // Attempting to register again with same email
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUp)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", containsString("already registered")));
    }

    @Test
    public void testLoginWithValidAndInvalidCredentials() throws Exception {
        String email = "bob_" + System.currentTimeMillis() + "@centsible.local";
        SignUpRequestDTO signUp = new SignUpRequestDTO(email, "ValidPassword99!", "Bob Builder");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUp)))
                .andExpect(status().isCreated());

        // 1. Successful login
        LoginRequestDTO validLogin = new LoginRequestDTO(email, "ValidPassword99!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.email", is(email)));

        // 2. Failed login with wrong password
        LoginRequestDTO invalidLogin = new LoginRequestDTO(email, "WrongPassword!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", containsString("Invalid email or password")));
    }

    @Test
    public void testProtectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/analytics/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testMultiTenantDataIsolation() throws Exception {
        // Register User 1
        String email1 = "tenant1_" + System.currentTimeMillis() + "@centsible.local";
        User user1 = userRepository.save(new User(email1, passwordEncoder.encode("Pass123!"), "Tenant One"));
        String token1 = jwtService.generateToken(user1);

        // Register User 2
        String email2 = "tenant2_" + System.currentTimeMillis() + "@centsible.local";
        User user2 = userRepository.save(new User(email2, passwordEncoder.encode("Pass123!"), "Tenant Two"));
        String token2 = jwtService.generateToken(user2);

        // Create transaction specifically for User 1
        Category cat1 = categoryRepository.save(new Category("Private One", user1));
        transactionRepository.save(new Transaction(
                LocalDate.now(), "USER 1 PRIVATE SECRET EXPENSE", new BigDecimal("-7777.00"), cat1, user1
        ));

        // User 1 requests transactions -> sees the transaction
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[?(@.description == 'USER 1 PRIVATE SECRET EXPENSE')]").exists());

        // User 2 requests transactions -> sees 0 transactions (User 1's transaction is NOT visible)
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.description == 'USER 1 PRIVATE SECRET EXPENSE')]").doesNotExist());

        // User 2 requests analytics summary -> totalExpense should be 0, not 7777.00
        mockMvc.perform(get("/api/analytics/summary")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpense", is(0)));
    }

    @Test
    public void testH2ConsoleGatedWhenDisabled() throws Exception {
        // Since spring.h2.console.enabled is false in test properties, access to /h2-console must be denied
        mockMvc.perform(get("/h2-console"))
                .andExpect(status().isUnauthorized());
    }
}
