package com.intelliguard.service;

import com.intelliguard.dto.TransactionMapper;
import com.intelliguard.dto.TransactionRequest;
import com.intelliguard.dto.TransactionResponse;
import com.intelliguard.engine.RuleEngine;
import com.intelliguard.entity.Transaction;
import com.intelliguard.exception.TransactionNotFoundException;
import com.intelliguard.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final RuleEngine ruleEngine;
    private final VelocityService velocityService;

    @Transactional
    public TransactionResponse processTransaction(TransactionRequest request) {
        log.info("Processing transaction: sender={} amount={} {}",
                request.getSenderId(), request.getAmount(), request.getCurrency());

        long startTime = System.currentTimeMillis();

        // Step 1: Convert request → entity
        Transaction transaction = transactionMapper.toEntity(request);

        // Step 2: Pre-load velocity data into Redis
        // VelocityRule will READ this data when the engine runs it
        // We call this here so the count includes the CURRENT transaction
        VelocityService.VelocityMetrics metrics = velocityService
                .recordAndGet(request.getSenderId(), request.getAmount());

        log.debug("Velocity metrics: {}txn/10min, {}txn/1hr, ₹{}/1hr",
                metrics.getTxnCountLast10Min(),
                metrics.getTxnCountLastHour(),
                metrics.getTotalAmountLastHour());

        // Step 3: Run all fraud rules (including VelocityRule which reads Redis)
        RuleEngine.EngineResult result = ruleEngine.evaluate(transaction);

        // Step 4: Apply engine result to transaction
        transaction.setStatus(result.getDecision().name());
        transaction.setFraudScore(result.getFraudScore());
        transaction.setFlagReason(result.getFlagReason());

        // Step 5: Save to database
        Transaction saved = transactionRepository.save(transaction);

        long decisionTime = System.currentTimeMillis() - startTime;

        log.info("Decision: {} | score: {} | time: {}ms | rules triggered: {}",
                saved.getStatus(), saved.getFraudScore(),
                decisionTime, result.getTriggeredRuleCount());

        return transactionMapper.toResponse(saved, decisionTime);
    }

    public List<TransactionResponse> getAllTransactions() {
        return transactionRepository.findAll()
                .stream()
                .map(transactionMapper::toResponse)
                .collect(Collectors.toList());
    }

    public TransactionResponse getTransactionById(String id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(
                        "Transaction not found with id: " + id));
        return transactionMapper.toResponse(transaction);
    }

    public List<TransactionResponse> getTransactionsByStatus(String status) {
        return transactionRepository.findByStatus(status)
                .stream()
                .map(transactionMapper::toResponse)
                .collect(Collectors.toList());
    }
}