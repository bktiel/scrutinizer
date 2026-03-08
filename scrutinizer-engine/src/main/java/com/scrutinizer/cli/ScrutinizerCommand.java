package com.scrutinizer.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.scrutinizer.engine.PostureEvaluator;
import com.scrutinizer.engine.PostureReport;
import com.scrutinizer.engine.RuleResult;
import com.scrutinizer.enrichment.EnrichedDependencyGraph;
import com.scrutinizer.enrichment.EnrichmentPipeline;
import com.scrutinizer.graph.GraphAnalyzer;
import com.scrutinizer.model.Component;
import com.scrutinizer.model.DependencyGraph;
import com.scrutinizer.parser.SbomParseException;
import com.scrutinizer.parser.SbomParser;
import com.scrutinizer.policy.PolicyDefinition;
import com.scrutinizer.policy.PolicyParseException;
import com.scrutinizer.policy.PolicyParser;
import com.scrutinizer.viz.GraphExportCommand;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * CLI command handler for Scrutinizer.
 * Parses command-line arguments and produces table or JSON output.
 *
 * Usage:
 *   scrutinizer --sbom &lt;path&gt; [--format table|json]
 *   scrutinizer --sbom &lt;path&gt; --policy &lt;path&gt; [--output &lt;path&gt;] [--format table|json]
 */
@Service
public class ScrutinizerCommand implements ExitCodeGenerator {

    private final SbomParser parser;
    private final GraphAnalyzer analyzer;
    private final PolicyParser policyParser;
    private final EnrichmentPipeline enrichmentPipeline;
    private final PostureEvaluator postureEvaluator;
    private final GraphExportCommand graphExport;
    private int exitCode = 0;

    public ScrutinizerCommand(SbomParser parser, GraphAnalyzer analyzer,
                               PolicyParser policyParser,
                               EnrichmentPipeline enrichmentPipeline,
                               PostureEvaluator postureEvaluator,
                               GraphExportCommand graphExport) {
        this.parser = parser;
        this.analyzer = analyzer;
        this.policyParser = policyParser;
        this.enrichmentPipeline = enrichmentPipeline;
        this.postureEvaluator = postureEvaluator;
        this.graphExport = graphExport;
    }

    public void run(String... args) {
        CliArgs cliArgs;
        try {
            cliArgs = parseArgs(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println("Usage: scrutinizer --sbom <path> [--policy <path>] [--output <path>] [--format table|json]");
            exitCode = 1;
            return;
        }

        DependencyGraph graph;
        String sbomJson;
        try {
            sbomJson = Files.readString(cliArgs.sbomPath());
            graph = parser.parseFile(cliArgs.sbomPath());
        } catch (IOException e) {
            System.err.println("Error: File not found: " + cliArgs.sbomPath());
            exitCode = 1;
            return;
        } catch (SbomParseException e) {
            System.err.println("Error: " + e.getMessage());
            exitCode = 1;
            return;
        }

        // Viz mode: export graph visualization
        if (cliArgs.vizFormat() != null) {
            if (cliArgs.outputPath() == null) {
                System.err.println("Error: --output is required when using --viz");
                exitCode = 1;
                return;
            }
            try {
                graphExport.export(cliArgs.sbomPath(), cliArgs.vizFormat(),
                        cliArgs.outputPath(), null);
            } catch (IOException e) {
                System.err.println("Error exporting graph: " + e.getMessage());
                exitCode = 1;
            }
            return;
        }

        // If no policy specified, just show inventory
        if (cliArgs.policyPath() == null) {
            if ("json".equals(cliArgs.format())) {
                printJson(graph);
            } else {
                printTable(graph);
            }
            return;
        }

        // Policy evaluation mode
        PolicyDefinition policy;
        try (FileInputStream fis = new FileInputStream(cliArgs.policyPath().toFile())) {
            policy = policyParser.parse(fis);
        } catch (IOException e) {
            System.err.println("Error: Policy file not found: " + cliArgs.policyPath());
            exitCode = 1;
            return;
        } catch (PolicyParseException e) {
            System.err.println("Error: Invalid policy: " + e.getMessage());
            exitCode = 1;
            return;
        }

        // Enrich the graph
        EnrichedDependencyGraph enrichedGraph = enrichmentPipeline.enrich(graph);

        // Evaluate
        PostureReport report = postureEvaluator.evaluate(enrichedGraph, policy, sbomJson);

        // Output
        if ("json".equals(cliArgs.format())) {
            printReportJson(report, cliArgs.outputPath());
        } else {
            printReportTable(report);
        }

        // Set exit code based on overall decision
        if (report.overallDecision() == RuleResult.Decision.FAIL) {
            exitCode = 2;
        } else if (report.overallDecision() == RuleResult.Decision.WARN) {
            exitCode = 0; // Warnings don't fail the gate
        }
    }

    private void printTable(DependencyGraph graph) {
        Map<String, Integer> stats = analyzer.summary(graph);
        System.out.println("SBOM Inventory: " + stats.get("total_components")
                + " components, " + stats.get("total_edges") + " dependency edges");
        System.out.println("-".repeat(72));
        System.out.printf("%-35s %-15s %-12s %s%n", "Name", "Version", "Type", "Scope");
        System.out.println("-".repeat(72));

        for (Component comp : graph.components()) {
            System.out.printf("%-35s %-15s %-12s %s%n",
                    comp.displayName(), comp.version(), comp.type(), comp.scope());
        }
        System.out.println("-".repeat(72));

        List<Component> roots = analyzer.findRootComponents(graph);
        if (!roots.isEmpty()) {
            StringJoiner joiner = new StringJoiner(", ");
            roots.forEach(r -> joiner.add(r.name()));
            System.out.println("\nRoot components: " + joiner);
        }
    }

    private void printJson(DependencyGraph graph) {
        Map<String, Object> output = new LinkedHashMap<>();

        List<Map<String, Object>> components = new ArrayList<>();
        for (Component c : graph.components()) {
            Map<String, Object> comp = new LinkedHashMap<>();
            comp.put("name", c.name());
            comp.put("version", c.version());
            comp.put("type", c.type());
            comp.put("group", c.group().orElse(null));
            comp.put("purl", c.purl().orElse(null));
            comp.put("scope", c.scope());
            comp.put("bom_ref", c.bomRef());
            components.add(comp);
        }
        output.put("components", components);

        List<Map<String, String>> edges = new ArrayList<>();
        for (var e : graph.edges()) {
            edges.add(Map.of("source", e.sourceRef(), "target", e.targetRef()));
        }
        output.put("edges", edges);
        output.put("summary", analyzer.summary(graph));

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            System.out.println(mapper.writeValueAsString(output));
        } catch (Exception e) {
            System.err.println("Error serializing JSON: " + e.getMessage());
            exitCode = 1;
        }
    }

