package com.scrutinizer.api.repository;

import com.scrutinizer.api.entity.ComponentResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ComponentResultRepository extends JpaRepository<ComponentResultEntity, UUID> {
}
