package com.expense.tracker.account.repository;

import com.expense.tracker.account.entity.Account;
import com.expense.tracker.authentication.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AccountRepository handles database operations for Account.
 * Spring Data JPA generates the implementation automatically.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUser(User user);
}