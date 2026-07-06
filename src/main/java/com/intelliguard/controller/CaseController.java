package com.intelliguard.controller;

import com.intelliguard.dto.ApiResponse;
import com.intelliguard.dto.CaseResponse;
import com.intelliguard.dto.PageResponse;
import com.intelliguard.service.FraudCaseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
public class CaseController {

    private final FraudCaseService fraudCaseService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ANALYST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<CaseResponse>>> listCases(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<CaseResponse> cases = fraudCaseService.listCases(
                status,
                assignedTo,
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(cases),
                "Fetched " + cases.getNumberOfElements() + " cases"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ANALYST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<CaseResponse>> getCase(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(fraudCaseService.getCase(id), "Case found"));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<CaseResponse>> assign(
            @PathVariable String id,
            @Valid @RequestBody AssignRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                fraudCaseService.assign(id, request.getAssignedTo()), "Case assigned"));
    }

    @PostMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('ANALYST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<CaseResponse>> addNote(
            @PathVariable String id,
            @Valid @RequestBody NoteRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                fraudCaseService.addNote(id, request.getNote()), "Note added"));
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<CaseResponse>> resolve(
            @PathVariable String id,
            @Valid @RequestBody ResolveRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                fraudCaseService.resolve(id, request.getResolution(), request.getNote()), "Case resolved"));
    }

    @Data
    public static class AssignRequest {
        @NotBlank
        private String assignedTo;
    }

    @Data
    public static class NoteRequest {
        @NotBlank
        private String note;
    }

    @Data
    public static class ResolveRequest {
        @NotBlank
        private String resolution;
        private String note;
    }
}
