package com.intelliguard.engine;

import com.intelliguard.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * The RuleEngine is the brain of fraud detection.
 * It runs ALL rules against a transaction and produces
 * a final aggregated decision.
 *
 * HOW SPRING MAGIC WORKS HERE:
 * Spring automatically collects every class that implements
 * FraudRule and injects them as a List<FraudRule>.
 * So when you add a new rule class with @Component,
 * it automatically gets picked up — zero config changes.
 *
 * This is called "auto-wiring by type" — a favourite interview topic.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RuleEngine {

    // Spring auto-injects ALL FraudRule implementations here
    private final List<FraudRule> rules;

    /**
     * Run all rules against the transaction.
     * Returns an EngineResult with final decision, score, and all reasons.
     */
    public EngineResult evaluate(Transaction transaction) {
        log.debug("Running {} rules for transaction from sender: {}",
                rules.size(), transaction.getSenderId());

        List<String> triggeredReasons = new ArrayList<>();
        DecisionType finalDecision = DecisionType.APPROVE;
        double totalScore = 0.0;

        // Run every rule
        for (FraudRule rule : rules) {
            RuleResult result = rule.evaluate(transaction);

            if (result.isTriggered()) {
                log.debug("Rule [{}] triggered: {} (score: {})",
                        rule.getRuleName(), result.getReason(), result.getScore());

                triggeredReasons.add(rule.getRuleName() + ": " + result.getReason());
                totalScore += result.getScore();

                // Escalate decision if this rule is more severe
                finalDecision = finalDecision.mostSevere(result.getDecision());
            }
        }

        // Cap score at 1.0
        double fraudScore = Math.min(totalScore, 1.0);

        // Round to 4 decimal places
        BigDecimal fraudScoreDecimal = BigDecimal.valueOf(fraudScore)
                .setScale(4, RoundingMode.HALF_UP);

        String combinedReason = triggeredReasons.isEmpty()
                ? null
                : String.join(" | ", triggeredReasons);

        log.info("RuleEngine decision: {} | score: {} | reasons: {}",
                finalDecision, fraudScoreDecimal, combinedReason);

        return EngineResult.builder()
                .decision(finalDecision)
                .fraudScore(fraudScoreDecimal)
                .flagReason(combinedReason)
                .triggeredRuleCount(triggeredReasons.size())
                .build();
    }

    /**
     * Result returned by the engine after evaluating all rules.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EngineResult {
        private DecisionType decision;
        private BigDecimal fraudScore;
        private String flagReason;
        private int triggeredRuleCount;
    }
}