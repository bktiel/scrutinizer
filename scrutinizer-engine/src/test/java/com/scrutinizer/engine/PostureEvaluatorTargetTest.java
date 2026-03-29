package com.scrutinizer.engine;

import com.scrutinizer.enrichment.EnrichedComponent;
import com.scrutinizer.enrichment.EnrichedDependencyGraph;
import com.scrutinizer.enrichment.ScorecardResult;
import com.scrutinizer.model.Component;
import com.scrutinizer.model.DependencyEdge;
import com.scrutinizer.policy.PolicyDefinition;
import com.scrutinizer.policy.Rule;
import com.scrutinizer.policy.ScoringConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PostureEvaluatorTargetTest {

    private PostureEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new PostureEvaluator(new RuleEvaluator());
    }

    private EnrichedDependencyGraph buildGraphWithDirectAndTransitive() {
        Component root = new Component("my-app", "1.0.0", "root-ref");
        Component directDep = new Component("express", "4.18.0", "express-ref",
                "library", null, "pkg:npm/express@4.18.0", null, "required");
        Component transitiveDep = new Component("qs", "6.11.0", "qs-ref",
                "library", null, "pkg:npm/qs@6.11.0", null, "required");

        ScorecardResult highScore = new ScorecardResult(8.0, Map.of(), "https://github.com/test/express", Instant.now());
        ScorecardResult lowScore = new ScorecardResult(2.0, Map.of(), "https://github.com/test/qs", Instant.now());

        EnrichedComponent enrichedRoot = EnrichedComponent.unenriched(root);
        EnrichedComponent enrichedDirect = EnrichedComponent.unenriched(directDep).withScorecard(highScore);
        EnrichedComponent enrichedTransitive = EnrichedComponent.unenriched(transitiveDep).withScorecard(lowScore);

        List<DependencyEdge> edges = List.of(
                new DependencyEdge("root-ref", "express-ref"),
                new DependencyEdge("express-ref", "qs-ref")
        );

        return new EnrichedDependencyGraph(
                List.of(enrichedRoot, enrichedDirect, enrichedTransitive),
                edges,
                "root-ref"
        );
    }

    @Test
    void directTargetRuleOnlyEvaluatesDirectDependencies() {
        EnrichedDependencyGraph graph = buildGraphWithDirectAndTransitive();

        Rule directOnly = new Rule("direct-check", "Direct scorecard >= 6.0",
                "scorecard.score", Rule.Operator.GTE, "6.0", Rule.Severity.FAIL, Rule.Target.DIRECT);

        PolicyDefinition policy = new PolicyDefinition("scrutinizer/v1", "test", "1.0",
                List.of(directOnly), ScoringConfig.defaultConfig());

        PostureReport report = evaluator.evaluate(graph, policy, "{}");

        for (PostureReport.ComponentReport cr : report.componentReports()) {
            for (RuleResult rr : cr.ruleResults()) {
                if (cr.componentRef().equals("express-ref")) {
                    assertThat(rr.decision()).isEqualTo(RuleResult.Decision.PASS);
                }
                if (cr.componentRef().equals("qs-ref")) {
                    assertThat(rr.actualValue()).isEqualTo("(skipped-target)");
                    assertThat(rr.decision()).isEqualTo(RuleResult.Decision.PASS);
                }
            }
        }
    }

    @Test
    void transitiveTargetRuleOnlyEvaluatesTransitiveDependencies() {
        EnrichedDependencyGraph graph = buildGraphWithDirectAndTransitive();

        Rule transitiveOnly = new Rule("transitive-check", "Transitive scorecard >= 5.0",
                "scorecard.score", Rule.Operator.GTE, "5.0", Rule.Severity.WARN, Rule.Target.TRANSITIVE);

        PolicyDefinition policy = new PolicyDefinition("scrutinizer/v1", "test", "1.0",
                List.of(transitiveOnly), ScoringConfig.defaultConfig());

        PostureReport report = evaluator.evaluate(graph, policy, "{}");

        for (PostureReport.ComponentReport cr : report.componentReports()) {
            for (RuleResult rr : cr.ruleResults()) {
                if (cr.componentRef().equals("express-ref")) {
                    assertThat(rr.actualValue()).isEqualTo("(skipped-target)");
                    assertThat(rr.decision()).isEqualTo(RuleResult.Decision.PASS);
                }
                if (cr.componentRef().equals("qs-ref")) {
                    assertThat(rr.decision()).isEqualTo(RuleResult.Decision.WARN);
                }
            }
        }
    }

    @Test
    void allTargetRuleEvaluatesEveryComponent() {
        EnrichedDependencyGraph graph = buildGraphWithDirectAndTransitive();

        Rule allRule = new Rule("all-check", "All scorecard >= 5.0",
                "scorecard.score", Rule.Operator.GTE, "5.0", Rule.Severity.FAIL, Rule.Target.ALL);

        PolicyDefinition policy = new PolicyDefinition("scrutinizer/v1", "test", "1.0",
                List.of(allRule), ScoringConfig.defaultConfig());

        PostureReport report = evaluator.evaluate(graph, policy, "{}");

        long skippedTargetCount = report.componentReports().stream()
                .flatMap(cr -> cr.ruleResults().stream())
                .filter(rr -> "(skipped-target)".equals(rr.actualValue()))
                .count();

        assertThat(skippedTargetCount).isZero();
    }

    @Test
    void defaultTargetIsAll() {
        Rule rule = new Rule("r1", "desc", "name", Rule.Operator.EQ, "test", Rule.Severity.FAIL);
        assertThat(rule.target()).isEqualTo(Rule.Target.ALL);
    }
}
