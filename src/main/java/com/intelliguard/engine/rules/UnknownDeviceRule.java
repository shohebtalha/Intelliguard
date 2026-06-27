package com.intelliguard.engine.rules;

import com.intelliguard.engine.FraudRule;
import com.intelliguard.engine.RuleResult;
import com.intelliguard.entity.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Flags transactions from unknown or suspicious device types.
 * Device type comes from the client (mobile app, web browser).
 * If the device is null, empty, or "UNKNOWN" on a large transaction,
 * it's a red flag — could be a script or automated attack.
 */
@Component
public class UnknownDeviceRule implements FraudRule {

    private static final Set<String> KNOWN_DEVICES = Set.of("MOBILE", "DESKTOP", "TABLET");
    private static final BigDecimal DEVICE_CHECK_THRESHOLD = new BigDecimal("10000"); // ₹10k

    @Override
    public RuleResult evaluate(Transaction transaction) {
        String device = transaction.getDeviceType();
        boolean isUnknownDevice = device == null
                || device.isBlank()
                || device.equalsIgnoreCase("UNKNOWN")
                || !KNOWN_DEVICES.contains(device.toUpperCase());

        boolean isSignificantAmount = transaction.getAmount()
                .compareTo(DEVICE_CHECK_THRESHOLD) > 0;

        if (isUnknownDevice && isSignificantAmount) {
            return RuleResult.review(
                    "Transaction over ₹10,000 from unrecognized device type: '" + device + "'",
                    0.30
            );
        }

        return RuleResult.pass();
    }

    @Override
    public String getRuleName() {
        return "UnknownDeviceRule";
    }
}