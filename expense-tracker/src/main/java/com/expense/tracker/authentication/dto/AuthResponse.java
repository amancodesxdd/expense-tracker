package com.expense.tracker.authentication.dto;

/**
 * DTO for authentication response.
 * Contains the JWT token and basic user info sent back to the client after login/register.
 */
public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private String email;
    private String fullName;

    public AuthResponse(String token, String email, String fullName) {
        this.token = token;
        this.email = email;
        this.fullName = fullName;
    }

    // Getters and setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
}
