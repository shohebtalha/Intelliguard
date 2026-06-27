package com.intelliguard.engine.rules;

import com.intelliguard.engine.FraudRule;
import com.intelliguard.engine.RuleResult;
import com.intelliguard.entity.Transaction;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Blocks transactions originating from countries flagged as
 * high-risk by FATF (Financial Action Task Force) —
 * the real international body that publishes these lists.
 *
 * Mentioning FATF in an interview shows you understand
 * the regulatory context, not just the code.
 */
@Component
public class CountryBlocklistRule implements FraudRule {

    /**
     * FATF high-risk and sanctioned jurisdictions.
     * Using ISO 3166-1 alpha-2 country codes.
     * In a real system this list would be loaded from a database
     * so compliance teams can update it without a code deploy.
     */
    private static final Set<String> HIGH_RISK_COUNTRIES = Set.of(
            "NG",  // Nigeria
            "KP",  // North Korea
            "IR",  // Iran
            "MM",  // Myanmar
            "PK",  // Pakistan (FATF grey list)
            "SY",  // Syria
            "YE",  // Yemen
            "AF"   // Afghanistan
    );

    private static final Set<String> REVIEW_COUNTRIES = Set.of(
            "VN",  // Vietnam
            "PH",  // Philippines
            "BD"   // Bangladesh
    );

    @Override
    public RuleResult evaluate(Transaction transaction) {
        String country = transaction.getCountry().toUpperCase();

        if (HIGH_RISK_COUNTRIES.contains(country)) {
            return RuleResult.block(
                    "Transaction from FATF high-risk country: " + country,
                    0.95
            );
        }

        if (REVIEW_COUNTRIES.contains(country)) {
            return RuleResult.review(
                    "Transaction from elevated-risk country: " + country + " — requires review",
                    0.40
            );
        }

        return RuleResult.pass();
    }

    @Override
    public String getRuleName() {
        return "CountryBlocklistRule";
    }
}