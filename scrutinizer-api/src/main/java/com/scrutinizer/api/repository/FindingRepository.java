package com.scrutinizer.api.repository;

import com.scrutinizer.api.entity.FindingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FindingRepository extends JpaRepository<FindingEntity, UUID> {

    @Query("SELECT f FROM FindingEntity f " +
           "JOIN f.componentResult cr " +
           "JOIN cr.postureRun pr " +
           "WHERE pr.id = :runId " +
           "ORDER BY f.ruleId ASC")
    @EntityGraph(attributePaths = {"componentResult"})
    Page<FindingEntity> findByPostureRunId(@Param("runId") UUID runId, Pageable pageable);

    @Query("SELECT f FROM FindingEntity f " +
           "JOIN f.componentResult cr " +
           "JOIN cr.postureRun pr " +
           "WHERE pr.id = :runId AND f.decision = :decision " +
           "ORDER BY f.ruleId ASC")
    @EntityGraph(attributePaths = {"componentResult"})
    Page<FindingEntity> findByPostureRunIdAndDecision(
            @Param("runId") UUID runId,
            @Param("decision") String decision,
            Pageable pageable);
}
