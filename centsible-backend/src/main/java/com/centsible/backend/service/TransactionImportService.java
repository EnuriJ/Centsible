package com.centsible.backend.service;

import com.centsible.backend.dto.ImportResultDTO;
import com.centsible.backend.model.Category;
import com.centsible.backend.model.Transaction;
import com.centsible.backend.repository.CategoryRepository;
import com.centsible.backend.repository.TransactionRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TransactionImportService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final CategorizationService categorizationService;

    // Supported date formatters
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US),
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US),
            DateTimeFormatter.ofPattern("dd/MM/yy"),
            DateTimeFormatter.ofPattern("d/M/yy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE
    };

    // Regex for matching lines in bank PDF statements like Commercial Bank of Ceylon:
    // e.g. "04 Aug 2026 01 Aug 2026 PURCHASE BARISTA NAWAM MAWAT 2,629.00 113,792.01"
    private static final Pattern STATEMENT_LINE_PATTERN = Pattern.compile(
            "^(\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4}|\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+" + // Posting Date
            "(\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4}|\\d{1,2}/\\d{1,2}/\\d{2,4})\\s+" + // Effective Date
            "(.+?)\\s+" +                                                          // Description / Particulars
            "([0-9,]+\\.\\d{2})(?:\\s+([0-9,]+\\.\\d{2}))?$"                       // Amount and optional Balance
    );

    public TransactionImportService(TransactionRepository transactionRepository,
                                     CategoryRepository categoryRepository,
                                     CategorizationService categorizationService) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.categorizationService = categorizationService;
    }

    /**
     * Ingests bank statements from either CSV or PDF files.
     */
    public ImportResultDTO importStatement(MultipartFile file) {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (filename.endsWith(".pdf") || "application/pdf".equalsIgnoreCase(file.getContentType())) {
            return importPdf(file);
        } else {
            return importCsv(file);
        }
    }

    /**
     * CSV Ingestion handling both simple 4-column CSVs and full bank exports
     * with separate Payments and Receipts columns, Effective Dates, and formatted numbers.
     */
    public ImportResultDTO importCsv(MultipartFile file) {
        int imported = 0;
        int incomeCount = 0;
        int expenseCount = 0;
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        Set<String> encounteredCategories = new HashSet<>();
        List<String> errors = new ArrayList<>();
        Map<String, Category> categoryCache = new HashMap<>();

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .setIgnoreEmptyLines(true)
                    .build()
                    .parse(reader);

            Map<String, Integer> headerMap = parser.getHeaderMap();

            for (CSVRecord record : parser) {
                try {
                    // Check if row is a non-transaction summary/balance row
                    String rawRow = String.join(" ", record.toList()).toUpperCase();
                    if (isNonTransactionRow(rawRow)) {
                        continue;
                    }

                    // 1. Resolve Date (Prioritize Effective Date over Posting Date)
                    LocalDate date = resolveDate(record, headerMap);
                    if (date == null) {
                        continue; // skip rows without a valid date
                    }

                    // 2. Resolve Description
                    String description = resolveDescription(record, headerMap);
                    if (description == null || description.isBlank()) {
                        continue;
                    }

                    // 3. Resolve Amount (Handles Payments vs Receipts or single amount)
                    BigDecimal amount = resolveAmount(record, headerMap);
                    if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
                        continue;
                    }

                    // 4. Resolve Category (explicit category or auto-categorize with rules engine)
                    Category category = resolveCategory(record, headerMap, description, categoryCache);

                    // 5. Deduplication check
                    boolean isDuplicate = transactionRepository
                            .existsByDateAndDescriptionAndAmountAndCategory(date, description, amount, category);
                    if (isDuplicate) {
                        errors.add("Row " + record.getRecordNumber() + ": duplicate transaction skipped (" + description + ")");
                        continue;
                    }

                    // 6. Persist
                    transactionRepository.save(new Transaction(date, description, amount, category));
                    imported++;
                    encounteredCategories.add(category.getName());

                    if (amount.compareTo(BigDecimal.ZERO) > 0) {
                        incomeCount++;
                        totalIncome = totalIncome.add(amount);
                    } else {
                        expenseCount++;
                        totalExpense = totalExpense.add(amount.abs());
                    }

                } catch (Exception rowError) {
                    errors.add("Row " + record.getRecordNumber() + ": " + rowError.getMessage());
                }
            }
        } catch (IOException e) {
            errors.add("Could not read uploaded CSV file: " + e.getMessage());
        }

        return new ImportResultDTO(
                imported,
                errors.size(),
                errors,
                incomeCount,
                expenseCount,
                totalIncome,
                totalExpense,
                encounteredCategories.size()
        );
    }

    /**
     * Ingests transactions directly from a PDF bank statement (e.g. Commercial Bank of Ceylon).
     */
    public ImportResultDTO importPdf(MultipartFile file) {
        int imported = 0;
        int incomeCount = 0;
        int expenseCount = 0;
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        Set<String> encounteredCategories = new HashSet<>();
        List<String> errors = new ArrayList<>();

        try {
            byte[] pdfBytes = file.getBytes();
            List<String> lines = PdfStatementExtractor.extractLines(pdfBytes);

            BigDecimal previousBalance = null;

            for (int lineNum = 0; lineNum < lines.size(); lineNum++) {
                String line = lines.get(lineNum).trim();

                // Skip non-transaction rows (balances, totals, headers)
                if (isNonTransactionRow(line.toUpperCase())) {
                    // Try to grab initial balance if it says BALANCE AS OF
                    if (line.toUpperCase().contains("BALANCE AS OF")) {
                        Matcher m = Pattern.compile("([0-9,]+\\.\\d{2})").matcher(line);
                        if (m.find()) {
                            previousBalance = parseAmountString(m.group(1));
                        }
                    }
                    continue;
                }

                Matcher matcher = STATEMENT_LINE_PATTERN.matcher(line);
                if (!matcher.matches()) {
                    continue;
                }

                try {
                    String postingDateStr = matcher.group(1);
                    String effectiveDateStr = matcher.group(2);
                    String description = matcher.group(3).trim();
                    String firstNumStr = matcher.group(4);
                    String secondNumStr = matcher.group(5);

                    // Prefer Effective Date over Posting Date
                    LocalDate date = parseFlexibleDate(effectiveDateStr);
                    if (date == null) {
                        date = parseFlexibleDate(postingDateStr);
                    }
                    if (date == null) continue;

                    BigDecimal transactionAmount;
                    BigDecimal currentBalance = null;

                    if (secondNumStr != null) {
                        // firstNum is transaction amount, secondNum is Balance
                        BigDecimal parsedVal = parseAmountString(firstNumStr);
                        currentBalance = parseAmountString(secondNumStr);

                        if (previousBalance != null && currentBalance != null) {
                            BigDecimal balanceDiff = currentBalance.subtract(previousBalance);
                            transactionAmount = balanceDiff;
                        } else {
                            // If previous balance wasn't tracked, infer from description
                            if (isIncomeDescription(description)) {
                                transactionAmount = parsedVal;
                            } else {
                                transactionAmount = parsedVal.negate();
                            }
                        }
                        previousBalance = currentBalance;
                    } else {
                        BigDecimal parsedVal = parseAmountString(firstNumStr);
                        transactionAmount = isIncomeDescription(description) ? parsedVal : parsedVal.negate();
                    }

                    // Auto-categorize
                    Category category = categorizationService.categorize(description);

                    // Deduplication check
                    boolean isDuplicate = transactionRepository
                            .existsByDateAndDescriptionAndAmountAndCategory(date, description, transactionAmount, category);
                    if (isDuplicate) {
                        errors.add("Line " + (lineNum + 1) + ": duplicate transaction skipped (" + description + ")");
                        continue;
                    }

                    transactionRepository.save(new Transaction(date, description, transactionAmount, category));
                    imported++;
                    encounteredCategories.add(category.getName());

                    if (transactionAmount.compareTo(BigDecimal.ZERO) > 0) {
                        incomeCount++;
                        totalIncome = totalIncome.add(transactionAmount);
                    } else {
                        expenseCount++;
                        totalExpense = totalExpense.add(transactionAmount.abs());
                    }

                } catch (Exception parseErr) {
                    errors.add("Line " + (lineNum + 1) + ": " + parseErr.getMessage());
                }
            }

        } catch (Exception e) {
            errors.add("Could not process uploaded PDF: " + e.getMessage());
        }

        return new ImportResultDTO(
                imported,
                errors.size(),
                errors,
                incomeCount,
                expenseCount,
                totalIncome,
                totalExpense,
                encounteredCategories.size()
        );
    }

    public static boolean isNonTransactionRow(String upper) {
        if (upper == null || upper.isBlank()) return true;
        return upper.contains("BALANCE AS OF") ||
                upper.contains("CLOSING BALANCE") ||
                upper.contains("OPENING BALANCE") ||
                upper.contains("TOTAL DEPOSITS") ||
                upper.contains("TOTAL WITHDRAWALS") ||
                upper.contains("INTEREST EARNED") ||
                upper.contains("INTEREST WITHHELD") ||
                upper.contains("* INCLUDES ADVANCE") ||
                upper.contains("STATEMENT OF ACCOUNT") ||
                upper.contains("STATEMENT DATE") ||
                upper.contains("ACCOUNT NO") ||
                upper.contains("PAGE 1") ||
                upper.contains("PAGE 2") ||
                upper.contains("POSTING DATE") ||
                upper.contains("EFFECTIVE DATE") ||
                upper.contains("THE ITEMS AND BALANCES");
    }

    private static boolean isIncomeDescription(String desc) {
        String u = desc.toUpperCase();
        return u.contains("INTEREST") && !u.contains("TAX") ||
                u.contains("DEPOSIT") ||
                u.contains("SALARY") ||
                u.contains("PAYCHECK") ||
                u.contains("RECEIPT") ||
                u.contains("CREDIT");
    }

    private LocalDate resolveDate(CSVRecord record, Map<String, Integer> headers) {
        // Look for Effective Date first
        for (String h : headers.keySet()) {
            if (h.equalsIgnoreCase("Effective Date") || h.equalsIgnoreCase("EffectiveDate") || h.equalsIgnoreCase("Value Date")) {
                LocalDate d = parseFlexibleDate(record.get(h));
                if (d != null) return d;
            }
        }
        // Fallback to Posting Date or standard Date
        for (String h : headers.keySet()) {
            if (h.equalsIgnoreCase("Date") || h.equalsIgnoreCase("Posting Date") || h.equalsIgnoreCase("PostingDate") || h.equalsIgnoreCase("Txn Date")) {
                LocalDate d = parseFlexibleDate(record.get(h));
                if (d != null) return d;
            }
        }
        return null;
    }

    private String resolveDescription(CSVRecord record, Map<String, Integer> headers) {
        for (String h : headers.keySet()) {
            if (h.equalsIgnoreCase("Particulars") || h.equalsIgnoreCase("Description") ||
                    h.equalsIgnoreCase("Narrative") || h.equalsIgnoreCase("Memo") || h.equalsIgnoreCase("Details")) {
                return record.get(h).trim();
            }
        }
        return null;
    }

    private BigDecimal resolveAmount(CSVRecord record, Map<String, Integer> headers) {
        // 1. Separate Payments and Receipts
        String paymentStr = null;
        String receiptStr = null;

        for (String h : headers.keySet()) {
            String lower = h.toLowerCase().trim();
            if (lower.equals("payments") || lower.equals("payment") || lower.equals("debit") || lower.equals("withdrawal") || lower.equals("withdrawals")) {
                paymentStr = record.get(h);
            } else if (lower.equals("receipts") || lower.equals("receipt") || lower.equals("credit") || lower.equals("deposit") || lower.equals("deposits")) {
                receiptStr = record.get(h);
            }
        }

        BigDecimal payment = parseAmountString(paymentStr);
        BigDecimal receipt = parseAmountString(receiptStr);

        if (receipt != null && receipt.compareTo(BigDecimal.ZERO) > 0) {
            return receipt; // positive (income)
        }
        if (payment != null && payment.compareTo(BigDecimal.ZERO) > 0) {
            return payment.negate(); // negative (expense)
        }

        // 2. Single Amount column
        for (String h : headers.keySet()) {
            if (h.equalsIgnoreCase("Amount") || h.equalsIgnoreCase("Total")) {
                return parseAmountString(record.get(h));
            }
        }

        return null;
    }

    private Category resolveCategory(CSVRecord record, Map<String, Integer> headers, String description, Map<String, Category> cache) {
        // If the CSV provides a category column with value, use it
        for (String h : headers.keySet()) {
            if (h.equalsIgnoreCase("Category")) {
                String catStr = record.get(h).trim();
                if (!catStr.isEmpty()) {
                    return cache.computeIfAbsent(catStr, name -> {
                        Category existing = categoryRepository.findByName(name);
                        return existing != null ? existing : categoryRepository.save(new Category(name));
                    });
                }
            }
        }

        // Otherwise, run through keyword auto-categorization
        return categorizationService.categorize(description);
    }

    public static BigDecimal parseAmountString(String s) {
        if (s == null || s.isBlank()) return null;
        String cleaned = s.replace(",", "").replaceAll("(?i)LKR|Rs\\.?|\\$", "").trim();
        if (cleaned.isEmpty() || cleaned.equals("-")) return null;
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static LocalDate parseFlexibleDate(String s) {
        if (s == null || s.isBlank()) return null;
        String trimmed = s.trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(trimmed, formatter);
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
