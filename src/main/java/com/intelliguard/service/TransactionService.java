package com.intelliguard.service;

import com.intelliguard.dto.TransactionMapper;
import com.intelliguard.dto.TransactionRequest;
import com.intelliguard.dto.TransactionResponse;
import com.intelliguard.entity.Transaction;
import com.intelliguard.exception.TransactionNotFoundException;
import com.intelliguard.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TransactionService contains ALL business logic.
 * The Controller just receives requests and calls this.
 * This service calls the Repository to save/fetch data.
 *
 * Rule: No database code in Controller. No HTTP code in Service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    /**
     * Main method — processes an incoming transaction through fraud detection.
     * Currently: saves with a basic decision. Day 3 we plug in the real rule engine.
     */
    @Transactional
    public TransactionResponse processTransaction(TransactionRequest request) {
        log.info("Processing transaction from sender: {} amount: {} {}",
                request.getSenderId(), request.getAmount(), request.getCurrency());

        // Start the clock — we track how long fraud detection takes
        long startTime = System.currentTimeMillis();

        // Step 1: Convert request to entity
        Transaction transaction = transactionMapper.toEntity(request);

        // Step 2: Run fraud analysis (placeholder — real engine comes Day 3)
        applyBasicFraudCheck(transaction);

        // Step 3: Save to database
        Transaction saved = transactionRepository.save(transaction);

        // Step 4: Calculate decision time
        long decisionTime = System.currentTimeMillis() - startTime;

        log.info("Transaction {} decision: {} in {}ms (score: {})",
                saved.getId(), saved.getStatus(), decisionTime, saved.getFraudScore());

        return transactionMapper.toResponse(saved, decisionTime);
    }

    /**
     * Fetch all transactions (for the dashboard table)
     */
    public List<TransactionResponse> getAllTransactions() {
        return transactionRepository.findAll()
                .stream()
                .map(transactionMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Fetch a single transaction by ID
     */
    public TransactionResponse getTransactionById(String id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction not found with id: " + id));
        return transactionMapper.toResponse(transaction);
    }

    /**
     * Fetch all transactions by status (APPROVED, BLOCKED, REVIEW)
     */
    public List<TransactionResponse> getTransactionsByStatus(String status) {
        return transactionRepository.findByStatus(status)
                .stream()
                .map(transactionMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Basic fraud check placeholder.
     * This gets REPLACED by the real RuleEngine on Day 3.
     * For now: block transactions over ₹5,00,000 or from Nigeria.
     */
    private void applyBasicFraudCheck(Transaction transaction) {
        BigDecimal HIGH_RISK_AMOUNT = new BigDecimal("500000");

        if ("NG".equalsIgnoreCase(transaction.getCountry()) ||
                "KP".equalsIgnoreCase(transaction.getCountry())) {
            transaction.setStatus("BLOCKED");
            transaction.setFraudScore(new BigDecimal("0.95"));
            transaction.setFlagReason("High-risk country detected");
        } else if (transaction.getAmount().compareTo(HIGH_RISK_AMOUNT) > 0) {
            transaction.setStatus("REVIEW");
            transaction.setFraudScore(new BigDecimal("0.60"));
            transaction.setFlagReason("Amount exceeds high-risk threshold");
        } else {
            transaction.setStatus("APPROVED");
            transaction.setFraudScore(new BigDecimal("0.05"));
            transaction.setFlagReason(null);
        }
    }
}