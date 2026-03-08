import json

from scrutinizer.cli import build_parser, main


class TestBuildParser:
    def test_has_sbom_argument(self):
        parser = build_parser()
        args = parser.parse_args(["--sbom", "test.json"])
        assert str(args.sbom) == "test.json"

    def test_default_format_is_table(self):
        parser = build_parser()
        args = parser.parse_args(["--sbom", "test.json"])
        assert args.output_format == "table"

    def test_json_format(self):
        parser = build_parser()
        args = parser.parse_args(["--sbom", "test.json", "--format", "json"])
        assert args.output_format == "json"


class TestMainTableOutput:
    def test_exit_code_zero(self, sample_sbom_path):
        code = main(["--sbom", str(sample_sbom_path)])
        assert code == 0

    def test_prints_inventory_header(self, sample_sbom_path, capsys):
        main(["--sbom", str(sample_sbom_path)])
        captured = capsys.readouterr()
        assert "SBOM Inventory: 5 components" in captured.out
        assert "6 dependency edges" in captured.out

    def test_prints_column_headers(self, sample_sbom_path, capsys):
        main(["--sbom", str(sample_sbom_path)])
        captured = capsys.readouterr()
        assert "Name" in captured.out
        assert "Version" in captured.out
        assert "Type" in captured.out
        assert "Scope" in captured.out

    def test_prints_component_names(self, sample_sbom_path, capsys):
        main(["--sbom", str(sample_sbom_path)])
        captured = capsys.readouterr()
        assert "express" in captured.out
        assert "body-parser" in captured.out
        assert "debug" in captured.out
        assert "ms" in captured.out
        assert "bytes" in captured.out


class TestMainJsonOutput:
    def test_valid_json(self, sample_sbom_path, capsys):
        main(["--sbom", str(sample_sbom_path), "--format", "json"])
        captured = capsys.readouterr()
        data = json.loads(captured.out)
        assert "components" in data
        assert "edges" in data
        assert "summary" in data

    def test_json_component_count(self, sample_sbom_path, capsys):
        main(["--sbom", str(sample_sbom_path), "--format", "json"])
        captured = capsys.readouterr()
        data = json.loads(captured.out)
        assert len(data["components"]) == 5
        assert data["summary"]["total_components"] == 5


class TestMainErrors:
    def test_file_not_found(self, capsys):
        code = main(["--sbom", "nonexistent.json"])
        assert code == 1
        captured = capsys.readouterr()
        assert "File not found" in captured.err

    def test_invalid_sbom(self, tmp_path, capsys):
        bad_file = tmp_path / "bad.json"
        bad_file.write_text('{"bomFormat": "SPDX"}')
        code = main(["--sbom", str(bad_file)])
        assert code == 1
        captured = capsys.readouterr()
        assert "Error" in captured.err
