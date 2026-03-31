package com.scrutinizer.api.repository;

import com.scrutinizer.api.entity.PolicyHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PolicyHistoryRepository extends JpaRepository<PolicyHistoryEntity, UUID> {
    List<PolicyHistoryEntity> findByPolicyIdOrderByChangedAtDesc(UUID policyId);
}
