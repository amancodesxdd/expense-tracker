package com.expense.tracker.authentication.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtAuthenticationFilter intercepts every incoming HTTP request.
 *
 * What it does:
 * 1. Reads the Authorization header (expects "Bearer <token>")
 * 2. Extracts the JWT token
 * 3. Validates the token using JwtService
 * 4. If valid, loads the user from database and sets Spring Security context
 *
 * Why a filter?
 * - Spring Security processes requests through a filter chain
 * - This filter runs BEFORE Spring Security checks if the user is allowed to access an endpoint
 * - It populates the SecurityContext so controllers can access the authenticated user
 *
 * Stateless authentication:
 * - No server-side session is created
 * - The token itself proves identity on every request
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsServiceImpl userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // If no Authorization header or doesn't start with "Bearer ", skip JWT processing
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract token (remove "Bearer " prefix)
        jwt = authHeader.substring(7);

        // Extract email from token
        try {
            userEmail = jwtService.extractUsername(jwt);
        } catch (Exception e) {
            // Token is malformed or invalid signature - skip authentication
            filterChain.doFilter(request, response);
            return;
        }

        // If we have an email and no authentication is already set in context
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // Validate token against user details
            if (jwtService.isTokenValid(jwt, userDetails)) {
                // Create authentication token and set it in SecurityContext
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
