package com.intelliguard.Kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Dead Letter Queue (DLQ) handler.
 *
 * WHAT IS A DLQ?
 * When a Kafka consumer fails to process a message
 * (exception thrown, timeout, etc.), Kafka can route
 * that message to a special "dead letter" topic instead
 * of losing it forever.
 *
 * WHY THIS MATTERS IN AN INTERVIEW:
 * "What happens if your Kafka consumer crashes mid-processing?"
 *
 * Answer: "Failed messages go to the DLQ. We log them,
 * alert the team, and have a manual retry process.
 * No transaction event is ever silently lost."
 *
 * This shows you think about failure scenarios —
 * a sign of production engineering maturity.
 */
@Component
@Slf4j
public class DeadLetterHandler {

    /**
     * Any message that fails processing 3 times gets routed here.
     * Topic name convention: original-topic.DLT (Dead Letter Topic)
     */
    @KafkaListener(
            topics = {"txn.enriched.DLT", "fraud.alerts.DLT"},
            groupId = "intelliguard-dlq-group"
    )
    public void handleDeadLetter(
            String rawMessage,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.error("[DLQ] Failed message from topic={} offset={} payload={}",
                topic, offset, rawMessage);

        // In production:
        // 1. Store in a dead_letter_events database table
        // 2. Send alert to PagerDuty / Slack
        // 3. Expose a /admin/dlq/retry endpoint for manual replay
    }
}