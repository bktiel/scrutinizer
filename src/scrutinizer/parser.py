from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from scrutinizer.models import Component, DependencyEdge, DependencyGraph


class SBOMParseError(Exception):
    """Raised when the SBOM JSON is malformed or missing required fields."""


def parse_sbom_file(path: Path) -> DependencyGraph:
    """Parse a CycloneDX JSON file from disk and return a DependencyGraph."""
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    return parse_sbom(data)


def parse_sbom(data: dict[str, Any]) -> DependencyGraph:
    """Parse a CycloneDX JSON dict into a normalized DependencyGraph."""
    _validate_format(data)
    root_ref = _extract_root_ref(data)
    components = _parse_components(data)
    edges = _parse_dependencies(data)
    return DependencyGraph(
        components=components,
        edges=edges,
        root_ref=root_ref,
    )


def _validate_format(data: dict[str, Any]) -> None:
    bom_format = data.get("bomFormat")
    if bom_format != "CycloneDX":
        raise SBOMParseError(
            f"Expected bomFormat 'CycloneDX', got '{bom_format}'"
        )
    spec_version = data.get("specVersion", "")
    if not spec_version.startswith("1."):
        raise SBOMParseError(f"Unsupported specVersion '{spec_version}'")


def _extract_root_ref(data: dict[str, Any]) -> str | None:
    metadata = data.get("metadata", {})
    root_component = metadata.get("component", {})
    return root_component.get("bom-ref")


def _parse_components(data: dict[str, Any]) -> tuple[Component, ...]:
    raw_components = data.get("components", [])
    components: list[Component] = []
    for raw in raw_components:
        bom_ref = raw.get("bom-ref")
        name = raw.get("name")
        if bom_ref is None or name is None:
            raise SBOMParseError(
                f"Component missing required field 'bom-ref' or 'name': {raw}"
            )
        components.append(
            Component(
                name=name,
                version=raw.get("version", ""),
                bom_ref=bom_ref,
                type=raw.get("type", "library"),
                group=raw.get("group"),
                purl=raw.get("purl"),
                description=raw.get("description"),
                scope=raw.get("scope", "required"),
            )
        )
    return tuple(sorted(components))


def _parse_dependencies(data: dict[str, Any]) -> tuple[DependencyEdge, ...]:
    raw_deps = data.get("dependencies", [])
    edges: list[DependencyEdge] = []
    for dep in raw_deps:
        source_ref = dep.get("ref")
        if source_ref is None:
            raise SBOMParseError(f"Dependency entry missing 'ref': {dep}")
        for target_ref in dep.get("dependsOn", []):
            edges.append(
                DependencyEdge(source_ref=source_ref, target_ref=target_ref)
            )
    return tuple(sorted(edges))
