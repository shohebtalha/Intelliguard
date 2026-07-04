package com.intelliguard;

import com.intelliguard.engine.DecisionType;
import com.intelliguard.engine.RuleEngine;
import com.intelliguard.engine.rules.AmountThresholdRule;
import com.intelliguard.engine.rules.CountryBlocklistRule;
import com.intelliguard.engine.rules.NightTimeRule;
import com.intelliguard.engine.rules.UnknownDeviceRule;
import com.intelliguard.entity.Transaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify the complete rule engine pipeline.
 * These tests use real rule instances (no mocks) to verify
 * the entire fraud detection logic end-to-end.
 */
class IntegrationTest {

    /**
     * Build a RuleEngine with all rules except velocity and spike
     * (those require Redis/DB which aren't available in unit tests)
     */
    private RuleEngine buildEngine() {
        return new RuleEngine(List.of(
                new AmountThresholdRule(),
                new CountryBlocklistRule(),
                new NightTimeRule(),
                new UnknownDeviceRule()
        ));
    }

    private Transaction buildTransaction(String country, BigDecimal amount, String device) {
        return Transaction.builder()
                .senderId("TEST_USER")
                .receiverId("RECEIVER")
                .amount(amount)
                .currency("INR")
                .country(country)
                .paymentMethod("UPI")
                .deviceType(device)
                .status("PENDING")
                .build();
    }

    @Test
    @DisplayName("Safe transaction - India, small amount, known device - should APPROVE")
    void safeTransaction_shouldApprove() {
        RuleEngine engine = buildEngine();
        Transaction txn = buildTransaction("IN", new BigDecimal("5000"), "MOBILE");

        RuleEngine.EngineResult result = engine.evaluate(txn);

        assertThat(result.getDecision()).isEqualTo(DecisionType.APPROVE);
        assertThat(result.getTriggeredRuleCount()).isEqualTo(0);
        assertThat(result.getFraudScore()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("High risk country - Nigeria - should BLOCK regardless of amount")
    void nigeriaTransaction_shouldBlock() {
        RuleEngine engine = buildEngine();
        Transaction txn = buildTransaction("NG", new BigDecimal("100"), "MOBILE");

        RuleEngine.EngineResult result = engine.evaluate(txn);

        assertThat(result.getDecision()).isEqualTo(DecisionType.BLOCK);
        assertThat(result.getTriggeredRuleCount()).isGreaterThan(0);
        assertThat(result.getFlagReason()).contains("FATF");
    }

    @Test
    @DisplayName("Massive amount over 10L - should BLOCK")
    void massiveAmount_shouldBlock() {
        RuleEngine engine = buildEngine();
        Transaction txn = buildTransaction("IN", new BigDecimal("1500000"), "MOBILE");

        RuleEngine.EngineResult result = engine.evaluate(txn);

        assertThat(result.getDecision()).isEqualTo(DecisionType.BLOCK);
        assertThat(result.getFlagReason()).contains("AmountThresholdRule");
    }

    @Test
    @DisplayName("Multiple rules triggered - worst decision wins")
    void multipleRules_worstDecisionWins() {
        RuleEngine engine = buildEngine();
        // Nigeria (BLOCK) + Unknown device (REVIEW) + Large amount (REVIEW)
        // Final decision should be BLOCK (most severe)
        Transaction txn = buildTransaction("NG", new BigDecimal("600000"), "UNKNOWN");

        RuleEngine.EngineResult result = engine.evaluate(txn);

        assertThat(result.getDecision()).isEqualTo(DecisionType.BLOCK);
        assertThat(result.getTriggeredRuleCount()).isGreaterThanOrEqualTo(2);
        assertThat(result.getFraudScore().doubleValue()).isGreaterThan(0.9);
    }

    @Test
    @DisplayName("Score is capped at 1.0 even when multiple rules fire")
    void fraudScore_shouldNotExceedOne() {
        RuleEngine engine = buildEngine();
        Transaction txn = buildTransaction("KP", new BigDecimal("2000000"), "UNKNOWN");

        RuleEngine.EngineResult result = engine.evaluate(txn);

        assertThat(result.getFraudScore().doubleValue()).isLessThanOrEqualTo(1.0);
    }
}