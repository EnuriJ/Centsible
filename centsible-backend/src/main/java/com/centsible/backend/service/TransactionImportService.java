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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TransactionImportService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public TransactionImportService(TransactionRepository transactionRepository,
                                     CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Expected CSV columns: date, description, amount, category
     * - date: ISO format, e.g. 2026-08-01
     * - amount: negative for expenses, positive for income
     * - category: created automatically if it doesn't already exist
     * <p>
     * Bad individual rows are skipped and reported rather than failing the
     * whole import - a single typo shouldn't block 200 good rows.
     */
    public ImportResultDTO importCsv(MultipartFile file) {
        int imported = 0;
        List<String> errors = new ArrayList<>();

        // Cache categories we've already looked up/created this request, so a
        // CSV with 100 "Groceries" rows only hits the DB for that category once.
        Map<String, Category> categoryCache = new HashMap<>();

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .setIgnoreEmptyLines(true)
                    .build()
                    .parse(reader);

            for (CSVRecord record : parser) {
                try {
                    LocalDate date = LocalDate.parse(record.get("date").trim());
                    String description = record.get("description").trim();
                    BigDecimal amount = new BigDecimal(record.get("amount").trim());
                    String categoryName = record.get("category").trim();

                    if (description.isEmpty() || categoryName.isEmpty()) {
                        throw new IllegalArgumentException("description and category cannot be empty");
                    }

                    Category category = categoryCache.computeIfAbsent(categoryName, name -> {
                        Category existing = categoryRepository.findByName(name);
                        return existing != null ? existing : categoryRepository.save(new Category(name));
                    });

                    boolean isDuplicate = transactionRepository
                            .existsByDateAndDescriptionAndAmountAndCategory(date, description, amount, category);
                    if (isDuplicate) {
                        errors.add("Row " + record.getRecordNumber() + ": duplicate transaction (already imported), skipped");
                        continue;
                    }

                    transactionRepository.save(new Transaction(date, description, amount, category));
                    imported++;
                } catch (Exception rowError) {
                    errors.add("Row " + record.getRecordNumber() + ": " + rowError.getMessage());
                }
            }
        } catch (IOException e) {
            errors.add("Could not read the uploaded file: " + e.getMessage());
        }

        return new ImportResultDTO(imported, errors.size(), errors);
    }
}
