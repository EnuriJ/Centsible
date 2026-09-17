package com.centsible.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionDTO {

    private Long id;
    private LocalDate date;
    private String description;
    private BigDecimal amount;
    private String category;
    private String type; // "INCOME" or "EXPENSE"

    public TransactionDTO() {
    }

    public TransactionDTO(Long id, LocalDate date, String description, BigDecimal amount, String category) {
        this.id = id;
        this.date = date;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.type = (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) ? "INCOME" : "EXPENSE";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
        this.type = (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) ? "INCOME" : "EXPENSE";
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
