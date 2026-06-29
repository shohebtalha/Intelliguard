package com.intelliguard;

import com.intelliguard.Kafka.TransactionProducer;
import com.intelliguard.dto.TransactionMapper;
import com.intelliguard.dto.TransactionRequest;
import com.intelliguard.dto.TransactionResponse;
import com.intelliguard.engine.DecisionType;
import com.intelliguard.engine.RuleEngine;
import com.intelliguard.entity.Transaction;
import com.intelliguard.repository.TransactionRepository;
import com.intelliguard.service.TransactionService;
import com.intelliguard.service.VelocityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private VelocityService velocityService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private RuleEngine ruleEngine;

    @Mock
    private TransactionProducer transactionProducer;

    @InjectMocks
    private TransactionService transactionService;

    private TransactionRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = TransactionRequest.builder()
                .senderId("USER_001")
                .receiverId("USER_002")
                .amount(new BigDecimal("5000.00"))
                .currency("INR")
                .country("IN")
                .paymentMethod("UPI")
                .deviceType("MOBILE")
                .ipAddress("192.168.1.1")
                .build();
    }

    @Test
    @DisplayName("Normal transaction from India should be APPROVED")
    void normalTransaction_shouldBeApproved() {
        Transaction mockTransaction = Transaction.builder()
                .id("test-uuid-123")
                .senderId("USER_001")
                .amount(new BigDecimal("5000.00"))
                .country("IN")
                .status("PENDING")
                .build();

        when(velocityService.recordAndGet(any(), any())).thenReturn(
                VelocityService.VelocityMetrics.builder()
                        .txnCountLast10Min(1L)
                        .txnCountLastHour(1L)
                        .totalAmountLastHour(new BigDecimal("5000"))
                        .isTxnCountSuspicious(false)
                        .isTxnRateSuspicious(false)
                        .isAmountSuspicious(false)
                        .build()
        );

        when(transactionMapper.toEntity(any())).thenReturn(mockTransaction);
        when(ruleEngine.evaluate(any())).thenReturn(
                RuleEngine.EngineResult.builder()
                        .decision(DecisionType.APPROVE)
                        .fraudScore(new BigDecimal("0.05"))
                        .flagReason(null)
                        .triggeredRuleCount(0)
                        .build()
        );
        when(transactionRepository.save(any())).thenReturn(mockTransaction);
        when(transactionMapper.toResponse(any(), any())).thenReturn(
                TransactionResponse.builder()
                        .status("APPROVE")
                        .fraudScore(new BigDecimal("0.05"))
                        .build()
        );

        TransactionResponse response = transactionService.processTransaction(validRequest);

        assertThat(response.getStatus()).isEqualTo("APPROVE");
        assertThat(response.getFraudScore()).isLessThan(new BigDecimal("0.5"));
    }

    @Test
    @DisplayName("Transaction from high-risk country should be BLOCKED")
    void highRiskCountryTransaction_shouldBeBlocked() {
        validRequest.setCountry("NG");

        Transaction mockTransaction = Transaction.builder()
                .id("test-uuid-456")
                .senderId("USER_001")
                .amount(new BigDecimal("5000.00"))
                .country("NG")
                .status("PENDING")
                .build();

        when(velocityService.recordAndGet(any(), any())).thenReturn(
                VelocityService.VelocityMetrics.builder()
                        .txnCountLast10Min(1L)
                        .txnCountLastHour(1L)
                        .totalAmountLastHour(new BigDecimal("5000"))
                        .isTxnCountSuspicious(false)
                        .isTxnRateSuspicious(false)
                        .isAmountSuspicious(false)
                        .build()
        );

        when(transactionMapper.toEntity(any())).thenReturn(mockTransaction);
        when(ruleEngine.evaluate(any())).thenReturn(
                RuleEngine.EngineResult.builder()
                        .decision(DecisionType.BLOCK)
                        .fraudScore(new BigDecimal("0.95"))
                        .flagReason("CountryBlocklistRule: Transaction from FATF high-risk country: NG")
                        .triggeredRuleCount(1)
                        .build()
        );
        when(transactionRepository.save(any())).thenReturn(mockTransaction);
        when(transactionMapper.toResponse(any(), any())).thenReturn(
                TransactionResponse.builder()
                        .status("BLOCK")
                        .fraudScore(new BigDecimal("0.95"))
                        .flagReason("High-risk country detected")
                        .build()
        );

        TransactionResponse response = transactionService.processTransaction(validRequest);

        assertThat(response.getStatus()).isEqualTo("BLOCK");
        assertThat(response.getFraudScore()).isGreaterThan(new BigDecimal("0.9"));
    }

    @Test
    @DisplayName("Transaction over 5 lakh should be flagged for REVIEW")
    void highAmountTransaction_shouldBeReview() {
        validRequest.setAmount(new BigDecimal("600000.00"));

        Transaction mockTransaction = Transaction.builder()
                .id("test-uuid-789")
                .senderId("USER_001")
                .amount(new BigDecimal("600000.00"))
                .country("IN")
                .status("PENDING")
                .build();
        when(velocityService.recordAndGet(any(), any())).thenReturn(
                VelocityService.VelocityMetrics.builder()
                        .txnCountLast10Min(1L)
                        .txnCountLastHour(1L)
                        .totalAmountLastHour(new BigDecimal("5000"))
                        .isTxnCountSuspicious(false)
                        .isTxnRateSuspicious(false)
                        .isAmountSuspicious(false)
                        .build()
        );

        when(transactionMapper.toEntity(any())).thenReturn(mockTransaction);
        when(ruleEngine.evaluate(any())).thenReturn(
                RuleEngine.EngineResult.builder()
                        .decision(DecisionType.REVIEW)
                        .fraudScore(new BigDecimal("0.45"))
                        .flagReason("AmountThresholdRule: Amount exceeds 5L threshold")
                        .triggeredRuleCount(1)
                        .build()
        );
        when(transactionRepository.save(any())).thenReturn(mockTransaction);
        when(transactionMapper.toResponse(any(), any())).thenReturn(
                TransactionResponse.builder()
                        .status("REVIEW")
                        .fraudScore(new BigDecimal("0.45"))
                        .build()
        );

        TransactionResponse response = transactionService.processTransaction(validRequest);

        assertThat(response.getStatus()).isEqualTo("REVIEW");
    }
}