package com.intelliguard.engine;

/**
 * The three possible outcomes of fraud analysis.
 * We use an enum (not a String) so it's impossible to
 * accidentally set status to "APPROVD" (typo).
 *
 * Priority order: BLOCK > REVIEW > APPROVE
 * If ANY rule says BLOCK, final decision is BLOCK.
 * If no BLOCK but any REVIEW, final decision is REVIEW.
 * Only if ALL rules say APPROVE, we approve.
 */
public enum DecisionType {
    APPROVE,
    REVIEW,
    BLOCK;

    /**
     * Returns the more severe of two decisions.
     * BLOCK beats REVIEW beats APPROVE.
     */
    public DecisionType mostSevere(DecisionType other) {
        if (this == BLOCK || other == BLOCK) return BLOCK;
        if (this == REVIEW || other == REVIEW) return REVIEW;
        return APPROVE;
    }
}