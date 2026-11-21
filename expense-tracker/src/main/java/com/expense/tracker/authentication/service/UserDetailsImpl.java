package com.expense.tracker.authentication.service;

import com.expense.tracker.authentication.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * UserDetailsImpl wraps our User entity to implement Spring Security's UserDetails interface.
 *
 * Spring Security needs UserDetails to authenticate users. Our User entity is a JPA entity
 * with database columns; UserDetails is a security interface with methods like getPassword(),
 * getAuthorities(), isAccountNonExpired(), etc.
 *
 * Why separate them?
 * - User entity: database persistence (id, email, passwordHash, createdAt, etc.)
 *   UserDetails: security contract (password, roles, account status)
 * Keeping them separate means your database model isn't forced to match Spring Security's interface.
 */
public class UserDetailsImpl implements UserDetails {

    private final User user;

    public UserDetailsImpl(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Return the user's role as a granted authority (e.g., "ROLE_USER")
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getIsActive();
    }

    public User getUser() {
        return user;
    }
}
