package com.centsible.backend;

import com.centsible.backend.config.DataSeeder;
import com.centsible.backend.dto.DeleteAccountRequestDTO;
import com.centsible.backend.model.Category;
import com.centsible.backend.model.CategoryRule;
import com.centsible.backend.model.Transaction;
import com.centsible.backend.model.User;
import com.centsible.backend.repository.CategoryRepository;
import com.centsible.backend.repository.CategoryRuleRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AccountDeletionTest {

    private static final String PASSWORD = "CorrectHorse99!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryRuleRepository categoryRuleRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private User createUserWithData(String prefix) {
        String email = prefix + "_" + System.nanoTime() + "@centsible.local";
        User user = userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), prefix));
        Category category = categoryRepository.save(new Category("Category of " + prefix, user));
        categoryRuleRepository.save(new CategoryRule("KEYWORD" + prefix.toUpperCase(), category, user));
        transactionRepository.save(new Transaction(
                LocalDate.now(), "TX OF " + prefix.toUpperCase(), new BigDecimal("-10.00"), category, user));
        return user;
    }

    private String body(String password) throws Exception {
        return objectMapper.writeValueAsString(new DeleteAccountRequestDTO(password));
    }

    @Test
    public void testDeleteWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(delete("/api/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testWrongPasswordIsForbiddenAndDeletesNothing() throws Exception {
        User user = createUserWithData("wrongpw");
        String token = jwtService.generateToken(user);

        mockMvc.perform(delete("/api/account")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("NotTheRightPassword!")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("Incorrect password")));

        assertTrue(userRepository.findById(user.getId()).isPresent(), "Account must survive a wrong password");
        assertEquals(1, transactionRepository.countByUserId(user.getId()));
    }

    @Test
    public void testCorrectPasswordDeletesOnlyThatUsersData() throws Exception {
        User doomed = createUserWithData("doomed");
        User bystander = createUserWithData("bystander");
        String token = jwtService.generateToken(doomed);

        mockMvc.perform(delete("/api/account")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(PASSWORD)))
                .andExpect(status().isNoContent());

        // Everything belonging to the deleted user is gone...
        assertTrue(userRepository.findById(doomed.getId()).isEmpty());
        assertEquals(0, transactionRepository.countByUserId(doomed.getId()));
        assertTrue(categoryRepository.findByUserIdOrderByNameAsc(doomed.getId()).isEmpty());
        assertTrue(categoryRuleRepository.findByUserIdOrderByPriorityDesc(doomed.getId()).isEmpty());

        // ...and nobody else's data was touched.
        assertTrue(userRepository.findById(bystander.getId()).isPresent());
        assertEquals(1, transactionRepository.countByUserId(bystander.getId()));
        assertEquals(1, categoryRepository.findByUserIdOrderByNameAsc(bystander.getId()).size());
        assertEquals(1, categoryRuleRepository.findByUserIdOrderByPriorityDesc(bystander.getId()).size());
    }

    @Test
    public void testDemoAccountCannotBeDeleted() throws Exception {
        User demo = userRepository.findByEmail(DataSeeder.DEMO_EMAIL)
                .orElseGet(() -> userRepository.save(
                        new User(DataSeeder.DEMO_EMAIL, passwordEncoder.encode("Password123!"), "Demo Portfolio")));
        String token = jwtService.generateToken(demo);

        mockMvc.perform(delete("/api/account")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Password123!")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", containsString("demo account")));

        assertTrue(userRepository.findByEmail(DataSeeder.DEMO_EMAIL).isPresent());
    }
}
