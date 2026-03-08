import dataclasses

import pytest

from scrutinizer.models import Component, DependencyEdge, DependencyGraph


class TestComponent:
    def test_creation_with_defaults(self):
        c = Component(name="lodash", version="4.17.21", bom_ref="lodash@4.17.21")
        assert c.name == "lodash"
        assert c.version == "4.17.21"
        assert c.bom_ref == "lodash@4.17.21"
        assert c.type == "library"
        assert c.group is None
        assert c.purl is None
        assert c.description is None
        assert c.scope == "required"

    def test_creation_with_all_fields(self):
        c = Component(
            name="utils",
            version="1.0.0",
            bom_ref="@scope/utils@1.0.0",
            type="framework",
            group="@scope",
            purl="pkg:npm/%40scope/utils@1.0.0",
            description="Utility library",
            scope="optional",
        )
        assert c.group == "@scope"
        assert c.type == "framework"

    def test_frozen_immutability(self):
        c = Component(name="a", version="1.0", bom_ref="a@1.0")
        with pytest.raises(dataclasses.FrozenInstanceError):
            c.name = "b"  # type: ignore[misc]

    def test_ordering(self):
        c_b = Component(name="b-lib", version="1.0", bom_ref="b@1.0")
        c_a = Component(name="a-lib", version="1.0", bom_ref="a@1.0")
        assert sorted([c_b, c_a]) == [c_a, c_b]

    def test_equality_and_hashing(self):
        c1 = Component(name="x", version="1.0", bom_ref="x@1.0")
        c2 = Component(name="x", version="1.0", bom_ref="x@1.0")
        assert c1 == c2
        assert hash(c1) == hash(c2)

    def test_inequality(self):
        c1 = Component(name="x", version="1.0", bom_ref="x@1.0")
        c2 = Component(name="x", version="2.0", bom_ref="x@2.0")
        assert c1 != c2


class TestDependencyEdge:
    def test_creation(self):
        e = DependencyEdge(source_ref="a@1.0", target_ref="b@1.0")
        assert e.source_ref == "a@1.0"
        assert e.target_ref == "b@1.0"

    def test_ordering(self):
        e1 = DependencyEdge(source_ref="b@1.0", target_ref="c@1.0")
        e2 = DependencyEdge(source_ref="a@1.0", target_ref="d@1.0")
        assert sorted([e1, e2]) == [e2, e1]

    def test_equality(self):
        e1 = DependencyEdge(source_ref="a", target_ref="b")
        e2 = DependencyEdge(source_ref="a", target_ref="b")
        assert e1 == e2
        assert hash(e1) == hash(e2)


class TestDependencyGraph:
    @pytest.fixture
    def simple_graph(self):
        c_a = Component(name="a", version="1.0", bom_ref="a@1.0")
        c_b = Component(name="b", version="1.0", bom_ref="b@1.0")
        c_c = Component(name="c", version="1.0", bom_ref="c@1.0")
        e1 = DependencyEdge(source_ref="a@1.0", target_ref="b@1.0")
        e2 = DependencyEdge(source_ref="a@1.0", target_ref="c@1.0")
        return DependencyGraph(
            components=(c_a, c_b, c_c),
            edges=(e1, e2),
            root_ref="a@1.0",
        )

    def test_component_count(self, simple_graph):
        assert simple_graph.component_count == 3

    def test_edge_count(self, simple_graph):
        assert simple_graph.edge_count == 2

    def test_get_component_by_ref_found(self, simple_graph):
        c = simple_graph.get_component_by_ref("b@1.0")
        assert c is not None
        assert c.name == "b"

    def test_get_component_by_ref_not_found(self, simple_graph):
        assert simple_graph.get_component_by_ref("z@1.0") is None

    def test_get_direct_dependencies(self, simple_graph):
        deps = simple_graph.get_direct_dependencies("a@1.0")
        assert len(deps) == 2
        assert deps[0].name == "b"
        assert deps[1].name == "c"

    def test_get_direct_dependencies_leaf(self, simple_graph):
        deps = simple_graph.get_direct_dependencies("b@1.0")
        assert deps == ()

    def test_get_dependents(self, simple_graph):
        deps = simple_graph.get_dependents("b@1.0")
        assert len(deps) == 1
        assert deps[0].name == "a"

    def test_empty_graph(self):
        g = DependencyGraph(components=(), edges=())
        assert g.component_count == 0
        assert g.edge_count == 0
        assert g.root_ref is None
