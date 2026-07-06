package com.intelliguard.config;

import com.intelliguard.repository.FraudCaseRepository;
import com.intelliguard.repository.ModelInferenceMetricRepository;
import com.intelliguard.repository.OutboxEventRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
public class ObservabilityConfig {

    private final MeterRegistry meterRegistry;
    private final OutboxEventRepository outboxEventRepository;
    private final FraudCaseRepository fraudCaseRepository;
    private final ModelInferenceMetricRepository modelInferenceMetricRepository;

    @PostConstruct
    public void registerGauges() {
        Gauge.builder("intelliguard_outbox_pending", outboxEventRepository,
                        repository -> repository.countByStatus("PENDING"))
                .description("Number of outbox events waiting to be published")
                .register(meterRegistry);

        Gauge.builder("intelliguard_cases_open", fraudCaseRepository,
                        repository -> repository.countByStatus("OPEN"))
                .description("Number of open fraud investigation cases")
                .register(meterRegistry);

        Gauge.builder("intelliguard_model_fallbacks_15m", modelInferenceMetricRepository,
                        repository -> repository.countByFallbackAndCreatedAtAfter(
                                true, LocalDateTime.now().minusMinutes(15)))
                .description("Number of model fallback inferences in the last 15 minutes")
                .register(meterRegistry);
    }
}
