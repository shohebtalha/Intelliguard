package com.intelliguard.controller;

import com.intelliguard.dto.ApiResponse;
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

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> submitTransaction(
            @Valid @RequestBody TransactionRequest request) {

        TransactionResponse response = transactionService.processTransaction(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Transaction processed successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAllTransactions(
            @RequestParam(required = false) String status) {

        List<TransactionResponse> transactions = (status != null && !status.isBlank())
                ? transactionService.getTransactionsByStatus(status.toUpperCase())
                : transactionService.getAllTransactions();

        return ResponseEntity.ok(
                ApiResponse.success(transactions, "Fetched " + transactions.size() + " transactions"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionById(
            @PathVariable String id) {

        TransactionResponse response = transactionService.getTransactionById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Transaction found"));
    }
}