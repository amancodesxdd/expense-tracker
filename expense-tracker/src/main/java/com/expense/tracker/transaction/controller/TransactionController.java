package com.expense.tracker.transaction.controller;

import com.expense.tracker.transaction.dto.TransactionRequest;
import com.expense.tracker.transaction.dto.TransactionResponse;
import com.expense.tracker.transaction.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Create a new income or expense transaction.
     *
     * POST /api/v1/transactions
     * Body: TransactionRequest JSON
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.createTransaction(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get a paginated list of transactions with optional filters.
     *
     * GET /api/v1/transactions?page=0&size=20&type=EXPENSE&startDate=2026-01-01&endDate=2026-12-31
     */
    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String search,
            @PageableDefault(page = 0, size = 20) Pageable pageable) {
        Page<TransactionResponse> transactions = transactionService.getTransactions(
                type, startDate, endDate, categoryId, minAmount, maxAmount, search, pageable);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Get a single transaction by ID.
     *
     * GET /api/v1/transactions/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable Long id) {
        TransactionResponse response = transactionService.getTransactionById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing transaction.
     *
     * PUT /api/v1/transactions/{id}
     * Body: TransactionRequest JSON
     */
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.updateTransaction(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Soft delete a transaction.
     *
     * DELETE /api/v1/transactions/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }
}