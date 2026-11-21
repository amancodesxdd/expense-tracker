package com.expense.tracker.authentication.service;

import com.expense.tracker.authentication.dto.RegisterRequest;
import com.expense.tracker.authentication.entity.User;
import com.expense.tracker.authentication.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserService handles business logic for user registration.
 *
 * Responsibilities:
 * - Validate that email is not already taken
 * - Hash the password using BCrypt before saving
 * - Create and persist the User entity
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructor injection. Spring provides UserRepository and PasswordEncoder beans.
     * PasswordEncoder is provided by Spring Security (BCrypt implementation).
     */
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Register a new user.
     *
     * @param request registration data from the client
     * @return the created User (without password hash exposed)
     * @throws IllegalArgumentException if email already exists
     */
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("USER")
                .isActive(true)
                .build();

        return userRepository.save(user);
    }
}
