package com.expense.tracker.account.service;

import com.expense.tracker.account.dto.AccountRequest;
import com.expense.tracker.account.dto.AccountResponse;
import com.expense.tracker.account.dto.TransferRequest;
import com.expense.tracker.account.dto.TransferResponse;
import com.expense.tracker.account.entity.Account;
import com.expense.tracker.account.repository.AccountRepository;
import com.expense.tracker.authentication.entity.User;
import com.expense.tracker.authentication.service.UserDetailsImpl;
import com.expense.tracker.transaction.dto.TransactionRequest;
import com.expense.tracker.transaction.entity.Category;
import com.expense.tracker.transaction.entity.Transaction;
import com.expense.tracker.transaction.repository.CategoryRepository;
import com.expense.tracker.transaction.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Get the currently authenticated user's ID from Spring Security context.
     */
    private Long getCurrentUserId() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userDetails.getUser().getId();
    }

    /**
     * Create a new account for the authenticated user.
     */
    public AccountResponse createAccount(AccountRequest request) {
        Long userId = getCurrentUserId();

        User user = new User();
        user.setId(userId);

        Account account = Account.builder()
                .user(user)
                .name(request.getName())
                .type(request.getType())
                .balance(request.getInitialBalance())
                .currency("INR")
                .build();

        Account saved = accountRepository.save(account);
        return mapToResponse(saved);
    }

    /**
     * Get all accounts for the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccounts() {
        Long userId = getCurrentUserId();

        User user = new User();
        user.setId(userId);

        List<Account> accounts = accountRepository.findByUser(user);
        return accounts.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Transfer money between two accounts owned by the authenticated user.
     * Uses @Transactional for atomicity and @Version for optimistic locking.
     */
    public TransferResponse transfer(TransferRequest request) {
        Long userId = getCurrentUserId();

        // Lock and load source account
        Account fromAccount = accountRepository.findById(request.getFromAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Source account not found: " + request.getFromAccountId()));
        if (!fromAccount.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Source account does not belong to you");
        }

        // Lock and load destination account
        Account toAccount = accountRepository.findById(request.getToAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Destination account not found: " + request.getToAccountId()));
        if (!toAccount.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Destination account does not belong to you");
        }

        // Check: source and destination must be different
        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new IllegalArgumentException("Source and destination accounts cannot be the same");
        }

        // Check: sufficient balance
        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance in source account");
        }

        // Debit source account
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        accountRepository.save(fromAccount);

        // Credit destination account
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));
        accountRepository.save(toAccount);

        // Get or create categories for transfer transactions
        Category transferOutCategory = getOrCreateCategory(userId, "Transfer Out", "EXPENSE");
        Category transferInCategory = getOrCreateCategory(userId, "Transfer In", "INCOME");

        // Create outgoing transaction (expense from source)
        TransactionRequest outRequest = new TransactionRequest();
        outRequest.setAccountId(fromAccount.getId());
        outRequest.setCategoryId(transferOutCategory.getId());
        outRequest.setType("EXPENSE");
        outRequest.setAmount(request.getAmount());
        outRequest.setDescription(request.getDescription() != null ? request.getDescription() : "Transfer to account " + toAccount.getName());
        outRequest.setNotes("Internal transfer");
        outRequest.setTransactionDate(LocalDate.now());

        // Create incoming transaction (income to destination)
        TransactionRequest inRequest = new TransactionRequest();
        inRequest.setAccountId(toAccount.getId());
        inRequest.setCategoryId(transferInCategory.getId());
        inRequest.setType("INCOME");
        inRequest.setAmount(request.getAmount());
        inRequest.setDescription(request.getDescription() != null ? request.getDescription() : "Transfer from account " + fromAccount.getName());
        inRequest.setNotes("Internal transfer");
        inRequest.setTransactionDate(LocalDate.now());

        // Save both transactions
        com.expense.tracker.transaction.entity.Transaction outTx = com.expense.tracker.transaction.entity.Transaction.builder()
                .user(fromAccount.getUser())
                .account(fromAccount)
                .category(transferOutCategory)
                .type("EXPENSE")
                .amount(request.getAmount())
                .description(outRequest.getDescription())
                .notes(outRequest.getNotes())
                .transactionDate(outRequest.getTransactionDate())
                .build();

        com.expense.tracker.transaction.entity.Transaction inTx = com.expense.tracker.transaction.entity.Transaction.builder()
                .user(toAccount.getUser())
                .account(toAccount)
                .category(transferInCategory)
                .type("INCOME")
                .amount(request.getAmount())
                .description(inRequest.getDescription())
                .notes(inRequest.getNotes())
                .transactionDate(inRequest.getTransactionDate())
                .build();

        transactionRepository.save(outTx);
        transactionRepository.save(inTx);

        TransferResponse response = new TransferResponse();
        response.setTransferId(outTx.getId());
        response.setFromAccountId(fromAccount.getId());
        response.setToAccountId(toAccount.getId());
        response.setAmount(request.getAmount());
        response.setStatus("COMPLETED");
        response.setDescription(outRequest.getDescription());
        response.setTimestamp(LocalDateTime.now());

        return response;
    }

    /**
     * Get transaction history for a specific account owned by the user.
     */
    @Transactional(readOnly = true)
    public List<com.expense.tracker.transaction.dto.TransactionResponse> getAccountTransactions(Long accountId) {
        Long userId = getCurrentUserId();

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountId));
        if (!account.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Account does not belong to you");
        }

        List<com.expense.tracker.transaction.entity.Transaction> transactions =
                transactionRepository.findByUserAndAccountOrderByTransactionDateDesc(userId, account.getId());

        return transactions.stream()
                .map(this::mapTransactionToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Find or create a category for the user.
     */
    private Category getOrCreateCategory(Long userId, String categoryName, String type) {
        Optional<Category> existing = categoryRepository.findByUserAndNameAndType(
                new User() {{ setId(userId); }}, categoryName, type);
        if (existing.isPresent()) {
            return existing.get();
        }

        User user = new User();
        user.setId(userId);
        Category category = Category.builder()
                .user(user)
                .name(categoryName)
                .type(type)
                .color("#000000")
                .build();
        return categoryRepository.save(category);
    }

    /**
     * Convert Account entity to AccountResponse DTO.
     */
    private AccountResponse mapToResponse(Account account) {
        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setUserId(account.getUser().getId());
        response.setName(account.getName());
        response.setType(account.getType());
        response.setBalance(account.getBalance());
        response.setCurrency(account.getCurrency());
        return response;
    }

    /**
     * Convert Transaction entity to TransactionResponse DTO.
     */
    private com.expense.tracker.transaction.dto.TransactionResponse mapTransactionToResponse(
            com.expense.tracker.transaction.entity.Transaction transaction) {
        com.expense.tracker.transaction.dto.TransactionResponse response =
                new com.expense.tracker.transaction.dto.TransactionResponse();
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