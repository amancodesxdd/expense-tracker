package com.expense.tracker.transaction.repository;

import com.expense.tracker.authentication.entity.User;
import com.expense.tracker.transaction.entity.Category;
import com.expense.tracker.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByUser(User user, Pageable pageable);

    Page<Transaction> findByUserAndType(User user, String type, Pageable pageable);

    Page<Transaction> findByUserAndCategory(User user, Category category, Pageable pageable);

    Page<Transaction> findByUserAndTransactionDateBetween(
            User user, LocalDate startDate, LocalDate endDate, Pageable pageable);

    List<Transaction> findByUserAndTransactionDateBetween(
            User user, LocalDate startDate, LocalDate endDate);

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.user.id = :userId
        AND (:type IS NULL OR t.type = :type)
        AND (:startDate IS NULL OR t.transactionDate >= :startDate)
        AND (:endDate IS NULL OR t.transactionDate <= :endDate)
        AND (:categoryId IS NULL OR t.category.id = :categoryId)
        AND (:minAmount IS NULL OR t.amount >= :minAmount)
        AND (:maxAmount IS NULL OR t.amount <= :maxAmount)
        AND (:search IS NULL OR (t.description LIKE %:search% OR t.notes LIKE %:search%))
        AND t.deletedAt IS NULL
        ORDER BY t.transactionDate DESC
    """)
    Page<Transaction> findByUserAndFilters(
            @Param("userId") Long userId,
            @Param("type") String type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("categoryId") Long categoryId,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId AND t.account.id = :accountId AND t.deletedAt IS NULL ORDER BY t.transactionDate DESC")
    List<Transaction> findByUserAndAccountOrderByTransactionDateDesc(
            @Param("userId") Long userId,
            @Param("accountId") Long accountId);
}