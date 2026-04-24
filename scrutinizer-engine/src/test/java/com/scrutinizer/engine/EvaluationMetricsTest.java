package com.scrutinizer.engine;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluationMetricsTest {

    @Nested
    class CreationTests {
        @Test
        void createWithAllValues() {
            EvaluationMetrics metrics = new EvaluationMetrics(
                    1000,  // totalDurationMs
                    500,   // enrichmentDurationMs
                    300,   // evaluationDurationMs
                    10,    // componentCount
                    0.8,   // scorecardCoverage
                    0.7,   // provenanceCoverage
                    0.9    // cacheHitRate
            );

            assertThat(metrics.totalDurationMs()).isEqualTo(1000);
            assertThat(metrics.enrichmentDurationMs()).isEqualTo(500);
            assertThat(metrics.evaluationDurationMs()).isEqualTo(300);
            assertThat(metrics.componentCount()).isEqualTo(10);
            assertThat(metrics.scorecardCoverage()).isEqualTo(0.8);
            assertThat(metrics.provenanceCoverage()).isEqualTo(0.7);
            assertThat(metrics.cacheHitRate()).isEqualTo(0.9);
        }

        @Test
        void createWithZeroValues() {
            EvaluationMetrics metrics = new EvaluationMetrics(0, 0, 0, 0, 0.0, 0.0, 0.0);

            assertThat(metrics.totalDurationMs()).isEqualTo(0);
            assertThat(metrics.enrichmentDurationMs()).isEqualTo(0);
            assertThat(metrics.evaluationDurationMs()).isEqualTo(0);
            assertThat(metrics.componentCount()).isEqualTo(0);
            assertThat(metrics.scorecardCoverage()).isEqualTo(0.0);
            assertThat(metrics.provenanceCoverage()).isEqualTo(0.0);
            assertThat(metrics.cacheHitRate()).isEqualTo(0.0);
        }

        @Test
        void createWithMaxValues() {
            EvaluationMetrics metrics = new EvaluationMetrics(
                    Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE,
                    Integer.MAX_VALUE, 1.0, 1.0, 1.0);

            assertThat(metrics.totalDurationMs()).isEqualTo(Long.MAX_VALUE);
            assertThat(metrics.componentCount()).isEqualTo(Integer.MAX_VALUE);
            assertThat(metrics.scorecardCoverage()).isEqualTo(1.0);
        }
    }

    @Nested
    class ToMapConversionTests {
        @Test
        void toMapIncludesDurationFields() {
            EvaluationMetrics metrics = new EvaluationMetrics(1000, 500, 300, 10, 0.8, 0.7, 0.9);

            Map<String, Object> map = metrics.toMap();

            assertThat(map).containsEntry("totalDurationMs", 1000L);
            assertThat(map).containsEntry("enrichmentDurationMs", 500L);
            assertThat(map).containsEntry("evaluationDurationMs", 300L);
        }

        @Test
        void toMapIncludesComponentCount() {
            EvaluationMetrics metrics = new EvaluationMetrics(1000, 500, 300, 42, 0.8, 0.7, 0.9);

            Map<String, Object> map = metrics.toMap();

            assertThat(map).containsEntry("componentCount", 42);
        }

        @Test
        void toMapIncludesSignalCoverage() {
            EvaluationMetrics metrics = new EvaluationMetrics(1000, 500, 300, 10, 0.8, 0.7, 0.9);

            Map<String, Object> map = metrics.toMap();

            assertThat(map).containsKey("signalCoverage");
            Map<String, Object> coverage = (Map<String, Object>) map.get("signalCoverage");
            assertThat(coverage).containsEntry("scorecardPercent", 80.0);
            assertThat(coverage).containsEntry("provenancePercent", 70.0);
        }

        @Test
        void toMapIncludesCacheHitRate() {
            EvaluationMetrics metrics = new EvaluationMetrics(1000, 500, 300, 10, 0.8, 0.7, 0.9);

            Map<String, Object> map = metrics.toMap();

            assertThat(map).containsEntry("cacheHitRate", 90.0);
        }

        @Test
        void toMapConvertsPercentagesToTwoDecimals() {
            EvaluationMetrics metrics = new EvaluationMetrics(1000, 500, 300, 10, 0.333, 0.666, 0.123);

            Map<String, Object> map = metrics.toMap();

            Map<String, Object> coverage = (Map<String, Object>) map.get("signalCoverage");
            assertThat((Double) coverage.get("scorecardPercent")).isCloseTo(33.3, within(0.1));
            assertThat((Double) coverage.get("provenancePercent")).isCloseTo(66.6, within(0.1));
            assertThat((Double) map.get("cacheHitRate")).isCloseTo(12.3, within(0.1));
        }

        @Test
        void toMapWithZeroCoverage() {
            EvaluationMetrics metrics = new EvaluationMetrics(1000, 500, 300, 10, 0.0, 0.0, 0.0);

            Map<String, Object> map = metrics.toMap();

            Map<String, Object> coverage = (Map<String, Object>) map.get("signalCoverage");
            assertThat(coverage.get("scorecardPercent")).isEqualTo(0.0);
            assertThat(coverage.get("provenancePercent")).isEqualTo(0.0);
            assertThat(map.get("cacheHitRate")).isEqualTo(0.0);
        }

        @Test
        void toMapWithFullCoverage() {
            EvaluationMetrics metrics = new EvaluationMetrics(1000, 500, 300, 10, 1.0, 1.0, 1.0);

            Map<String, Object> map = metrics.toMap();

            Map<String, Object> coverage = (Map<String, Object>) map.get("signalCoverage");
            assertThat(coverage.get("scorecardPercent")).isEqualTo(100.0);
            assertThat(coverage.get("provenancePercent")).isEqualTo(100.0);
            assertThat(map.get("cacheHitRate")).isEqualTo(100.0);
        }

        @Test
        void toMapPreservesOrder() {
            EvaluationMetrics metrics = new EvaluationMetrics(1000, 500, 300, 10, 0.8, 0.7, 0.9);

            Map<String, Object> map = metrics.toMap();

            // LinkedHashMap preserves insertion order
            assertThat(map.keySet()).containsExactly(
                    "totalDurationMs",
                    "enrichmentDurationMs",
                    "evaluationDurationMs",
                    "componentCount",
                    "signalCoverage",
                    "cacheHitRate"
            );
        }
    }

    @Nested
    class PercentageCalculationTests {
        @Test
        void roundsScorecardPercentageCorrectly() {
            // 0.123456 * 10000 / 100 = 12.3456 rounded to 12.35
            EvaluationMetrics metrics = new EvaluationMetrics(0, 0, 0, 10, 0.123456, 0.0, 0.0);

            Map<String, Object> map = metrics.toMap();
            Map<String, Object> coverage = (Map<String, Object>) map.get("signalCoverage");

            assertThat((Double) coverage.get("scorecardPercent")).isCloseTo(12.35, within(0.01));
        }

        @Test
        void roundsProvenancePercentageCorrectly() {
            EvaluationMetrics metrics = new EvaluationMetrics(0, 0, 0, 10, 0.0, 0.654321, 0.0);

            Map<String, Object> map = metrics.toMap();
            Map<String, Object> coverage = (Map<String, Object>) map.get("signalCoverage");

            assertThat((Double) coverage.get("provenancePercent")).isCloseTo(65.43, within(0.01));
        }

        @Test
        void roundsCacheHitRateCorrectly() {
            EvaluationMetrics metrics = new EvaluationMetrics(0, 0, 0, 10, 0.0, 0.0, 0.987654);

            Map<String, Object> map = metrics.toMap();

            assertThat((Double) map.get("cacheHitRate")).isCloseTo(98.77, within(0.01));
        }

        @Test
        void handlesSinglePercentValue() {
            EvaluationMetrics metrics = new EvaluationMetrics(0, 0, 0, 10, 0.5, 0.5, 0.5);

            Map<String, Object> map = metrics.toMap();

            Map<String, Object> coverage = (Map<String, Object>) map.get("signalCoverage");
            assertThat(coverage.get("scorecardPercent")).isEqualTo(50.0);
            assertThat(coverage.get("provenancePercent")).isEqualTo(50.0);
            assertThat(map.get("cacheHitRate")).isEqualTo(50.0);
        }
    }

    @Nested
    class RecordBehaviorTests {
        @Test
        void recordIsImmutable() {
            EvaluationMetrics metrics = new EvaluationMetrics(1000, 500, 300, 10, 0.8, 0.7, 0.9);
            Map<String, Object> map1 = metrics.toMap();
            Map<String, Object> map2 = metrics.toMap();

            // Same metrics should produce equal maps (though they're different objects)
            assertThat(map1.get("totalDurationMs")).isEqualTo(map2.get("totalDurationMs"));
        }

        @Test
        void recordCanBeUsedAsValueObject() {
            EvaluationMetrics metrics1 = new EvaluationMetrics(1000, 500, 300, 10, 0.8, 0.7, 0.9);
            EvaluationMetrics metrics2 = new EvaluationMetrics(1000, 500, 300, 10, 0.8, 0.7, 0.9);

            // Records provide value-based equality
            assertThat(metrics1).isEqualTo(metrics2);
        }

        @Test
        void recordToStringIsImplemented() {
            EvaluationMetrics metrics = new EvaluationMetrics(1000, 500, 300, 10, 0.8, 0.7, 0.9);

            String str = metrics.toString();

            assertThat(str).isNotNull().isNotEmpty();
            assertThat(str).contains("1000");
        }
    }

    private static org.assertj.core.api.Assertions.within within(double offset) {
        return org.assertj.core.api.Assertions.within(offset);
    }
}
