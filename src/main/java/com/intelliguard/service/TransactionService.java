package com.intelliguard.service;

import com.intelliguard.dto.TransactionEvent;
import com.intelliguard.dto.TransactionMapper;
import com.intelliguard.dto.TransactionRequest;
import com.intelliguard.dto.TransactionResponse;
import com.intelliguard.engine.RuleEngine;
import com.intelliguard.entity.Transaction;
import com.intelliguard.exception.TransactionNotFoundException;
import com.intelliguard.Kafka.TransactionProducer;
import com.intelliguard.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final TransactionProducer transactionProducer;

    @Transactional
    public TransactionResponse processTransaction(TransactionRequest request) {
        log.info("Processing transaction: sender={} amount={} {}",
                request.getSenderId(), request.getAmount(), request.getCurrency());

        long startTime = System.currentTimeMillis();

        // Step 1: Convert request → entity
        Transaction transaction = transactionMapper.toEntity(request);

        // Step 2: Record velocity in Redis
        velocityService.recordAndGet(request.getSenderId(), request.getAmount());

        // Step 3: Run all fraud rules
        RuleEngine.EngineResult result = ruleEngine.evaluate(transaction);

        // Step 4: Apply decision to transaction
        transaction.setStatus(result.getDecision().name());
        transaction.setFraudScore(result.getFraudScore());
        transaction.setFlagReason(result.getFlagReason());

        // Step 5: Save to database
        Transaction saved = transactionRepository.save(transaction);
        long decisionTime = System.currentTimeMillis() - startTime;

        log.info("Decision: {} | score: {} | time: {}ms",
                saved.getStatus(), saved.getFraudScore(), decisionTime);

        // Step 6: Build Kafka event
        TransactionEvent event = TransactionEvent.builder()
                .transactionId(saved.getId())
                .senderId(saved.getSenderId())
                .receiverId(saved.getReceiverId())
                .amount(saved.getAmount())
                .currency(saved.getCurrency())
                .country(saved.getCountry())
                .paymentMethod(saved.getPaymentMethod())
                .deviceType(saved.getDeviceType())
                .ipAddress(saved.getIpAddress())
                .status(saved.getStatus())
                .fraudScore(saved.getFraudScore())
                .flagReason(saved.getFlagReason())
                .eventType("TXN_PROCESSED")
                .eventTime(LocalDateTime.now())
                .decisionTimeMs(decisionTime)
                .build();

        // Step 7: Publish to txn.enriched (every transaction)
        transactionProducer.publishTransactionProcessed(event);

        // Step 8: Publish to fraud.alerts (only BLOCK or REVIEW)
        if ("BLOCK".equals(saved.getStatus()) || "REVIEW".equals(saved.getStatus())) {
            event.setEventType("FRAUD_ALERT");
            transactionProducer.publishFraudAlert(event);
        }

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