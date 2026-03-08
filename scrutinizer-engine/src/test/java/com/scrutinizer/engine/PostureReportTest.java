package com.scrutinizer.engine;

import com.scrutinizer.policy.PolicyDefinition;
import com.scrutinizer.policy.Rule;
import com.scrutinizer.policy.ScoringConfig;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PostureReportTest {

    @Test
    void createFromResults() {
        PolicyDefinition policy = new PolicyDefinition("scrutinizer/v1", "test-policy", "1.0",
                List.of(new Rule("r1", "Test", "name", Rule.Operator.EQ, "lib", Rule.Severity.FAIL)),
                ScoringConfig.defaultConfig());

        Map<String, List<RuleResult>> results = new LinkedHashMap<>();
        results.put("comp-a", List.of(
                new RuleResult("comp-a", "r1", RuleResult.Decision.PASS, "lib", "lib", "Test")));
        results.put("comp-b", List.of(
                new RuleResult("comp-b", "r1", RuleResult.Decision.FAIL, "other", "lib", "Test")));

        PostureReport report = PostureReport.create(policy, "abc123", results);

        assertThat(report.policyName()).isEqualTo("test-policy");
        assertThat(report.policyVersion()).isEqualTo("1.0");
        assertThat(report.sbomHash()).isEqualTo("abc123");
        assertThat(report.componentReports()).hasSize(2);
        assertThat(report.summary().pass()).isEqualTo(1);
        assertThat(report.summary().fail()).isEqualTo(1);
        assertThat(report.summary().total()).isEqualTo(2);
    }

    @Test
    void toMapProducesSerializableStructure() {
        PostureReport report = new PostureReport.Builder()
                .policyName("test")
                .policyVersion("1.0")
                .sbomHash("hash123")
                .overallDecision(RuleResult.Decision.WARN)
                .postureScore(5.0)
                .summary(new PostureReport.Summary(1, 1, 0, 0, 0, 2))
                .componentReports(List.of(
                        new PostureReport.ComponentReport("comp-a", List.of(
                                new RuleResult("comp-a", "r1", RuleResult.Decision.PASS, "v", "v", "desc")))))
                .build();

        Map<String, Object> map = report.toMap();
        assertThat(map).containsKey("timestamp");
        assertThat(map).containsEntry("overallDecision", "WARN");
        assertThat(map).containsEntry("sbomHash", "hash123");
        assertThat(((Map<?, ?>) map.get("policy")).get("name")).isEqualTo("test");
        assertThat(((List<?>) map.get("components"))).hasSize(1);
    }

    @Test
    void componentReportsAreSortedDeterministically() {
        PolicyDefinition policy = new PolicyDefinition("scrutinizer/v1", "test", "1.0",
                List.of(new Rule("r1", "Test", "name", Rule.Operator.EQ, "x", Rule.Severity.WARN)),
                ScoringConfig.defaultConfig());

        // Insert in reverse order
        Map<String, List<RuleResult>> results = new LinkedHashMap<>();
        results.put("z-comp", List.of(
                new RuleResult("z-comp", "r1", RuleResult.Decision.WARN, "a", "x", "Test")));
        results.put("a-comp", List.of(
                new RuleResult("a-comp", "r1", RuleResult.Decision.PASS, "x", "x", "Test")));

        PostureReport report = PostureReport.create(policy, "hash", results);

        // Should be sorted alphabetically
        assertThat(report.componentReports().get(0).componentRef()).isEqualTo("a-comp");
        assertThat(report.componentReports().get(1).componentRef()).isEqualTo("z-comp");
    }
}
