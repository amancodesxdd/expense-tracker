package com.expense.tracker.authentication.controller;

import com.expense.tracker.authentication.dto.AuthResponse;
import com.expense.tracker.authentication.dto.LoginRequest;
import com.expense.tracker.authentication.dto.RegisterRequest;
import com.expense.tracker.authentication.entity.User;
import com.expense.tracker.authentication.service.JwtService;
import com.expense.tracker.authentication.service.UserDetailsServiceImpl;
import com.expense.tracker.authentication.service.UserDetailsImpl;
import com.expense.tracker.authentication.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AuthController handles all authentication endpoints.
 *
 * Endpoints:
 *   POST /api/v1/auth/register     - Register a new user (returns JWT)
 *   POST /api/v1/auth/login        - Login with email + password (returns JWT + refresh token)
 *   POST /api/v1/auth/refresh      - Exchange refresh token for new access token
 *   GET  /api/v1/auth/me           - Get current logged-in user profile (protected)
 *   POST /api/v1/auth/logout       - Logout (invalidate refresh token)
 *
 * Stateless authentication:
 * - No server-side sessions
 * - Client sends JWT in Authorization header: Bearer <token>
 * - Every request is independently verified
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final UserService userService;

    // In-memory refresh token store (for learning purposes only)
    // In production, store refresh tokens in database with expiry and revocation support
    private static final Map<String, String> REFRESH_TOKEN_STORE = new HashMap<>();

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserDetailsServiceImpl userDetailsService,
            UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userService = userService;
    }

    /**
     * Register a new user and return access + refresh tokens.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.register(request);
            UserDetails userDetails = new com.expense.tracker.authentication.service.UserDetailsImpl(user);

            String accessToken = jwtService.generateAccessToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);
            REFRESH_TOKEN_STORE.put(user.getEmail(), refreshToken);

            AuthResponse response = new AuthResponse(accessToken, user.getEmail(), user.getFullName());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Login with email and password. Returns access token + refresh token.
     *
     * Authentication flow:
     * 1. Spring Security AuthenticationManager verifies credentials via DaoAuthenticationProvider
     * 2. UserDetailsService loads user from DB, PasswordEncoder compares hashed passwords
     * 3. If valid, we generate JWT access token + refresh token
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            // Authenticate via Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            // Load user details and generate tokens
            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
            String accessToken = jwtService.generateAccessToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);
            REFRESH_TOKEN_STORE.put(request.getEmail(), refreshToken);

            User user = ((com.expense.tracker.authentication.service.UserDetailsImpl) userDetails).getUser();
            AuthResponse response = new AuthResponse(accessToken, user.getEmail(), user.getFullName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid email or password");
        }
    }

    /**
     * Exchange a valid refresh token for a new access token.
     * This avoids forcing the user to log in again when the short-lived access token expires.
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null) {
            return ResponseEntity.badRequest().body("Refresh token is required");
        }

        try {
            String userEmail = jwtService.extractUsername(refreshToken);
            String storedRefreshToken = REFRESH_TOKEN_STORE.get(userEmail);

            if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
                return ResponseEntity.status(401).body("Invalid or revoked refresh token");
            }

            if (jwtService.isTokenExpired(refreshToken)) {
                return ResponseEntity.status(401).body("Refresh token expired");
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            String newAccessToken = jwtService.generateAccessToken(userDetails);

            User user = ((com.expense.tracker.authentication.service.UserDetailsImpl) userDetails).getUser();
            AuthResponse response = new AuthResponse(newAccessToken, user.getEmail(), user.getFullName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Invalid refresh token");
        }
    }

    /**
     * Get current logged-in user profile.
     * Protected endpoint — requires valid JWT in Authorization header.
     *
     * The JwtAuthenticationFilter extracts the JWT, validates it, and sets
     * SecurityContextHolder. We read the current user from there.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Not authenticated");
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userDetails.getUser();

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("email", user.getEmail());
        profile.put("fullName", user.getFullName());
        profile.put("role", user.getRole());
        profile.put("isActive", user.getIsActive());

        return ResponseEntity.ok(profile);
    }

    /**
     * Logout — invalidate the refresh token.
     * In production, also maintain a token blacklist or store refresh tokens in DB.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken != null) {
            try {
                String userEmail = jwtService.extractUsername(refreshToken);
                REFRESH_TOKEN_STORE.remove(userEmail);
            } catch (Exception ignored) {
                // Token already invalid
            }
        }
        return ResponseEntity.ok("Logged out successfully");
    }
}
