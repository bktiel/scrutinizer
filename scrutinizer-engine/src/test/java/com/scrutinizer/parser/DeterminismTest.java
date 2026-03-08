package com.scrutinizer.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scrutinizer.model.DependencyGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class DeterminismTest {

    private SbomParser parser;
    private ObjectMapper mapper;
    private JsonNode sampleData;

    @BeforeEach
    void setUp() throws IOException {
        mapper = new ObjectMapper();
        parser = new SbomParser(mapper);
        try (InputStream is = getClass().getResourceAsStream("/fixtures/sample_npm_sbom.json")) {
            sampleData = mapper.readTree(is);
        }
    }

    @Test
    void identicalAcross100Parses() {
        DependencyGraph baseline = parser.parse(sampleData);
        for (int i = 0; i < 100; i++) {
            DependencyGraph result = parser.parse(sampleData);
            assertThat(result).isEqualTo(baseline);
        }
    }

    @Test
    void shuffledComponentsProduceSameGraph() {
        DependencyGraph baseline = parser.parse(sampleData);

        for (int seed = 0; seed < 20; seed++) {
            ObjectNode shuffled = shuffleSbom(sampleData, new Random(seed));
            DependencyGraph result = parser.parse(shuffled);
            assertThat(result).isEqualTo(baseline);
            assertThat(result.components()).isEqualTo(baseline.components());
            assertThat(result.edges()).isEqualTo(baseline.edges());
        }
    }

    @Test
    void hashStability() {
        DependencyGraph g1 = parser.parse(sampleData);
        DependencyGraph g2 = parser.parse(sampleData);
        assertThat(g1.hashCode()).isEqualTo(g2.hashCode());
        assertThat(g1.components().hashCode()).isEqualTo(g2.components().hashCode());
        assertThat(g1.edges().hashCode()).isEqualTo(g2.edges().hashCode());
    }

    @Test
    void shuffledHashStability() {
        DependencyGraph baseline = parser.parse(sampleData);
        ObjectNode shuffled = shuffleSbom(sampleData, new Random(42));
        DependencyGraph result = parser.parse(shuffled);
        assertThat(result.hashCode()).isEqualTo(baseline.hashCode());
        assertThat(result.components().hashCode()).isEqualTo(baseline.components().hashCode());
        assertThat(result.edges().hashCode()).isEqualTo(baseline.edges().hashCode());
    }

    /**
     * Creates a deep copy of the SBOM with shuffled components and dependencies.
     */
    private ObjectNode shuffleSbom(JsonNode original, Random rng) {
        ObjectNode copy = original.deepCopy();

        // Shuffle components array
        ArrayNode components = (ArrayNode) copy.get("components");
        if (components != null) {
            List<JsonNode> compList = new ArrayList<>();
            components.forEach(compList::add);
            Collections.shuffle(compList, rng);
            components.removeAll();
            compList.forEach(components::add);
        }

        // Shuffle dependencies array and each dependsOn list
        ArrayNode dependencies = (ArrayNode) copy.get("dependencies");
        if (dependencies != null) {
            List<JsonNode> depList = new ArrayList<>();
            dependencies.forEach(depList::add);
            Collections.shuffle(depList, rng);

            // Shuffle each dependsOn
            for (JsonNode dep : depList) {
                ArrayNode dependsOn = (ArrayNode) dep.get("dependsOn");
                if (dependsOn != null && dependsOn.size() > 1) {
                    List<JsonNode> targets = new ArrayList<>();
                    dependsOn.forEach(targets::add);
                    Collections.shuffle(targets, rng);
                    dependsOn.removeAll();
                    targets.forEach(dependsOn::add);
                }
            }

            dependencies.removeAll();
            depList.forEach(dependencies::add);
        }

        return copy;
    }
}
