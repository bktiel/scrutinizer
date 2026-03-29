package com.scrutinizer.engine;

import java.util.LinkedHashMap;
import java.util.Map;

public record EvaluationMetrics(
        long totalDurationMs,
        long enrichmentDurationMs,
        long evaluationDurationMs,
        int componentCount,
        double scorecardCoverage,
        double provenanceCoverage,
        double cacheHitRate
) {
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("totalDurationMs", totalDurationMs);
        map.put("enrichmentDurationMs", enrichmentDurationMs);
        map.put("evaluationDurationMs", evaluationDurationMs);
        map.put("componentCount", componentCount);

        Map<String, Object> coverage = new LinkedHashMap<>();
        coverage.put("scorecardPercent", Math.round(scorecardCoverage * 10000.0) / 100.0);
        coverage.put("provenancePercent", Math.round(provenanceCoverage * 10000.0) / 100.0);
        map.put("signalCoverage", coverage);

        map.put("cacheHitRate", Math.round(cacheHitRate * 10000.0) / 100.0);
        return map;
    }
}
