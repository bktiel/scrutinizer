package com.scrutinizer.api.dto;

import java.time.Instant;
import java.util.UUID;

public record PolicyExceptionDto(
    UUID id,
    UUID projectId,
    UUID policyId,
    String ruleId,
    String packageName,
    String packageVersion,
    String justification,
    String createdBy,
    String approvedBy,
    String status,
    String scope,
    Instant expiresAt,
    Instant createdAt,
    Instant updatedAt
) {}
