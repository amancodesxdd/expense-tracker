package com.expense.tracker.authentication.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SecurityConfig configures Spring Security for the application.
 *
 * Key decisions:
 * 1. Stateless sessions (no HTTP sessions) — REST APIs should be stateless
 * 2. CSRF disabled — not needed for token-based APIs (CSRF is for cookie-based browser sessions)
 * 3. Permit all /api/v1/auth/** endpoints (login, register) without authentication
 * 4. Require authentication for all other endpoints
 * 5. Add JwtAuthenticationFilter BEFORE UsernamePasswordAuthenticationFilter
 *
 * Authentication vs Authorization:
 * - Authentication: Who are you? (JWT proves your identity)
 * - Authorization: What are you allowed to do? (role-based checks on endpoints)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Defines the security filter chain.
     * This is the heart of Spring Security configuration.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF: stateless JWT APIs don't need CSRF protection
                // CSRF is designed for cookie-based authentication in browser forms
                .csrf(csrf -> csrf.disable())

                // No HTTP sessions — stateless authentication
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Allow everyone to access auth endpoints
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )

                // Add our JWT filter before Spring's default username/password filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * AuthenticationManager is the entry point for Spring Security authentication.
     * It uses DaoAuthenticationProvider, which loads user via UserDetailsService
     * and verifies password using PasswordEncoder.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsServiceImpl userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(authProvider);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
