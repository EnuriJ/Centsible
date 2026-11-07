package com.centsible.backend.statement;

import com.centsible.backend.service.statement.ParsedStatement;
import com.centsible.backend.service.statement.ParsedTransaction;
import com.centsible.backend.service.statement.PdfWordExtractor;
import com.centsible.backend.service.statement.StatementTableParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plain unit tests (no Spring): PDF in, transactions out, for each statement layout.
 * Every statement is checked for correct dates, descriptions and money-in / money-out signs,
 * and for being accepted without any "didn't add up" warnings.
 */
public class StatementPdfReadingTest {

    private static ParsedStatement read(byte[] pdf) throws IOException {
        return StatementTableParser.parse(PdfWordExtractor.extract(pdf));
    }

    private static void assertRow(ParsedTransaction t, String date, String descriptionContains, String amount) {
        assertEquals(LocalDate.parse(date), t.date());
        assertTrue(t.description().contains(descriptionContains),
                "Expected description to contain '" + descriptionContains + "' but was '" + t.description() + "'");
        assertEquals(0, new BigDecimal(amount).compareTo(t.amount()),
                "Amount for '" + descriptionContains + "' should be " + amount + " but was " + t.amount());
    }

    @Test
    public void readsSampathStyleStatementWithSignedAmountsAndNoRepeatedHeading() throws Exception {
        ParsedStatement statement = read(SyntheticStatements.sampathStyle());
        List<ParsedTransaction> rows = statement.transactions();

        assertEquals(5, rows.size(), "Rows on page 2 (no heading) must still be read");
        assertRow(rows.get(0), "2026-03-05", "Grocery store", "-4250.00");
        assertRow(rows.get(1), "2026-03-08", "Salary March", "250000.00");
        assertRow(rows.get(2), "2026-03-12", "Electricity bill", "-3100.50");
        assertRow(rows.get(3), "2026-03-20", "Transfer to savings", "-50000.00");
        assertRow(rows.get(4), "2026-03-28", "Int.Pd", "154.46");
        assertTrue(statement.notes().isEmpty(), "Everything reconciles, so no warnings: " + statement.notes());
    }

    @Test
    public void ignoresPageFootersInsteadOfAppendingThemToDescriptions() throws Exception {
        for (ParsedTransaction t : read(SyntheticStatements.sampathStyle()).transactions()) {
            assertTrue(!t.description().contains("Company No"), "Footer leaked into: " + t.description());
        }
    }

    @Test
    public void readsSeylanStyleStatementWrappedHeadingNewestFirstAndRepeatedHeading() throws Exception {
        ParsedStatement statement = read(SyntheticStatements.seylanStyle());
        List<ParsedTransaction> rows = statement.transactions();

        assertEquals(6, rows.size());
        // Kept in the order printed (newest first)
        assertRow(rows.get(0), "2026-02-28", "TAX", "-0.60");
        assertRow(rows.get(1), "2026-02-28", "INTEREST", "6.00");
        assertRow(rows.get(2), "2026-02-15", "Funds transfer from savings", "5000.00");
        assertRow(rows.get(3), "2026-02-10", "CARD FEES ANNUAL", "-600.00");
        assertRow(rows.get(4), "2026-02-02", "TAX", "-0.50");
        assertRow(rows.get(5), "2026-02-02", "INTEREST", "5.00");
        assertTrue(statement.notes().isEmpty(), "Everything reconciles, so no warnings: " + statement.notes());
    }

    @Test
    public void joinsDescriptionsThatWrapOntoASecondLine() throws Exception {
        ParsedTransaction fee = read(SyntheticStatements.seylanStyle()).transactions().get(3);
        assertTrue(fee.description().contains("REF 99887766"), "Wrapped line missing: " + fee.description());
    }

    @Test
    public void readsCommercialStyleStatementWithPaymentsAndReceiptsColumns() throws Exception {
        ParsedStatement statement = read(SyntheticStatements.commercialStyle());
        List<ParsedTransaction> rows = statement.transactions();

        assertEquals(4, rows.size(), "The opening balance line is not a transaction");
        // The effective date (second date column) is preferred, as before
        assertRow(rows.get(0), "2026-02-01", "COFFEE SHOP", "-1250.00");
        assertRow(rows.get(1), "2026-02-09", "TAXI RIDE", "-800.50");
        assertRow(rows.get(2), "2026-02-28", "INTEREST", "120.25");
        assertRow(rows.get(3), "2026-02-28", "ADVANCE INCOME TAX", "-12.03");
        assertTrue(statement.notes().isEmpty(), "Everything reconciles, so no warnings: " + statement.notes());
    }

    @Test
    public void worksOutDirectionFromRunningBalanceWhenAmountsCarryNoMarker() throws Exception {
        List<ParsedTransaction> rows = read(SyntheticStatements.unmarkedAmounts()).transactions();

        assertEquals(2, rows.size());
        assertRow(rows.get(0), "2026-04-01", "Corner shop", "-200.00");
        assertRow(rows.get(1), "2026-04-02", "Refund", "50.00");
    }

    @Test
    public void reportsRowsThatDoNotAddUpInsteadOfImportingThemSilently() throws Exception {
        // Corrupt the third running balance: 342,649.50 -> 342,000.00
        ParsedStatement statement = read(
                SyntheticStatements.sampathStyle("95,750.00", "345,750.00", "342,000.00", "292,649.50", "292,803.96"));

        assertEquals(5, statement.transactions().size());
        assertTrue(statement.notes().stream().anyMatch(n -> n.contains("didn't add up")),
                "A mismatch must be reported: " + statement.notes());
    }

    @Test
    public void aPdfWithNoTextProducesNoTransactions() throws Exception {
        assertTrue(read(SyntheticStatements.noText()).transactions().isEmpty());
    }

    @Test
    public void parsesTheDateFormatsBanksUse() {
        assertEquals(LocalDate.parse("2026-10-13"), StatementTableParser.parseDate("13-10-2026"));
        assertEquals(LocalDate.parse("2026-10-13"), StatementTableParser.parseDate("13/10/2026"));
        assertEquals(LocalDate.parse("2026-10-13"), StatementTableParser.parseDate("13/10/26"));
        assertEquals(LocalDate.parse("2026-10-13"), StatementTableParser.parseDate("13.10.2026"));
        assertEquals(LocalDate.parse("2026-10-13"), StatementTableParser.parseDate("13 Oct 2026"));
        assertEquals(LocalDate.parse("2026-10-13"), StatementTableParser.parseDate("13-Oct-2026"));
        assertEquals(LocalDate.parse("2026-10-13"), StatementTableParser.parseDate("2026-10-13"));
        assertEquals(LocalDate.parse("2026-01-02"), StatementTableParser.parseDate("2026 Jan 02"));
        assertEquals(LocalDate.parse("2026-10-13"), StatementTableParser.parseDate("13-10-2026 07:16:01"));
        assertNull(StatementTableParser.parseDate("Temple construction"));
        assertNull(StatementTableParser.parseDate("1,200.00"));
        assertNull(StatementTableParser.parseDate("31-02-2026"));
    }
}
