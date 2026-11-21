package com.expense.tracker.transaction.repository;

import com.expense.tracker.authentication.entity.User;
import com.expense.tracker.transaction.entity.Category;
import com.expense.tracker.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByUser(User user, Pageable pageable);

    Page<Transaction> findByUserAndType(User user, String type, Pageable pageable);

    Page<Transaction> findByUserAndCategory(User user, Category category, Pageable pageable);

    Page<Transaction> findByUserAndTransactionDateBetween(
            User user, LocalDate startDate, LocalDate endDate, Pageable pageable);

    List<Transaction> findByUserAndTransactionDateBetween(
            User user, LocalDate startDate, LocalDate endDate);
}