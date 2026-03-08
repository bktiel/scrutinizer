package com.scrutinizer.engine;

import com.scrutinizer.enrichment.EnrichedComponent;
import com.scrutinizer.enrichment.ScorecardResult;
import com.scrutinizer.enrichment.ProvenanceResult;
import com.scrutinizer.model.Component;
import com.scrutinizer.policy.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEvaluatorTest {

    private RuleEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new RuleEvaluator();
    }

    private Component testComponent(String name, String version) {
        return new Component(name, version, name + "-ref", "library",
                Optional.of("com.test"), Optional.of("pkg:maven/com.test/" + name + "@" + version),
                Optional.empty(), "required");
    }

    private EnrichedComponent enrichedWithScorecard(Component comp, double score) {
        ScorecardResult sr = new ScorecardResult(score,
                Map.of("Code-Review", 8.0, "Maintained", 6.0),
                "https://github.com/test/repo",
                Instant.now().toString());
        return new EnrichedComponent(comp).withScorecard(sr);
    }

    private EnrichedComponent enrichedWithProvenance(Component comp, boolean present) {
        ProvenanceResult pr = present
                ? ProvenanceResult.detected(ProvenanceResult.SlsaLevel.L2, "sigstore")
                : ProvenanceResult.absent();
        return new EnrichedComponent(comp).withProvenance(pr);
    }

    @Nested
    class EqualityOperators {

        @Test
        void eqPassesOnMatch() {
            Component comp = testComponent("jackson-core", "2.15.0");
            EnrichedComponent ec = new EnrichedComponent(comp);
            Rule rule = new Rule("r1", "Check name", "name", Rule.Operator.EQ, "jackson-core", Rule.Severity.FAIL);

            RuleResult result = evaluator.evaluate(rule, ec);
            assertThat(result.decision()).isEqualTo(RuleResult.Decision.PASS);
        }

        @Test
        void eqFailsOnMismatch() {
            Component comp = testComponent("jackson-core", "2.15.0");
            EnrichedComponent ec = new EnrichedComponent(comp);
            Rule rule = new Rule("r1", "Check name", "name", Rule.Operator.EQ, "jackson-databind", Rule.Severity.FAIL);

            RuleResult result = evaluator.evaluate(rule, ec);
            assertThat(result.decision()).isEqualTo(RuleResult.Decision.FAIL);
        }

        @Test
        void neqPassesOnMismatch() {
            Component comp = testComponent("jackson-core", "2.15.0");
            EnrichedComponent ec = new EnrichedComponent(comp);
            Rule rule = new Rule("r1", "Check not banned", "name", Rule.Operator.NEQ, "log4j", Rule.Severity.FAIL);

            RuleResult result = evaluator.evaluate(rule, ec);
            assertThat(result.decision()).isEqualTo(RuleResult.Decision.PASS);
        }

        @Test
        void eqIsCaseInsensitive() {
            Component comp = testComponent("Jackson-Core", "2.15.0");
            EnrichedComponent ec = new EnrichedComponent(comp);
            Rule rule = new Rule("r1", "Match name", "name", Rule.Operator.EQ, "jackson-core", Rule.Severity.WARN);

            RuleResult result = evaluator.evaluate(rule, ec);
            assertThat(result.decision()).isEqualTo(RuleResult.Decision.PASS);
        }
    }

    @Nested
    class NumericOperators {

        @Test
        void gtePassesAboveThreshold() {
            Component comp = testComponent("spring-core", "5.0.0");
            EnrichedComponent ec = enrichedWithScorecard(comp, 7.5);
            Rule rule = new Rule("r1", "Scorecard baseline", "scorecard.score",
                    Rule.Operator.GTE, "4.0", Rule.Severity.WARN);

            RuleResult result = evaluator.evaluate(rule, ec);
            assertThat(result.decision()).isEqualTo(RuleResult.Decision.PASS);
        }

        @Test
        void gteFailsBelowThreshold() {
            Component comp = testComponent("spring-core", "5.0.0");
            EnrichedComponent ec = enrichedWithScorecard(comp, 3.0);
            Rule rule = new Rule("r1", "Scorecard baseline", "scorecard.score",
                    Rule.Operator.GTE, "4.0", Rule.Severity.WARN);

            RuleResult result = evaluator.evaluate(rule, ec);
            assertThat(result.decision()).isEqualTo(RuleResult.Decision.WARN);
        }

        @Test
        void ltPassesBelowThreshold() {
            Component comp = testComponent("spring-core", "5.0.0");
            EnrichedComponent ec = enrichedWithScorecard(comp, 3.0);
            Rule rule = new Rule("r1", "Score low", "scorecard.score",
                    Rule.Operator.LT, "5.0", Rule.Severity.FAIL);

            RuleResult result = evaluator.evaluate(rule, ec);
            assertThat(result.decision()).isEqualTo(RuleResult.Decision.PASS);
        }

        @Test
        void gtPassesAbove() {
            Component comp = testComponent("lib", "1.0.0");
            EnrichedComponent ec = enrichedWithScorecard(comp, 8.0);
            Rule rule = new Rule("r1", "High score", "scorecard.score",
                    Rule.Operator.GT, "7.0", Rule.Severity.FAIL);

            RuleResult result = evaluator.evaluate(rule, ec);
            assertThat(result.decision()).isEqualTo(RuleResult.Decision.PASS);
        }
    }

    @Nested
    class SetOperators {

        @Test
        void inPassesWhenValueInList() {
            Component comp = testComponent("lib", "1.0.0");
            EnrichedComponent ec = new EnrichedComponent(comp);
            Rule rule = new Rule("r1", "Allowed types", "type",
                    Rule.Operator.IN, "library,framework,application", Rule.Severity.FAIL);

            RuleResult result = evaluator.evaluate(rule, ec);
            assertThat(result.decision()).isEqualTo(RuleResult.Decision.PASS);
        }

        @Test
        void inFailsWhenValueNotInList() {
            Component comp = new Component("lib", "1.0.0", "lib-ref", "firmware",
                    Optional.empty(), Optional.empty(), Optional.empty(), "required");
            EnrichedComponent ec = new EnrichedComponent(comp);
            Rule rule = new Rule("r1", "Allowed types", "type",
                    Rule.Operator.IN, "library,framework,application", Rule.Severity.FAIL);

            RuleResult result = evaluator.evaluate(rule, ec);
            assertThat(result.decision()).isEqualTo(RuleResult.Decision.FAIL);
        }

        @Test
        void notInPassesWhenValueNotInList() {
            Component comp = testComponent("lib", "1.0.0");
            EnrichedComponent ec = new EnrichedComponent(comp);
            Rule rule = new Rule("r1", "Not banned", "name",
                    Rule.Operator.NOT_IN, "log4j,commons-logging", Rule.Severity.FAIL);

            RuleResult result = evaluator.evaluate(rule, ec);
            assertThat(result.decision()).isEqualTo(RuleResult.Decision.PASS);
        }
    }

    @Nested
    class ExistsOperator {

        @Test
        void existsPassesWhenFieldPresent() {
            Component comp = testComponent("lib", "1.0.0");
            EnrichedComponent ec = new EnrichedComponent(comp);
            Rule rule = new Rule("r1", "Has purl", "purl",
                    Rule.Operator.EXISTS, null, Rule.Severity.WARN);

            RuleResult result = evaluator.evaluate(rule, ec);
            assertThat(result.decision()).isEqualTo(RuleResult.Decision.PASS);
        }

        @Test
        void existsFailsWhenFieldAbsent() {
            Component comp = new Component("lib", "1.0.0", "lib-ref", "library",
                    Optional.empty(), Optional.empty(), Optional.empty(), "required");
            EnrichedComponent ec = new EnrichedComponent(comp);
            Rule rule = new Rule("r1", "Has purl", "purl",
                    Rule.Operator.EXISTS, null, Rule.Severity.WARN);

            RuleResult result = evaluator.evaluate(rule, ec);
            assertThat(result.decision()).isEqualTo(RuleResult.Decision.WARN);
        }
    }

    @Nested
    class SkipSeverity {

        @Test
        void skipReturnsSkipWhenFieldMatches() {
            Component comp = new Component("lib", "1.0.0", "lib-ref", "library",
                    Optional.empty(), Optional.empty(), Optional.empty(), "optional");
            EnrichedComponent ec = new EnrichedComponent(comp);
            Rule rule = new Rule("r1", "Skip optional", "scope",
                    Rule.Operator.EQ, "optional", Rule.Severity.SKIP);

            RuleResult result = evaluator.evaluate(rule, ec);
            assertThat(result.decision()).isEqualTo(RuleResult.Decision.SKIP);
        }

        @Test
        void skipReturnsPassWhenFieldDoesNotMatch() {
            Component comp = testComponent("lib", "1.0.0");
            EnrichedComponent ec = new EnrichedComponent(comp);
            Rule rule = new Rule("r1", "Skip optional", "scope",
                    Rule.Operator.EQ, "optional", Rule.Severity.SKIP);

            RuleResult result = evaluator.evaluate(rule, ec);
            assertThat(result.decision()).isEqualTo(RuleResult.Decision.PASS);
        }
    }

    @Nested
    class MissingData {

        @Test
        void missingFieldReturnsInfo() {
            Component comp = testComponent("lib", "1.0.0");
            EnrichedComponent ec = new EnrichedComponent(comp); // no scorecard
            Rule rule = new Rule("r1", "Score check", "scorecard.score",
                    Rule.Operator.GTE, "4.0", Rule.Severity.FAIL);

            RuleResult result = evaluator.evaluate(rule, ec);
            assertThat(result.decision()).isEqualTo(RuleResult.Decision.INFO);
            assertThat(result.actualValue()).isEqualTo("(no data)");
        }
    }

    @Nested
    class SeverityMapping {

        @Test
        void failSeverityMapsTooFailDecision() {
            Component comp = testComponent("lib", "1.0.0");
            EnrichedComponent ec = new EnrichedComponent(comp);
            Rule rule = new Rule("r1", "Test", "name", Rule.Operator.EQ, "other", Rule.Severity.FAIL);

            assertThat(evaluator.evaluate(rule, ec).decision()).isEqualTo(RuleResult.Decision.FAIL);
        }

        @Test
        void warnSeverityMapsToWarnDecision() {
            Component comp = testComponent("lib", "1.0.0");
            EnrichedComponent ec = new EnrichedComponent(comp);
            Rule rule = new Rule("r1", "Test", "name", Rule.Operator.EQ, "other", Rule.Severity.WARN);

            assertThat(evaluator.evaluate(rule, ec).decision()).isEqualTo(RuleResult.Decision.WARN);
        }

        @Test
        void infoSeverityMapsToInfoDecision() {
            Component comp = testComponent("lib", "1.0.0");
            EnrichedComponent ec = new EnrichedComponent(comp);
            Rule rule = new Rule("r1", "Test", "name", Rule.Operator.EQ, "other", Rule.Severity.INFO);

            assertThat(evaluator.evaluate(rule, ec).decision()).isEqualTo(RuleResult.Decision.INFO);
        }
    }
}
