package com.expense.tracker.authentication.service;

import com.expense.tracker.authentication.entity.User;
import com.expense.tracker.authentication.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * UserDetailsServiceImpl loads a User from the database and wraps it in UserDetailsImpl.
 *
 * Spring Security calls loadUserByUsername() when a user logs in.
 * We look up the user by email, then wrap it in UserDetailsImpl so Spring Security
 * can check passwords, roles, and account status.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new UserDetailsImpl(user);
    }
}
