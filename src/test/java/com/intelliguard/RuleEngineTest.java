package com.intelliguard;

import com.intelliguard.engine.DecisionType;
import com.intelliguard.engine.RuleResult;
import com.intelliguard.engine.rules.AmountThresholdRule;
import com.intelliguard.engine.rules.CountryBlocklistRule;
import com.intelliguard.engine.rules.NightTimeRule;
import com.intelliguard.engine.rules.UnknownDeviceRule;
import com.intelliguard.entity.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests each rule in complete isolation.
 * No Spring context needed — just pure Java objects.
 * These run in milliseconds.
 */
class RuleEngineTest {

    private AmountThresholdRule amountRule;
    private CountryBlocklistRule countryRule;
    private NightTimeRule nightTimeRule;
    private UnknownDeviceRule deviceRule;

    private Transaction baseTransaction;

    @BeforeEach
    void setUp() {
        amountRule   = new AmountThresholdRule();
        countryRule  = new CountryBlocklistRule();
        nightTimeRule = new NightTimeRule();
        deviceRule   = new UnknownDeviceRule();

        // A normal, safe transaction
        baseTransaction = Transaction.builder()
                .senderId("USER_001")
                .receiverId("USER_002")
                .amount(new BigDecimal("5000.00"))
                .currency("INR")
                .country("IN")
                .paymentMethod("UPI")
                .deviceType("MOBILE")
                .ipAddress("192.168.1.1")
                .status("PENDING")
                .build();
    }

    // ─── AmountThresholdRule tests ────────────────────────────────

    @Test
    @DisplayName("Amount under 5L should pass")
    void amount_underThreshold_shouldPass() {
        baseTransaction.setAmount(new BigDecimal("4999.00"));
        RuleResult result = amountRule.evaluate(baseTransaction);
        assertThat(result.isTriggered()).isFalse();
        assertThat(result.getDecision()).isEqualTo(DecisionType.APPROVE);
    }

    @Test
    @DisplayName("Amount between 5L and 10L should trigger REVIEW")
    void amount_between5LAnd10L_shouldReview() {
        baseTransaction.setAmount(new BigDecimal("700000.00"));
        RuleResult result = amountRule.evaluate(baseTransaction);
        assertThat(result.isTriggered()).isTrue();
        assertThat(result.getDecision()).isEqualTo(DecisionType.REVIEW);
        assertThat(result.getScore()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Amount over 10L should trigger BLOCK")
    void amount_over10L_shouldBlock() {
        baseTransaction.setAmount(new BigDecimal("1500000.00"));
        RuleResult result = amountRule.evaluate(baseTransaction);
        assertThat(result.isTriggered()).isTrue();
        assertThat(result.getDecision()).isEqualTo(DecisionType.BLOCK);
        assertThat(result.getScore()).isGreaterThanOrEqualTo(0.8);
    }

    // ─── CountryBlocklistRule tests ───────────────────────────────

    @Test
    @DisplayName("India (IN) should pass country check")
    void country_india_shouldPass() {
        baseTransaction.setCountry("IN");
        RuleResult result = countryRule.evaluate(baseTransaction);
        assertThat(result.isTriggered()).isFalse();
    }

    @Test
    @DisplayName("Nigeria (NG) should be BLOCKED")
    void country_nigeria_shouldBlock() {
        baseTransaction.setCountry("NG");
        RuleResult result = countryRule.evaluate(baseTransaction);
        assertThat(result.isTriggered()).isTrue();
        assertThat(result.getDecision()).isEqualTo(DecisionType.BLOCK);
        assertThat(result.getScore()).isGreaterThanOrEqualTo(0.9);
    }

    @Test
    @DisplayName("North Korea (KP) should be BLOCKED")
    void country_northKorea_shouldBlock() {
        baseTransaction.setCountry("KP");
        RuleResult result = countryRule.evaluate(baseTransaction);
        assertThat(result.getDecision()).isEqualTo(DecisionType.BLOCK);
    }

    // ─── UnknownDeviceRule tests ──────────────────────────────────

    @Test
    @DisplayName("Known device MOBILE should pass")
    void device_mobile_shouldPass() {
        baseTransaction.setDeviceType("MOBILE");
        baseTransaction.setAmount(new BigDecimal("50000"));
        RuleResult result = deviceRule.evaluate(baseTransaction);
        assertThat(result.isTriggered()).isFalse();
    }

    @Test
    @DisplayName("UNKNOWN device with large amount should REVIEW")
    void device_unknown_largeAmount_shouldReview() {
        baseTransaction.setDeviceType("UNKNOWN");
        baseTransaction.setAmount(new BigDecimal("50000"));
        RuleResult result = deviceRule.evaluate(baseTransaction);
        assertThat(result.isTriggered()).isTrue();
        assertThat(result.getDecision()).isEqualTo(DecisionType.REVIEW);
    }

    @Test
    @DisplayName("UNKNOWN device with small amount should pass")
    void device_unknown_smallAmount_shouldPass() {
        baseTransaction.setDeviceType("UNKNOWN");
        baseTransaction.setAmount(new BigDecimal("100")); // under threshold
        RuleResult result = deviceRule.evaluate(baseTransaction);
        assertThat(result.isTriggered()).isFalse();
    }

    // ─── DecisionType severity tests ─────────────────────────────

    @Test
    @DisplayName("BLOCK should always win over REVIEW")
    void decisionType_blockBeatsReview() {
        assertThat(DecisionType.BLOCK.mostSevere(DecisionType.REVIEW))
                .isEqualTo(DecisionType.BLOCK);
    }

    @Test
    @DisplayName("REVIEW should beat APPROVE")
    void decisionType_reviewBeatsApprove() {
        assertThat(DecisionType.REVIEW.mostSevere(DecisionType.APPROVE))
                .isEqualTo(DecisionType.REVIEW);
    }
}