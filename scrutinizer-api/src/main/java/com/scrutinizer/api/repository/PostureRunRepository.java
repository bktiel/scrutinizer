package com.scrutinizer.api.repository;

import com.scrutinizer.api.entity.PostureRunEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostureRunRepository extends JpaRepository<PostureRunEntity, UUID> {

    Page<PostureRunEntity> findByApplicationNameOrderByRunTimestampDesc(String applicationName, Pageable pageable);

    Page<PostureRunEntity> findAllByOrderByRunTimestampDesc(Pageable pageable);

    @Query("SELECT DISTINCT r.applicationName FROM PostureRunEntity r ORDER BY r.applicationName")
    List<String> findDistinctApplicationNames();

    List<PostureRunEntity> findByApplicationNameOrderByRunTimestampAsc(String applicationName);
}
