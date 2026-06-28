package com.intelliguard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configures how Java objects are stored in Redis.
 *
 * Redis stores everything as bytes. We need to tell it:
 * - Keys   → store as plain Strings  (readable in Redis CLI)
 * - Values → store as Strings too    (numbers become "14", "98000.00")
 *
 * Without this config, Spring uses Java serialization which
 * stores unreadable binary data — hard to debug.
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Long> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Long> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Keys stored as plain strings: "velocity:USER_001:count"
        template.setKeySerializer(new StringRedisSerializer());

        // Values stored as strings: "14" instead of binary
        template.setValueSerializer(new GenericToStringSerializer<>(Long.class));

        return template;
    }
}