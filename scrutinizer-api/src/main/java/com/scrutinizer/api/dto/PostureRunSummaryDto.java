package com.scrutinizer.api.dto;

import java.time.Instant;
import java.util.UUID;

public record PostureRunSummaryDto(
        UUID id,
        String applicationName,
        String policyName,
        String policyVersion,
        String overallDecision,
        double postureScore,
        Instant runTimestamp
) {}
