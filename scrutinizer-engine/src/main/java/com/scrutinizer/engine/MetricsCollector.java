package com.scrutinizer.engine;

import com.scrutinizer.enrichment.EnrichedComponent;
import com.scrutinizer.enrichment.EnrichedDependencyGraph;
import com.scrutinizer.enrichment.EnrichmentPipeline;
import com.scrutinizer.graph.DependencyGraph;
import com.scrutinizer.policy.Policy;
import com.scrutinizer.scoring.ScorecardService;
import com.scrutinizer.provenance.ProvenanceService;

import java.util.List;

public class MetricsCollector {

    private final EnrichmentPipeline enrichmentPipeline;
    private final PostureEvaluator postureEvaluator;
    private final ScorecardService scorecardService;
    private final ProvenanceService provenanceService;

    private long enrichmentDurationMs;
    private long evaluationDurationMs;

    public MetricsCollector(EnrichmentPipeline enrichmentPipeline,
                            PostureEvaluator postureEvaluator,
                            ScorecardService scorecardService,
                            ProvenanceService provenanceService) {
        this.enrichmentPipeline = enrichmentPipeline;
        this.postureEvaluator = postureEvaluator;
        this.scorecardService = scorecardService;
        this.provenanceService = provenanceService;
    }

    public EnrichedDependencyGraph timedEnrich(DependencyGraph graph) {
        long start = System.currentTimeMillis();
        EnrichedDependencyGraph enriched = enrichmentPipeline.enrich(graph);
        enrichmentDurationMs = System.currentTimeMillis() - start;
        return enriched;
    }

    public PostureReport timedEvaluate(EnrichedDependencyGraph enrichedGraph, Policy policy) {
        long start = System.currentTimeMillis();
        PostureReport report = postureEvaluator.evaluate(enrichedGraph, policy);
        evaluationDurationMs = System.currentTimeMillis() - start;
        return report;
    }

    public EvaluationMetrics buildMetrics(EnrichedDependencyGraph enrichedGraph, long totalDurationMs) {
        int componentCount = enrichedGraph.components().size();

        long withScorecard = enrichedGraph.components().stream()
                .filter(c -> c.scorecardResult() != null)
                .count();
        long withProvenance = enrichedGraph.components().stream()
                .filter(c -> c.provenanceResult() != null && c.provenanceResult().present())
                .count();

        double scorecardCoverage = componentCount > 0
                ? (double) withScorecard / componentCount : 0.0;
        double provenanceCoverage = componentCount > 0
                ? (double) withProvenance / componentCount : 0.0;

        int totalCacheSize = scorecardService.cacheSize() + provenanceService.cacheSize();
        double cacheHitRate = componentCount > 0 && totalCacheSize > 0
                ? Math.min(1.0, (double) totalCacheSize / componentCount) : 0.0;

        return new EvaluationMetrics(
                totalDurationMs,
                enrichmentDurationMs,
                evaluationDurationMs,
                componentCount,
                scorecardCoverage,
                provenanceCoverage,
                cacheHitRate
        );
    }
}
