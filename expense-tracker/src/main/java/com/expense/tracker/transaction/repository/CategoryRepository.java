package com.expense.tracker.transaction.repository;

import com.expense.tracker.authentication.entity.User;
import com.expense.tracker.transaction.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUser(User user);

    List<Category> findByUserAndType(User user, String type);

    Optional<Category> findByUserAndNameAndType(User user, String name, String type);
}