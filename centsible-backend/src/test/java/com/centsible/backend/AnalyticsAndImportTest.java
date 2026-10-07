package com.centsible.backend;

import com.centsible.backend.dto.FinancialSummaryDTO;
import com.centsible.backend.dto.ImportResultDTO;
import com.centsible.backend.model.Category;
import com.centsible.backend.model.CategoryRule;
import com.centsible.backend.model.Transaction;
import com.centsible.backend.model.User;
import com.centsible.backend.repository.CategoryRepository;
import com.centsible.backend.repository.TransactionRepository;
import com.centsible.backend.repository.UserRepository;
import com.centsible.backend.service.AnalyticsService;
import com.centsible.backend.service.CategorizationService;
import com.centsible.backend.service.TransactionImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AnalyticsAndImportTest {

    @Autowired
    private TransactionImportService transactionImportService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private CategorizationService categorizationService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    public void setUp() {
        testUser = userRepository.findByEmail("test@centsible.local")
                .orElseGet(() -> userRepository.save(new User("test@centsible.local", "hashedpassword", "Test User")));
        categorizationService.seedDefaultCategoriesAndRules(testUser);
    }

    @Test
    public void testAutoCategorization() {
        Category baristaCat = categorizationService.categorize("PURCHASE BARISTA NAWAM MAWAT", testUser);
        assertEquals("Dining Out", baristaCat.getName());

        Category pickMeCat = categorizationService.categorize("PURCHASE PICKME RIDE", testUser);
        assertEquals("Transport", pickMeCat.getName());

        Category interestCat = categorizationService.categorize("INTEREST", testUser);
        assertEquals("Bank Interest", interestCat.getName());

        Category taxCat = categorizationService.categorize("ADVANCE INCOME TAX", testUser);
        assertEquals("Bank Taxes & Fees", taxCat.getName());

        Category unknownCat = categorizationService.categorize("RANDOM UNKNOWN STORE 9999", testUser);
        assertEquals("Uncategorized", unknownCat.getName());
    }

    @Test
    public void testCsvWithPaymentsAndReceiptsAndEffectiveDate() {
        String csvContent = "Posting Date,Effective Date,Particulars,Payments,Receipts,Balance\n" +
                "BALANCE AS OF 31/07/26,,,\"116,421.01\"\n" +
                "04 Aug 2026,01 Aug 2026,PURCHASE BARISTA NAWAM MAWAT,\"2,629.00\",,\"113,792.01\"\n" +
                "11 Aug 2026,10 Aug 2026,PURCHASE PICKME RIDE,792.87,,\"112,999.14\"\n" +
                "31 Aug 2026,31 Aug 2026,INTEREST,,207.71,\"97,941.08\"\n" +
                "31 Aug 2026,31 Aug 2026,ADVANCE INCOME TAX,20.77,,\"97,920.31\"\n" +
                "CLOSING BALANCE AS OF 31/08/26,,,,\"97,920.31\"\n" +
                "TOTAL DEPOSITS 1 ITEMS,,,207.71\n";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "combank_statement.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        ImportResultDTO result = transactionImportService.importCsv(file, testUser);

        assertTrue(result.getImported() >= 4, "Should import 4 real transactions");
        assertTrue(result.getIncomeCount() >= 1, "Should detect 1 receipt/income (Interest)");
        assertTrue(result.getExpenseCount() >= 3, "Should detect 3 payments/expenses");

        List<Transaction> baristaTx = transactionRepository.findByUserId(testUser.getId()).stream()
                .filter(t -> t.getDescription().contains("BARISTA"))
                .toList();
        assertFalse(baristaTx.isEmpty());
        assertEquals(LocalDate.of(2026, 8, 1), baristaTx.get(0).getDate());
        assertEquals(new BigDecimal("-2629.00"), baristaTx.get(0).getAmount());
        assertEquals("Dining Out", baristaTx.get(0).getCategory().getName());
    }

    @Test
    public void testRuleLearningAndBackfill() {
        Category uncategorized = categorizationService.getOrCreateCategory("Uncategorized", testUser);
        Transaction t1 = transactionRepository.save(
                new Transaction(LocalDate.of(2026, 8, 20), "SPAR SUPERMARKET THALAHENA", new BigDecimal("-4500.00"), uncategorized, testUser)
        );

        CategoryRule rule = categorizationService.learnRule("SPAR", "Groceries", true, testUser);
        assertNotNull(rule);
        assertEquals("SPAR", rule.getKeyword());
        assertEquals("Groceries", rule.getCategory().getName());

        Transaction updated = transactionRepository.findByIdAndUserId(t1.getId(), testUser.getId()).orElseThrow();
        assertEquals("Groceries", updated.getCategory().getName());

        Category future = categorizationService.categorize("PURCHASE SPAR BREAD", testUser);
        assertEquals("Groceries", future.getName());
    }

    @Test
    public void testFinancialSummaryInLkr() {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 31);

        FinancialSummaryDTO summary = analyticsService.getFinancialSummary(testUser.getId(), start, end, null);

        assertNotNull(summary);
        assertNotNull(summary.getNarrative());
        assertTrue(summary.getNarrative().contains("Rs."), "Narrative should format currency as Rs. in LKR");
    }

    @Test
    public void testDuplicateAndParseErrorSeparation() {
        String csvContent = "Posting Date,Effective Date,Particulars,Payments,Receipts,Balance\n" +
                "15 Sep 2026,15 Sep 2026,SUPERMARKET ONE,1,500.00,,50,000.00\n" +
                "16 Sep 2026,16 Sep 2026,SALARY CREDIT,,75,000.00,125,000.00\n" +
                "17 Sep 2026,17 Sep 2026,COFFEE SHOP,850.00,,124,150.00\n";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test_statement.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        ImportResultDTO firstPass = transactionImportService.importCsv(file, testUser);
        int initialImported = firstPass.getImported();
        assertTrue(initialImported > 0, "First pass should import transactions");
        assertEquals(0, firstPass.getDuplicateCount(), "First pass should have 0 duplicates");
        assertTrue(firstPass.getParseErrors().isEmpty(), "First pass should have no parse errors");

        ImportResultDTO secondPass = transactionImportService.importCsv(file, testUser);
        assertEquals(0, secondPass.getImported(), "Second pass should import 0 transactions");
        assertEquals(initialImported, secondPass.getDuplicateCount(), "Second pass duplicateCount must dynamically match first pass imported count");
        assertTrue(secondPass.getParseErrors().isEmpty(), "Second pass should have no parse errors");

        String mixedContent = "Posting Date,Effective Date,Particulars,Payments,Receipts,Balance\n" +
                "15 Sep 2026,15 Sep 2026,SUPERMARKET ONE,1,500.00,,50,000.00\n" +
                "18 Sep 2026,18 Sep 2026,NEW BOOKSTORE,2,200.00,,121,950.00\n" +
                "19 Sep 2026,19 Sep 2026,BROKEN ROW,NOT_AN_AMOUNT,,121,950.00\n";

        MockMultipartFile mixedFile = new MockMultipartFile(
                "file",
                "mixed_statement.csv",
                "text/csv",
                mixedContent.getBytes(StandardCharsets.UTF_8)
        );

        ImportResultDTO mixedResult = transactionImportService.importCsv(mixedFile, testUser);
        assertEquals(1, mixedResult.getImported(), "Should import the 1 new transaction");
        assertEquals(1, mixedResult.getDuplicateCount(), "Should detect the 1 duplicate transaction");
        assertEquals(1, mixedResult.getParseErrors().size(), "Should record the 1 row parse error");
        assertTrue(mixedResult.getParseErrors().get(0).contains("Row 3"), "Parse error should mention Row 3");
    }
}
