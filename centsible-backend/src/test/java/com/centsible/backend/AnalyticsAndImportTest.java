package com.centsible.backend;

import com.centsible.backend.dto.CategoryIncomeDTO;
import com.centsible.backend.dto.FinancialSummaryDTO;
import com.centsible.backend.dto.ImportResultDTO;
import com.centsible.backend.dto.MonthlyCashFlowDTO;
import com.centsible.backend.service.AnalyticsService;
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

    @Test
    public void testCsvImportWithIncomeAndExpenses() {
        String csvContent = "date,description,amount,category\n" +
                "2026-08-01,Client Project Alpha,1500.00,Freelance\n" +
                "2026-08-02,Trader Joes,-65.40,Groceries\n" +
                "2026-08-03,Monthly Subway Pass,-80.00,Transport\n" +
                "2026-08-15,Consulting Gig,750.00,Consulting\n";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "august_transactions.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        ImportResultDTO result = transactionImportService.importCsv(file);

        assertTrue(result.getImported() >= 4, "Should import all 4 valid rows");
        assertEquals(2, result.getIncomeCount(), "Should detect 2 income transactions");
        assertEquals(2, result.getExpenseCount(), "Should detect 2 expense transactions");
        assertEquals(new BigDecimal("2250.00"), result.getTotalIncome());
        assertEquals(new BigDecimal("145.40"), result.getTotalExpense());
    }

    @Test
    public void testFinancialSummaryAndNarrative() {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 7, 31);

        FinancialSummaryDTO summary = analyticsService.getFinancialSummary(start, end, null);

        assertNotNull(summary);
        assertTrue(summary.getTotalIncome().compareTo(BigDecimal.ZERO) > 0, "Seeded data should have income");
        assertTrue(summary.getTotalExpense().compareTo(BigDecimal.ZERO) > 0, "Seeded data should have expenses");
        assertNotNull(summary.getTopExpenseCategory());
        assertNotNull(summary.getNarrative());
        assertTrue(summary.getNarrative().contains("income"), "Narrative should mention income");
        assertTrue(summary.getNarrative().contains("expenses totaled"), "Narrative should mention expenses");
    }

    @Test
    public void testCashFlowOverTime() {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 7, 31);

        List<MonthlyCashFlowDTO> cashFlow = analyticsService.getCashFlowOverTime(start, end, null);

        assertNotNull(cashFlow);
        assertFalse(cashFlow.isEmpty(), "Cash flow should have monthly data points");
        for (MonthlyCashFlowDTO item : cashFlow) {
            assertNotNull(item.getMonth());
            assertNotNull(item.getTotalIncome());
            assertNotNull(item.getTotalExpense());
            assertNotNull(item.getNetSavings());
        }
    }

    @Test
    public void testIncomeByCategory() {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 7, 31);

        List<CategoryIncomeDTO> incomeByCat = analyticsService.getIncomeByCategory(start, end);

        assertNotNull(incomeByCat);
        assertFalse(incomeByCat.isEmpty(), "Should have seeded income category");
        assertEquals("Income", incomeByCat.get(0).getCategory());
        assertTrue(incomeByCat.get(0).getTotalIncome().compareTo(BigDecimal.ZERO) > 0);
    }
}
