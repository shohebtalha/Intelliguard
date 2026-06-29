package com.intelliguard.Kafka;

import com.intelliguard.config.KafkaTopicConfig;
import com.intelliguard.dto.TransactionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * TransactionConsumer listens to Kafka topics and reacts to events.
 *
 * In a full microservices system, this consumer would live in a
 * SEPARATE service (e.g. notification-service, analytics-service).
 * For our monolith, it's in the same app but still fully decoupled —
 * the producer has zero knowledge of who is consuming.
 *
 * @KafkaListener — Spring automatically runs this method
 * every time a new message arrives on the topic.
 * No polling, no cron job — it's event-driven.
 */
@Component
@Slf4j
public class TransactionConsumer {

    /**
     * Listens to ALL processed transactions.
     * Could feed into: analytics, reporting, ML training data collection.
     */
    @KafkaListener(
            topics = KafkaTopicConfig.TOPIC_TXN_ENRICHED,
            groupId = "intelliguard-analytics-group"
    )
    public void consumeProcessedTransaction(
            @Payload TransactionEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("[ANALYTICS] txn={} sender={} amount={} status={} score={} partition={} offset={}",
                event.getTransactionId(),
                event.getSenderId(),
                event.getAmount(),
                event.getStatus(),
                event.getFraudScore(),
                partition,
                offset);

        // In a real system: push to data warehouse, update ML training dataset, etc.
    }

    /**
     * Listens ONLY to fraud alerts (BLOCKED or REVIEW).
     * Could trigger: SMS to customer, email to fraud team, PagerDuty alert.
     */
    @KafkaListener(
            topics = KafkaTopicConfig.TOPIC_FRAUD_ALERTS,
            groupId = "intelliguard-alerts-group"
    )
    public void consumeFraudAlert(
            @Payload TransactionEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.warn("[FRAUD ALERT] ⚠️  txn={} sender={} amount=₹{} decision={} score={} reason={}",
                event.getTransactionId(),
                event.getSenderId(),
                event.getAmount(),
                event.getStatus(),
                event.getFraudScore(),
                event.getFlagReason());

        // In a real system:
        // - Send SMS: "Your transaction was blocked. Call us if this wasn't you."
        // - Email fraud team with full transaction details
        // - Create a case in the fraud investigation system
        // - Trigger PagerDuty if score > 0.95
    }
}