package com.scrutinizer.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostureRunDetailDto(
        UUID id,
        String applicationName,
        String sbomHash,
        String policyName,
        String policyVersion,
        String overallDecision,
        double postureScore,
        String summaryJson,
        Instant runTimestamp,
        Instant createdAt,
        List<ComponentResultDto> componentResults
) {
    public record ComponentResultDto(
            UUID id,
            String componentRef,
            String componentName,
            String componentVersion,
            String purl,
            boolean isDirect,
            String decision,
            List<FindingDto> findings
    ) {}
}
