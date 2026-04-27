package com.scrutinizer.api.repository;

import com.scrutinizer.api.entity.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, UUID> {

    Optional<ProjectEntity> findByName(String name);

    Optional<ProjectEntity> findByRepositoryUrl(String repositoryUrl);

    List<ProjectEntity> findAllByOrderByNameAsc();
}
