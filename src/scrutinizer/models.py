from __future__ import annotations

from dataclasses import dataclass
from typing import Optional


@dataclass(frozen=True, order=True)
class Component:
    """A single software component extracted from an SBOM."""

    name: str
    version: str
    bom_ref: str
    type: str = "library"
    group: Optional[str] = None
    purl: Optional[str] = None
    description: Optional[str] = None
    scope: str = "required"


@dataclass(frozen=True, order=True)
class DependencyEdge:
    """A directed edge: source depends on target."""

    source_ref: str
    target_ref: str


@dataclass(frozen=True)
class DependencyGraph:
    """A normalized, deterministic dependency graph.

    Components and edges are stored as sorted tuples to enforce
    immutability and guarantee deterministic iteration order.
    """

    components: tuple[Component, ...]
    edges: tuple[DependencyEdge, ...]
    root_ref: Optional[str] = None

    @property
    def component_count(self) -> int:
        return len(self.components)

    @property
    def edge_count(self) -> int:
        return len(self.edges)

    def get_component_by_ref(self, bom_ref: str) -> Optional[Component]:
        for c in self.components:
            if c.bom_ref == bom_ref:
                return c
        return None

    def get_direct_dependencies(self, bom_ref: str) -> tuple[Component, ...]:
        target_refs = sorted(
            e.target_ref for e in self.edges if e.source_ref == bom_ref
        )
        result = []
        for ref in target_refs:
            comp = self.get_component_by_ref(ref)
            if comp is not None:
                result.append(comp)
        return tuple(result)

    def get_dependents(self, bom_ref: str) -> tuple[Component, ...]:
        source_refs = sorted(
            e.source_ref for e in self.edges if e.target_ref == bom_ref
        )
        result = []
        for ref in source_refs:
            comp = self.get_component_by_ref(ref)
            if comp is not None:
                result.append(comp)
        return tuple(result)
