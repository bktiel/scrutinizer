package com.scrutinizer.engine;

import com.scrutinizer.policy.Rule;
import com.scrutinizer.policy.ScoringConfig;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PostureScorerTest {

    private RuleResult result(RuleResult.Decision decision) {
        return new RuleResult("comp-ref", "rule-1", decision, "actual", "expected", "test rule");
    }

    private RuleResult resultWithRule(String ruleId, RuleResult.Decision decision) {
        return new RuleResult("comp-ref", ruleId, decision, "actual", "expected", "test rule");
    }

    @Nested
    class PassFailMethod {
        private final ScoringConfig config = new ScoringConfig(
                ScoringConfig.Method.PASS_FAIL, Map.of(), 7.0, 4.0);

        @Test
        void allPassReturnsPass() {
            List<RuleResult> results = List.of(result(RuleResult.Decision.PASS), result(RuleResult.Decision.PASS));
            assertThat(PostureScorer.computeOverallDecision(results, config))
                    .isEqualTo(RuleResult.Decision.PASS);
        }

        @Test
        void anyFailReturnsFail() {
            List<RuleResult> results = List.of(
                    result(RuleResult.Decision.PASS),
                    result(RuleResult.Decision.FAIL),
                    result(RuleResult.Decision.PASS));
            assertThat(PostureScorer.computeOverallDecision(results, config))
                    .isEqualTo(RuleResult.Decision.FAIL);
        }

        @Test
        void warnWithoutFailReturnsWarn() {
            List<RuleResult> results = List.of(
                    result(RuleResult.Decision.PASS),
                    result(RuleResult.Decision.WARN));
            assertThat(PostureScorer.computeOverallDecision(results, config))
                    .isEqualTo(RuleResult.Decision.WARN);
        }

        @Test
        void failTakesPriorityOverWarn() {
            List<RuleResult> results = List.of(
                    result(RuleResult.Decision.WARN),
                    result(RuleResult.Decision.FAIL));
            assertThat(PostureScorer.computeOverallDecision(results, config))
                    .isEqualTo(RuleResult.Decision.FAIL);
        }
    }

    @Nested
    class WeightedAverageMethod {
        private final ScoringConfig config = new ScoringConfig(
                ScoringConfig.Method.WEIGHTED_AVERAGE, Map.of(), 7.0, 4.0);

        @Test
        void allPassScores10() {
            List<RuleResult> results = List.of(
                    result(RuleResult.Decision.PASS),
                    result(RuleResult.Decision.PASS));
            // avg = 10.0 >= 7.0 threshold -> PASS
            assertThat(PostureScorer.computeOverallDecision(results, config))
                    .isEqualTo(RuleResult.Decision.PASS);
        }

        @Test
        void allFailScores0() {
            List<RuleResult> results = List.of(
                    result(RuleResult.Decision.FAIL),
                    result(RuleResult.Decision.FAIL));
            // avg = 0.0 < 4.0 threshold -> FAIL
            assertThat(PostureScorer.computeOverallDecision(results, config))
                    .isEqualTo(RuleResult.Decision.FAIL);
        }

        @Test
        void mixedScoresInWarnRange() {
            List<RuleResult> results = List.of(
                    result(RuleResult.Decision.PASS),  // 10
                    result(RuleResult.Decision.FAIL));  // 0
            // avg = 5.0, which is >= 4.0 (warn) but < 7.0 (pass) -> WARN
            assertThat(PostureScorer.computeOverallDecision(results, config))
                    .isEqualTo(RuleResult.Decision.WARN);
        }

        @Test
        void mixedScoresBelowWarnThreshold() {
            List<RuleResult> results = List.of(
                    result(RuleResult.Decision.PASS),  // 10
                    result(RuleResult.Decision.FAIL),  // 0
                    result(RuleResult.Decision.FAIL),  // 0
                    result(RuleResult.Decision.FAIL)); // 0
            // avg = 2.5 < 4.0 -> FAIL
            assertThat(PostureScorer.computeOverallDecision(results, config))
                    .isEqualTo(RuleResult.Decision.FAIL);
        }

        @Test
        void weightsAppliedToFieldBasedScoring() {
            ScoringConfig weightedConfig = new ScoringConfig(
                    ScoringConfig.Method.WEIGHTED_AVERAGE,
                    Map.of("scorecard.score", 0.8, "provenance.present", 0.2),
                    7.0, 4.0);

            Rule scorecardRule = new Rule("sc-check", "Scorecard", "scorecard.score",
                    Rule.Operator.GTE, "5.0", Rule.Severity.FAIL);
            Rule provenanceRule = new Rule("prov-check", "Provenance", "provenance.present",
                    Rule.Operator.EQ, "true", Rule.Severity.WARN);

            List<RuleResult> results = List.of(
                    resultWithRule("sc-check", RuleResult.Decision.PASS),
                    resultWithRule("prov-check", RuleResult.Decision.FAIL)
            );

            RuleResult.Decision decision = PostureScorer.computeOverallDecision(
                    results, weightedConfig, List.of(scorecardRule, provenanceRule));

            // scorecard: 10.0 * 0.8 = 8.0, provenance: 0.0 * 0.2 = 0.0
            // total = 8.0 / 1.0 = 8.0 >= 7.0 -> PASS
            assertThat(decision).isEqualTo(RuleResult.Decision.PASS);
        }

        @Test
        void weightsHeavilyOnFailingField() {
            ScoringConfig weightedConfig = new ScoringConfig(
                    ScoringConfig.Method.WEIGHTED_AVERAGE,
                    Map.of("scorecard.score", 0.2, "provenance.present", 0.8),
                    7.0, 4.0);

            Rule scorecardRule = new Rule("sc-check", "Scorecard", "scorecard.score",
                    Rule.Operator.GTE, "5.0", Rule.Severity.FAIL);
            Rule provenanceRule = new Rule("prov-check", "Provenance", "provenance.present",
                    Rule.Operator.EQ, "true", Rule.Severity.WARN);

            List<RuleResult> results = List.of(
                    resultWithRule("sc-check", RuleResult.Decision.PASS),
                    resultWithRule("prov-check", RuleResult.Decision.FAIL)
            );

            RuleResult.Decision decision = PostureScorer.computeOverallDecision(
                    results, weightedConfig, List.of(scorecardRule, provenanceRule));

            // scorecard: 10.0 * 0.2 = 2.0, provenance: 0.0 * 0.8 = 0.0
            // total = 2.0 / 1.0 = 2.0 < 4.0 -> FAIL
            assertThat(decision).isEqualTo(RuleResult.Decision.FAIL);
        }
    }

    @Nested
    class SkipAndInfoFiltering {
        private final ScoringConfig config = new ScoringConfig(
                ScoringConfig.Method.PASS_FAIL, Map.of(), 7.0, 4.0);

        @Test
        void skipResultsAreIgnored() {
            List<RuleResult> results = List.of(
                    result(RuleResult.Decision.PASS),
                    result(RuleResult.Decision.SKIP));
            assertThat(PostureScorer.computeOverallDecision(results, config))
                    .isEqualTo(RuleResult.Decision.PASS);
        }

        @Test
        void infoResultsAreIgnored() {
            List<RuleResult> results = List.of(
                    result(RuleResult.Decision.PASS),
                    result(RuleResult.Decision.INFO));
            assertThat(PostureScorer.computeOverallDecision(results, config))
                    .isEqualTo(RuleResult.Decision.PASS);
        }

        @Test
        void allSkipReturnsPass() {
            List<RuleResult> results = List.of(
                    result(RuleResult.Decision.SKIP),
                    result(RuleResult.Decision.SKIP));
            assertThat(PostureScorer.computeOverallDecision(results, config))
                    .isEqualTo(RuleResult.Decision.PASS);
        }

        @Test
        void emptyListReturnsPass() {
            assertThat(PostureScorer.computeOverallDecision(List.of(), config))
                    .isEqualTo(RuleResult.Decision.PASS);
        }
    }

    @Nested
    class ComputeScore {

        @Test
        void allPassScores10() {
            List<RuleResult> results = List.of(
                    result(RuleResult.Decision.PASS),
                    result(RuleResult.Decision.PASS));
            assertThat(PostureScorer.computeScore(results)).isCloseTo(10.0, within(0.01));
        }

        @Test
        void allFailScores0() {
            List<RuleResult> results = List.of(
                    result(RuleResult.Decision.FAIL),
                    result(RuleResult.Decision.FAIL));
            assertThat(PostureScorer.computeScore(results)).isCloseTo(0.0, within(0.01));
        }

        @Test
        void allWarnScores5() {
            List<RuleResult> results = List.of(
                    result(RuleResult.Decision.WARN),
                    result(RuleResult.Decision.WARN));
            assertThat(PostureScorer.computeScore(results)).isCloseTo(5.0, within(0.01));
        }

        @Test
        void mixedScore() {
            List<RuleResult> results = List.of(
                    result(RuleResult.Decision.PASS),  // 10
                    result(RuleResult.Decision.WARN),  // 5
                    result(RuleResult.Decision.FAIL)); // 0
            // (10 + 5 + 0) / 3 = 5.0
            assertThat(PostureScorer.computeScore(results)).isCloseTo(5.0, within(0.01));
        }

        @Test
        void emptyReturns10() {
            assertThat(PostureScorer.computeScore(List.of())).isCloseTo(10.0, within(0.01));
        }

        @Test
        void skipAndInfoExcluded() {
            List<RuleResult> results = List.of(
                    result(RuleResult.Decision.PASS),
                    result(RuleResult.Decision.SKIP),
                    result(RuleResult.Decision.INFO));
            // Only PASS counted: 10/1 = 10.0
            assertThat(PostureScorer.computeScore(results)).isCloseTo(10.0, within(0.01));
        }
    }
}
