package com.scrutinizer.enrichment;

import com.scrutinizer.model.Component;
import com.scrutinizer.model.DependencyEdge;
import com.scrutinizer.model.DependencyGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EnrichmentPipelineTest {

    @Nested
    class EnrichmentTests {
        private EnrichmentPipeline pipeline;

        @BeforeEach
        void setUp() {
            ScorecardService scorecardService = new TestScorecardService();
            ProvenanceService provenanceService = new TestProvenanceService();
            pipeline = new EnrichmentPipeline(scorecardService, provenanceService);
        }

        @Test
        void enrichComponentsWithPurls() {
            Component c = new Component("axios", "1.7.2", "axios@1.7.2", "library",
                    null, "pkg:npm/axios@1.7.2", null, "required");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            EnrichedDependencyGraph enriched = pipeline.enrich(graph);

            assertThat(enriched.componentCount()).isEqualTo(1);
            EnrichedComponent ec = enriched.components().get(0);
            assertThat(ec.component().name()).isEqualTo("axios");
        }

        @Test
        void enrichComponentsWithoutPurls() {
            Component c = new Component("unknown", "1.0", "unknown@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            EnrichedDependencyGraph enriched = pipeline.enrich(graph);

            assertThat(enriched.componentCount()).isEqualTo(1);
            EnrichedComponent ec = enriched.components().get(0);
            assertThat(ec.component()).isEqualTo(c);
        }

        @Test
        void gracefullyHandlesMissingEnrichment() {
            Component c = new Component("nosignal", "1.0", "nosignal@1.0", "library",
                    null, "pkg:npm/nosignal@1.0", null, "required");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            EnrichedDependencyGraph enriched = pipeline.enrich(graph);

            assertThat(enriched.componentCount()).isEqualTo(1);
            EnrichedComponent ec = enriched.components().get(0);
            assertThat(ec.scorecardResult()).isEmpty();
            assertThat(ec.provenanceResult()).isEmpty();
        }

        @Test
        void enrichMultipleComponents() {
            Component c1 = new Component("pkg1", "1.0", "pkg1@1.0", "library",
                    null, "pkg:npm/pkg1@1.0", null, "required");
            Component c2 = new Component("pkg2", "1.0", "pkg2@1.0");
            Component c3 = new Component("pkg3", "1.0", "pkg3@1.0", "library",
                    null, "pkg:npm/pkg3@1.0", null, "required");

            DependencyGraph graph = new DependencyGraph(List.of(c1, c2, c3), List.of(), null);

            EnrichedDependencyGraph enriched = pipeline.enrich(graph);

            assertThat(enriched.componentCount()).isEqualTo(3);
        }

        @Test
        void preservesComponentOrder() {
            Component c1 = new Component("a", "1.0", "a@1.0");
            Component c2 = new Component("b", "1.0", "b@1.0");
            Component c3 = new Component("c", "1.0", "c@1.0");

            DependencyGraph graph = new DependencyGraph(List.of(c1, c2, c3), List.of(), null);

            EnrichedDependencyGraph enriched = pipeline.enrich(graph);

            assertThat(enriched.components())
                    .extracting(ec -> ec.component().name())
                    .containsExactly("a", "b", "c");
        }

        @Test
        void preservesDependencyEdges() {
            Component c1 = new Component("a", "1.0", "a@1.0");
            Component c2 = new Component("b", "1.0", "b@1.0");
            DependencyEdge edge = new DependencyEdge("a@1.0", "b@1.0");

            DependencyGraph graph = new DependencyGraph(List.of(c1, c2), List.of(edge), null);

            EnrichedDependencyGraph enriched = pipeline.enrich(graph);

            assertThat(enriched.edgeCount()).isEqualTo(1);
            assertThat(enriched.edges()).containsExactly(edge);
        }

        @Test
        void preservesRootRef() {
            Component c = new Component("root", "1.0", "root@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), "root@1.0");

            EnrichedDependencyGraph enriched = pipeline.enrich(graph);

            assertThat(enriched.rootRef()).hasValue("root@1.0");
        }
    }

    @Nested
    class WarmCacheTests {
        private EnrichmentPipeline pipeline;

        @BeforeEach
        void setUp() {
            ScorecardService scorecardService = new TestScorecardService();
            ProvenanceService provenanceService = new TestProvenanceService();
            pipeline = new EnrichmentPipeline(scorecardService, provenanceService);
        }

        @Test
        void warmCacheReturnsStats() {
            Component c = new Component("axios", "1.7.2", "axios@1.7.2", "library",
                    null, "pkg:npm/axios@1.7.2", null, "required");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            Map<String, Object> stats = pipeline.warmCache(graph);

            assertThat(stats).containsKeys("componentsProcessed", "scorecardAttempted",
                    "scorecardSuccess", "provenanceAttempted", "provenanceSuccess",
                    "scorecardCacheSize", "provenanceCacheSize");
        }

        @Test
        void warmCacheCountsComponentsProcessed() {
            Component c1 = new Component("pkg1", "1.0", "pkg1@1.0");
            Component c2 = new Component("pkg2", "1.0", "pkg2@1.0");
            Component c3 = new Component("pkg3", "1.0", "pkg3@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c1, c2, c3), List.of(), null);

            Map<String, Object> stats = pipeline.warmCache(graph);

            assertThat(stats.get("componentsProcessed")).isEqualTo(3);
        }

        @Test
        void warmCacheCountsScorecardAttempts() {
            Component c1 = new Component("axios", "1.7.2", "axios@1.7.2", "library",
                    null, "pkg:npm/axios@1.7.2", null, "required");
            Component c2 = new Component("nosig", "1.0", "nosig@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c1, c2), List.of(), null);

            Map<String, Object> stats = pipeline.warmCache(graph);

            assertThat(stats.get("scorecardAttempted")).isEqualTo(1);
        }

        @Test
        void warmCacheCountsProvenanceAttempts() {
            Component c1 = new Component("axios", "1.7.2", "axios@1.7.2", "library",
                    null, "pkg:npm/axios@1.7.2", null, "required");
            Component c2 = new Component("nosig", "1.0", "nosig@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c1, c2), List.of(), null);

            Map<String, Object> stats = pipeline.warmCache(graph);

            assertThat(stats.get("provenanceAttempted")).isEqualTo(1);
        }

        @Test
        void warmCachePopulatesScorecardCache() {
            Component c = new Component("axios", "1.7.2", "axios@1.7.2", "library",
                    null, "pkg:npm/axios@1.7.2", null, "required");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            Map<String, Object> stats = pipeline.warmCache(graph);

            assertThat(stats.get("scorecardCacheSize")).isGreaterThanOrEqualTo(0);
        }

        @Test
        void warmCachePopulatesProvenanceCache() {
            Component c = new Component("axios", "1.7.2", "axios@1.7.2", "library",
                    null, "pkg:npm/axios@1.7.2", null, "required");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            Map<String, Object> stats = pipeline.warmCache(graph);

            assertThat(stats.get("provenanceCacheSize")).isGreaterThanOrEqualTo(0);
        }

        @Test
        void warmCacheOnEmptyGraph() {
            DependencyGraph graph = new DependencyGraph(List.of(), List.of(), null);

            Map<String, Object> stats = pipeline.warmCache(graph);

            assertThat(stats.get("componentsProcessed")).isEqualTo(0);
            assertThat(stats.get("scorecardAttempted")).isEqualTo(0);
        }
    }

    @Nested
    class CacheManagementTests {
        private EnrichmentPipeline pipeline;
        private ScorecardService scorecardService;
        private ProvenanceService provenanceService;

        @BeforeEach
        void setUp() {
            scorecardService = new TestScorecardService();
            provenanceService = new TestProvenanceService();
            pipeline = new EnrichmentPipeline(scorecardService, provenanceService);
        }

        @Test
        void cacheStatsReturnsCacheSizes() {
            Component c = new Component("axios", "1.7.2", "axios@1.7.2", "library",
                    null, "pkg:npm/axios@1.7.2", null, "required");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);
            pipeline.enrich(graph);

            Map<String, Object> stats = pipeline.cacheStats();

            assertThat(stats).containsKeys("scorecardCacheSize", "provenanceCacheSize");
            assertThat(stats.get("scorecardCacheSize")).isNotNull();
            assertThat(stats.get("provenanceCacheSize")).isNotNull();
        }

        @Test
        void clearCachesEmptiesAllCaches() {
            Component c = new Component("axios", "1.7.2", "axios@1.7.2", "library",
                    null, "pkg:npm/axios@1.7.2", null, "required");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);
            pipeline.enrich(graph);

            assertThat(scorecardService.cacheSize()).isGreaterThanOrEqualTo(0);

            pipeline.clearCaches();

            assertThat(scorecardService.cacheSize()).isEqualTo(0);
            assertThat(provenanceService.cacheSize()).isEqualTo(0);
        }

        @Test
        void cacheStatsAfterClearIsEmpty() {
            Component c = new Component("axios", "1.7.2", "axios@1.7.2", "library",
                    null, "pkg:npm/axios@1.7.2", null, "required");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);
            pipeline.enrich(graph);
            pipeline.clearCaches();

            Map<String, Object> stats = pipeline.cacheStats();

            assertThat(stats.get("scorecardCacheSize")).isEqualTo(0);
            assertThat(stats.get("provenanceCacheSize")).isEqualTo(0);
        }
    }

    // Test implementations of services with controlled behavior
    private static class TestScorecardService extends ScorecardService {
        public TestScorecardService() {
            super(null);
        }

        @Override
        public Optional<ScorecardResult> getScorecard(String repoUrl) {
            if (repoUrl.contains("axios")) {
                return Optional.of(new ScorecardResult(7.5,
                        Map.of("Maintained", 10.0), repoUrl, Instant.now()));
            }
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
            if (purl != null && purl.contains("axios")) {
                return ProvenanceResult.detected(
                        ProvenanceResult.SlsaLevel.SLSA_L2, "npm-sigstore");
            }
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
}
