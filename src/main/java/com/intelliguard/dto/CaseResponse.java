package com.intelliguard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class CaseResponse {
    private String id;
    private String tenantId;
    private String transactionId;
    private String senderId;
    private String status;
    private String priority;
    private String assignedTo;
    private BigDecimal fraudScore;
    private String decision;
    private String reason;
    private String resolution;
    private String resolutionNote;
    private String resolvedBy;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private List<CaseNoteResponse> notes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CaseNoteResponse {
        private String id;
        private String note;
        private String createdBy;
        private LocalDateTime createdAt;
    }
}
