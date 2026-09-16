package com.centsible.backend.controller;

import com.centsible.backend.dto.CategoryIncomeDTO;
import com.centsible.backend.dto.CategorySpendDTO;
import com.centsible.backend.dto.FinancialSummaryDTO;
import com.centsible.backend.dto.MonthlyCashFlowDTO;
import com.centsible.backend.dto.MonthlySpendDTO;
import com.centsible.backend.service.AnalyticsService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class AnalyticsController {

    private static final LocalDate DEFAULT_START = LocalDate.of(1900, 1, 1);
    private static final LocalDate DEFAULT_END = LocalDate.of(9999, 12, 31);

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    // GET /api/analytics/summary?startDate=...&endDate=...&category=...
    @GetMapping("/summary")
    public FinancialSummaryDTO getSummary(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String category) {
        LocalDate start = startDate != null && !startDate.isBlank() ? LocalDate.parse(startDate) : DEFAULT_START;
        LocalDate end = endDate != null && !endDate.isBlank() ? LocalDate.parse(endDate) : DEFAULT_END;
        return analyticsService.getFinancialSummary(start, end, category);
    }

    // GET /api/analytics/spend-by-category?startDate=...&endDate=...
    @GetMapping("/spend-by-category")
    public List<CategorySpendDTO> spendByCategory(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDate start = startDate != null && !startDate.isBlank() ? LocalDate.parse(startDate) : DEFAULT_START;
        LocalDate end = endDate != null && !endDate.isBlank() ? LocalDate.parse(endDate) : DEFAULT_END;
        return analyticsService.getSpendByCategory(start, end);
    }

    // GET /api/analytics/income-by-category?startDate=...&endDate=...
    @GetMapping("/income-by-category")
    public List<CategoryIncomeDTO> incomeByCategory(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDate start = startDate != null && !startDate.isBlank() ? LocalDate.parse(startDate) : DEFAULT_START;
        LocalDate end = endDate != null && !endDate.isBlank() ? LocalDate.parse(endDate) : DEFAULT_END;
        return analyticsService.getIncomeByCategory(start, end);
    }

    // GET /api/analytics/spend-over-time?startDate=...&endDate=...&category=...
    @GetMapping("/spend-over-time")
    public List<MonthlySpendDTO> spendOverTime(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String category) {
        LocalDate start = startDate != null && !startDate.isBlank() ? LocalDate.parse(startDate) : DEFAULT_START;
        LocalDate end = endDate != null && !endDate.isBlank() ? LocalDate.parse(endDate) : DEFAULT_END;
        return analyticsService.getSpendOverTime(start, end, category);
    }

    // GET /api/analytics/cash-flow-over-time?startDate=...&endDate=...&category=...
    @GetMapping("/cash-flow-over-time")
    public List<MonthlyCashFlowDTO> cashFlowOverTime(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String category) {
        LocalDate start = startDate != null && !startDate.isBlank() ? LocalDate.parse(startDate) : DEFAULT_START;
        LocalDate end = endDate != null && !endDate.isBlank() ? LocalDate.parse(endDate) : DEFAULT_END;
        return analyticsService.getCashFlowOverTime(start, end, category);
    }
}
