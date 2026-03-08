package com.scrutinizer.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A normalized, deterministic dependency graph.
 * Components and edges are stored as unmodifiable sorted lists
 * to enforce immutability and guarantee deterministic iteration order.
 */
public final class DependencyGraph {

    private final List<Component> components;
    private final List<DependencyEdge> edges;
    private final String rootRef; // nullable

    public DependencyGraph(List<Component> components, List<DependencyEdge> edges, String rootRef) {
        // Defensive sort + unmodifiable copy for determinism and immutability
        var sortedComponents = new ArrayList<>(components);
        Collections.sort(sortedComponents);
        this.components = Collections.unmodifiableList(sortedComponents);

        var sortedEdges = new ArrayList<>(edges);
        Collections.sort(sortedEdges);
        this.edges = Collections.unmodifiableList(sortedEdges);

        this.rootRef = rootRef;
    }

    public List<Component> components() { return components; }
    public List<DependencyEdge> edges() { return edges; }
    public Optional<String> rootRef() { return Optional.ofNullable(rootRef); }

    public int componentCount() { return components.size(); }
    public int edgeCount() { return edges.size(); }

    /**
     * Look up a component by its bom-ref.
     */
    public Optional<Component> getComponentByRef(String bomRef) {
        return components.stream()
                .filter(c -> c.bomRef().equals(bomRef))
                .findFirst();
    }

    /**
     * Get the direct dependencies of a component (sorted).
     */
    public List<Component> getDirectDependencies(String bomRef) {
        List<String> targetRefs = edges.stream()
                .filter(e -> e.sourceRef().equals(bomRef))
                .map(DependencyEdge::targetRef)
                .sorted()
                .collect(Collectors.toList());

        List<Component> result = new ArrayList<>();
        for (String ref : targetRefs) {
            getComponentByRef(ref).ifPresent(result::add);
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Get components that depend on the given component (reverse lookup, sorted).
     */
    public List<Component> getDependents(String bomRef) {
        List<String> sourceRefs = edges.stream()
                .filter(e -> e.targetRef().equals(bomRef))
                .map(DependencyEdge::sourceRef)
                .sorted()
                .collect(Collectors.toList());

        List<Component> result = new ArrayList<>();
        for (String ref : sourceRefs) {
            getComponentByRef(ref).ifPresent(result::add);
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DependencyGraph g)) return false;
        return components.equals(g.components) && edges.equals(g.edges)
                && java.util.Objects.equals(rootRef, g.rootRef);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(components, edges, rootRef);
    }
}
