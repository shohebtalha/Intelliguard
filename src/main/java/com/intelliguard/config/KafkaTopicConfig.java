package com.intelliguard.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Defines all Kafka topics IntelliGuard uses.
 * Spring Kafka auto-creates these topics on startup
 * if they don't already exist.
 *
 * OUR TOPICS:
 * 1. txn.raw— every incoming transaction gets published here
 * 2. txn.enriched  — after fraud analysis, enriched result published here
 * 3. fraud.alerts  — only BLOCKED/REVIEW transactions published here
 *
 * Think of topics like named channels on a walkie-talkie.
 * Anyone can publish to a channel, anyone can listen to it.
 */
@Configuration
public class KafkaTopicConfig {

    public static final String TOPIC_TXN_RAW      = "txn.raw";
    public static final String TOPIC_TXN_ENRICHED  = "txn.enriched";
    public static final String TOPIC_FRAUD_ALERTS  = "fraud.alerts";

    @Bean
    public NewTopic transactionRawTopic() {
        return TopicBuilder.name(TOPIC_TXN_RAW)
                .partitions(3)   // 3 partitions = 3 parallel consumers possible
                .replicas(1)     // 1 replica (we only have 1 broker in dev)
                .build();
    }

    @Bean
    public NewTopic transactionEnrichedTopic() {
        return TopicBuilder.name(TOPIC_TXN_ENRICHED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic fraudAlertsTopic() {
        return TopicBuilder.name(TOPIC_FRAUD_ALERTS)
                .partitions(1)   // alerts are low volume, 1 partition is fine
                .replicas(1)
                .build();
    }
}