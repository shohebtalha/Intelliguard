package com.intelliguard.engine;

import com.intelliguard.entity.Transaction;

/**
 * The contract every fraud rule must follow.
 * This is the Strategy Pattern:
 * - We define one interface (FraudRule)
 * - Each rule is a separate class implementing this interface
 * - The RuleEngine runs ALL rules without knowing their details
 *
 * Adding a new fraud rule = create a new class, implement evaluate().
 * Zero changes to existing code. This is the Open/Closed Principle.
 *
 * Interview tip: "I used the Strategy pattern so each rule is
 * independently testable and new rules can be added without
 * modifying the engine."
 */
public interface FraudRule {

    /**
     * Evaluate this rule against a transaction.
     * @param transaction the transaction to analyze
     * @return RuleResult with decision and reason
     */
    RuleResult evaluate(Transaction transaction);

    /**
     * Human-readable name of this rule.
     * Used in logs and the SHAP explanation panel.
     */
    String getRuleName();
}