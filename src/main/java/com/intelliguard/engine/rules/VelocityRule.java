package com.intelliguard.engine.rules;

import com.intelliguard.engine.FraudRule;
import com.intelliguard.engine.RuleResult;
import com.intelliguard.entity.Transaction;
import com.intelliguard.service.VelocityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * VelocityRule uses VelocityService metrics to flag
 * transactions that are happening suspiciously fast.
 *
 * This rule is different from the others — it doesn't
 * just look at the current transaction. It looks at
 * HISTORY stored in Redis to make its decision.
 *
 * This is what makes it powerful. A single ₹5,000 UPI
 * payment looks completely safe. But if it's the 15th
 * such payment in 8 minutes, it's definitely fraud.
 */
@Component
@RequiredArgsConstructor
public class VelocityRule implements FraudRule {

    private final VelocityService velocityService;

    @Override
    public RuleResult evaluate(Transaction transaction) {

        // VelocityRule should only READ, not record again
        VelocityService.VelocityMetrics metrics = velocityService.getMetrics(transaction.getSenderId());

        // Check 1: Too many transactions in 10 minutes → BLOCK
        if (metrics.isTxnCountSuspicious()) {
            return RuleResult.block(
                    "Velocity spike: " + metrics.getTxnCountLast10Min() +
                            " transactions in last 10 minutes (max allowed: 10)",
                    0.85
            );
        }

        // Check 2: Too many transactions in 1 hour → REVIEW
        if (metrics.isTxnRateSuspicious()) {
            return RuleResult.review(
                    "High transaction rate: " + metrics.getTxnCountLastHour() +
                            " transactions in last hour (max allowed: 30)",
                    0.50
            );
        }

        // Check 3: Total amount too high in 1 hour → REVIEW
        if (metrics.isAmountSuspicious()) {
            return RuleResult.review(
                    "High cumulative amount: ₹" + metrics.getTotalAmountLastHour() +
                            " sent in last hour (max allowed: ₹5,00,000)",
                    0.55
            );
        }

        return RuleResult.pass();
    }

    @Override
    public String getRuleName() {
        return "VelocityRule";
    }
}