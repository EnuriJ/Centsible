package com.centsible.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public class ImportResultDTO {

    private int imported;
    private int skipped;
    private List<String> errors;
    private int incomeCount;
    private int expenseCount;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private int categoriesCount;

    public ImportResultDTO() {
    }

    public ImportResultDTO(int imported, int skipped, List<String> errors) {
        this(imported, skipped, errors, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, 0);
    }

    public ImportResultDTO(int imported, int skipped, List<String> errors,
                           int incomeCount, int expenseCount,
                           BigDecimal totalIncome, BigDecimal totalExpense,
                           int categoriesCount) {
        this.imported = imported;
        this.skipped = skipped;
        this.errors = errors;
        this.incomeCount = incomeCount;
        this.expenseCount = expenseCount;
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.categoriesCount = categoriesCount;
    }

    public int getImported() {
        return imported;
    }

    public void setImported(int imported) {
        this.imported = imported;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public int getIncomeCount() {
        return incomeCount;
    }

    public void setIncomeCount(int incomeCount) {
        this.incomeCount = incomeCount;
    }

    public int getExpenseCount() {
        return expenseCount;
    }

    public void setExpenseCount(int expenseCount) {
        this.expenseCount = expenseCount;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public void setTotalExpense(BigDecimal totalExpense) {
        this.totalExpense = totalExpense;
    }

    public int getCategoriesCount() {
        return categoriesCount;
    }

    public void setCategoriesCount(int categoriesCount) {
        this.categoriesCount = categoriesCount;
    }
}
