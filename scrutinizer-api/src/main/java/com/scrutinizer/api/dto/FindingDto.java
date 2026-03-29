package com.scrutinizer.api.dto;

import java.util.UUID;

public record FindingDto(
        UUID id,
        String ruleId,
        String decision,
        String severity,
        String field,
        String actualValue,
        String expectedValue,
        String description,
        String remediation,
        String componentRef,
        String componentName
) {}
