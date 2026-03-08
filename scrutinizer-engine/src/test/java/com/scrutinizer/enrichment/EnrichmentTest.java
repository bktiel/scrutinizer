package com.scrutinizer.enrichment;

import com.scrutinizer.model.Component;
import com.scrutinizer.model.DependencyEdge;
import com.scrutinizer.model.DependencyGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EnrichmentTest {

    @Nested
    class PurlResolverTest {
        @Test
        void resolvesNpmUnscopedPurl() {
            Optional<String> url = PurlResolver.toRepoUrl("pkg:npm/axios@1.7.2");
            assertThat(url).hasValue("github.com/axios/axios");
        }

        @Test
        void resolvesNpmScopedPurl() {
            Optional<String> url = PurlResolver.toRepoUrl("pkg:npm/%40mui/material@5.15.19");
            assertThat(url).hasValue("github.com/mui/material");
        }

        @Test
        void resolvesMavenSpringPurl() {
            Optional<String> url = PurlResolver.toRepoUrl(
                    "pkg:maven/org.springframework/spring-core@6.1.8");
            assertThat(url).hasValue("github.com/spring-projects/spring-core");
        }

        @Test
        void resolvesMavenJacksonPurl() {
            Optional<String> url = PurlResolver.toRepoUrl(
                    "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.17.1");
            assertThat(url).hasValue("github.com/FasterXML/jackson-databind");
        }

        @Test
        void returnsEmptyForNull() {
            assertThat(PurlResolver.toRepoUrl(null)).isEmpty();
        }

        @Test
        void returnsEmptyForUnknownScheme() {
            assertThat(PurlResolver.toRepoUrl("pkg:pypi/requests@2.31.0")).isEmpty();
        }
    }

    @Nested
    class ScorecardResultTest {
        @Test
        void creation() {
            ScorecardResult result = new ScorecardResult(
                    7.5,
                    Map.of("Maintained", 10.0, "Vulnerabilities", 8.0),
                    "github.com/axios/axios",
                    Instant.now()
            );
            assertThat(result.overallScore()).isEqualTo(7.5);
            assertThat(result.checkScores()).containsEntry("Maintained", 10.0);
            assertThat(result.repoUrl()).isEqualTo("github.com/axios/axios");
        }

        @Test
        void equality() {
            Instant now = Instant.now();
            ScorecardResult r1 = new ScorecardResult(7.5, Map.of("A", 10.0), "repo", now);
            ScorecardResult r2 = new ScorecardResult(7.5, Map.of("A", 10.0), "repo", now);
            assertThat(r1).isEqualTo(r2);
        }
    }

    @Nested
    class ProvenanceResultTest {
        @Test
        void absentProvenance() {
            ProvenanceResult result = ProvenanceResult.absent();
            assertThat(result.isPresent()).isFalse();
            assertThat(result.level()).isEmpty();
            assertThat(result.source()).isEmpty();
        }

        @Test
        void detectedProvenance() {
            ProvenanceResult result = ProvenanceResult.detected(
                    ProvenanceResult.SlsaLevel.SLSA_L2, "npm-sigstore");
            assertThat(result.isPresent()).isTrue();
            assertThat(result.level()).hasValue(ProvenanceResult.SlsaLevel.SLSA_L2);
            assertThat(result.source()).hasValue("npm-sigstore");
        }

        @Test
        void equality() {
            ProvenanceResult r1 = ProvenanceResult.detected(
                    ProvenanceResult.SlsaLevel.SLSA_L2, "npm-sigstore");
            ProvenanceResult r2 = ProvenanceResult.detected(
                    ProvenanceResult.SlsaLevel.SLSA_L2, "npm-sigstore");
            assertThat(r1).isEqualTo(r2);
        }
    }

    @Nested
    class EnrichedComponentTest {
        @Test
        void unenriched() {
            Component c = new Component("axios", "1.7.2", "axios@1.7.2");
            EnrichedComponent ec = EnrichedComponent.unenriched(c);
            assertThat(ec.component()).isEqualTo(c);
            assertThat(ec.scorecardResult()).isEmpty();
            assertThat(ec.provenanceResult()).isEmpty();
            assertThat(ec.hasEnrichment()).isFalse();
        }

        @Test
        void withScorecard() {
            Component c = new Component("axios", "1.7.2", "axios@1.7.2");
            ScorecardResult sr = new ScorecardResult(7.5, Map.of(), "repo", Instant.now());
            EnrichedComponent ec = EnrichedComponent.unenriched(c).withScorecard(sr);
            assertThat(ec.scorecardResult()).isPresent();
            assertThat(ec.hasEnrichment()).isTrue();
        }

        @Test
        void withProvenance() {
            Component c = new Component("axios", "1.7.2", "axios@1.7.2");
            ProvenanceResult pr = ProvenanceResult.detected(
                    ProvenanceResult.SlsaLevel.SLSA_L2, "npm-sigstore");
            EnrichedComponent ec = EnrichedComponent.unenriched(c).withProvenance(pr);
            assertThat(ec.provenanceResult()).isPresent();
            assertThat(ec.hasEnrichment()).isTrue();
        }

        @Test
        void immutableChaining() {
            Component c = new Component("axios", "1.7.2", "axios@1.7.2");
            ScorecardResult sr = new ScorecardResult(7.5, Map.of(), "repo", Instant.now());
            ProvenanceResult pr = ProvenanceResult.detected(
                    ProvenanceResult.SlsaLevel.SLSA_L2, "npm-sigstore");
            EnrichedComponent original = EnrichedComponent.unenriched(c);
            EnrichedComponent withBoth = original.withScorecard(sr).withProvenance(pr);
            // Original is unmodified
            assertThat(original.hasEnrichment()).isFalse();
            assertThat(withBoth.scorecardResult()).isPresent();
            assertThat(withBoth.provenanceResult()).isPresent();
        }

        @Test
        void ordering() {
            Component cA = new Component("a-lib", "1.0", "a@1.0");
            Component cB = new Component("b-lib", "1.0", "b@1.0");
            EnrichedComponent ecB = EnrichedComponent.unenriched(cB);
            EnrichedComponent ecA = EnrichedComponent.unenriched(cA);
            assertThat(ecA.compareTo(ecB)).isLessThan(0);
        }
    }

    @Nested
    class EnrichedDependencyGraphTest {
        @Test
        void fromGraph() {
            Component cA = new Component("a", "1.0", "a@1.0");
            Component cB = new Component("b", "1.0", "b@1.0");
            DependencyEdge edge = new DependencyEdge("a@1.0", "b@1.0");
            DependencyGraph graph = new DependencyGraph(
                    List.of(cA, cB), List.of(edge), "a@1.0");

            EnrichedDependencyGraph enriched = EnrichedDependencyGraph.fromGraph(graph);
            assertThat(enriched.componentCount()).isEqualTo(2);
            assertThat(enriched.edgeCount()).isEqualTo(1);
            assertThat(enriched.rootRef()).hasValue("a@1.0");
            assertThat(enriched.scorecardCoverage()).isEqualTo(0);
            assertThat(enriched.provenanceCoverage()).isEqualTo(0);
        }

        @Test
        void coverageCounting() {
            Component c = new Component("a", "1.0", "a@1.0");
            ScorecardResult sr = new ScorecardResult(7.5, Map.of(), "repo", Instant.now());
            ProvenanceResult pr = ProvenanceResult.detected(
                    ProvenanceResult.SlsaLevel.SLSA_L2, "npm-sigstore");
            EnrichedComponent ec = EnrichedComponent.unenriched(c)
                    .withScorecard(sr).withProvenance(pr);

            Component c2 = new Component("b", "1.0", "b@1.0");
            EnrichedComponent ec2 = EnrichedComponent.unenriched(c2);

            EnrichedDependencyGraph graph = new EnrichedDependencyGraph(
                    List.of(ec, ec2), List.of(), null);
            assertThat(graph.scorecardCoverage()).isEqualTo(1);
            assertThat(graph.provenanceCoverage()).isEqualTo(1);
        }

        @Test
        void summary() {
            Component c = new Component("a", "1.0", "a@1.0");
            EnrichedDependencyGraph graph = new EnrichedDependencyGraph(
                    List.of(EnrichedComponent.unenriched(c)), List.of(), null);
            Map<String, Object> s = graph.summary();
            assertThat(s.get("total_components")).isEqualTo(1);
            assertThat(s.get("total_edges")).isEqualTo(0);
            assertThat(s.get("scorecard_coverage")).isEqualTo(0L);
            assertThat(s.get("provenance_coverage")).isEqualTo(0L);
        }
    }
}
