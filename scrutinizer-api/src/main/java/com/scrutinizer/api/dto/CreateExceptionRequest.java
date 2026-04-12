package com.scrutinizer.api.dto;

import java.time.Instant;
import java.util.UUID;

public record CreateExceptionRequest(
    UUID projectId,
    UUID policyId,
    String ruleId,
    String packageName,
    String packageVersion,
    String justification,
    String scope,
    Instant expiresAt
) {}
