package com.scrutinizer.enrichment;

import com.scrutinizer.model.DependencyEdge;
import com.scrutinizer.model.DependencyGraph;

import java.util.*;
import java.util.stream.Collectors;

/**
 * An enriched dependency graph where each component carries supply-chain
 * signal metadata (Scorecard scores, SLSA provenance). Preserves the
 * same deterministic, immutable guarantees as DependencyGraph.
 */
public final class EnrichedDependencyGraph {

    private final List<EnrichedComponent> components;
    private final List<DependencyEdge> edges;
    private final String rootRef;

    public EnrichedDependencyGraph(List<EnrichedComponent> components,
                                    List<DependencyEdge> edges,
                                    String rootRef) {
        var sorted = new ArrayList<>(components);
        Collections.sort(sorted);
        this.components = Collections.unmodifiableList(sorted);

        var sortedEdges = new ArrayList<>(edges);
        Collections.sort(sortedEdges);
        this.edges = Collections.unmodifiableList(sortedEdges);

        this.rootRef = rootRef;
    }

    /** Create from a base graph with no enrichment data. */
    public static EnrichedDependencyGraph fromGraph(DependencyGraph graph) {
        List<EnrichedComponent> enriched = graph.components().stream()
                .map(EnrichedComponent::unenriched)
                .collect(Collectors.toList());
        return new EnrichedDependencyGraph(enriched, graph.edges(),
                graph.rootRef().orElse(null));
    }

    public List<EnrichedComponent> components() { return components; }
    public List<DependencyEdge> edges() { return edges; }
    public Optional<String> rootRef() { return Optional.ofNullable(rootRef); }
    public int componentCount() { return components.size(); }
    public int edgeCount() { return edges.size(); }

    public Optional<EnrichedComponent> getComponentByRef(String bomRef) {
        return components.stream()
                .filter(ec -> ec.component().bomRef().equals(bomRef))
                .findFirst();
    }

    /** Count how many components have scorecard data. */
    public long scorecardCoverage() {
        return components.stream()
                .filter(ec -> ec.scorecardResult().isPresent())
                .count();
    }

    /** Count how many components have provenance data. */
    public long provenanceCoverage() {
        return components.stream()
                .filter(ec -> ec.provenanceResult().isPresent()
                        && ec.provenanceResult().get().isPresent())
                .count();
    }

    /** Summary statistics including enrichment coverage. */
    public Map<String, Object> summary() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_components", componentCount());
        stats.put("total_edges", edgeCount());
        stats.put("scorecard_coverage", scorecardCoverage());
        stats.put("provenance_coverage", provenanceCoverage());
        return Collections.unmodifiableMap(stats);
    }
}
