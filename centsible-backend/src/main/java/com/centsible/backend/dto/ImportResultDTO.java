package com.centsible.backend.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ImportResultDTO {

    private int imported;
    private int skipped;
    private int duplicateCount;
    private List<String> parseErrors;
    private List<String> errors;
    private int incomeCount;
    private int expenseCount;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private int categoriesCount;

    public ImportResultDTO() {
        this.parseErrors = new ArrayList<>();
        this.errors = this.parseErrors;
    }

    public ImportResultDTO(int imported, int skipped, List<String> errors) {
        this(imported, 0, errors, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        this.skipped = skipped;
    }

    public ImportResultDTO(int imported, int duplicateCount, List<String> parseErrors,
                           int incomeCount, int expenseCount,
                           BigDecimal totalIncome, BigDecimal totalExpense,
                           int categoriesCount) {
        this.imported = imported;
        this.duplicateCount = duplicateCount;
        this.parseErrors = parseErrors != null ? parseErrors : new ArrayList<>();
        this.errors = this.parseErrors;
        this.skipped = duplicateCount + this.parseErrors.size();
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

    public int getDuplicateCount() {
        return duplicateCount;
    }

    public void setDuplicateCount(int duplicateCount) {
        this.duplicateCount = duplicateCount;
        this.skipped = this.duplicateCount + (this.parseErrors != null ? this.parseErrors.size() : 0);
    }

    public List<String> getParseErrors() {
        return parseErrors;
    }

    public void setParseErrors(List<String> parseErrors) {
        this.parseErrors = parseErrors != null ? parseErrors : new ArrayList<>();
        this.errors = this.parseErrors;
        this.skipped = this.duplicateCount + this.parseErrors.size();
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors != null ? errors : new ArrayList<>();
        this.parseErrors = this.errors;
        this.skipped = this.duplicateCount + this.errors.size();
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
