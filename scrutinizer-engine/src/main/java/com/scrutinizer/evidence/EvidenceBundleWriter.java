package com.scrutinizer.evidence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.scrutinizer.engine.Finding;
import com.scrutinizer.engine.PostureReport;
import com.scrutinizer.engine.RuleResult;
import com.scrutinizer.enrichment.EnrichedDependencyGraph;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

public final class EvidenceBundleWriter {

    private static final String ENGINE_VERSION = "0.1.0";
    private static final String SCHEMA_VERSION = "1.0";

    private EvidenceBundleWriter() {}

    public static void write(PostureReport report,
                              EnrichedDependencyGraph graph,
                              Path outputDir) throws IOException {
        Files.createDirectories(outputDir);

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        Path reportPath = outputDir.resolve("posture-report.json");
        String reportJson = mapper.writeValueAsString(report.toMap());
        Files.writeString(reportPath, reportJson);

        Path csvPath = outputDir.resolve("inventory.csv");
        Map<String, RuleResult.Decision> componentDecisions = buildComponentDecisions(report);
        Map<String, List<Finding>> findingsByComponent = report.findings().stream()
                .collect(Collectors.groupingBy(Finding::componentRef));
        InventoryCsvExporter.export(graph, componentDecisions, findingsByComponent, csvPath);

        List<EvidenceManifest.ArtifactEntry> entries = new ArrayList<>();
        entries.add(createArtifactEntry(reportPath, "application/json"));
        entries.add(createArtifactEntry(csvPath, "text/csv"));

        EvidenceManifest manifest = new EvidenceManifest(ENGINE_VERSION, SCHEMA_VERSION, entries);
        Path manifestPath = outputDir.resolve("evidence-manifest.json");
        String manifestJson = mapper.writeValueAsString(manifest.toMap());
        Files.writeString(manifestPath, manifestJson);
    }

    private static Map<String, RuleResult.Decision> buildComponentDecisions(PostureReport report) {
        Map<String, RuleResult.Decision> decisions = new LinkedHashMap<>();
        for (PostureReport.ComponentReport cr : report.componentReports()) {
            RuleResult.Decision worst = RuleResult.Decision.PASS;
            for (RuleResult rr : cr.ruleResults()) {
                if (rr.decision() == RuleResult.Decision.FAIL) {
                    worst = RuleResult.Decision.FAIL;
                    break;
                } else if (rr.decision() == RuleResult.Decision.WARN
                        && worst != RuleResult.Decision.FAIL) {
                    worst = RuleResult.Decision.WARN;
                }
            }
            decisions.put(cr.componentRef(), worst);
        }
        return decisions;
    }

    private static EvidenceManifest.ArtifactEntry createArtifactEntry(Path file, String contentType) throws IOException {
        byte[] content = Files.readAllBytes(file);
        String sha256 = computeSha256(content);
        return new EvidenceManifest.ArtifactEntry(
                file.getFileName().toString(), sha256, content.length, contentType);
    }

    private static String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return "sha256-unavailable";
        }
    }
}
