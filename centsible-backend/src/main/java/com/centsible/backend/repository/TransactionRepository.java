package com.centsible.backend.repository;

import com.centsible.backend.model.Category;
import com.centsible.backend.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByCategoryId(Long categoryId);

    List<Transaction> findByDateBetween(LocalDate start, LocalDate end);

    // All transactions in a date range, oldest first (used for cash flow & summary)
    List<Transaction> findByDateBetweenOrderByDateAsc(LocalDate startDate, LocalDate endDate);

    // All expense rows in a date range, oldest first.
    List<Transaction> findByAmountLessThanAndDateBetweenOrderByDateAsc(
            BigDecimal amount, LocalDate startDate, LocalDate endDate);

    // All income rows in a date range, oldest first.
    List<Transaction> findByAmountGreaterThanAndDateBetweenOrderByDateAsc(
            BigDecimal amount, LocalDate startDate, LocalDate endDate);

    // Used by CSV import to skip rows that already exist
    boolean existsByDateAndDescriptionAndAmountAndCategory(
            LocalDate date, String description, BigDecimal amount, Category category);

    // Aggregate expenses (amount < 0), sums absolute value per category
    @Query("SELECT t.category.name AS category, SUM(ABS(t.amount)) AS totalSpent " +
           "FROM Transaction t " +
           "WHERE t.amount < 0 " +
           "AND t.date BETWEEN :startDate AND :endDate " +
           "GROUP BY t.category.name " +
           "ORDER BY totalSpent DESC")
    List<CategorySpendProjection> aggregateSpendByCategory(
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    // Aggregate income (amount > 0), sums value per category
    @Query("SELECT t.category.name AS category, SUM(t.amount) AS totalIncome " +
           "FROM Transaction t " +
           "WHERE t.amount > 0 " +
           "AND t.date BETWEEN :startDate AND :endDate " +
           "GROUP BY t.category.name " +
           "ORDER BY totalIncome DESC")
    List<CategoryIncomeProjection> aggregateIncomeByCategory(
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT MIN(t.date) FROM Transaction t")
    LocalDate findMinDate();

    @Query("SELECT MAX(t.date) FROM Transaction t")
    LocalDate findMaxDate();

}
