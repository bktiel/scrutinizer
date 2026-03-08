package com.scrutinizer.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scrutinizer.model.DependencyGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SbomParserTest {

    private SbomParser parser;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        parser = new SbomParser(mapper);
    }

    private JsonNode loadSampleSbom() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/fixtures/sample_npm_sbom.json")) {
            return mapper.readTree(is);
        }
    }

    @Nested
    class ParseFile {
        @Test
        void parsesSampleFixture() throws IOException {
            Path path = Path.of("src/test/resources/fixtures/sample_npm_sbom.json");
            DependencyGraph graph = parser.parseFile(path);
            assertThat(graph.componentCount()).isEqualTo(5);
            assertThat(graph.edgeCount()).isEqualTo(6);
        }

        @Test
        void fileNotFound() {
            assertThatThrownBy(() -> parser.parseFile(Path.of("nonexistent.json")))
                    .isInstanceOf(IOException.class);
        }
    }

    @Nested
    class Parse {
        @Test
        void minimalValidSbom() {
            ObjectNode data = mapper.createObjectNode();
            data.put("bomFormat", "CycloneDX");
            data.put("specVersion", "1.5");
            ArrayNode components = data.putArray("components");
            ObjectNode comp = components.addObject();
            comp.put("name", "foo");
            comp.put("version", "1.0");
            comp.put("bom-ref", "foo@1.0");
            data.putArray("dependencies");

            DependencyGraph graph = parser.parse(data);
            assertThat(graph.componentCount()).isEqualTo(1);
            assertThat(graph.edgeCount()).isEqualTo(0);
            assertThat(graph.components().get(0).name()).isEqualTo("foo");
        }

        @Test
        void rootRefExtractedFromMetadata() throws IOException {
            DependencyGraph graph = parser.parse(loadSampleSbom());
            assertThat(graph.rootRef()).hasValue("my-npm-app@1.0.0");
        }

        @Test
        void rootRefNoneWithoutMetadata() {
            ObjectNode data = mapper.createObjectNode();
            data.put("bomFormat", "CycloneDX");
            data.put("specVersion", "1.5");
            data.putArray("components");
            data.putArray("dependencies");

            DependencyGraph graph = parser.parse(data);
            assertThat(graph.rootRef()).isEmpty();
        }

        @Test
        void componentsSortedByName() throws IOException {
            DependencyGraph graph = parser.parse(loadSampleSbom());
            var names = graph.components().stream().map(c -> c.name()).toList();
            assertThat(names).isSorted();
        }

        @Test
        void edgesSorted() throws IOException {
            DependencyGraph graph = parser.parse(loadSampleSbom());
            for (int i = 1; i < graph.edges().size(); i++) {
                assertThat(graph.edges().get(i).compareTo(graph.edges().get(i - 1)))
                        .isGreaterThanOrEqualTo(0);
            }
        }

        @Test
        void optionalFieldsParsed() {
            ObjectNode data = mapper.createObjectNode();
            data.put("bomFormat", "CycloneDX");
            data.put("specVersion", "1.5");
            ArrayNode components = data.putArray("components");
            ObjectNode comp = components.addObject();
            comp.put("name", "bar");
            comp.put("version", "2.0");
            comp.put("bom-ref", "bar@2.0");
            comp.put("type", "framework");
            comp.put("group", "@scope");
            comp.put("purl", "pkg:npm/%40scope/bar@2.0");
            comp.put("description", "A framework");
            comp.put("scope", "optional");
            data.putArray("dependencies");

            DependencyGraph graph = parser.parse(data);
            var c = graph.components().get(0);
            assertThat(c.type()).isEqualTo("framework");
            assertThat(c.group()).hasValue("@scope");
            assertThat(c.purl()).hasValue("pkg:npm/%40scope/bar@2.0");
            assertThat(c.description()).hasValue("A framework");
            assertThat(c.scope()).isEqualTo("optional");
        }
    }

    @Nested
    class Validation {
        @Test
        void rejectsNonCycloneDxFormat() {
            ObjectNode data = mapper.createObjectNode();
            data.put("bomFormat", "SPDX");
            data.put("specVersion", "1.5");

            assertThatThrownBy(() -> parser.parse(data))
                    .isInstanceOf(SbomParseException.class)
                    .hasMessageContaining("CycloneDX");
        }

        @Test
        void rejectsUnsupportedSpecVersion() {
            ObjectNode data = mapper.createObjectNode();
            data.put("bomFormat", "CycloneDX");
            data.put("specVersion", "2.0");

            assertThatThrownBy(() -> parser.parse(data))
                    .isInstanceOf(SbomParseException.class)
                    .hasMessageContaining("specVersion");
        }

        @Test
        void rejectsMissingBomRef() {
            ObjectNode data = mapper.createObjectNode();
            data.put("bomFormat", "CycloneDX");
            data.put("specVersion", "1.5");
            ArrayNode components = data.putArray("components");
            ObjectNode comp = components.addObject();
            comp.put("name", "foo");
            comp.put("version", "1.0");
            // no bom-ref

            assertThatThrownBy(() -> parser.parse(data))
                    .isInstanceOf(SbomParseException.class)
                    .hasMessageContaining("bom-ref");
        }

        @Test
        void rejectsMissingName() {
            ObjectNode data = mapper.createObjectNode();
            data.put("bomFormat", "CycloneDX");
            data.put("specVersion", "1.5");
            ArrayNode components = data.putArray("components");
            ObjectNode comp = components.addObject();
            comp.put("version", "1.0");
            comp.put("bom-ref", "foo@1.0");
            // no name

            assertThatThrownBy(() -> parser.parse(data))
                    .isInstanceOf(SbomParseException.class)
                    .hasMessageContaining("name");
        }

        @Test
        void rejectsDependencyMissingRef() {
            ObjectNode data = mapper.createObjectNode();
            data.put("bomFormat", "CycloneDX");
            data.put("specVersion", "1.5");
            data.putArray("components");
            ArrayNode deps = data.putArray("dependencies");
            ObjectNode dep = deps.addObject();
            ArrayNode dependsOn = dep.putArray("dependsOn");
            dependsOn.add("a@1.0");
            // no ref

            assertThatThrownBy(() -> parser.parse(data))
                    .isInstanceOf(SbomParseException.class)
                    .hasMessageContaining("ref");
        }
    }
}
