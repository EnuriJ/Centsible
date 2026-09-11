package com.centsible.backend.dto;

import java.math.BigDecimal;

public class MonthlySpendDTO {

    private String month; // e.g. "2026-06"
    private BigDecimal totalSpent;

    public MonthlySpendDTO(String month, BigDecimal totalSpent) {
        this.month = month;
        this.totalSpent = totalSpent;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }
}
