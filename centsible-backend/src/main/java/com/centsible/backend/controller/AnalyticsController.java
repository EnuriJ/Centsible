package com.centsible.backend.controller;

import com.centsible.backend.dto.CategorySpendDTO;
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

    // Sentinel bounds used when the caller doesn't specify a date range,
    // so "no filter" doesn't require special-casing null dates downstream.
    private static final LocalDate DEFAULT_START = LocalDate.of(1900, 1, 1);
    private static final LocalDate DEFAULT_END = LocalDate.of(9999, 12, 31);

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    // GET /api/analytics/spend-by-category?startDate=2026-06-01&endDate=2026-07-31
    @GetMapping("/spend-by-category")
    public List<CategorySpendDTO> spendByCategory(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : DEFAULT_START;
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : DEFAULT_END;
        return analyticsService.getSpendByCategory(start, end);
    }

    // GET /api/analytics/spend-over-time?startDate=...&endDate=...&category=Groceries
    @GetMapping("/spend-over-time")
    public List<MonthlySpendDTO> spendOverTime(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String category) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : DEFAULT_START;
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : DEFAULT_END;
        return analyticsService.getSpendOverTime(start, end, category);
    }
}
