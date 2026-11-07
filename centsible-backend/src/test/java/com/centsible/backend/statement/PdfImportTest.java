package com.centsible.backend.statement;

import com.centsible.backend.dto.ImportResultDTO;
import com.centsible.backend.model.User;
import com.centsible.backend.repository.TransactionRepository;
import com.centsible.backend.repository.UserRepository;
import com.centsible.backend.service.CategorizationService;
import com.centsible.backend.service.TransactionImportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end PDF import: a statement PDF goes in through the same service the upload
 * endpoint uses and the right transactions, with the right signs, come out the other side.
 * The statements are made up (see SyntheticStatements); the layouts mirror three Sri Lankan banks.
 */
@SpringBootTest
public class PdfImportTest {

    @Autowired
    private TransactionImportService importService;

    @Autowired
    private CategorizationService categorizationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private User freshUser() {
        User user = userRepository.save(
                new User("pdf_" + System.nanoTime() + "@centsible.local", "hashedpassword", "PDF Tester"));
        categorizationService.seedDefaultCategoriesAndRules(user);
        return user;
    }

    private static MockMultipartFile pdf(byte[] bytes) {
        return new MockMultipartFile("file", "statement.pdf", "application/pdf", bytes);
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "Expected " + expected + " but was " + actual);
    }

    @Test
    public void importsSampathStyleStatementAndSkipsItOnSecondUpload() throws Exception {
        User user = freshUser();
        byte[] statement = SyntheticStatements.sampathStyle();

        ImportResultDTO first = importService.importPdf(pdf(statement), user);
        assertEquals(5, first.getImported());
        assertEquals(2, first.getIncomeCount());
        assertEquals(3, first.getExpenseCount());
        assertMoney("250154.46", first.getTotalIncome());
        assertMoney("57350.50", first.getTotalExpense());
        assertEquals(5L, transactionRepository.countByUserId(user.getId()));

        ImportResultDTO second = importService.importPdf(pdf(statement), user);
        assertEquals(0, second.getImported());
        assertEquals(5, second.getDuplicateCount());
    }

    @Test
    public void importsSeylanStyleStatement() throws Exception {
        User user = freshUser();

        ImportResultDTO result = importService.importPdf(pdf(SyntheticStatements.seylanStyle()), user);

        assertEquals(6, result.getImported());
        assertEquals(3, result.getIncomeCount());
        assertEquals(3, result.getExpenseCount());
        assertMoney("5011.00", result.getTotalIncome());
        assertMoney("601.10", result.getTotalExpense());
    }

    @Test
    public void importsCommercialStyleStatement() throws Exception {
        User user = freshUser();

        ImportResultDTO result = importService.importPdf(pdf(SyntheticStatements.commercialStyle()), user);

        assertEquals(4, result.getImported());
        assertEquals(1, result.getIncomeCount());
        assertEquals(3, result.getExpenseCount());
        assertMoney("120.25", result.getTotalIncome());
        assertMoney("2062.53", result.getTotalExpense());
    }

    @Test
    public void aPdfWithNoReadableTextGetsAClearMessageInsteadOfSilence() throws Exception {
        User user = freshUser();

        ImportResultDTO result = importService.importPdf(pdf(SyntheticStatements.noText()), user);

        assertEquals(0, result.getImported());
        assertTrue(result.getParseErrors().stream().anyMatch(m -> m.contains("No transactions were found")),
                "Expected a helpful message but got " + result.getParseErrors());
    }

    @Test
    public void aFileThatIsNotReallyAPdfDoesNotCrashTheImport() {
        User user = freshUser();

        ImportResultDTO result = importService.importPdf(pdf("this is not a pdf".getBytes()), user);

        assertEquals(0, result.getImported());
        assertTrue(!result.getParseErrors().isEmpty());
    }
}
