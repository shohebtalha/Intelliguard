package com.intelliguard.repository;

import com.intelliguard.entity.FraudCase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FraudCaseRepository extends JpaRepository<FraudCase, String> {
    Optional<FraudCase> findByTenantIdAndTransactionId(String tenantId, String transactionId);

    Optional<FraudCase> findByIdAndTenantId(String id, String tenantId);

    Page<FraudCase> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    Page<FraudCase> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, String status, Pageable pageable);

    Page<FraudCase> findByTenantIdAndAssignedToAndStatusOrderByCreatedAtDesc(
            String tenantId, String assignedTo, String status, Pageable pageable);

    long countByStatus(String status);
}
