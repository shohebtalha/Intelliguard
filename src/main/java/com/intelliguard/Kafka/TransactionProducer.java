package com.intelliguard.Kafka;

import com.intelliguard.config.KafkaTopicConfig;
import com.intelliguard.dto.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * TransactionProducer publishes events to Kafka topics.
 *
 * HOW KAFKA WORKS (simple version):
 * - Producer sends a message to a TOPIC
 * - Kafka stores it durably (even if consumers are offline)
 * - Consumer reads from the topic when ready
 *
 * WHY ASYNC?
 * We don't want the HTTP response to wait for Kafka.
 * The fraud decision is done — save to DB and respond immediately.
 * Kafka publishing happens in the background.
 *
 * This is the "fire and forget" pattern for non-critical side effects.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionProducer {

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    /**
     * Publish every processed transaction to txn.enriched topic.
     * Key = senderId so all transactions from same sender go to same partition
     * (preserves ordering per sender).
     */
    public void publishTransactionProcessed(TransactionEvent event) {
        publish(KafkaTopicConfig.TOPIC_TXN_ENRICHED, event.getSenderId(), event);
    }

    /**
     * Publish BLOCKED or REVIEW transactions to fraud.alerts topic.
     * This is what triggers real-time alerts on the dashboard.
     */
    public void publishFraudAlert(TransactionEvent event) {
        publish(KafkaTopicConfig.TOPIC_FRAUD_ALERTS, event.getTransactionId(), event);
        log.warn("FRAUD ALERT published: txn={} sender={} score={} reason={}",
                event.getTransactionId(), event.getSenderId(),
                event.getFraudScore(), event.getFlagReason());
    }

    /**
     * Core publish method.
     * CompletableFuture lets us handle success/failure async
     * without blocking the main thread.
     */
    private void publish(String topic, String key, TransactionEvent event) {
        CompletableFuture<SendResult<String, TransactionEvent>> future =
                kafkaTemplate.send(topic, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish to topic {}: {}", topic, ex.getMessage());
            } else {
                log.debug("Published to topic={} partition={} offset={}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}