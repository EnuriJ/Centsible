package com.centsible.backend.repository;

import com.centsible.backend.model.Category;
import com.centsible.backend.model.Transaction;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserId(Long userId, Sort sort);

    List<Transaction> findByUserId(Long userId);

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    List<Transaction> findByUserIdAndCategoryId(Long userId, Long categoryId);

    List<Transaction> findByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end);

    // All transactions in a date range for a user, oldest first (used for cash flow & summary)
    List<Transaction> findByUserIdAndDateBetweenOrderByDateAsc(Long userId, LocalDate startDate, LocalDate endDate);

    // All expense rows in a date range for a user, oldest first
    List<Transaction> findByUserIdAndAmountLessThanAndDateBetweenOrderByDateAsc(
            Long userId, BigDecimal amount, LocalDate startDate, LocalDate endDate);

    // All income rows in a date range for a user, oldest first
    List<Transaction> findByUserIdAndAmountGreaterThanAndDateBetweenOrderByDateAsc(
            Long userId, BigDecimal amount, LocalDate startDate, LocalDate endDate);

    // Used by CSV import to skip rows that already exist for the user
    boolean existsByUserIdAndDateAndDescriptionAndAmountAndCategory(
            Long userId, LocalDate date, String description, BigDecimal amount, Category category);

    long countByUserId(Long userId);

    // Aggregate expenses (amount < 0), sums absolute value per category for a specific user
    @Query("SELECT t.category.name AS category, SUM(ABS(t.amount)) AS totalSpent " +
           "FROM Transaction t " +
           "WHERE t.user.id = :userId " +
           "AND t.amount < 0 " +
           "AND t.date BETWEEN :startDate AND :endDate " +
           "GROUP BY t.category.name " +
           "ORDER BY totalSpent DESC")
    List<CategorySpendProjection> aggregateSpendByCategory(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // Aggregate income (amount > 0), sums value per category for a specific user
    @Query("SELECT t.category.name AS category, SUM(t.amount) AS totalIncome " +
           "FROM Transaction t " +
           "WHERE t.user.id = :userId " +
           "AND t.amount > 0 " +
           "AND t.date BETWEEN :startDate AND :endDate " +
           "GROUP BY t.category.name " +
           "ORDER BY totalIncome DESC")
    List<CategoryIncomeProjection> aggregateIncomeByCategory(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT MIN(t.date) FROM Transaction t WHERE t.user.id = :userId")
    LocalDate findMinDateByUserId(@Param("userId") Long userId);

    @Query("SELECT MAX(t.date) FROM Transaction t WHERE t.user.id = :userId")
    LocalDate findMaxDateByUserId(@Param("userId") Long userId);

}
