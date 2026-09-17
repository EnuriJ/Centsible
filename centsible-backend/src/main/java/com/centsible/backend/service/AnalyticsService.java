package com.centsible.backend.service;

import com.centsible.backend.dto.CategoryIncomeDTO;
import com.centsible.backend.dto.CategorySpendDTO;
import com.centsible.backend.dto.FinancialSummaryDTO;
import com.centsible.backend.dto.MonthlyCashFlowDTO;
import com.centsible.backend.dto.MonthlySpendDTO;
import com.centsible.backend.model.Transaction;
import com.centsible.backend.repository.CategoryIncomeProjection;
import com.centsible.backend.repository.CategorySpendProjection;
import com.centsible.backend.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US);
    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US);

    private final TransactionRepository transactionRepository;

    public AnalyticsService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<CategorySpendDTO> getSpendByCategory(LocalDate startDate, LocalDate endDate) {
        return transactionRepository.aggregateSpendByCategory(startDate, endDate).stream()
                .map(p -> new CategorySpendDTO(p.getCategory(), p.getTotalSpent()))
                .collect(Collectors.toList());
    }

    public List<CategoryIncomeDTO> getIncomeByCategory(LocalDate startDate, LocalDate endDate) {
        List<CategoryIncomeProjection> projections = transactionRepository.aggregateIncomeByCategory(startDate, endDate);
        List<Transaction> incomeTransactions = transactionRepository
                .findByAmountGreaterThanAndDateBetweenOrderByDateAsc(BigDecimal.ZERO, startDate, endDate);

        Map<String, Integer> countByCategory = new HashMap<>();
        for (Transaction t : incomeTransactions) {
            String catName = t.getCategory() != null ? t.getCategory().getName() : "Uncategorized";
            countByCategory.put(catName, countByCategory.getOrDefault(catName, 0) + 1);
        }

        BigDecimal totalIncome = projections.stream()
                .map(CategoryIncomeProjection::getTotalIncome)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategoryIncomeDTO> result = new ArrayList<>();
        for (CategoryIncomeProjection p : projections) {
            double percentage = 0.0;
            if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
                percentage = p.getTotalIncome()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(totalIncome, 1, RoundingMode.HALF_UP)
                        .doubleValue();
            }
            int count = countByCategory.getOrDefault(p.getCategory(), 0);
            result.add(new CategoryIncomeDTO(p.getCategory(), p.getTotalIncome(), percentage, count));
        }

        return result;
    }

    public List<MonthlySpendDTO> getSpendOverTime(LocalDate startDate, LocalDate endDate, String category) {
        List<Transaction> expenses = transactionRepository
                .findByAmountLessThanAndDateBetweenOrderByDateAsc(BigDecimal.ZERO, startDate, endDate);

        if (category != null && !category.isBlank()) {
            expenses = expenses.stream()
                    .filter(t -> t.getCategory() != null && t.getCategory().getName().equalsIgnoreCase(category))
                    .collect(Collectors.toList());
        }

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

    public List<MonthlyCashFlowDTO> getCashFlowOverTime(LocalDate startDate, LocalDate endDate, String category) {
        List<Transaction> transactions = transactionRepository.findByDateBetweenOrderByDateAsc(startDate, endDate);

        if (category != null && !category.isBlank()) {
            transactions = transactions.stream()
                    .filter(t -> t.getCategory() != null && t.getCategory().getName().equalsIgnoreCase(category))
                    .collect(Collectors.toList());
        }

        Map<YearMonth, BigDecimal[]> byMonth = new TreeMap<>();
        // array index 0 = income, index 1 = expenses
        for (Transaction t : transactions) {
            YearMonth month = YearMonth.from(t.getDate());
            BigDecimal[] sums = byMonth.computeIfAbsent(month, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if (t.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                sums[0] = sums[0].add(t.getAmount());
            } else if (t.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                sums[1] = sums[1].add(t.getAmount().abs());
            }
        }

        List<MonthlyCashFlowDTO> result = new ArrayList<>();
        for (Map.Entry<YearMonth, BigDecimal[]> entry : byMonth.entrySet()) {
            BigDecimal income = entry.getValue()[0];
            BigDecimal expense = entry.getValue()[1];
            BigDecimal net = income.subtract(expense);
            result.add(new MonthlyCashFlowDTO(entry.getKey().toString(), income, expense, net));
        }

        return result;
    }

    public FinancialSummaryDTO getFinancialSummary(LocalDate startDate, LocalDate endDate, String category) {
        List<Transaction> transactions = transactionRepository.findByDateBetweenOrderByDateAsc(startDate, endDate);

        if (category != null && !category.isBlank()) {
            transactions = transactions.stream()
                    .filter(t -> t.getCategory() != null && t.getCategory().getName().equalsIgnoreCase(category))
                    .collect(Collectors.toList());
        }

        FinancialSummaryDTO dto = new FinancialSummaryDTO();

        if (transactions.isEmpty()) {
            dto.setStartDate(startDate);
            dto.setEndDate(endDate);
            dto.setTotalIncome(BigDecimal.ZERO);
            dto.setTotalExpense(BigDecimal.ZERO);
            dto.setNetSavings(BigDecimal.ZERO);
            dto.setSavingsRate(0.0);
            dto.setTotalTransactions(0);
            dto.setNarrative("No transactions found for the selected period. Upload a CSV file or adjust your filters to view your financial summary.");
            return dto;
        }

        LocalDate effectiveStart = transactions.get(0).getDate();
        LocalDate effectiveEnd = transactions.get(transactions.size() - 1).getDate();

        // If specific dates were provided by the caller (within reasonable bounds), use those
        if (startDate.isAfter(LocalDate.of(1950, 1, 1))) {
            effectiveStart = startDate;
        }
        if (endDate.isBefore(LocalDate.of(9990, 1, 1))) {
            effectiveEnd = endDate;
        }

        dto.setStartDate(effectiveStart);
        dto.setEndDate(effectiveEnd);

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;
        int incomeCount = 0;
        int expenseCount = 0;

        Map<String, BigDecimal> expenseByCat = new HashMap<>();
        Map<String, BigDecimal> incomeByCat = new HashMap<>();

        for (Transaction t : transactions) {
            String catName = t.getCategory() != null ? t.getCategory().getName() : "General";
            if (t.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                incomeCount++;
                totalIncome = totalIncome.add(t.getAmount());
                incomeByCat.merge(catName, t.getAmount(), BigDecimal::add);
            } else if (t.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                expenseCount++;
                BigDecimal spent = t.getAmount().abs();
                totalExpense = totalExpense.add(spent);
                expenseByCat.merge(catName, spent, BigDecimal::add);
            }
        }

        BigDecimal netSavings = totalIncome.subtract(totalExpense);
        double savingsRate = 0.0;
        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = netSavings
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalIncome, 1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        // Identify top expense category
        String topExpenseCategory = null;
        BigDecimal topExpenseAmount = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : expenseByCat.entrySet()) {
            if (entry.getValue().compareTo(topExpenseAmount) > 0) {
                topExpenseAmount = entry.getValue();
                topExpenseCategory = entry.getKey();
            }
        }

        double topExpensePercentage = 0.0;
        if (totalExpense.compareTo(BigDecimal.ZERO) > 0 && topExpenseAmount.compareTo(BigDecimal.ZERO) > 0) {
            topExpensePercentage = topExpenseAmount
                    .multiply(BigDecimal.valueOf(100))
                    .divide(totalExpense, 1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        // Identify top income category
        String topIncomeCategory = null;
        BigDecimal topIncomeAmount = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : incomeByCat.entrySet()) {
            if (entry.getValue().compareTo(topIncomeAmount) > 0) {
                topIncomeAmount = entry.getValue();
                topIncomeCategory = entry.getKey();
            }
        }

        dto.setTotalIncome(totalIncome);
        dto.setTotalExpense(totalExpense);
        dto.setNetSavings(netSavings);
        dto.setSavingsRate(savingsRate);
        dto.setIncomeCount(incomeCount);
        dto.setExpenseCount(expenseCount);
        dto.setTotalTransactions(transactions.size());
        dto.setTopExpenseCategory(topExpenseCategory);
        dto.setTopExpenseAmount(topExpenseAmount);
        dto.setTopExpensePercentage(topExpensePercentage);
        dto.setTopIncomeCategory(topIncomeCategory);
        dto.setTopIncomeAmount(topIncomeAmount);

        // Format narrative in LKR (Rs. )
        String periodText;
        if (effectiveStart.getYear() == effectiveEnd.getYear() &&
            effectiveStart.getMonth() == effectiveEnd.getMonth() &&
            effectiveStart.getDayOfMonth() == 1 &&
            effectiveEnd.getDayOfMonth() == effectiveEnd.lengthOfMonth()) {
            periodText = "for the month of " + effectiveStart.format(MONTH_YEAR_FORMATTER);
        } else if (effectiveStart.equals(effectiveEnd)) {
            periodText = "on " + effectiveStart.format(DATE_FORMATTER);
        } else {
            periodText = "from " + effectiveStart.format(DATE_FORMATTER) + " to " + effectiveEnd.format(DATE_FORMATTER);
        }
        dto.setFormattedDateRange(periodText);

        StringBuilder narrative = new StringBuilder();
        narrative.append("Your income ").append(periodText).append(" is ").append(formatLkr(totalIncome)).append(". ");

        narrative.append("Your expenses totaled ").append(formatLkr(totalExpense));
        if (topExpenseCategory != null && topExpenseAmount.compareTo(BigDecimal.ZERO) > 0) {
            narrative.append(" with majority spent on the ")
                    .append(topExpenseCategory)
                    .append(" category (")
                    .append(formatLkr(topExpenseAmount))
                    .append(", ")
                    .append(String.format(Locale.US, "%.1f%%", topExpensePercentage))
                    .append(" of expenses). ");
        } else {
            narrative.append(". ");
        }

        if (netSavings.compareTo(BigDecimal.ZERO) >= 0) {
            narrative.append("Your net savings is ")
                    .append(formatLkr(netSavings))
                    .append(" with a savings rate of ")
                    .append(String.format(Locale.US, "%.1f%%", savingsRate))
                    .append(".");
        } else {
            narrative.append("Your expenses exceeded your income by ")
                    .append(formatLkr(netSavings.abs()))
                    .append(".");
        }

        dto.setNarrative(narrative.toString());
        return dto;
    }

    private String formatLkr(BigDecimal amount) {
        if (amount == null) return "Rs. 0.00";
        return "Rs. " + String.format(Locale.US, "%,.2f", amount);
    }
}
