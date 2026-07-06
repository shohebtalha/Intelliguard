package com.intelliguard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelliguard.dto.TransactionEvent;
import com.intelliguard.entity.OutboxEvent;
import com.intelliguard.repository.OutboxEventRepository;
import com.intelliguard.service.CurrentUserService;
import com.intelliguard.service.OutboxEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @Mock
    private CurrentUserService currentUserService;

    private OutboxEventService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        service = new OutboxEventService(outboxEventRepository, kafkaTemplate, objectMapper, currentUserService);
    }

    @Test
    void publishPendingEvents_success_shouldMarkPublished() throws Exception {
        TransactionEvent payload = TransactionEvent.builder()
                .transactionId("txn-1")
                .senderId("sender-1")
                .eventType("TXN_PROCESSED")
                .eventTime(LocalDateTime.now())
                .build();
        OutboxEvent event = OutboxEvent.builder()
                .id("event-1")
                .tenantId("tenant-a")
                .topic("transactions")
                .eventKey("sender-1")
                .payload(objectMapper.writeValueAsString(payload))
                .status("PENDING")
                .attempts(0)
                .nextAttemptAt(LocalDateTime.now())
                .build();

        when(outboxEventRepository.claimBatch(eq("PENDING"), any(), eq(25))).thenReturn(List.of(event));
        when(kafkaTemplate.send(eq("transactions"), eq("sender-1"), any(TransactionEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        service.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo("PUBLISHED");
        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getLastError()).isNull();
    }

    @Test
    void publishPendingEvents_failure_shouldRetryPendingUntilMaxAttempts() throws Exception {
        TransactionEvent payload = TransactionEvent.builder()
                .transactionId("txn-1")
                .senderId("sender-1")
                .eventType("TXN_PROCESSED")
                .eventTime(LocalDateTime.now())
                .build();
        OutboxEvent event = OutboxEvent.builder()
                .id("event-1")
                .tenantId("tenant-a")
                .topic("transactions")
                .eventKey("sender-1")
                .payload(objectMapper.writeValueAsString(payload))
                .status("PENDING")
                .attempts(4)
                .nextAttemptAt(LocalDateTime.now())
                .build();

        CompletableFuture<SendResult<String, TransactionEvent>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("kafka unavailable"));
        when(outboxEventRepository.claimBatch(eq("PENDING"), any(), eq(25))).thenReturn(List.of(event));
        when(kafkaTemplate.send(eq("transactions"), eq("sender-1"), any(TransactionEvent.class)))
                .thenReturn(failed);

        service.publishPendingEvents();

        assertThat(event.getAttempts()).isEqualTo(5);
        assertThat(event.getStatus()).isEqualTo("FAILED");
        assertThat(event.getLastError()).contains("kafka unavailable");
    }
}
