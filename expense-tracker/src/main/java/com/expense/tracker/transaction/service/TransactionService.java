package com.expense.tracker.transaction.service;

import com.expense.tracker.account.entity.Account;
import com.expense.tracker.account.repository.AccountRepository;
import com.expense.tracker.authentication.entity.User;
import com.expense.tracker.authentication.service.UserDetailsImpl;
import com.expense.tracker.transaction.dto.TransactionRequest;
import com.expense.tracker.transaction.dto.TransactionResponse;
import com.expense.tracker.transaction.entity.Category;
import com.expense.tracker.transaction.entity.Transaction;
import com.expense.tracker.transaction.repository.CategoryRepository;
import com.expense.tracker.transaction.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Get the currently authenticated user's ID from Spring Security context.
     */
    private Long getCurrentUserId() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userDetails.getUser().getId();
    }

    /**
     * Create a new income or expense transaction.
     */
    @Transactional
    public TransactionResponse createTransaction(TransactionRequest request) {
        Long userId = getCurrentUserId();

        // Validate category belongs to this user
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + request.getCategoryId()));
        if (!category.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Category does not belong to you");
        }

        // Validate account belongs to this user
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + request.getAccountId()));
        if (!account.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Account does not belong to you");
        }

        Transaction transaction = Transaction.builder()
                .user(category.getUser())
                .account(account)
                .category(category)
                .type(request.getType())
                .amount(request.getAmount())
                .description(request.getDescription())
                .notes(request.getNotes())
                .transactionDate(request.getTransactionDate())
                .build();

        Transaction saved = transactionRepository.save(transaction);
        return mapToResponse(saved);
    }

    /**
     * Get a single transaction by ID. Only the owner can access it.
     */
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(Long transactionId) {
        Long userId = getCurrentUserId();

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + transactionId));

        if (!transaction.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You do not have access to this transaction");
        }

        return mapToResponse(transaction);
    }

    /**
     * Get paginated list of transactions for the current user with optional filters.
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(
            String type,
            LocalDate startDate,
            LocalDate endDate,
            Long categoryId,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            String search,
            Pageable pageable) {
        Long userId = getCurrentUserId();

        Page<Transaction> transactions = transactionRepository.findByUserAndFilters(
                userId, type, startDate, endDate, categoryId, minAmount, maxAmount, search, pageable);

        return transactions.map(this::mapToResponse);
    }

    /**
     * Update an existing transaction. Only the owner can update it.
     */
    @Transactional
    public TransactionResponse updateTransaction(Long transactionId, TransactionRequest request) {
        Long userId = getCurrentUserId();

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + transactionId));

        if (!transaction.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You do not have access to this transaction");
        }

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found: " + request.getCategoryId()));
            if (!category.getUser().getId().equals(userId)) {
                throw new IllegalArgumentException("Category does not belong to you");
            }
            transaction.setCategory(category);
        }

        if (request.getAccountId() != null) {
            Account account = accountRepository.findById(request.getAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Account not found: " + request.getAccountId()));
            if (!account.getUser().getId().equals(userId)) {
                throw new IllegalArgumentException("Account does not belong to you");
            }
            transaction.setAccount(account);
        }

        if (request.getType() != null) {
            transaction.setType(request.getType());
        }
        if (request.getAmount() != null) {
            transaction.setAmount(request.getAmount());
        }
        if (request.getDescription() != null) {
            transaction.setDescription(request.getDescription());
        }
        if (request.getNotes() != null) {
            transaction.setNotes(request.getNotes());
        }
        if (request.getTransactionDate() != null) {
            transaction.setTransactionDate(request.getTransactionDate());
        }

        Transaction updated = transactionRepository.save(transaction);
        return mapToResponse(updated);
    }

    /**
     * Soft delete a transaction. Only the owner can delete it.
     */
    @Transactional
    public void deleteTransaction(Long transactionId) {
        Long userId = getCurrentUserId();

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + transactionId));

        if (!transaction.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You do not have access to this transaction");
        }

        transaction.setDeletedAt(LocalDateTime.now());
        transactionRepository.save(transaction);
    }

    /**
     * Convert Transaction entity to TransactionResponse DTO.
     */
    private TransactionResponse mapToResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setUserId(transaction.getUser().getId());
        response.setAccountId(transaction.getAccount().getId());
        response.setCategoryId(transaction.getCategory().getId());
        response.setAccountName(transaction.getAccount().getName());
        response.setCategoryName(transaction.getCategory().getName());
        response.setType(transaction.getType());
        response.setAmount(transaction.getAmount());
        response.setDescription(transaction.getDescription());
        response.setNotes(transaction.getNotes());
        response.setTransactionDate(transaction.getTransactionDate());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setUpdatedAt(transaction.getUpdatedAt());
        return response;
    }
}