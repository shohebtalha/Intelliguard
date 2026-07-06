package com.intelliguard.repository;

import com.intelliguard.entity.ModelDriftSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModelDriftSnapshotRepository extends JpaRepository<ModelDriftSnapshot, String> {
    List<ModelDriftSnapshot> findTop20ByModelVersionOrderByCreatedAtDesc(String modelVersion);
}
