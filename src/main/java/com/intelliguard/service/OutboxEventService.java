package com.intelliguard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelliguard.dto.TransactionEvent;
import com.intelliguard.entity.OutboxEvent;
import com.intelliguard.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_FAILED = "FAILED";
    private static final int MAX_ATTEMPTS = 5;
    private static final int BATCH_SIZE = 25;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final CurrentUserService currentUserService;

    @Transactional
    public void enqueue(String topic, String key, TransactionEvent event) {
        try {
            outboxEventRepository.save(OutboxEvent.builder()
                    .tenantId(currentUserService.tenantId())
                    .topic(topic)
                    .eventKey(key)
                    .payload(objectMapper.writeValueAsString(event))
                    .status(STATUS_PENDING)
                    .attempts(0)
                    .nextAttemptAt(LocalDateTime.now())
                    .build());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to enqueue outbox event", ex);
        }
    }

    @Scheduled(fixedDelayString = "${app.outbox.publish-delay-ms:2000}")
    @Transactional
    public void publishPendingEvents() {
        outboxEventRepository.claimBatch(STATUS_PENDING, LocalDateTime.now(), BATCH_SIZE)
                .forEach(event -> {
                    event.setStatus(STATUS_IN_PROGRESS);
                    publish(event);
                });
    }

    private void publish(OutboxEvent event) {
        try {
            TransactionEvent payload = objectMapper.readValue(event.getPayload(), TransactionEvent.class);
            kafkaTemplate.send(event.getTopic(), event.getEventKey(), payload).get();
            event.setStatus(STATUS_PUBLISHED);
            event.setPublishedAt(LocalDateTime.now());
            event.setLastError(null);
            log.debug("Published outbox event {} to {}", event.getId(), event.getTopic());
        } catch (Exception ex) {
            int attempts = event.getAttempts() + 1;
            event.setAttempts(attempts);
            event.setLastError(ex.getMessage());
            event.setNextAttemptAt(LocalDateTime.now().plusSeconds(Math.min(60, attempts * 10L)));
            if (attempts >= MAX_ATTEMPTS) {
                event.setStatus(STATUS_FAILED);
            } else {
                event.setStatus(STATUS_PENDING);
            }
            log.warn("Outbox publish failed for event {} attempt {}: {}",
                    event.getId(), attempts, ex.getMessage());
        }
    }
}
