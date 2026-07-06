package com.intelliguard;

import com.intelliguard.entity.AuditLog;
import com.intelliguard.entity.Transaction;
import com.intelliguard.repository.AuditLogRepository;
import com.intelliguard.service.AuditLogService;
import com.intelliguard.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private CurrentUserService currentUserService;

    private AuditLogService service;

    @BeforeEach
    void setUp() {
        service = new AuditLogService(auditLogRepository, currentUserService);
        when(currentUserService.username()).thenReturn("analyst");
    }

    @Test
    void logDecision_shouldCreateTamperEvidentHashChain() {
        Transaction transaction = Transaction.builder()
                .id("txn-1")
                .tenantId("tenant-a")
                .senderId("sender")
                .receiverId("receiver")
                .amount(new BigDecimal("100.00"))
                .currency("INR")
                .country("IN")
                .status("REVIEW")
                .fraudScore(new BigDecimal("0.5000"))
                .flagReason("Velocity")
                .modelVersion("model-v1")
                .build();
        AuditLog previous = AuditLog.builder().recordHash("previous-hash").build();
        when(auditLogRepository.findTopByTenantIdOrderByCreatedAtDesc("tenant-a"))
                .thenReturn(Optional.of(previous));

        service.logDecision(transaction, 12L);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        org.mockito.Mockito.verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getPreviousHash()).isEqualTo("previous-hash");
        assertThat(saved.getRecordHash()).isNotBlank();
        assertThat(saved.getRecordHash()).isNotEqualTo(saved.getPreviousHash());
    }
}
