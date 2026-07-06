package com.intelliguard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    public boolean allow(String bucket, String identifier, int limit, Duration window) {
        String safeIdentifier = identifier == null || identifier.isBlank() ? "unknown" : identifier;
        String key = "rate-limit:" + bucket + ":" + safeIdentifier;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, window);
        }
        return count == null || count <= limit;
    }
}
