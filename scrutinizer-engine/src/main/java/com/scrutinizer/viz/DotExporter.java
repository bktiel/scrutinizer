package com.scrutinizer.viz;

import com.scrutinizer.model.Component;
import com.scrutinizer.model.DependencyEdge;
import com.scrutinizer.model.DependencyGraph;

import java.util.*;

/**
 * Exports a DependencyGraph to Graphviz DOT format.
 * Produces a directed graph with node labels and optional grouping by type/scope.
 */
public final class DotExporter {

    private DotExporter() {}

    /**
     * Export the graph to DOT format with default styling.
     */
    public static String export(DependencyGraph graph) {
        return export(graph, DotOptions.defaults());
    }

    /**
     * Export the graph to DOT format with custom options.
     */
    public static String export(DependencyGraph graph, DotOptions options) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph dependencies {\n");
        sb.append("    rankdir=LR;\n");
        sb.append("    node [shape=box, style=filled, fontname=\"Helvetica\", fontsize=10];\n");
        sb.append("    edge [color=\"#666666\", arrowsize=0.7];\n");
        sb.append("    bgcolor=\"transparent\";\n");
        sb.append("\n");

        // Map bomRef -> component for quick lookup
        Map<String, Component> componentMap = new LinkedHashMap<>();
        for (Component c : graph.components()) {
            componentMap.put(c.bomRef(), c);
        }

        // Find root components for special styling
        Set<String> rootRefs = new HashSet<>();
        Set<String> depTargets = new HashSet<>();
        for (DependencyEdge edge : graph.edges()) {
            depTargets.add(edge.targetRef());
        }
        for (Component c : graph.components()) {
            if (!depTargets.contains(c.bomRef())) {
                rootRefs.add(c.bomRef());
            }
        }

        // Node definitions
        for (Component c : graph.components()) {
            String nodeId = sanitizeId(c.bomRef());
            String label = c.displayName() + "\\n" + c.version();
            String color = getNodeColor(c, rootRefs.contains(c.bomRef()), options);
            sb.append("    ").append(nodeId)
                    .append(" [label=\"").append(label)
                    .append("\", fillcolor=\"").append(color)
                    .append("\"];\n");
        }

        sb.append("\n");

        // Edge definitions
        for (DependencyEdge edge : graph.edges()) {
            String from = sanitizeId(edge.sourceRef());
            String to = sanitizeId(edge.targetRef());
            sb.append("    ").append(from).append(" -> ").append(to).append(";\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    private static String getNodeColor(Component c, boolean isRoot, DotOptions options) {
        if (isRoot) return options.rootColor();

        return switch (c.scope()) {
            case "required" -> options.requiredColor();
            case "optional" -> options.optionalColor();
            case "excluded" -> options.excludedColor();
            default -> options.defaultColor();
        };
    }

    /**
     * Sanitize a bomRef into a valid DOT node identifier.
     */
    static String sanitizeId(String bomRef) {
        return "\"" + bomRef.replace("\"", "\\\"") + "\"";
    }

    /**
     * Configuration options for DOT export styling.
     */
    public record DotOptions(
            String rootColor,
            String requiredColor,
            String optionalColor,
            String excludedColor,
            String defaultColor
    ) {
        public static DotOptions defaults() {
            return new DotOptions("#4A90D9", "#A8D08D", "#F4D03F", "#E74C3C", "#BDC3C7");
        }
    }
}
