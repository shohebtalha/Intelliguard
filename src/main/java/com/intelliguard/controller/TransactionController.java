package com.intelliguard.controller;

import com.intelliguard.dto.TransactionRequest;
import com.intelliguard.dto.TransactionResponse;
import com.intelliguard.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller is the FRONT DOOR of our application.
 * It receives HTTP requests and hands them to the Service.
 * It does NOT contain any business logic — just routing.
 *
 * @RestController = @Controller + @ResponseBody (auto-converts to JSON)
 * @RequestMapping = all endpoints in this class start with /api/transactions
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // allows React frontend to call this API
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * POST /api/transactions
     * Submit a new transaction for fraud analysis.
     *
     * @Valid triggers the validation annotations on TransactionRequest
     * Returns 201 CREATED with the fraud decision in the response body
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> submitTransaction(
            @Valid @RequestBody TransactionRequest request) {

        log.info("Received transaction request from sender: {}", request.getSenderId());
        TransactionResponse response = transactionService.processTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/transactions
     * Fetch all transactions (for the dashboard table).
     * Optional: filter by status with ?status=BLOCKED
     */
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getAllTransactions(
            @RequestParam(required = false) String status) {

        List<TransactionResponse> transactions;

        if (status != null && !status.isBlank()) {
            transactions = transactionService.getTransactionsByStatus(status.toUpperCase());
        } else {
            transactions = transactionService.getAllTransactions();
        }

        return ResponseEntity.ok(transactions);
    }

    /**
     * GET /api/transactions/{id}
     * Fetch a single transaction by its UUID.
     * Returns 404 if not found (handled by GlobalExceptionHandler).
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable String id) {
        TransactionResponse response = transactionService.getTransactionById(id);
        return ResponseEntity.ok(response);
    }
}