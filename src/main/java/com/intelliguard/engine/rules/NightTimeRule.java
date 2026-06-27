package com.intelliguard.engine.rules;

import com.intelliguard.engine.FraudRule;
import com.intelliguard.engine.RuleResult;
import com.intelliguard.entity.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * Flags large transactions happening between 1am and 5am.
 * Why? Legitimate large transfers (business payments, property)
 * almost never happen in the middle of the night.
 * Fraudsters often operate at night hoping fewer analysts are watching.
 * We only flag if BOTH conditions are true:
 * 1. It's between 1am–5am
 * 2. Amount is over ₹50,000
 * A ₹100 transaction at 3am is fine. A ₹2,00,000 transaction at 3am is suspicious.
 */
@Component
public class NightTimeRule implements FraudRule {

    private static final LocalTime NIGHT_START = LocalTime.of(1, 0);  // 1:00 AM
    private static final LocalTime NIGHT_END   = LocalTime.of(5, 0);  // 5:00 AM
    private static final BigDecimal NIGHT_AMOUNT_THRESHOLD = new BigDecimal("50000"); // ₹50k

    @Override
    public RuleResult evaluate(Transaction transaction) {
        LocalTime now = LocalTime.now();
        boolean isNightTime = now.isAfter(NIGHT_START) && now.isBefore(NIGHT_END);
        boolean isLargeAmount = transaction.getAmount().compareTo(NIGHT_AMOUNT_THRESHOLD) > 0;

        if (isNightTime && isLargeAmount) {
            return RuleResult.review(
                    "Large transaction of ₹" + transaction.getAmount() +
                            " at unusual hour (" + now + ") — flagged for review",
                    0.35
            );
        }

        return RuleResult.pass();
    }

    @Override
    public String getRuleName() {
        return "NightTimeRule";
    }
}