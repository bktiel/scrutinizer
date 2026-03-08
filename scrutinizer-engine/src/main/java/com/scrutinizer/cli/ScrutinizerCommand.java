package com.scrutinizer.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.scrutinizer.graph.GraphAnalyzer;
import com.scrutinizer.model.Component;
import com.scrutinizer.model.DependencyGraph;
import com.scrutinizer.parser.SbomParseException;
import com.scrutinizer.parser.SbomParser;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * CLI command handler for Scrutinizer.
 * Parses command-line arguments and produces table or JSON output.
 */
@Service
public class ScrutinizerCommand implements ExitCodeGenerator {

    private final SbomParser parser;
    private final GraphAnalyzer analyzer;
    private int exitCode = 0;

    public ScrutinizerCommand(SbomParser parser, GraphAnalyzer analyzer) {
        this.parser = parser;
        this.analyzer = analyzer;
    }

    public void run(String... args) {
        CliArgs cliArgs;
        try {
            cliArgs = parseArgs(args);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println("Usage: scrutinizer --sbom <path> [--format table|json]");
            exitCode = 1;
            return;
        }

        DependencyGraph graph;
        try {
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

        if ("json".equals(cliArgs.format())) {
            printJson(graph);
        } else {
            printTable(graph);
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

    @Override
    public int getExitCode() {
        return exitCode;
    }

    // Simple argument parsing (no external dependency)
    record CliArgs(Path sbomPath, String format) {}

    static CliArgs parseArgs(String[] args) {
        Path sbomPath = null;
        String format = "table";

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--sbom" -> {
                    if (i + 1 >= args.length) throw new IllegalArgumentException("--sbom requires a path argument");
                    sbomPath = Path.of(args[++i]);
                }
                case "--format" -> {
                    if (i + 1 >= args.length) throw new IllegalArgumentException("--format requires an argument");
                    format = args[++i];
                    if (!"table".equals(format) && !"json".equals(format)) {
                        throw new IllegalArgumentException("--format must be 'table' or 'json', got '" + format + "'");
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

        return new CliArgs(sbomPath, format);
    }
}
