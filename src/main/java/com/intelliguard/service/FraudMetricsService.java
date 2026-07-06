package com.intelliguard.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class FraudMetricsService {

    private final MeterRegistry meterRegistry;

    public void recordDecision(String tenantId, String decision, long latencyMs, boolean mlUsed) {
        meterRegistry.counter("intelliguard_fraud_decisions_total",
                        "tenant", safe(tenantId),
                        "decision", safe(decision),
                        "ml_used", String.valueOf(mlUsed))
                .increment();

        Timer.builder("intelliguard_fraud_decision_latency")
                .description("Fraud decision latency")
                .tag("tenant", safe(tenantId))
                .tag("decision", safe(decision))
                .tag("ml_used", String.valueOf(mlUsed))
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(Duration.ofMillis(Math.max(latencyMs, 0)));
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
