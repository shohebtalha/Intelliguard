package com.intelliguard.repository;

import com.intelliguard.entity.ModelRegistryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModelRegistryRepository extends JpaRepository<ModelRegistryEntry, String> {
    Optional<ModelRegistryEntry> findByVersion(String version);

    Optional<ModelRegistryEntry> findTopByStatusOrderByPromotedAtDesc(String status);

    List<ModelRegistryEntry> findAllByOrderByCreatedAtDesc();
}
