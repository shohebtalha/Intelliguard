package com.intelliguard.engine.rules;

import com.intelliguard.engine.FraudRule;
import com.intelliguard.engine.RuleResult;
import com.intelliguard.entity.Transaction;
import com.intelliguard.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Detects when a transaction amount is abnormally high
 * compared to the sender's recent transaction history.
 *
 * Example:
 * USER_001 normally sends ₹500–₹2,000 via UPI.
 * Suddenly they send ₹95,000.
 * That's 47x their average — highly suspicious.
 *
 * This is called "amount anomaly detection" and is
 * one of the most effective fraud signals because
 * fraudsters often go big when they get access to an account.
 */
@Component
@RequiredArgsConstructor
public class AmountSpikeRule implements FraudRule {

    private final TransactionRepository transactionRepository;

    // If current amount is more than 10x the recent average → flag
    private static final BigDecimal SPIKE_MULTIPLIER = new BigDecimal("10");
    // Minimum average to compare against (ignore new accounts with tiny history)
    private static final BigDecimal MIN_AVERAGE = new BigDecimal("100");
    // Only flag if amount is also meaningfully large
    private static final BigDecimal MIN_SPIKE_AMOUNT = new BigDecimal("5000");

    @Override
    public RuleResult evaluate(Transaction transaction) {
        // Look at last 30 days of this sender's transactions
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        BigDecimal recentTotal = transactionRepository
                .sumRecentAmount(transaction.getSenderId(), thirtyDaysAgo);

        long recentCount = transactionRepository
                .countRecentTransactions(transaction.getSenderId(), thirtyDaysAgo);

        // Need at least 3 transactions to establish a baseline
        if (recentCount < 3 || recentTotal.compareTo(BigDecimal.ZERO) == 0) {
            return RuleResult.pass(); // not enough history to compare
        }

        // Calculate average transaction amount
        BigDecimal average = recentTotal.divide(
                new BigDecimal(recentCount), 2, RoundingMode.HALF_UP);

        // Only check if average is meaningful
        if (average.compareTo(MIN_AVERAGE) < 0) {
            return RuleResult.pass();
        }

        // Calculate the spike ratio: current / average
        BigDecimal spikeRatio = transaction.getAmount().divide(average, 2, RoundingMode.HALF_UP);
        boolean isSpike = spikeRatio.compareTo(SPIKE_MULTIPLIER) > 0;
        boolean isLargeEnough = transaction.getAmount().compareTo(MIN_SPIKE_AMOUNT) > 0;

        if (isSpike && isLargeEnough) {
            return RuleResult.review(
                    "Amount spike detected: ₹" + transaction.getAmount() +
                            " is " + spikeRatio + "x the sender's 30-day average of ₹" + average,
                    0.60
            );
        }

        return RuleResult.pass();
    }

    @Override
    public String getRuleName() {
        return "AmountSpikeRule";
    }
}