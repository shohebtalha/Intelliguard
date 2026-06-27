package com.intelliguard.engine;

import lombok.*;

/**
 * Every fraud rule returns a RuleResult.
 * It contains:
 * - triggered: did this rule fire?
 * - decision: what does this rule recommend?
 * - reason: human-readable explanation (shown in SHAP panel)
 * - score: how much does this rule contribute to fraud score (0.0 to 1.0)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleResult {

    private boolean triggered;
    private DecisionType decision;
    private String reason;
    private double score; // contribution to overall fraud score

    // Factory method for when rule does NOT fire
    public static RuleResult pass() {
        return RuleResult.builder()
                .triggered(false)
                .decision(DecisionType.APPROVE)
                .reason(null)
                .score(0.0)
                .build();
    }

    // Factory method for when rule fires and wants to BLOCK
    public static RuleResult block(String reason, double score) {
        return RuleResult.builder()
                .triggered(true)
                .decision(DecisionType.BLOCK)
                .reason(reason)
                .score(score)
                .build();
    }

    // Factory method for when rule fires and wants REVIEW
    public static RuleResult review(String reason, double score) {
        return RuleResult.builder()
                .triggered(true)
                .decision(DecisionType.REVIEW)
                .reason(reason)
                .score(score)
                .build();
    }
}