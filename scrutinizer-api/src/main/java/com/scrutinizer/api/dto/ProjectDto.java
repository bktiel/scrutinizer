package com.scrutinizer.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ProjectDto(
    UUID id,
    String name,
    String description,
    String repositoryUrl,
    String gitlabProjectId,
    String defaultBranch,
    UUID policyId,
    String policyName,
    Instant createdAt,
    Instant updatedAt,
    ProjectStatsDto stats
) {}
