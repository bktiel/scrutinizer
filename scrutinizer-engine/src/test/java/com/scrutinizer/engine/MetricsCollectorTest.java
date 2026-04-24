package com.scrutinizer.engine;

import com.scrutinizer.enrichment.*;
import com.scrutinizer.model.Component;
import com.scrutinizer.model.DependencyGraph;
import com.scrutinizer.policy.PolicyDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsCollectorTest {

    private MetricsCollector collector;
    private EnrichmentPipeline enrichmentPipeline;
    private PostureEvaluator postureEvaluator;
    private ScorecardService scorecardService;
    private ProvenanceService provenanceService;

    @BeforeEach
    void setUp() {
        scorecardService = new TestScorecardService();
        provenanceService = new TestProvenanceService();
        enrichmentPipeline = new EnrichmentPipeline(scorecardService, provenanceService);
        postureEvaluator = new TestPostureEvaluator();
        collector = new MetricsCollector(enrichmentPipeline, postureEvaluator,
                scorecardService, provenanceService);
    }

    @Nested
    class EnrichmentTimingTests {
        @Test
        void timedEnrichMeasuresDuration() {
            Component c = new Component("pkg", "1.0", "pkg@1.0", "library",
                    null, "pkg:npm/pkg@1.0", null, "required");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            EnrichedDependencyGraph result = collector.timedEnrich(graph);

            assertThat(result).isNotNull();
            assertThat(result.componentCount()).isEqualTo(1);
        }

        @Test
        void timedEnrichRecordsDuration() {
            Component c = new Component("pkg", "1.0", "pkg@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            EnrichedDependencyGraph result = collector.timedEnrich(graph);

            // Duration is captured internally and included in buildMetrics
            EvaluationMetrics metrics = collector.buildMetrics(result, 100);
            assertThat(metrics.enrichmentDurationMs()).isGreaterThanOrEqualTo(0);
        }

        @Test
        void enrichmentDurationIsIncludedInMetrics() {
            Component c = new Component("pkg", "1.0", "pkg@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            EnrichedDependencyGraph enriched = collector.timedEnrich(graph);
            EvaluationMetrics metrics = collector.buildMetrics(enriched, 100);

            assertThat(metrics.enrichmentDurationMs()).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    class EvaluationTimingTests {
        @Test
        void timedEvaluateMeasuresDuration() {
            Component c = new Component("pkg", "1.0", "pkg@1.0");
            EnrichedDependencyGraph enriched = EnrichedDependencyGraph.fromGraph(
                    new DependencyGraph(List.of(c), List.of(), null));

            PostureReport report = collector.timedEvaluate(enriched,
                    new PolicyDefinition("scrutinizer/v1", "test", "1.0", List.of(), null),
                    "{}");

            assertThat(report).isNotNull();
        }

        @Test
        void evaluationDurationIsIncludedInMetrics() {
            Component c = new Component("pkg", "1.0", "pkg@1.0");
            EnrichedDependencyGraph enriched = EnrichedDependencyGraph.fromGraph(
                    new DependencyGraph(List.of(c), List.of(), null));

            collector.timedEvaluate(enriched,
                    new PolicyDefinition("scrutinizer/v1", "test", "1.0", List.of(), null),
                    "{}");

            EvaluationMetrics metrics = collector.buildMetrics(enriched, 100);

            assertThat(metrics.evaluationDurationMs()).isGreaterThanOrEqualTo(0);
        }
    }

    @Nested
    class MetricsBuildingTests {
        @Test
        void buildMetricsIncludesComponentCount() {
            Component c1 = new Component("pkg1", "1.0", "pkg1@1.0");
            Component c2 = new Component("pkg2", "1.0", "pkg2@1.0");
            EnrichedDependencyGraph enriched = EnrichedDependencyGraph.fromGraph(
                    new DependencyGraph(List.of(c1, c2), List.of(), null));

            EvaluationMetrics metrics = collector.buildMetrics(enriched, 100);

            assertThat(metrics.componentCount()).isEqualTo(2);
        }

        @Test
        void buildMetricsCalculatesScorecardCoverage() {
            Component c1 = new Component("axios", "1.0", "axios@1.0", "library",
                    null, "pkg:npm/axios@1.0", null, "required");
            Component c2 = new Component("other", "1.0", "other@1.0");

            EnrichedComponent ec1 = EnrichedComponent.unenriched(c1)
                    .withScorecard(new ScorecardResult(7.5, Map.of(), "repo", Instant.now()));
            EnrichedComponent ec2 = EnrichedComponent.unenriched(c2);

            EnrichedDependencyGraph enriched = new EnrichedDependencyGraph(
                    List.of(ec1, ec2), List.of(), null);

            EvaluationMetrics metrics = collector.buildMetrics(enriched, 100);

            assertThat(metrics.scorecardCoverage()).isEqualTo(0.5);
        }

        @Test
        void buildMetricsCalculatesProvenanceCoverage() {
            Component c1 = new Component("axios", "1.0", "axios@1.0", "library",
                    null, "pkg:npm/axios@1.0", null, "required");
            Component c2 = new Component("other", "1.0", "other@1.0");

            ProvenanceResult provenance = ProvenanceResult.detected(
                    ProvenanceResult.SlsaLevel.SLSA_L2, "npm-sigstore");

            EnrichedComponent ec1 = EnrichedComponent.unenriched(c1)
                    .withProvenance(provenance);
            EnrichedComponent ec2 = EnrichedComponent.unenriched(c2);

            EnrichedDependencyGraph enriched = new EnrichedDependencyGraph(
                    List.of(ec1, ec2), List.of(), null);

            EvaluationMetrics metrics = collector.buildMetrics(enriched, 100);

            assertThat(metrics.provenanceCoverage()).isEqualTo(0.5);
        }

        @Test
        void buildMetricsCalculatesCacheHitRate() {
            Component c = new Component("pkg", "1.0", "pkg@1.0");
            EnrichedDependencyGraph enriched = EnrichedDependencyGraph.fromGraph(
                    new DependencyGraph(List.of(c), List.of(), null));

            EvaluationMetrics metrics = collector.buildMetrics(enriched, 100);

            assertThat(metrics.cacheHitRate()).isGreaterThanOrEqualTo(0.0);
            assertThat(metrics.cacheHitRate()).isLessThanOrEqualTo(1.0);
        }

        @Test
        void buildMetricsIncludesTotalDuration() {
            Component c = new Component("pkg", "1.0", "pkg@1.0");
            EnrichedDependencyGraph enriched = EnrichedDependencyGraph.fromGraph(
                    new DependencyGraph(List.of(c), List.of(), null));

            EvaluationMetrics metrics = collector.buildMetrics(enriched, 500);

            assertThat(metrics.totalDurationMs()).isEqualTo(500);
        }

        @Test
        void buildMetricsWithZeroComponents() {
            EnrichedDependencyGraph enriched = new EnrichedDependencyGraph(
                    List.of(), List.of(), null);

            EvaluationMetrics metrics = collector.buildMetrics(enriched, 100);

            assertThat(metrics.componentCount()).isEqualTo(0);
            assertThat(metrics.scorecardCoverage()).isEqualTo(0.0);
            assertThat(metrics.provenanceCoverage()).isEqualTo(0.0);
        }

        @Test
        void buildMetricsWithFullCoverage() {
            Component c1 = new Component("axios", "1.0", "axios@1.0", "library",
                    null, "pkg:npm/axios@1.0", null, "required");
            Component c2 = new Component("lodash", "1.0", "lodash@1.0", "library",
                    null, "pkg:npm/lodash@1.0", null, "required");

            ScorecardResult scorecard = new ScorecardResult(7.5, Map.of(), "repo", Instant.now());
            ProvenanceResult provenance = ProvenanceResult.detected(
                    ProvenanceResult.SlsaLevel.SLSA_L2, "npm-sigstore");

            EnrichedComponent ec1 = EnrichedComponent.unenriched(c1)
                    .withScorecard(scorecard).withProvenance(provenance);
            EnrichedComponent ec2 = EnrichedComponent.unenriched(c2)
                    .withScorecard(scorecard).withProvenance(provenance);

            EnrichedDependencyGraph enriched = new EnrichedDependencyGraph(
                    List.of(ec1, ec2), List.of(), null);

            EvaluationMetrics metrics = collector.buildMetrics(enriched, 100);

            assertThat(metrics.scorecardCoverage()).isEqualTo(1.0);
            assertThat(metrics.provenanceCoverage()).isEqualTo(1.0);
        }
    }

    @Nested
    class IntegrationTests {
        @Test
        void endToEndEnrichAndEvaluate() {
            Component c = new Component("pkg", "1.0", "pkg@1.0", "library",
                    null, "pkg:npm/pkg@1.0", null, "required");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), "pkg@1.0");

            EnrichedDependencyGraph enriched = collector.timedEnrich(graph);
            PostureReport report = collector.timedEvaluate(enriched,
                    new PolicyDefinition("scrutinizer/v1", "test", "1.0", List.of(), null),
                    "{}");

            EvaluationMetrics metrics = collector.buildMetrics(enriched, 500);

            assertThat(metrics.totalDurationMs()).isEqualTo(500);
            assertThat(metrics.componentCount()).isEqualTo(1);
            assertThat(metrics.enrichmentDurationMs()).isGreaterThanOrEqualTo(0);
            assertThat(metrics.evaluationDurationMs()).isGreaterThanOrEqualTo(0);
        }
    }

    // Test implementations
    private static class TestScorecardService extends ScorecardService {
        public TestScorecardService() {
            super(null);
        }

        @Override
        public Optional<ScorecardResult> getScorecard(String repoUrl) {
            return Optional.empty();
        }

        @Override
        public int cacheSize() {
            return 0;
        }

        @Override
        public void clearCache() {
        }
    }

    private static class TestProvenanceService extends ProvenanceService {
        public TestProvenanceService() {
            super(null);
        }

        @Override
        public ProvenanceResult checkProvenance(String purl) {
            return ProvenanceResult.absent();
        }

        @Override
        public int cacheSize() {
            return 0;
        }

        @Override
        public void clearCache() {
        }
    }

    private static class TestPostureEvaluator extends PostureEvaluator {
        public TestPostureEvaluator() {
            super(null);
        }

        @Override
        public PostureReport evaluate(EnrichedDependencyGraph graph,
                                     PolicyDefinition policy, String sbomJson) {
            return new PostureReport.Builder()
                    .policyName("test")
                    .policyVersion("1.0")
                    .sbomHash("abc")
                    .build();
        }
    }
}
