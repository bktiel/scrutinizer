package com.scrutinizer.api.dto;

import java.time.Instant;
import java.util.UUID;

public record PolicyDto(
        UUID id,
        String name,
        String version,
        String description,
        String policyYaml,
        Instant createdAt,
        Instant updatedAt
) {}
