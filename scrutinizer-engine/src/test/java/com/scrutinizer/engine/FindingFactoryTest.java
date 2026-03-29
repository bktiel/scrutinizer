package com.scrutinizer.engine;

import com.scrutinizer.enrichment.EnrichedComponent;
import com.scrutinizer.enrichment.EnrichedDependencyGraph;
import com.scrutinizer.enrichment.ScorecardResult;
import com.scrutinizer.model.Component;
import com.scrutinizer.model.DependencyEdge;
import com.scrutinizer.policy.Rule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FindingFactoryTest {

    private EnrichedDependencyGraph buildTestGraph() {
        Component root = new Component("my-app", "1.0.0", "root-ref");
        Component directDep = new Component("express", "4.18.0", "express-ref",
                "library", null, "pkg:npm/express@4.18.0", null, "required");
        Component transitiveDep = new Component("qs", "6.11.0", "qs-ref",
                "library", null, "pkg:npm/qs@6.11.0", null, "required");

        ScorecardResult lowScore = new ScorecardResult(2.0, Map.of(), "https://github.com/test/qs", Instant.now());

        EnrichedComponent enrichedRoot = EnrichedComponent.unenriched(root);
        EnrichedComponent enrichedDirect = EnrichedComponent.unenriched(directDep);
        EnrichedComponent enrichedTransitive = EnrichedComponent.unenriched(transitiveDep).withScorecard(lowScore);

        return new EnrichedDependencyGraph(
                List.of(enrichedRoot, enrichedDirect, enrichedTransitive),
                List.of(
                        new DependencyEdge("root-ref", "express-ref"),
                        new DependencyEdge("express-ref", "qs-ref")
                ),
                "root-ref"
        );
    }

    @Test
    void createsFindingsForNonPassResults() {
        EnrichedDependencyGraph graph = buildTestGraph();

        Rule scorecardRule = new Rule("sc-check", "Scorecard >= 5.0",
                "scorecard.score", Rule.Operator.GTE, "5.0", Rule.Severity.FAIL);

        Map<String, List<RuleResult>> results = new LinkedHashMap<>();
        results.put("express-ref", List.of(
                new RuleResult("express-ref", "sc-check", RuleResult.Decision.INFO,
                        "(no data)", "5.0", "Scorecard >= 5.0")
        ));
        results.put("qs-ref", List.of(
                new RuleResult("qs-ref", "sc-check", RuleResult.Decision.FAIL,
                        "2.0", "5.0", "Scorecard >= 5.0")
        ));

        List<Finding> findings = FindingFactory.createFindings(results, List.of(scorecardRule), graph);

        assertThat(findings).hasSize(2);

        Finding qsFinding = findings.stream()
                .filter(f -> f.componentRef().equals("qs-ref"))
                .findFirst()
                .orElseThrow();

        assertThat(qsFinding.decision()).isEqualTo(RuleResult.Decision.FAIL);
        assertThat(qsFinding.isDirect()).isFalse();
        assertThat(qsFinding.depthFromRoot()).isEqualTo(2);
        assertThat(qsFinding.remediation()).contains("OpenSSF Scorecard");
        assertThat(qsFinding.evidenceChain()).isNotEmpty();
    }

    @Test
    void skipsPassAndSkipResults() {
        EnrichedDependencyGraph graph = buildTestGraph();

        Rule nameRule = new Rule("name-check", "Check name",
                "name", Rule.Operator.EQ, "express", Rule.Severity.FAIL);

        Map<String, List<RuleResult>> results = new LinkedHashMap<>();
        results.put("express-ref", List.of(
                new RuleResult("express-ref", "name-check", RuleResult.Decision.PASS,
                        "express", "express", "Check name")
        ));
        results.put("qs-ref", List.of(
                new RuleResult("qs-ref", "name-check", RuleResult.Decision.SKIP,
                        "qs", "express", "Check name")
        ));

        List<Finding> findings = FindingFactory.createFindings(results, List.of(nameRule), graph);

        assertThat(findings).isEmpty();
    }

    @Test
    void directDependencyMarkedCorrectly() {
        EnrichedDependencyGraph graph = buildTestGraph();

        Rule rule = new Rule("sc-check", "Scorecard >= 5.0",
                "scorecard.score", Rule.Operator.GTE, "5.0", Rule.Severity.WARN);

        Map<String, List<RuleResult>> results = new LinkedHashMap<>();
        results.put("express-ref", List.of(
                new RuleResult("express-ref", "sc-check", RuleResult.Decision.INFO,
                        "(no data)", "5.0", "Scorecard >= 5.0")
        ));

        List<Finding> findings = FindingFactory.createFindings(results, List.of(rule), graph);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).isDirect()).isTrue();
        assertThat(findings.get(0).depthFromRoot()).isEqualTo(1);
    }

    @Test
    void findingIdsAreSequential() {
        EnrichedDependencyGraph graph = buildTestGraph();

        Rule rule = new Rule("sc-check", "Scorecard check",
                "scorecard.score", Rule.Operator.GTE, "5.0", Rule.Severity.FAIL);

        Map<String, List<RuleResult>> results = new LinkedHashMap<>();
        results.put("express-ref", List.of(
                new RuleResult("express-ref", "sc-check", RuleResult.Decision.FAIL,
                        "3.0", "5.0", "")
        ));
        results.put("qs-ref", List.of(
                new RuleResult("qs-ref", "sc-check", RuleResult.Decision.FAIL,
                        "2.0", "5.0", "")
        ));

        List<Finding> findings = FindingFactory.createFindings(results, List.of(rule), graph);

        assertThat(findings.get(0).id()).isEqualTo("F-0001");
        assertThat(findings.get(1).id()).isEqualTo("F-0002");
    }

    @Test
    void findingToMapContainsAllFields() {
        Finding finding = new Finding("F-0001", "ref-1", "express", "sc-check",
                RuleResult.Decision.FAIL, "FAIL", "scorecard.score",
                "2.0", "5.0", "Low score", "Upgrade", true, 1,
                List.of("evidence line 1"));

        Map<String, Object> map = finding.toMap();

        assertThat(map).containsKeys("id", "componentRef", "componentName", "ruleId",
                "decision", "severity", "field", "actualValue", "expectedValue",
                "description", "remediation", "isDirect", "depthFromRoot", "evidenceChain");
        assertThat(map.get("isDirect")).isEqualTo(true);
    }
}
