from __future__ import annotations

from scrutinizer.models import Component, DependencyGraph


def get_all_transitive_dependencies(
    graph: DependencyGraph,
    bom_ref: str,
) -> tuple[Component, ...]:
    """Return all transitive dependencies of a component via BFS."""
    visited: set[str] = set()
    queue: list[str] = [bom_ref]
    result_refs: set[str] = set()

    while queue:
        current = queue.pop(0)
        if current in visited:
            continue
        visited.add(current)
        for edge in graph.edges:
            if edge.source_ref == current and edge.target_ref not in visited:
                result_refs.add(edge.target_ref)
                queue.append(edge.target_ref)

    components = []
    for ref in result_refs:
        comp = graph.get_component_by_ref(ref)
        if comp is not None:
            components.append(comp)
    return tuple(sorted(components))


def find_root_components(graph: DependencyGraph) -> tuple[Component, ...]:
    """Find components that are not depended on by anything."""
    all_refs = {c.bom_ref for c in graph.components}
    depended_on = {e.target_ref for e in graph.edges}
    root_refs = all_refs - depended_on
    roots = [c for c in graph.components if c.bom_ref in root_refs]
    return tuple(sorted(roots))


def summary(graph: DependencyGraph) -> dict[str, int]:
    return {
        "total_components": graph.component_count,
        "total_edges": graph.edge_count,
        "root_components": len(find_root_components(graph)),
    }
