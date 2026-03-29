package com.scrutinizer.evidence;

import com.scrutinizer.engine.Finding;
import com.scrutinizer.engine.PostureReport;
import com.scrutinizer.engine.RuleResult;
import com.scrutinizer.enrichment.EnrichedComponent;
import com.scrutinizer.enrichment.EnrichedDependencyGraph;
import com.scrutinizer.model.Component;
import com.scrutinizer.model.DependencyEdge;
import com.scrutinizer.policy.PolicyDefinition;
import com.scrutinizer.policy.Rule;
import com.scrutinizer.policy.ScoringConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceBundleTest {

    @TempDir
    Path tempDir;

    private EnrichedDependencyGraph buildGraph() {
        Component root = new Component("app", "1.0.0", "root-ref");
        Component dep = new Component("lib-a", "2.0.0", "lib-a-ref",
                "library", "com.test", "pkg:maven/com.test/lib-a@2.0.0", null, "required");

        return new EnrichedDependencyGraph(
                List.of(EnrichedComponent.unenriched(root), EnrichedComponent.unenriched(dep)),
                List.of(new DependencyEdge("root-ref", "lib-a-ref")),
                "root-ref"
        );
    }

    @Test
    void inventoryCsvContainsHeaderAndRows() throws IOException {
        EnrichedDependencyGraph graph = buildGraph();
        Path csvPath = tempDir.resolve("inventory.csv");

        InventoryCsvExporter.export(graph,
                Map.of("lib-a-ref", RuleResult.Decision.WARN),
                Map.of(),
                csvPath);

        List<String> lines = Files.readAllLines(csvPath);
        assertThat(lines.get(0)).startsWith("name,version,group");
        assertThat(lines).hasSizeGreaterThan(1);
    }

    @Test
    void evidenceManifestContainsArtifacts() {
        EvidenceManifest manifest = new EvidenceManifest("0.1.0", "1.0",
                List.of(new EvidenceManifest.ArtifactEntry("report.json", "abc123", 1024, "application/json")));

        Map<String, Object> map = manifest.toMap();
        assertThat(map).containsKeys("generatedAt", "engineVersion", "schemaVersion", "artifacts");
        assertThat(manifest.artifacts()).hasSize(1);
        assertThat(manifest.artifacts().get(0).filename()).isEqualTo("report.json");
    }

    @Test
    void evidenceBundleWriterCreatesAllFiles() throws IOException {
        EnrichedDependencyGraph graph = buildGraph();

        Rule rule = new Rule("r1", "test", "name", Rule.Operator.EQ, "other", Rule.Severity.FAIL);
        PolicyDefinition policy = new PolicyDefinition("scrutinizer/v1", "test-policy", "1.0",
                List.of(rule), ScoringConfig.defaultConfig());

        Map<String, List<RuleResult>> results = new LinkedHashMap<>();
        results.put("lib-a-ref", List.of(
                new RuleResult("lib-a-ref", "r1", RuleResult.Decision.FAIL, "lib-a", "other", "test")
        ));

        List<Finding> findings = List.of(
                new Finding("F-0001", "lib-a-ref", "lib-a", "r1",
                        RuleResult.Decision.FAIL, "FAIL", "name", "lib-a", "other",
                        "test", "Fix it", true, 1, List.of("evidence"))
        );

        PostureReport report = PostureReport.create(policy, "hash123", results, findings);

        Path bundleDir = tempDir.resolve("bundle");
        EvidenceBundleWriter.write(report, graph, bundleDir);

        assertThat(bundleDir.resolve("posture-report.json")).exists();
        assertThat(bundleDir.resolve("inventory.csv")).exists();
        assertThat(bundleDir.resolve("evidence-manifest.json")).exists();

        String manifest = Files.readString(bundleDir.resolve("evidence-manifest.json"));
        assertThat(manifest).contains("posture-report.json");
        assertThat(manifest).contains("inventory.csv");
        assertThat(manifest).contains("sha256");
    }
}
