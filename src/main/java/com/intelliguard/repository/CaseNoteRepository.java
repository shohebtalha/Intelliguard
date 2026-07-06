package com.intelliguard.repository;

import com.intelliguard.entity.CaseNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseNoteRepository extends JpaRepository<CaseNote, String> {
    List<CaseNote> findByTenantIdAndCaseIdOrderByCreatedAtAsc(String tenantId, String caseId);
}
