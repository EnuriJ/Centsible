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

    // All expense rows in a date range, oldest first. Used to bucket by
    // month (and optionally filter by category) in the service layer.
    List<Transaction> findByAmountLessThanAndDateBetweenOrderByDateAsc(
            BigDecimal amount, LocalDate startDate, LocalDate endDate);

    // Used by CSV import to skip rows that already exist, so re-uploading
    // the same file (or an overlapping export) doesn't create duplicates.
    boolean existsByDateAndDescriptionAndAmountAndCategory(
            LocalDate date, String description, BigDecimal amount, Category category);

    // Only counts expenses (amount < 0), sums their absolute value per category,
    // restricted to a date range. Income transactions are excluded so "spend"
    // actually means spend.
    @Query("SELECT t.category.name AS category, SUM(ABS(t.amount)) AS totalSpent " +
           "FROM Transaction t " +
           "WHERE t.amount < 0 " +
           "AND t.date BETWEEN :startDate AND :endDate " +
           "GROUP BY t.category.name " +
           "ORDER BY totalSpent DESC")
    List<CategorySpendProjection> aggregateSpendByCategory(
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

}
