package com.expense.tracker.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public class AccountRequest {

    @NotBlank(message = "Account name is required")
    @jakarta.validation.constraints.Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Account type is required")
    @Pattern(regexp = "CASH|SAVINGS|BANK", message = "Type must be CASH, SAVINGS, or BANK")
    private String type;

    @NotNull(message = "Initial balance is required")
    @PositiveOrZero(message = "Initial balance must be zero or positive")
    private BigDecimal initialBalance;

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getInitialBalance() { return initialBalance; }
    public void setInitialBalance(BigDecimal initialBalance) { this.initialBalance = initialBalance; }
}