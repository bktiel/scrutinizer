package com.scrutinizer.evidence;

import com.scrutinizer.enrichment.EnrichedComponent;
import com.scrutinizer.enrichment.EnrichedDependencyGraph;
import com.scrutinizer.enrichment.PurlResolver;
import com.scrutinizer.engine.Finding;
import com.scrutinizer.engine.RuleResult;
import com.scrutinizer.model.DependencyEdge;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public final class InventoryCsvExporter {

    private static final String HEADER = "name,version,group,purl,type,scope,ecosystem,isDirect,scorecardScore,provenancePresent,provenanceLevel,decision,failingRuleIds";

    private InventoryCsvExporter() {}

    public static void export(EnrichedDependencyGraph graph,
                               Map<String, RuleResult.Decision> componentDecisions,
                               Map<String, List<Finding>> findingsByComponent,
                               Path outputPath) throws IOException {
        Set<String> directRefs = computeDirectRefs(graph);

        List<EnrichedComponent> sorted = new ArrayList<>(graph.components());
        sorted.sort(Comparator.comparing(ec -> ec.component().name() + ":" + ec.component().version()));

        try (Writer writer = Files.newBufferedWriter(outputPath)) {
            writer.write(HEADER);
            writer.write('\n');

            for (EnrichedComponent ec : sorted) {
                String ref = ec.component().bomRef();
                boolean isDirect = directRefs.contains(ref);

                String ecosystem = ec.component().purl()
                        .flatMap(PurlResolver::extractEcosystem).orElse("");

                String scorecardScore = ec.scorecardResult()
                        .map(sr -> String.valueOf(sr.overallScore())).orElse("");

                String provenancePresent = ec.provenanceResult()
                        .map(pr -> String.valueOf(pr.isPresent())).orElse("");

                String provenanceLevel = ec.provenanceResult()
                        .flatMap(pr -> pr.level())
                        .map(Enum::name).orElse("");

                RuleResult.Decision decision = componentDecisions.getOrDefault(ref, RuleResult.Decision.PASS);

                List<Finding> findings = findingsByComponent.getOrDefault(ref, List.of());
                String failingRuleIds = findings.stream()
                        .filter(f -> f.decision() == RuleResult.Decision.FAIL)
                        .map(Finding::ruleId)
                        .collect(Collectors.joining(";"));

                writer.write(String.join(",",
                        escapeCsv(ec.component().name()),
                        escapeCsv(ec.component().version()),
                        escapeCsv(ec.component().group().orElse("")),
                        escapeCsv(ec.component().purl().orElse("")),
                        escapeCsv(ec.component().type()),
                        escapeCsv(ec.component().scope()),
                        escapeCsv(ecosystem),
                        String.valueOf(isDirect),
                        scorecardScore,
                        provenancePresent,
                        provenanceLevel,
                        decision.name(),
                        escapeCsv(failingRuleIds)
                ));
                writer.write('\n');
            }
        }
    }

    private static Set<String> computeDirectRefs(EnrichedDependencyGraph graph) {
        Optional<String> rootRef = graph.rootRef();
        if (rootRef.isEmpty()) return Set.of();
        return graph.edges().stream()
                .filter(e -> e.sourceRef().equals(rootRef.get()))
                .map(DependencyEdge::targetRef)
                .collect(Collectors.toSet());
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
