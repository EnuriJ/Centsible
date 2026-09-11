package com.centsible.backend.service;

import com.centsible.backend.dto.CategorySpendDTO;
import com.centsible.backend.dto.MonthlySpendDTO;
import com.centsible.backend.model.Transaction;
import com.centsible.backend.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final TransactionRepository transactionRepository;

    public AnalyticsService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<CategorySpendDTO> getSpendByCategory(LocalDate startDate, LocalDate endDate) {
        return transactionRepository.aggregateSpendByCategory(startDate, endDate).stream()
                .map(p -> new CategorySpendDTO(p.getCategory(), p.getTotalSpent()))
                .collect(Collectors.toList());
    }

    public List<MonthlySpendDTO> getSpendOverTime(LocalDate startDate, LocalDate endDate, String category) {
        List<Transaction> expenses = transactionRepository
                .findByAmountLessThanAndDateBetweenOrderByDateAsc(BigDecimal.ZERO, startDate, endDate);

        if (category != null && !category.isBlank()) {
            expenses = expenses.stream()
                    .filter(t -> t.getCategory() != null && t.getCategory().getName().equalsIgnoreCase(category))
                    .collect(Collectors.toList());
        }

        // TreeMap keeps months in chronological order automatically, since
        // YearMonth is naturally comparable (2026-06 < 2026-07).
        Map<YearMonth, BigDecimal> byMonth = new TreeMap<>();
        for (Transaction t : expenses) {
            YearMonth month = YearMonth.from(t.getDate());
            BigDecimal spent = t.getAmount().abs();
            byMonth.merge(month, spent, BigDecimal::add);
        }

        return byMonth.entrySet().stream()
                .map(e -> new MonthlySpendDTO(e.getKey().toString(), e.getValue()))
                .collect(Collectors.toList());
    }
}
