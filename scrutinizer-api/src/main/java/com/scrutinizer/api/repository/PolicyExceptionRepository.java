package com.scrutinizer.api.repository;

import com.scrutinizer.api.entity.PolicyExceptionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface PolicyExceptionRepository extends JpaRepository<PolicyExceptionEntity, UUID> {

    List<PolicyExceptionEntity> findByProjectIdAndStatus(UUID projectId, String status);

    List<PolicyExceptionEntity> findByProjectId(UUID projectId);

    List<PolicyExceptionEntity> findByProjectIdAndStatusAndExpiresAtAfter(UUID projectId, String status, Instant now);

    Page<PolicyExceptionEntity> findByProjectIdAndStatus(UUID projectId, String status, Pageable pageable);
}
