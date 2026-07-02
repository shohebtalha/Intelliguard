package com.intelliguard.controller;

import com.intelliguard.dto.ApiResponse;
import com.intelliguard.entity.AuditLog;
import com.intelliguard.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AuditController exposes audit log data.
 *
 * GET /api/audit                          → all audit logs
 * GET /api/audit/transaction/{txnId}      → logs for a transaction
 * GET /api/audit/sender/{senderId}        → logs for a sender
 *
 * In production: MANAGER and ADMIN roles only.
 * For demo: open to any authenticated user.
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuditController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AuditLog>>> getAllAuditLogs() {
        List<AuditLog> logs = auditLogService.getAllAuditLogs();
        return ResponseEntity.ok(
                ApiResponse.success(logs, "Fetched " + logs.size() + " audit records"));
    }

    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getByTransaction(
            @PathVariable String transactionId) {
        List<AuditLog> logs = auditLogService.getAuditLogsByTransaction(transactionId);
        return ResponseEntity.ok(ApiResponse.success(logs, "Audit logs for transaction"));
    }

    @GetMapping("/sender/{senderId}")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getBySender(
            @PathVariable String senderId) {
        List<AuditLog> logs = auditLogService.getAuditLogsBySender(senderId);
        return ResponseEntity.ok(ApiResponse.success(logs, "Audit logs for sender"));
    }
}