package com.intelliguard.service;

import com.intelliguard.dto.CaseResponse;
import com.intelliguard.entity.CaseNote;
import com.intelliguard.entity.FraudCase;
import com.intelliguard.entity.Transaction;
import com.intelliguard.exception.TransactionNotFoundException;
import com.intelliguard.repository.CaseNoteRepository;
import com.intelliguard.repository.FraudCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FraudCaseService {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_INVESTIGATING = "INVESTIGATING";
    public static final String STATUS_RESOLVED = "RESOLVED";

    private final FraudCaseRepository fraudCaseRepository;
    private final CaseNoteRepository caseNoteRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public FraudCase openCaseForDecision(Transaction transaction) {
        if (!"REVIEW".equals(transaction.getStatus()) && !"BLOCK".equals(transaction.getStatus())) {
            return null;
        }
        return fraudCaseRepository.findByTenantIdAndTransactionId(
                        transaction.getTenantId(), transaction.getId())
                .orElseGet(() -> fraudCaseRepository.save(FraudCase.builder()
                        .tenantId(transaction.getTenantId())
                        .transactionId(transaction.getId())
                        .senderId(transaction.getSenderId())
                        .status(STATUS_OPEN)
                        .priority(priority(transaction.getFraudScore(), transaction.getStatus()))
                        .fraudScore(transaction.getFraudScore())
                        .decision(transaction.getStatus())
                        .reason(transaction.getFlagReason())
                        .build()));
    }

    public Page<CaseResponse> listCases(String status, String assignedTo, Pageable pageable) {
        String tenantId = currentUserService.tenantId();
        Page<FraudCase> cases;
        if (assignedTo != null && !assignedTo.isBlank() && status != null && !status.isBlank()) {
            cases = fraudCaseRepository.findByTenantIdAndAssignedToAndStatusOrderByCreatedAtDesc(
                    tenantId, assignedTo, status.toUpperCase(), pageable);
        } else if (status != null && !status.isBlank()) {
            cases = fraudCaseRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(
                    tenantId, status.toUpperCase(), pageable);
        } else {
            cases = fraudCaseRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
        }
        return cases.map(this::toResponseWithoutNotes);
    }

    public CaseResponse getCase(String id) {
        FraudCase fraudCase = findTenantCase(id);
        return toResponse(fraudCase);
    }

    @Transactional
    public CaseResponse assign(String id, String assignedTo) {
        FraudCase fraudCase = findTenantCase(id);
        ensureNotResolved(fraudCase);
        fraudCase.setAssignedTo(assignedTo);
        fraudCase.setStatus(STATUS_INVESTIGATING);
        addNoteInternal(fraudCase.getId(), "Assigned to " + assignedTo);
        return toResponse(fraudCase);
    }

    @Transactional
    public CaseResponse addNote(String id, String note) {
        FraudCase fraudCase = findTenantCase(id);
        addNoteInternal(fraudCase.getId(), note);
        return toResponse(fraudCase);
    }

    @Transactional
    public CaseResponse resolve(String id, String resolution, String note) {
        FraudCase fraudCase = findTenantCase(id);
        ensureNotResolved(fraudCase);
        fraudCase.setStatus(STATUS_RESOLVED);
        fraudCase.setResolution(resolution);
        fraudCase.setResolutionNote(note);
        fraudCase.setResolvedBy(currentUserService.username());
        fraudCase.setResolvedAt(LocalDateTime.now());
        if (note != null && !note.isBlank()) {
            addNoteInternal(fraudCase.getId(), "Resolved as " + resolution + ": " + note);
        }
        return toResponse(fraudCase);
    }

    private FraudCase findTenantCase(String id) {
        return fraudCaseRepository.findByIdAndTenantId(id, currentUserService.tenantId())
                .orElseThrow(() -> new TransactionNotFoundException("Case not found with id: " + id));
    }

    private void addNoteInternal(String caseId, String note) {
        caseNoteRepository.save(CaseNote.builder()
                .tenantId(currentUserService.tenantId())
                .caseId(caseId)
                .note(note)
                .createdBy(currentUserService.username())
                .build());
    }

    private void ensureNotResolved(FraudCase fraudCase) {
        if (STATUS_RESOLVED.equals(fraudCase.getStatus())) {
            throw new IllegalStateException("Resolved cases cannot be modified");
        }
    }

    private String priority(BigDecimal score, String decision) {
        if ("BLOCK".equals(decision)) {
            return "HIGH";
        }
        if (score != null && score.doubleValue() >= 0.65) {
            return "HIGH";
        }
        if (score != null && score.doubleValue() >= 0.45) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private CaseResponse toResponse(FraudCase fraudCase) {
        List<CaseResponse.CaseNoteResponse> notes = caseNoteRepository
                .findByTenantIdAndCaseIdOrderByCreatedAtAsc(fraudCase.getTenantId(), fraudCase.getId())
                .stream()
                .map(note -> CaseResponse.CaseNoteResponse.builder()
                        .id(note.getId())
                        .note(note.getNote())
                        .createdBy(note.getCreatedBy())
                        .createdAt(note.getCreatedAt())
                        .build())
                .toList();
        return toResponseWithoutNotes(fraudCase).toBuilder().notes(notes).build();
    }

    private CaseResponse toResponseWithoutNotes(FraudCase fraudCase) {
        return CaseResponse.builder()
                .id(fraudCase.getId())
                .tenantId(fraudCase.getTenantId())
                .transactionId(fraudCase.getTransactionId())
                .senderId(fraudCase.getSenderId())
                .status(fraudCase.getStatus())
                .priority(fraudCase.getPriority())
                .assignedTo(fraudCase.getAssignedTo())
                .fraudScore(fraudCase.getFraudScore())
                .decision(fraudCase.getDecision())
                .reason(fraudCase.getReason())
                .resolution(fraudCase.getResolution())
                .resolutionNote(fraudCase.getResolutionNote())
                .resolvedBy(fraudCase.getResolvedBy())
                .resolvedAt(fraudCase.getResolvedAt())
                .createdAt(fraudCase.getCreatedAt())
                .build();
    }
}
