package com.intelliguard.repository;

import com.intelliguard.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    List<AuditLog> findByTransactionId(String transactionId);

    List<AuditLog> findBySenderId(String senderId);

    List<AuditLog> findByDecision(String decision);

    List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime from, LocalDateTime to);

    List<AuditLog> findAllByOrderByCreatedAtDesc();
}