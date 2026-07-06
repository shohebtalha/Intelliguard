package com.intelliguard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VelocityService {

    private final StringRedisTemplate redisTemplate;

    private static final String TXN_WINDOW_KEY = "velocity:%s:txn";
    private static final String AMOUNT_WINDOW_KEY = "velocity:%s:amount";

    private static final long MAX_TXN_PER_10MIN = 10;
    private static final long MAX_TXN_PER_HOUR = 30;
    private static final BigDecimal MAX_AMOUNT_PER_HOUR = new BigDecimal("500000");

    public VelocityMetrics recordAndGet(String senderId, BigDecimal amount) {
        long now = System.currentTimeMillis();
        String uniqueSuffix = now + ":" + UUID.randomUUID();
        String txnKey = String.format(TXN_WINDOW_KEY, senderId);
        String amountKey = String.format(AMOUNT_WINDOW_KEY, senderId);

        redisTemplate.opsForZSet().add(txnKey, uniqueSuffix, now);
        redisTemplate.opsForZSet().add(amountKey, toPaise(amount) + ":" + uniqueSuffix, now);
        redisTemplate.expire(txnKey, Duration.ofHours(2));
        redisTemplate.expire(amountKey, Duration.ofHours(2));

        return getMetrics(senderId, now);
    }

    public VelocityMetrics getMetrics(String senderId) {
        return getMetrics(senderId, System.currentTimeMillis());
    }

    private VelocityMetrics getMetrics(String senderId, long now) {
        String txnKey = String.format(TXN_WINDOW_KEY, senderId);
        String amountKey = String.format(AMOUNT_WINDOW_KEY, senderId);
        long oneHourAgo = now - Duration.ofHours(1).toMillis();
        long tenMinutesAgo = now - Duration.ofMinutes(10).toMillis();

        removeExpired(txnKey, oneHourAgo);
        removeExpired(amountKey, oneHourAgo);

        long count10min = countRange(txnKey, tenMinutesAgo, now);
        long count1hour = countRange(txnKey, oneHourAgo, now);
        BigDecimal amountLastHour = sumAmounts(amountKey, oneHourAgo, now);

        log.debug("Velocity for {}: {}txn/10min, {}txn/1hr, {}/1hr",
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

    private void removeExpired(String key, long oldestAllowedScore) {
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, oldestAllowedScore - 1);
    }

    private long countRange(String key, long from, long to) {
        Long count = redisTemplate.opsForZSet().count(key, from, to);
        return count != null ? count : 0L;
    }

    private BigDecimal sumAmounts(String key, long from, long to) {
        Set<String> entries = redisTemplate.opsForZSet().rangeByScore(key, from, to);
        if (entries == null || entries.isEmpty()) {
            return BigDecimal.ZERO;
        }

        long totalPaise = entries.stream()
                .mapToLong(this::parsePaise)
                .sum();

        return new BigDecimal(totalPaise).divide(new BigDecimal("100"));
    }

    private long parsePaise(String entry) {
        int separator = entry.indexOf(':');
        if (separator <= 0) {
            return 0L;
        }
        try {
            return Long.parseLong(entry.substring(0, separator));
        } catch (NumberFormatException ex) {
            log.warn("Invalid velocity amount entry: {}", entry);
            return 0L;
        }
    }

    private long toPaise(BigDecimal amount) {
        return amount.multiply(new BigDecimal("100")).longValue();
    }

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
