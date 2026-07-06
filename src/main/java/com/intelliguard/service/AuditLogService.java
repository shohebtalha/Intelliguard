package com.intelliguard.service;

import com.intelliguard.entity.AuditLog;
import com.intelliguard.entity.Transaction;
import com.intelliguard.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;

/**
 * AuditLogService creates a permanent record of every fraud decision.
 *
 * Key design decisions:
 *
 * 1. @Async — audit logging happens in a background thread.
 *    The HTTP response goes back to the client immediately.
 *    Audit log is written after. This keeps latency low.
 *
 * 2. Never throws exceptions — if audit logging fails,
 *    we LOG the failure but never let it affect the
 *    transaction response. Audit is important but not
 *    critical enough to fail the payment decision.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;

    /**
     * Create an audit log entry for a fraud decision.
     * Runs asynchronously — doesn't block the HTTP response.
     */
    @Async
    public void logDecision(Transaction transaction, Long decisionTimeMs) {
        try {
            // Get the currently logged-in user (from JWT token)
            String performedBy = getCurrentUsername();
            String previousHash = auditLogRepository
                    .findTopByTenantIdOrderByCreatedAtDesc(transaction.getTenantId())
                    .map(AuditLog::getRecordHash)
                    .orElse("GENESIS");

            AuditLog auditLog = AuditLog.builder()
                    .tenantId(transaction.getTenantId())
                    .transactionId(transaction.getId())
                    .senderId(transaction.getSenderId())
                    .receiverId(transaction.getReceiverId())
                    .amount(transaction.getAmount())
                    .currency(transaction.getCurrency())
                    .country(transaction.getCountry())
                    .decision(transaction.getStatus())
                    .fraudScore(transaction.getFraudScore())
                    .flagReason(transaction.getFlagReason())
                    .modelVersion(transaction.getModelVersion())
                    .decisionTimeMs(decisionTimeMs)
                    .performedBy(performedBy)
                    .previousHash(previousHash)
                    .build();
            auditLog.setRecordHash(hash(auditLog));

            auditLogRepository.save(auditLog);

            log.debug("Audit log created for transaction: {} decision: {}",
                    transaction.getId(), transaction.getStatus());

        } catch (Exception e) {
            // Never let audit logging failure affect the transaction
            log.error("Failed to create audit log for transaction {}: {}",
                    transaction.getId(), e.getMessage());
        }
    }

    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findByTenantIdOrderByCreatedAtDesc(currentUserService.tenantId());
    }

    public List<AuditLog> getAuditLogsByTransaction(String transactionId) {
        return auditLogRepository.findByTenantIdAndTransactionId(currentUserService.tenantId(), transactionId);
    }

    public List<AuditLog> getAuditLogsBySender(String senderId) {
        return auditLogRepository.findByTenantIdAndSenderId(currentUserService.tenantId(), senderId);
    }

    private String getCurrentUsername() {
        return currentUserService.username();
    }

    private String hash(AuditLog auditLog) {
        String payload = String.join("|",
                nullSafe(auditLog.getPreviousHash()),
                nullSafe(auditLog.getTenantId()),
                nullSafe(auditLog.getTransactionId()),
                nullSafe(auditLog.getSenderId()),
                nullSafe(auditLog.getReceiverId()),
                nullSafe(auditLog.getAmount()),
                nullSafe(auditLog.getCurrency()),
                nullSafe(auditLog.getCountry()),
                nullSafe(auditLog.getDecision()),
                nullSafe(auditLog.getFraudScore()),
                nullSafe(auditLog.getFlagReason()),
                nullSafe(auditLog.getModelVersion()),
                nullSafe(auditLog.getDecisionTimeMs()),
                nullSafe(auditLog.getPerformedBy())
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash audit log", ex);
        }
    }

    private String nullSafe(Object value) {
        return value == null ? "" : value.toString();
    }
}
