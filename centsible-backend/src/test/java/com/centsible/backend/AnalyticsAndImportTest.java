package com.centsible.backend;

import com.centsible.backend.dto.CategoryIncomeDTO;
import com.centsible.backend.dto.FinancialSummaryDTO;
import com.centsible.backend.dto.ImportResultDTO;
import com.centsible.backend.dto.MonthlyCashFlowDTO;
import com.centsible.backend.model.Category;
import com.centsible.backend.model.CategoryRule;
import com.centsible.backend.model.Transaction;
import com.centsible.backend.repository.CategoryRepository;
import com.centsible.backend.repository.TransactionRepository;
import com.centsible.backend.service.AnalyticsService;
import com.centsible.backend.service.CategorizationService;
import com.centsible.backend.service.TransactionImportService;
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

    @Test
    public void testAutoCategorization() {
        Category baristaCat = categorizationService.categorize("PURCHASE BARISTA NAWAM MAWAT");
        assertEquals("Dining Out", baristaCat.getName());

        Category pickMeCat = categorizationService.categorize("PURCHASE PICKME RIDE");
        assertEquals("Transport", pickMeCat.getName());

        Category interestCat = categorizationService.categorize("INTEREST");
        assertEquals("Bank Interest", interestCat.getName());

        Category taxCat = categorizationService.categorize("ADVANCE INCOME TAX");
        assertEquals("Bank Taxes & Fees", taxCat.getName());

        Category unknownCat = categorizationService.categorize("RANDOM UNKNOWN STORE 9999");
        assertEquals("Uncategorized", unknownCat.getName());
    }

    @Test
    public void testCsvWithPaymentsAndReceiptsAndEffectiveDate() {
        String csvContent = "Posting Date,Effective Date,Particulars,Payments,Receipts,Balance\n" +
                "BALANCE AS OF 31/07/26,,,116,421.01\n" +
                "04 Aug 2026,01 Aug 2026,PURCHASE BARISTA NAWAM MAWAT,2,629.00,,113,792.01\n" +
                "11 Aug 2026,10 Aug 2026,PURCHASE PICKME RIDE,792.87,,112,999.14\n" +
                "31 Aug 2026,31 Aug 2026,INTEREST,,207.71,97,941.08\n" +
                "31 Aug 2026,31 Aug 2026,ADVANCE INCOME TAX,20.77,,97,920.31\n" +
                "CLOSING BALANCE AS OF 31/08/26,,,,97,920.31\n" +
                "TOTAL DEPOSITS 1 ITEMS,,,207.71\n";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "combank_statement.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        ImportResultDTO result = transactionImportService.importCsv(file);

        // 4 real transactions (Barista, Pickme, Interest, Tax), balance & totals rows ignored
        assertTrue(result.getImported() >= 4, "Should import 4 real transactions");
        assertTrue(result.getIncomeCount() >= 1, "Should detect 1 receipt/income (Interest)");
        assertTrue(result.getExpenseCount() >= 3, "Should detect 3 payments/expenses");

        // Verify Effective Date was used: Barista effective date is 2026-08-01 (not posting date 2026-08-04)
        List<Transaction> baristaTx = transactionRepository.findAll().stream()
                .filter(t -> t.getDescription().contains("BARISTA"))
                .toList();
        assertFalse(baristaTx.isEmpty());
        assertEquals(LocalDate.of(2026, 8, 1), baristaTx.get(0).getDate());
        assertEquals(new BigDecimal("-2629.00"), baristaTx.get(0).getAmount());
        assertEquals("Dining Out", baristaTx.get(0).getCategory().getName());
    }

    @Test
    public void testRuleLearningAndBackfill() {
        // Create an uncategorized transaction
        Category uncategorized = categorizationService.getOrCreateCategory("Uncategorized");
        Transaction t1 = transactionRepository.save(
                new Transaction(LocalDate.of(2026, 8, 20), "SPAR SUPERMARKET THALAHENA", new BigDecimal("-4500.00"), uncategorized)
        );

        // Learn rule: "SPAR" -> "Groceries"
        CategoryRule rule = categorizationService.learnRule("SPAR", "Groceries", true);
        assertNotNull(rule);
        assertEquals("SPAR", rule.getKeyword());
        assertEquals("Groceries", rule.getCategory().getName());

        // Verify transaction was backfilled
        Transaction updated = transactionRepository.findById(t1.getId()).orElseThrow();
        assertEquals("Groceries", updated.getCategory().getName());

        // Verify future transactions auto-categorize
        Category future = categorizationService.categorize("PURCHASE SPAR BREAD");
        assertEquals("Groceries", future.getName());
    }

    @Test
    public void testFinancialSummaryInLkr() {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 31);

        FinancialSummaryDTO summary = analyticsService.getFinancialSummary(start, end, null);

        assertNotNull(summary);
        assertNotNull(summary.getNarrative());
        assertTrue(summary.getNarrative().contains("Rs."), "Narrative should format currency as Rs. in LKR");
    }
}
