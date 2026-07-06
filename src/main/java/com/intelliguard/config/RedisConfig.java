package com.intelliguard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    public StringRedisTemplate redisTemplate(RedisConnectionFactory factory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(factory);

        // Keys stored as plain strings: "velocity:USER_001:count"
        template.setKeySerializer(new StringRedisSerializer());

        template.setValueSerializer(new StringRedisSerializer());

        return template;
    }
}
