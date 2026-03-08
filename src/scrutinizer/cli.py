from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

from scrutinizer.graph import find_root_components, summary
from scrutinizer.parser import SBOMParseError, parse_sbom_file


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="scrutinizer",
        description="Parse CycloneDX SBOMs and inspect dependency graphs.",
    )
    parser.add_argument(
        "--sbom",
        type=Path,
        required=True,
        help="Path to a CycloneDX JSON SBOM file.",
    )
    parser.add_argument(
        "--format",
        choices=["table", "json"],
        default="table",
        dest="output_format",
        help="Output format (default: table).",
    )
    return parser


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)

    try:
        graph = parse_sbom_file(args.sbom)
    except FileNotFoundError:
        print(f"Error: File not found: {args.sbom}", file=sys.stderr)
        return 1
    except SBOMParseError as e:
        print(f"Error: {e}", file=sys.stderr)
        return 1

    if args.output_format == "json":
        _print_json(graph)
    else:
        _print_table(graph)

    return 0


def _print_table(graph):
    stats = summary(graph)
    print(
        f"SBOM Inventory: {stats['total_components']} components, "
        f"{stats['total_edges']} dependency edges"
    )
    print("-" * 72)
    print(f"{'Name':<35} {'Version':<15} {'Type':<12} {'Scope'}")
    print("-" * 72)
    for comp in graph.components:
        name = comp.name
        if comp.group:
            name = f"{comp.group}/{comp.name}"
        print(f"{name:<35} {comp.version:<15} {comp.type:<12} {comp.scope}")
    print("-" * 72)

    roots = find_root_components(graph)
    if roots:
        print(f"\nRoot components: {', '.join(r.name for r in roots)}")


def _print_json(graph):
    output = {
        "components": [
            {
                "name": c.name,
                "version": c.version,
                "type": c.type,
                "group": c.group,
                "purl": c.purl,
                "scope": c.scope,
                "bom_ref": c.bom_ref,
            }
            for c in graph.components
        ],
        "edges": [
            {"source": e.source_ref, "target": e.target_ref}
            for e in graph.edges
        ],
        "summary": summary(graph),
    }
    print(json.dumps(output, indent=2))
