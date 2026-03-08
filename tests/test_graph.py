from scrutinizer.graph import (
    find_root_components,
    get_all_transitive_dependencies,
    summary,
)
from scrutinizer.models import Component, DependencyEdge, DependencyGraph


class TestGetAllTransitiveDependencies:
    def test_from_root(self, sample_graph):
        deps = get_all_transitive_dependencies(
            sample_graph, "my-npm-app@1.0.0"
        )
        names = {c.name for c in deps}
        assert names == {"express", "body-parser", "debug", "ms", "bytes"}

    def test_from_express(self, sample_graph):
        deps = get_all_transitive_dependencies(
            sample_graph, "express@4.18.2"
        )
        names = {c.name for c in deps}
        assert names == {"body-parser", "debug", "ms", "bytes"}

    def test_from_leaf(self, sample_graph):
        deps = get_all_transitive_dependencies(sample_graph, "ms@2.0.0")
        assert deps == ()

    def test_from_debug(self, sample_graph):
        deps = get_all_transitive_dependencies(sample_graph, "debug@2.6.9")
        assert len(deps) == 1
        assert deps[0].name == "ms"

    def test_cycle_handling(self):
        c_a = Component(name="a", version="1.0", bom_ref="a@1.0")
        c_b = Component(name="b", version="1.0", bom_ref="b@1.0")
        graph = DependencyGraph(
            components=(c_a, c_b),
            edges=(
                DependencyEdge(source_ref="a@1.0", target_ref="b@1.0"),
                DependencyEdge(source_ref="b@1.0", target_ref="a@1.0"),
            ),
        )
        deps = get_all_transitive_dependencies(graph, "a@1.0")
        assert len(deps) == 1
        assert deps[0].name == "b"

    def test_result_is_sorted(self, sample_graph):
        deps = get_all_transitive_dependencies(
            sample_graph, "my-npm-app@1.0.0"
        )
        names = [c.name for c in deps]
        assert names == sorted(names)


class TestFindRootComponents:
    def test_sample_graph_roots(self, sample_graph):
        roots = find_root_components(sample_graph)
        # express is targeted by my-npm-app (not in components list),
        # but that edge's source isn't in components, so express is
        # still depended on by the root ref edge. Let's check what
        # the actual roots are based on edges.
        root_names = {r.name for r in roots}
        # express is targeted by my-npm-app@1.0.0 edge, so it IS depended on
        # body-parser is targeted by express, so it IS depended on
        # The only component not targeted by any edge is express
        # Wait - express IS targeted by my-npm-app@1.0.0.
        # All 5 components are targets of some edge. So there are no roots
        # among the components list (the true root my-npm-app is in metadata only).
        # Actually let me trace:
        # edges target: express, body-parser, debug, debug, ms, bytes
        # So targets = {express, body-parser, debug, ms, bytes} = all 5
        # root_refs = all_refs - depended_on = empty set
        assert root_names == set()

    def test_simple_graph_with_root(self):
        c_a = Component(name="a", version="1.0", bom_ref="a@1.0")
        c_b = Component(name="b", version="1.0", bom_ref="b@1.0")
        graph = DependencyGraph(
            components=(c_a, c_b),
            edges=(DependencyEdge(source_ref="a@1.0", target_ref="b@1.0"),),
        )
        roots = find_root_components(graph)
        assert len(roots) == 1
        assert roots[0].name == "a"


class TestSummary:
    def test_sample_graph_summary(self, sample_graph):
        s = summary(sample_graph)
        assert s["total_components"] == 5
        assert s["total_edges"] == 6
        assert s["root_components"] == 0  # root is in metadata only
