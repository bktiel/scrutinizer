package com.scrutinizer.api.dto;

import java.time.Instant;

public record TrendDataPoint(
        Instant timestamp,
        double postureScore,
        String overallDecision,
        String policyVersion
) {}
