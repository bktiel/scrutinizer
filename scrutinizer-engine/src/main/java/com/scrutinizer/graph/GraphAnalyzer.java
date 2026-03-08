package com.scrutinizer.graph;

import com.scrutinizer.model.Component;
import com.scrutinizer.model.DependencyEdge;
import com.scrutinizer.model.DependencyGraph;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Graph analysis utilities for dependency traversal and insight extraction.
 */
@Service
public class GraphAnalyzer {

    /**
     * Return all transitive dependencies of a component via BFS.
     * Handles cycles gracefully via a visited set.
     */
    public List<Component> getAllTransitiveDependencies(DependencyGraph graph, String bomRef) {
        Set<String> visited = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        Set<String> resultRefs = new LinkedHashSet<>();

        queue.add(bomRef);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (visited.contains(current)) {
                continue;
            }
            visited.add(current);

            for (DependencyEdge edge : graph.edges()) {
                if (edge.sourceRef().equals(current) && !visited.contains(edge.targetRef())) {
                    resultRefs.add(edge.targetRef());
                    queue.add(edge.targetRef());
                }
            }
        }

        List<Component> components = new ArrayList<>();
        for (String ref : resultRefs) {
            graph.getComponentByRef(ref).ifPresent(components::add);
        }
        Collections.sort(components);
        return Collections.unmodifiableList(components);
    }

    /**
     * Find components that are not depended on by anything.
     */
    public List<Component> findRootComponents(DependencyGraph graph) {
        Set<String> allRefs = graph.components().stream()
                .map(Component::bomRef)
                .collect(Collectors.toSet());

        Set<String> dependedOn = graph.edges().stream()
                .map(DependencyEdge::targetRef)
                .collect(Collectors.toSet());

        Set<String> rootRefs = new HashSet<>(allRefs);
        rootRefs.removeAll(dependedOn);

        List<Component> roots = graph.components().stream()
                .filter(c -> rootRefs.contains(c.bomRef()))
                .sorted()
                .collect(Collectors.toList());

        return Collections.unmodifiableList(roots);
    }

    /**
     * Returns summary statistics about the graph.
     */
    public Map<String, Integer> summary(DependencyGraph graph) {
        return Map.of(
                "total_components", graph.componentCount(),
                "total_edges", graph.edgeCount(),
                "root_components", findRootComponents(graph).size()
        );
    }
}
