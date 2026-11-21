package com.expense.tracker.authentication.repository;

import com.expense.tracker.authentication.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * UserRepository handles database operations for the User entity.
 *
 * Spring Data JPA automatically generates the implementation at runtime.
 * You don't write SQL - just define method names and Spring implements them.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by email. Returns Optional because the user might not exist.
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user with the given email already exists.
     */
    boolean existsByEmail(String email);
}
