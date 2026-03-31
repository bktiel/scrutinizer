package com.scrutinizer.api.repository;

import com.scrutinizer.api.entity.PolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PolicyRepository extends JpaRepository<PolicyEntity, UUID> {
    Optional<PolicyEntity> findByName(String name);
}
