import copy

import pytest

from scrutinizer.parser import SBOMParseError, parse_sbom, parse_sbom_file


class TestParseSbomFile:
    def test_parses_sample_fixture(self, sample_sbom_path):
        graph = parse_sbom_file(sample_sbom_path)
        assert graph.component_count == 5
        assert graph.edge_count == 6

    def test_file_not_found(self, tmp_path):
        with pytest.raises(FileNotFoundError):
            parse_sbom_file(tmp_path / "nonexistent.json")


class TestParseSbom:
    def test_minimal_valid_sbom(self):
        data = {
            "bomFormat": "CycloneDX",
            "specVersion": "1.5",
            "components": [
                {"name": "foo", "version": "1.0", "bom-ref": "foo@1.0"}
            ],
            "dependencies": [],
        }
        graph = parse_sbom(data)
        assert graph.component_count == 1
        assert graph.edge_count == 0
        assert graph.components[0].name == "foo"

    def test_root_ref_extracted_from_metadata(self, sample_sbom_data):
        graph = parse_sbom(sample_sbom_data)
        assert graph.root_ref == "my-npm-app@1.0.0"

    def test_root_ref_none_without_metadata(self):
        data = {
            "bomFormat": "CycloneDX",
            "specVersion": "1.5",
            "components": [],
            "dependencies": [],
        }
        graph = parse_sbom(data)
        assert graph.root_ref is None

    def test_components_sorted_by_name(self, sample_sbom_data):
        graph = parse_sbom(sample_sbom_data)
        names = [c.name for c in graph.components]
        assert names == sorted(names)

    def test_edges_sorted(self, sample_sbom_data):
        graph = parse_sbom(sample_sbom_data)
        edge_tuples = [(e.source_ref, e.target_ref) for e in graph.edges]
        assert edge_tuples == sorted(edge_tuples)

    def test_optional_fields_parsed(self):
        data = {
            "bomFormat": "CycloneDX",
            "specVersion": "1.5",
            "components": [
                {
                    "name": "bar",
                    "version": "2.0",
                    "bom-ref": "bar@2.0",
                    "type": "framework",
                    "group": "@scope",
                    "purl": "pkg:npm/%40scope/bar@2.0",
                    "description": "A framework",
                    "scope": "optional",
                }
            ],
            "dependencies": [],
        }
        graph = parse_sbom(data)
        c = graph.components[0]
        assert c.type == "framework"
        assert c.group == "@scope"
        assert c.purl == "pkg:npm/%40scope/bar@2.0"
        assert c.description == "A framework"
        assert c.scope == "optional"


class TestValidation:
    def test_rejects_non_cyclonedx_format(self):
        data = {"bomFormat": "SPDX", "specVersion": "1.5"}
        with pytest.raises(SBOMParseError, match="CycloneDX"):
            parse_sbom(data)

    def test_rejects_unsupported_spec_version(self):
        data = {"bomFormat": "CycloneDX", "specVersion": "2.0"}
        with pytest.raises(SBOMParseError, match="specVersion"):
            parse_sbom(data)

    def test_rejects_missing_bom_ref(self):
        data = {
            "bomFormat": "CycloneDX",
            "specVersion": "1.5",
            "components": [{"name": "foo", "version": "1.0"}],
        }
        with pytest.raises(SBOMParseError, match="bom-ref"):
            parse_sbom(data)

    def test_rejects_missing_name(self):
        data = {
            "bomFormat": "CycloneDX",
            "specVersion": "1.5",
            "components": [{"version": "1.0", "bom-ref": "foo@1.0"}],
        }
        with pytest.raises(SBOMParseError, match="name"):
            parse_sbom(data)

    def test_rejects_dependency_missing_ref(self):
        data = {
            "bomFormat": "CycloneDX",
            "specVersion": "1.5",
            "components": [],
            "dependencies": [{"dependsOn": ["a@1.0"]}],
        }
        with pytest.raises(SBOMParseError, match="ref"):
            parse_sbom(data)
