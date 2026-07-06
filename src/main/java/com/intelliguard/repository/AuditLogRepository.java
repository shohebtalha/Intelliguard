package com.intelliguard.repository;

import com.intelliguard.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    List<AuditLog> findByTenantIdAndTransactionId(String tenantId, String transactionId);

    List<AuditLog> findByTenantIdAndSenderId(String tenantId, String senderId);

    List<AuditLog> findByTenantIdAndDecision(String tenantId, String decision);

    List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime from, LocalDateTime to);

    List<AuditLog> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    Optional<AuditLog> findTopByTenantIdOrderByCreatedAtDesc(String tenantId);
}
