import json
from pathlib import Path

import pytest

from scrutinizer.models import DependencyGraph

FIXTURES_DIR = Path(__file__).parent / "fixtures"


@pytest.fixture
def sample_sbom_path() -> Path:
    return FIXTURES_DIR / "sample_npm_sbom.json"


@pytest.fixture
def sample_sbom_data(sample_sbom_path: Path) -> dict:
    with open(sample_sbom_path) as f:
        return json.load(f)


@pytest.fixture
def sample_graph(sample_sbom_path: Path) -> DependencyGraph:
    from scrutinizer.parser import parse_sbom_file

    return parse_sbom_file(sample_sbom_path)
