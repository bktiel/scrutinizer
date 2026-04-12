package com.scrutinizer.api.dto;

import java.time.Instant;

public record ProjectStatsDto(
    long totalRuns,
    long totalComponents,
    long passCount,
    long failCount,
    long warnCount,
    double latestScore,
    String latestDecision,
    Instant lastRunAt,
    double provenanceCoverage,
    double scorecardCoverage
) {}
