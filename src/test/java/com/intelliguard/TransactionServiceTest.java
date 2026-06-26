package com.intelliguard;

import com.intelliguard.dto.TransactionMapper;
import com.intelliguard.dto.TransactionRequest;
import com.intelliguard.dto.TransactionResponse;
import com.intelliguard.entity.Transaction;
import com.intelliguard.repository.TransactionRepository;
import com.intelliguard.service.TransactionService;
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

/**
 * Unit tests for TransactionService.
 * We use Mockito to fake the database — so tests run
 * without needing PostgreSQL running.
 *
 * @ExtendWith(MockitoExtension.class) — enables Mockito
 * @Mock — creates a fake version of the class
 * @InjectMocks — creates the real service, injecting the mocks
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

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
        // Arrange — set up fake database response
        Transaction mockTransaction = Transaction.builder()
                .id("test-uuid-123")
                .senderId("USER_001")
                .amount(new BigDecimal("5000.00"))
                .country("IN")
                .status("APPROVED")
                .fraudScore(new BigDecimal("0.05"))
                .build();

        when(transactionMapper.toEntity(any())).thenReturn(mockTransaction);
        when(transactionRepository.save(any())).thenReturn(mockTransaction);
        when(transactionMapper.toResponse(any(), any())).thenReturn(
                TransactionResponse.builder()
                        .status("APPROVED")
                        .fraudScore(new BigDecimal("0.05"))
                        .build()
        );

        // Act
        TransactionResponse response = transactionService.processTransaction(validRequest);

        // Assert
        assertThat(response.getStatus()).isEqualTo("APPROVED");
        assertThat(response.getFraudScore()).isLessThan(new BigDecimal("0.5"));
    }

    @Test
    @DisplayName("Transaction from high-risk country should be BLOCKED")
    void highRiskCountryTransaction_shouldBeBlocked() {
        validRequest.setCountry("NG"); // Nigeria — high risk

        Transaction mockTransaction = Transaction.builder()
                .id("test-uuid-456")
                .senderId("USER_001")
                .amount(new BigDecimal("5000.00"))
                .country("NG")
                .status("BLOCKED")
                .fraudScore(new BigDecimal("0.95"))
                .flagReason("High-risk country detected")
                .build();

        when(transactionMapper.toEntity(any())).thenReturn(mockTransaction);
        when(transactionRepository.save(any())).thenReturn(mockTransaction);
        when(transactionMapper.toResponse(any(), any())).thenReturn(
                TransactionResponse.builder()
                        .status("BLOCKED")
                        .fraudScore(new BigDecimal("0.95"))
                        .flagReason("High-risk country detected")
                        .build()
        );

        TransactionResponse response = transactionService.processTransaction(validRequest);

        assertThat(response.getStatus()).isEqualTo("BLOCKED");
        assertThat(response.getFraudScore()).isGreaterThan(new BigDecimal("0.9"));
        assertThat(response.getFlagReason()).contains("country");
    }

    @Test
    @DisplayName("Transaction over 5 lakh should be flagged for REVIEW")
    void highAmountTransaction_shouldBeReview() {
        validRequest.setAmount(new BigDecimal("600000.00")); // over ₹5 lakh

        Transaction mockTransaction = Transaction.builder()
                .id("test-uuid-789")
                .senderId("USER_001")
                .amount(new BigDecimal("600000.00"))
                .country("IN")
                .status("REVIEW")
                .fraudScore(new BigDecimal("0.60"))
                .flagReason("Amount exceeds high-risk threshold")
                .build();

        when(transactionMapper.toEntity(any())).thenReturn(mockTransaction);
        when(transactionRepository.save(any())).thenReturn(mockTransaction);
        when(transactionMapper.toResponse(any(), any())).thenReturn(
                TransactionResponse.builder()
                        .status("REVIEW")
                        .fraudScore(new BigDecimal("0.60"))
                        .build()
        );

        TransactionResponse response = transactionService.processTransaction(validRequest);

        assertThat(response.getStatus()).isEqualTo("REVIEW");
    }
}