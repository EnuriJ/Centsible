package com.centsible.backend.dto;

import java.math.BigDecimal;

public class CategorySpendDTO {

    private String category;
    private BigDecimal totalSpent;

    public CategorySpendDTO(String category, BigDecimal totalSpent) {
        this.category = category;
        this.totalSpent = totalSpent;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }
}
