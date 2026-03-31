package com.scrutinizer.api.dto;

import java.time.Instant;
import java.util.UUID;

public record PolicyHistoryDto(
        UUID id,
        String policyYaml,
        String changedBy,
        Instant changedAt
) {}
