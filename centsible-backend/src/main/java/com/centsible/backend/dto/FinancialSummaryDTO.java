package com.centsible.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FinancialSummaryDTO {

    private LocalDate startDate;
    private LocalDate endDate;
    private String formattedDateRange;
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal netSavings;
    private Double savingsRate; // Percentage e.g. 42.5
    private String topExpenseCategory;
    private BigDecimal topExpenseAmount;
    private Double topExpensePercentage;
    private String topIncomeCategory;
    private BigDecimal topIncomeAmount;
    private int incomeCount;
    private int expenseCount;
    private int totalTransactions;
    private String narrative;

    public FinancialSummaryDTO() {
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getFormattedDateRange() {
        return formattedDateRange;
    }

    public void setFormattedDateRange(String formattedDateRange) {
        this.formattedDateRange = formattedDateRange;
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

    public BigDecimal getNetSavings() {
        return netSavings;
    }

    public void setNetSavings(BigDecimal netSavings) {
        this.netSavings = netSavings;
    }

    public Double getSavingsRate() {
        return savingsRate;
    }

    public void setSavingsRate(Double savingsRate) {
        this.savingsRate = savingsRate;
    }

    public String getTopExpenseCategory() {
        return topExpenseCategory;
    }

    public void setTopExpenseCategory(String topExpenseCategory) {
        this.topExpenseCategory = topExpenseCategory;
    }

    public BigDecimal getTopExpenseAmount() {
        return topExpenseAmount;
    }

    public void setTopExpenseAmount(BigDecimal topExpenseAmount) {
        this.topExpenseAmount = topExpenseAmount;
    }

    public Double getTopExpensePercentage() {
        return topExpensePercentage;
    }

    public void setTopExpensePercentage(Double topExpensePercentage) {
        this.topExpensePercentage = topExpensePercentage;
    }

    public String getTopIncomeCategory() {
        return topIncomeCategory;
    }

    public void setTopIncomeCategory(String topIncomeCategory) {
        this.topIncomeCategory = topIncomeCategory;
    }

    public BigDecimal getTopIncomeAmount() {
        return topIncomeAmount;
    }

    public void setTopIncomeAmount(BigDecimal topIncomeAmount) {
        this.topIncomeAmount = topIncomeAmount;
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

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(int totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public String getNarrative() {
        return narrative;
    }

    public void setNarrative(String narrative) {
        this.narrative = narrative;
    }
}
