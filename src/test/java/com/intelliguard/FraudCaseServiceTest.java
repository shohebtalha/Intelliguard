package com.intelliguard;

import com.intelliguard.entity.FraudCase;
import com.intelliguard.entity.Transaction;
import com.intelliguard.repository.CaseNoteRepository;
import com.intelliguard.repository.FraudCaseRepository;
import com.intelliguard.service.CurrentUserService;
import com.intelliguard.service.FraudCaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudCaseServiceTest {

    @Mock
    private FraudCaseRepository fraudCaseRepository;

    @Mock
    private CaseNoteRepository caseNoteRepository;

    @Mock
    private CurrentUserService currentUserService;

    private FraudCaseService service;

    @BeforeEach
    void setUp() {
        service = new FraudCaseService(fraudCaseRepository, caseNoteRepository, currentUserService);
    }

    @Test
    void openCaseForDecision_shouldCreateCaseForReviewTransaction() {
        Transaction transaction = Transaction.builder()
                .id("txn-1")
                .tenantId("tenant-a")
                .senderId("sender-1")
                .status("REVIEW")
                .fraudScore(new BigDecimal("0.6000"))
                .flagReason("Velocity spike")
                .build();

        when(fraudCaseRepository.findByTenantIdAndTransactionId("tenant-a", "txn-1"))
                .thenReturn(Optional.empty());
        when(fraudCaseRepository.save(any(FraudCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FraudCase fraudCase = service.openCaseForDecision(transaction);

        assertThat(fraudCase.getStatus()).isEqualTo("OPEN");
        assertThat(fraudCase.getPriority()).isEqualTo("MEDIUM");
        assertThat(fraudCase.getTenantId()).isEqualTo("tenant-a");
        assertThat(fraudCase.getTransactionId()).isEqualTo("txn-1");
    }

    @Test
    void openCaseForDecision_shouldIgnoreApprovedTransaction() {
        Transaction transaction = Transaction.builder()
                .id("txn-1")
                .tenantId("tenant-a")
                .senderId("sender-1")
                .status("APPROVE")
                .build();

        FraudCase fraudCase = service.openCaseForDecision(transaction);

        assertThat(fraudCase).isNull();
        verify(fraudCaseRepository, never()).save(any());
    }

    @Test
    void assign_shouldRejectResolvedCase() {
        FraudCase fraudCase = FraudCase.builder()
                .id("case-1")
                .tenantId("tenant-a")
                .status("RESOLVED")
                .build();
        when(currentUserService.tenantId()).thenReturn("tenant-a");
        when(fraudCaseRepository.findByIdAndTenantId("case-1", "tenant-a"))
                .thenReturn(Optional.of(fraudCase));

        assertThatThrownBy(() -> service.assign("case-1", "analyst"))
                .isInstanceOf(IllegalStateException.class);
    }
}
