package com.scrutinizer.api.dto;

import java.util.UUID;

public record CreateProjectRequest(
    String name,
    String description,
    String repositoryUrl,
    String gitlabProjectId,
    String defaultBranch,
    UUID policyId
) {}
