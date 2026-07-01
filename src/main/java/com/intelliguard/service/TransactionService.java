package com.intelliguard.service;

import com.intelliguard.dto.TransactionEvent;
import com.intelliguard.dto.TransactionMapper;
import com.intelliguard.dto.TransactionRequest;
import com.intelliguard.dto.TransactionResponse;
import com.intelliguard.engine.DecisionType;
import com.intelliguard.engine.RuleEngine;
import com.intelliguard.entity.Transaction;
import com.intelliguard.exception.TransactionNotFoundException;
import com.intelliguard.Kafka.TransactionProducer;
import com.intelliguard.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final MLScoringService mlScoringService;
    private final TransactionProducer transactionProducer;

    // ML score thresholds
    private static final double ML_BLOCK_THRESHOLD  = 0.75;
    private static final double ML_REVIEW_THRESHOLD = 0.45;

    // Weight: 60% ML, 40% rules (ML is more powerful)
    private static final double ML_WEIGHT   = 0.6;
    private static final double RULE_WEIGHT = 0.4;

    @Transactional
    public TransactionResponse processTransaction(TransactionRequest request) {
        log.info("Processing transaction: sender={} amount={} {}",
                request.getSenderId(), request.getAmount(), request.getCurrency());

        long startTime = System.currentTimeMillis();

        // Step 1: Convert request → entity
        Transaction transaction = transactionMapper.toEntity(request);

        // Step 2: Record velocity in Redis
        velocityService.recordAndGet(request.getSenderId(), request.getAmount());

        // Step 3: Run rule engine
        RuleEngine.EngineResult ruleResult = ruleEngine.evaluate(transaction);
        double ruleScore = ruleResult.getFraudScore().doubleValue();

        // Step 4: Run ML model
        double mlScore = mlScoringService.predictFraudProbability(transaction);
        boolean mlAvailable = mlScore >= 0;

        // Step 5: Combine scores — weighted ensemble
        double finalScore;
        DecisionType finalDecision;

        if (mlAvailable) {
            // Weighted combination: 60% ML + 40% rules
            finalScore = (mlScore * ML_WEIGHT) + (ruleScore * RULE_WEIGHT);

            // ML can override rules if very confident
            if (mlScore >= ML_BLOCK_THRESHOLD) {
                finalDecision = DecisionType.BLOCK;
            } else if (mlScore >= ML_REVIEW_THRESHOLD) {
                finalDecision = ruleResult.getDecision().mostSevere(DecisionType.REVIEW);
            } else {
                finalDecision = ruleResult.getDecision();
            }

            log.info("Scores — ML: {} Rules: {} Combined: {} Decision: {}",
                    String.format("%.4f", mlScore),
                    String.format("%.4f", ruleScore),
                    String.format("%.4f", finalScore),
                    finalDecision);
        } else {
            // ML not available — use rules only
            finalScore = ruleScore;
            finalDecision = ruleResult.getDecision();
            log.info("ML unavailable — rule-only score: {} decision: {}",
                    String.format("%.4f", ruleScore), finalDecision);
        }

        // Cap score at 1.0 and round
        BigDecimal finalScoreDecimal = BigDecimal.valueOf(Math.min(finalScore, 1.0))
                .setScale(4, RoundingMode.HALF_UP);

        // Step 6: Apply decision to transaction
        transaction.setStatus(finalDecision.name());
        transaction.setFraudScore(finalScoreDecimal);

        // Combine flag reasons
        String flagReason = ruleResult.getFlagReason();
        if (mlAvailable && mlScore >= ML_REVIEW_THRESHOLD) {
            String mlReason = String.format("MLScore: %.4f", mlScore);
            flagReason = flagReason != null
                    ? flagReason + " | " + mlReason
                    : mlReason;
        }
        transaction.setFlagReason(flagReason);

        // Step 7: Save to database
        Transaction saved = transactionRepository.save(transaction);
        long decisionTime = System.currentTimeMillis() - startTime;

        log.info("Final decision: {} | score: {} | time: {}ms",
                saved.getStatus(), saved.getFraudScore(), decisionTime);

        // Step 8: Publish to Kafka
        TransactionEvent event = buildEvent(saved, decisionTime);
        transactionProducer.publishTransactionProcessed(event);

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

    private TransactionEvent buildEvent(Transaction saved, long decisionTime) {
        return TransactionEvent.builder()
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
    }
}