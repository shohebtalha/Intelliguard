package com.intelliguard.engine.rules;

import com.intelliguard.engine.FraudRule;
import com.intelliguard.engine.RuleResult;
import com.intelliguard.entity.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Flags transactions that exceed amount thresholds.
 *
 * Logic:
 * - Over ₹10,00,000 (10 lakh) → BLOCK immediately
 * - Over ₹5,00,000 (5 lakh)  → REVIEW for manual check
 * - Under ₹5,00,000          → APPROVE (from amount perspective)
 *
 * @Component makes Spring manage this as a bean —
 * it gets auto-detected and injected into the RuleEngine list.
 */
@Component
public class AmountThresholdRule implements FraudRule {

    private static final BigDecimal BLOCK_THRESHOLD  = new BigDecimal("1000000"); // ₹10 lakh
    private static final BigDecimal REVIEW_THRESHOLD = new BigDecimal("500000");  // ₹5 lakh

    @Override
    public RuleResult evaluate(Transaction transaction) {
        BigDecimal amount = transaction.getAmount();

        if (amount.compareTo(BLOCK_THRESHOLD) >= 0) {
            return RuleResult.block(
                    "Amount ₹" + amount + " exceeds maximum single-transaction limit of ₹10,00,000",
                    0.80
            );
        }

        if (amount.compareTo(REVIEW_THRESHOLD) >= 0) {
            return RuleResult.review(
                    "Amount ₹" + amount + " exceeds ₹5,00,000 — flagged for manual review",
                    0.45
            );
        }

        return RuleResult.pass();
    }

    @Override
    public String getRuleName() {
        return "AmountThresholdRule";
    }
}