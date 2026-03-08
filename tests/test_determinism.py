import copy
import random

from scrutinizer.parser import parse_sbom


class TestDeterministicParsing:
    def test_identical_across_100_parses(self, sample_sbom_data):
        baseline = parse_sbom(sample_sbom_data)
        for _ in range(100):
            result = parse_sbom(sample_sbom_data)
            assert result == baseline

    def test_shuffled_components_produce_same_graph(self, sample_sbom_data):
        baseline = parse_sbom(sample_sbom_data)
        for seed in range(20):
            shuffled = copy.deepcopy(sample_sbom_data)
            rng = random.Random(seed)
            rng.shuffle(shuffled["components"])
            rng.shuffle(shuffled["dependencies"])
            for dep in shuffled["dependencies"]:
                rng.shuffle(dep.get("dependsOn", []))
            result = parse_sbom(shuffled)
            assert result == baseline
            assert result.components == baseline.components
            assert result.edges == baseline.edges

    def test_hash_stability(self, sample_sbom_data):
        g1 = parse_sbom(sample_sbom_data)
        g2 = parse_sbom(sample_sbom_data)
        assert hash(g1.components) == hash(g2.components)
        assert hash(g1.edges) == hash(g2.edges)

    def test_shuffled_hash_stability(self, sample_sbom_data):
        baseline = parse_sbom(sample_sbom_data)
        shuffled = copy.deepcopy(sample_sbom_data)
        rng = random.Random(42)
        rng.shuffle(shuffled["components"])
        rng.shuffle(shuffled["dependencies"])
        result = parse_sbom(shuffled)
        assert hash(result.components) == hash(baseline.components)
        assert hash(result.edges) == hash(baseline.edges)
