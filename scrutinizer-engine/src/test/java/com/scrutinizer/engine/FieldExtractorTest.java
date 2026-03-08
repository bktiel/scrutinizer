package com.scrutinizer.engine;

import com.scrutinizer.enrichment.EnrichedComponent;
import com.scrutinizer.enrichment.ProvenanceResult;
import com.scrutinizer.enrichment.ScorecardResult;
import com.scrutinizer.model.Component;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class FieldExtractorTest {

    private Component baseComponent() {
        return new Component("jackson-core", "2.15.3", "jackson-core-ref", "library",
                "com.fasterxml.jackson", "pkg:maven/com.fasterxml.jackson/jackson-core@2.15.3",
                "Core Jackson processing", "required");
    }

    @Nested
    class ComponentFields {

        @Test
        void extractsName() {
            EnrichedComponent ec = EnrichedComponent.unenriched(baseComponent());
            assertThat(FieldExtractor.extract(ec, "name")).hasValue("jackson-core");
        }

        @Test
        void extractsVersion() {
            EnrichedComponent ec = EnrichedComponent.unenriched(baseComponent());
            assertThat(FieldExtractor.extract(ec, "version")).hasValue("2.15.3");
        }

        @Test
        void extractsType() {
            EnrichedComponent ec = EnrichedComponent.unenriched(baseComponent());
            assertThat(FieldExtractor.extract(ec, "type")).hasValue("library");
        }

        @Test
        void extractsScope() {
            EnrichedComponent ec = EnrichedComponent.unenriched(baseComponent());
            assertThat(FieldExtractor.extract(ec, "scope")).hasValue("required");
        }

        @Test
        void extractsGroup() {
            EnrichedComponent ec = EnrichedComponent.unenriched(baseComponent());
            assertThat(FieldExtractor.extract(ec, "group")).hasValue("com.fasterxml.jackson");
        }

        @Test
        void extractsPurl() {
            EnrichedComponent ec = EnrichedComponent.unenriched(baseComponent());
            assertThat(FieldExtractor.extract(ec, "purl")).isPresent();
        }

        @Test
        void extractsBomRef() {
            EnrichedComponent ec = EnrichedComponent.unenriched(baseComponent());
            assertThat(FieldExtractor.extract(ec, "bomRef")).hasValue("jackson-core-ref");
        }
    }

    @Nested
    class ScorecardFields {

        @Test
        void extractsOverallScore() {
            ScorecardResult sr = new ScorecardResult(7.5,
                    Map.of("Code-Review", 8.0), "https://github.com/test", Instant.now());
            EnrichedComponent ec = EnrichedComponent.unenriched(baseComponent()).withScorecard(sr);

            assertThat(FieldExtractor.extract(ec, "scorecard.score")).hasValue("7.5");
        }

        @Test
        void extractsCheckScore() {
            ScorecardResult sr = new ScorecardResult(7.5,
                    Map.of("Code-Review", 8.0, "Maintained", 6.0),
                    "https://github.com/test", Instant.now());
            EnrichedComponent ec = EnrichedComponent.unenriched(baseComponent()).withScorecard(sr);

            assertThat(FieldExtractor.extract(ec, "scorecard.checks.Code-Review")).hasValue("8.0");
            assertThat(FieldExtractor.extract(ec, "scorecard.checks.Maintained")).hasValue("6.0");
        }

        @Test
        void missingCheckReturnsEmpty() {
            ScorecardResult sr = new ScorecardResult(7.5,
                    Map.of("Code-Review", 8.0), "https://github.com/test", Instant.now());
            EnrichedComponent ec = EnrichedComponent.unenriched(baseComponent()).withScorecard(sr);

            assertThat(FieldExtractor.extract(ec, "scorecard.checks.NonExistent")).isEmpty();
        }

        @Test
        void noScorecardReturnsEmpty() {
            EnrichedComponent ec = EnrichedComponent.unenriched(baseComponent());
            assertThat(FieldExtractor.extract(ec, "scorecard.score")).isEmpty();
        }
    }

    @Nested
    class ProvenanceFields {

        @Test
        void extractsProvenancePresent() {
            ProvenanceResult pr = ProvenanceResult.detected(ProvenanceResult.SlsaLevel.SLSA_L2, "sigstore");
            EnrichedComponent ec = EnrichedComponent.unenriched(baseComponent()).withProvenance(pr);

            assertThat(FieldExtractor.extract(ec, "provenance.present")).hasValue("true");
        }

        @Test
        void extractsProvenanceLevel() {
            ProvenanceResult pr = ProvenanceResult.detected(ProvenanceResult.SlsaLevel.SLSA_L2, "sigstore");
            EnrichedComponent ec = EnrichedComponent.unenriched(baseComponent()).withProvenance(pr);

            assertThat(FieldExtractor.extract(ec, "provenance.level")).hasValue("SLSA_L2");
        }

        @Test
        void extractsProvenanceSource() {
            ProvenanceResult pr = ProvenanceResult.detected(ProvenanceResult.SlsaLevel.SLSA_L2, "sigstore");
            EnrichedComponent ec = EnrichedComponent.unenriched(baseComponent()).withProvenance(pr);

            assertThat(FieldExtractor.extract(ec, "provenance.source")).hasValue("sigstore");
        }

        @Test
        void absentProvenanceReturnsFalse() {
            ProvenanceResult pr = ProvenanceResult.absent();
            EnrichedComponent ec = EnrichedComponent.unenriched(baseComponent()).withProvenance(pr);

            assertThat(FieldExtractor.extract(ec, "provenance.present")).hasValue("false");
        }

        @Test
        void noProvenanceReturnsEmpty() {
            EnrichedComponent ec = EnrichedComponent.unenriched(baseComponent());
            assertThat(FieldExtractor.extract(ec, "provenance.present")).isEmpty();
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void nullFieldPathReturnsEmpty() {
            EnrichedComponent ec = EnrichedComponent.unenriched(baseComponent());
            assertThat(FieldExtractor.extract(ec, null)).isEmpty();
        }

        @Test
        void unknownFieldPathReturnsEmpty() {
            EnrichedComponent ec = EnrichedComponent.unenriched(baseComponent());
            assertThat(FieldExtractor.extract(ec, "nonexistent.field")).isEmpty();
        }
    }
}
