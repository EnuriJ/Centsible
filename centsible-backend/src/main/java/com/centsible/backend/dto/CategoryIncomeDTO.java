package com.centsible.backend.dto;

import java.math.BigDecimal;

public class CategoryIncomeDTO {

    private String category;
    private BigDecimal totalIncome;
    private Double percentage;
    private int count;

    public CategoryIncomeDTO() {
    }

    public CategoryIncomeDTO(String category, BigDecimal totalIncome, Double percentage, int count) {
        this.category = category;
        this.totalIncome = totalIncome;
        this.percentage = percentage;
        this.count = count;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public Double getPercentage() {
        return percentage;
    }

    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
