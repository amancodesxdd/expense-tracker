package com.expense.tracker.transaction.repository;

import com.expense.tracker.authentication.entity.User;
import com.expense.tracker.transaction.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * CategoryRepository handles database operations for Category.
 * Spring Data JPA generates the implementation automatically.
 * We only expose methods that scope results to the authenticated user.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUser(User user);

    List<Category> findByUserAndType(User user, String type);
}