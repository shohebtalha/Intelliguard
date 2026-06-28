package com.intelliguard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * VelocityService tracks HOW FAST a user is transacting.
 *
 * WHAT IS VELOCITY IN FRAUD?
 * Normal person: 2-3 transactions per day.
 * Fraudster using stolen card: 20 transactions in 5 minutes.
 *
 * We use Redis because:
 * 1. It's in-memory — microsecond reads, no database round trip
 * 2. It has built-in key expiry — counters auto-reset after the window
 * 3. INCR is atomic — safe for concurrent requests
 *
 * HOW SLIDING WINDOW WORKS:
 * Key: "velocity:USER_001:txn_count:10min"
 * Value: 14  (14 transactions in last 10 minutes)
 * TTL: 600 seconds (auto-deletes after 10 minutes)
 *
 * Every new transaction → INCR the key → check if over limit.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VelocityService {

    private final RedisTemplate<String, Long> redisTemplate;

    // Key patterns — notice the structure: service:userId:metric:window
    private static final String TXN_COUNT_KEY   = "velocity:%s:txn_count:%s";
    private static final String AMOUNT_SUM_KEY  = "velocity:%s:amount_sum:%s";

    // Thresholds
    private static final long MAX_TXN_PER_10MIN  = 10;   // max 10 transactions in 10 minutes
    private static final long MAX_TXN_PER_HOUR   = 30;   // max 30 transactions per hour
    private static final BigDecimal MAX_AMOUNT_PER_HOUR = new BigDecimal("500000"); // ₹5L per hour

    /**
     * Record a new transaction and return velocity metrics for this sender.
     * Call this BEFORE the fraud check so metrics are current.
     */
    public VelocityMetrics recordAndGet(String senderId, BigDecimal amount) {
        // Increment counters in Redis
        long count10min = incrementCounter(senderId, "10min", Duration.ofMinutes(10));
        long count1hour = incrementCounter(senderId, "1hour", Duration.ofHours(1));
        BigDecimal amountLastHour = incrementAmount(senderId, amount, Duration.ofHours(1));

        log.debug("Velocity for {}: {}txn/10min, {}txn/1hr, ₹{}/1hr",
                senderId, count10min, count1hour, amountLastHour);

        return VelocityMetrics.builder()
                .txnCountLast10Min(count10min)
                .txnCountLastHour(count1hour)
                .totalAmountLastHour(amountLastHour)
                .isTxnCountSuspicious(count10min > MAX_TXN_PER_10MIN)
                .isTxnRateSuspicious(count1hour > MAX_TXN_PER_HOUR)
                .isAmountSuspicious(amountLastHour.compareTo(MAX_AMOUNT_PER_HOUR) > 0)
                .build();
    }

    /**
     * Increment transaction count for a time window.
     * Redis INCR is atomic — thread safe even with 1000 concurrent requests.
     */
    private long incrementCounter(String senderId, String window, Duration ttl) {
        String key = String.format(TXN_COUNT_KEY, senderId, window);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) count = 1L;

        // Set expiry only on first increment (when key is new)
        if (count == 1) {
            redisTemplate.expire(key, ttl);
        }
        return count;
    }

    /**
     * Accumulate transaction amounts for a time window.
     * Stored as paise (multiply by 100) to avoid decimal issues in Redis.
     */
    private BigDecimal incrementAmount(String senderId, BigDecimal amount, Duration ttl) {
        String key = String.format(AMOUNT_SUM_KEY, senderId, "1hour");

        // Store as paise (₹1 = 100 paise) to keep it as a Long in Redis
        long amountInPaise = amount.multiply(new BigDecimal("100")).longValue();
        Long totalPaise = redisTemplate.opsForValue().increment(key, amountInPaise);
        if (totalPaise == null) totalPaise = amountInPaise;

        if (totalPaise == amountInPaise) {
            redisTemplate.expire(key, ttl);
        }

        // Convert back to rupees
        return new BigDecimal(totalPaise).divide(new BigDecimal("100"));
    }

    /**
     * Result object containing all velocity metrics for a sender.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class VelocityMetrics {
        private long txnCountLast10Min;
        private long txnCountLastHour;
        private BigDecimal totalAmountLastHour;
        private boolean isTxnCountSuspicious;
        private boolean isTxnRateSuspicious;
        private boolean isAmountSuspicious;
    }
}