    private void printReportTable(PostureReport report) {
        System.out.println("Posture Report: " + report.policyName() + " v" + report.policyVersion());
        System.out.println("Overall Decision: " + report.overallDecision());
        System.out.printf("Posture Score: %.1f / 10.0%n", report.postureScore());
        System.out.println("-".repeat(72));

        PostureReport.Summary s = report.summary();
        System.out.printf("Rules evaluated: %d  |  PASS: %d  WARN: %d  FAIL: %d  INFO: %d  SKIP: %d%n",
                s.total(), s.pass(), s.warn(), s.fail(), s.info(), s.skip());
        System.out.println("-".repeat(72));

        // Show only non-PASS results for conciseness
        for (PostureReport.ComponentReport cr : report.componentReports()) {
            List<RuleResult> issues = cr.ruleResults().stream()
                    .filter(r -> r.decision() != RuleResult.Decision.PASS)
                    .toList();
            if (!issues.isEmpty()) {
                System.out.println("\n  " + cr.componentRef() + ":");
                for (RuleResult rr : issues) {
                    System.out.printf("    [%s] %s — %s (actual=%s, expected=%s)%n",
                            rr.decision(), rr.ruleId(), rr.description(),
                            rr.actualValue(), rr.expectedValue());
                }
            }
        }
        System.out.println("-".repeat(72));
    }

    private void printReportJson(PostureReport report, Path outputPath) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            String json = mapper.writeValueAsString(report.toMap());

            if (outputPath != null) {
                Files.writeString(outputPath, json);
                System.out.println("Report written to: " + outputPath);
            } else {
                System.out.println(json);
            }
        } catch (Exception e) {
            System.err.println("Error serializing report: " + e.getMessage());
            exitCode = 1;
        }
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    // Simple argument parsing (no external dependency)
    record CliArgs(Path sbomPath, Path policyPath, Path outputPath, String format, String vizFormat) {}

    static CliArgs parseArgs(String[] args) {
        Path sbomPath = null;
        Path policyPath = null;
        Path outputPath = null;
        String format = "table";
        String vizFormat = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--sbom" -> {
                    if (i + 1 >= args.length) throw new IllegalArgumentException("--sbom requires a path argument");
                    sbomPath = Path.of(args[++i]);
                }
                case "--policy" -> {
                    if (i + 1 >= args.length) throw new IllegalArgumentException("--policy requires a path argument");
                    policyPath = Path.of(args[++i]);
                }
                case "--output" -> {
                    if (i + 1 >= args.length) throw new IllegalArgumentException("--output requires a path argument");
                    outputPath = Path.of(args[++i]);
                }
                case "--format" -> {
                    if (i + 1 >= args.length) throw new IllegalArgumentException("--format requires an argument");
                    format = args[++i];
                    if (!"table".equals(format) && !"json".equals(format)) {
                        throw new IllegalArgumentException("--format must be 'table' or 'json', got '" + format + "'");
                    }
                }
                case "--viz" -> {
                    if (i + 1 >= args.length) throw new IllegalArgumentException("--viz requires an argument (dot|html)");
                    vizFormat = args[++i];
                    if (!"dot".equals(vizFormat) && !"html".equals(vizFormat)) {
                        throw new IllegalArgumentException("--viz must be 'dot' or 'html', got '" + vizFormat + "'");
                    }
                }
                default -> {
                    // Skip Spring Boot args (e.g., --spring.*)
                    if (!args[i].startsWith("--spring") && !args[i].startsWith("--debug")) {
                        throw new IllegalArgumentException("Unknown argument: " + args[i]);
                    }
                }
            }
        }

        if (sbomPath == null) {
            throw new IllegalArgumentException("--sbom is required");
        }

        return new CliArgs(sbomPath, policyPath, outputPath, format, vizFormat);
    }
}
