package com.expense.tracker.account.controller;

import com.expense.tracker.account.dto.AccountRequest;
import com.expense.tracker.account.dto.AccountResponse;
import com.expense.tracker.account.dto.TransferRequest;
import com.expense.tracker.account.dto.TransferResponse;
import com.expense.tracker.account.service.AccountService;
import com.expense.tracker.transaction.dto.TransactionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Create a new account for the authenticated user.
     *
     * POST /api/v1/accounts
     */
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @jakarta.validation.Valid @RequestBody AccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all accounts for the authenticated user.
     *
     * GET /api/v1/accounts
     */
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAccounts() {
        List<AccountResponse> accounts = accountService.getAccounts();
        return ResponseEntity.ok(accounts);
    }

    /**
     * Transfer money between two accounts owned by the authenticated user.
     *
     * POST /api/v1/accounts/transfer
     */
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(
            @jakarta.validation.Valid @RequestBody TransferRequest request) {
        TransferResponse response = accountService.transfer(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get transaction history for a specific account.
     *
     * GET /api/v1/accounts/{id}/transactions
     */
    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionResponse>> getAccountTransactions(@PathVariable Long id) {
        List<TransactionResponse> transactions = accountService.getAccountTransactions(id);
        return ResponseEntity.ok(transactions);
    }
}