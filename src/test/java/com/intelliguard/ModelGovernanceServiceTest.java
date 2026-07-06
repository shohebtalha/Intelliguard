package com.intelliguard;

import com.intelliguard.entity.ModelDriftSnapshot;
import com.intelliguard.entity.ModelRegistryEntry;
import com.intelliguard.repository.ModelDriftSnapshotRepository;
import com.intelliguard.repository.ModelInferenceMetricRepository;
import com.intelliguard.repository.ModelRegistryRepository;
import com.intelliguard.service.ModelGovernanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelGovernanceServiceTest {

    @Mock
    private ModelRegistryRepository modelRegistryRepository;
    @Mock
    private ModelInferenceMetricRepository metricRepository;
    @Mock
    private ModelDriftSnapshotRepository driftSnapshotRepository;

    private ModelGovernanceService service;

    @BeforeEach
    void setUp() {
        service = new ModelGovernanceService(
                modelRegistryRepository, metricRepository, driftSnapshotRepository);
    }

    @Test
    void ensureChampionRegistered_shouldCreateChampionWhenMissing() {
        when(modelRegistryRepository.findByVersion("model-v1")).thenReturn(Optional.empty());
        when(modelRegistryRepository.save(any(ModelRegistryEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ModelRegistryEntry entry = service.ensureChampionRegistered("model-v1", "classpath:model.onnx");

        assertThat(entry.getStatus()).isEqualTo("CHAMPION");
        assertThat(entry.getVersion()).isEqualTo("model-v1");
        assertThat(entry.getPromotedAt()).isNotNull();
    }

    @Test
    void createDriftSnapshot_shouldAlertWhenFallbackRateIsHigh() {
        when(metricRepository.countByModelVersionAndCreatedAtAfter(eq("model-v1"), any(LocalDateTime.class)))
                .thenReturn(100L);
        when(metricRepository.countByModelVersionAndFallbackAndCreatedAtAfter(
                eq("model-v1"), eq(true), any(LocalDateTime.class))).thenReturn(8L);
        when(metricRepository.countByModelVersionAndFraudProbabilityGreaterThanEqualAndCreatedAtAfter(
                eq("model-v1"), any(BigDecimal.class), any(LocalDateTime.class))).thenReturn(10L);
        when(metricRepository.averageScore(eq("model-v1"), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("0.25000"));
        when(driftSnapshotRepository.save(any(ModelDriftSnapshot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ModelDriftSnapshot snapshot = service.createDriftSnapshot("model-v1", 60);

        assertThat(snapshot.getStatus()).isEqualTo("ALERT");
        assertThat(snapshot.getFallbackRate()).isEqualByComparingTo("0.08000");
    }
}
