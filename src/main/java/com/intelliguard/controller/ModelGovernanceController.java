package com.intelliguard.controller;

import com.intelliguard.dto.ApiResponse;
import com.intelliguard.entity.ModelDriftSnapshot;
import com.intelliguard.entity.ModelRegistryEntry;
import com.intelliguard.service.MLScoringService;
import com.intelliguard.service.ModelGovernanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class ModelGovernanceController {

    private final ModelGovernanceService modelGovernanceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<ModelRegistryEntry>>> listModels() {
        return ResponseEntity.ok(ApiResponse.success(modelGovernanceService.listModels(), "Models fetched"));
    }

    @PostMapping("/drift-snapshots")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<ModelDriftSnapshot>> createDriftSnapshot(
            @RequestParam(defaultValue = MLScoringService.MODEL_VERSION) String modelVersion,
            @RequestParam(defaultValue = "60") int windowMinutes) {
        return ResponseEntity.ok(ApiResponse.success(
                modelGovernanceService.createDriftSnapshot(modelVersion, windowMinutes),
                "Drift snapshot created"));
    }

    @GetMapping("/drift-snapshots")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<ModelDriftSnapshot>>> recentDriftSnapshots(
            @RequestParam(defaultValue = MLScoringService.MODEL_VERSION) String modelVersion) {
        return ResponseEntity.ok(ApiResponse.success(
                modelGovernanceService.recentDriftSnapshots(modelVersion),
                "Drift snapshots fetched"));
    }
}
