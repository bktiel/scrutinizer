package com.scrutinizer.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrutinizer.model.Component;
import com.scrutinizer.model.DependencyEdge;
import com.scrutinizer.model.DependencyGraph;
import com.scrutinizer.parser.SbomParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class GraphAnalyzerTest {

    private GraphAnalyzer analyzer;
    private DependencyGraph sampleGraph;

    @BeforeEach
    void setUp() throws IOException {
        analyzer = new GraphAnalyzer();
        SbomParser parser = new SbomParser(new ObjectMapper());
        sampleGraph = parser.parseFile(Path.of("src/test/resources/fixtures/sample_npm_sbom.json"));
    }

    @Nested
    class GetAllTransitiveDependencies {
        @Test
        void fromRoot() {
            var deps = analyzer.getAllTransitiveDependencies(sampleGraph, "my-npm-app@1.0.0");
            Set<String> names = deps.stream().map(Component::name).collect(Collectors.toSet());
            assertThat(names).containsExactlyInAnyOrder("express", "body-parser", "debug", "ms", "bytes");
        }

        @Test
        void fromExpress() {
            var deps = analyzer.getAllTransitiveDependencies(sampleGraph, "express@4.18.2");
            Set<String> names = deps.stream().map(Component::name).collect(Collectors.toSet());
            assertThat(names).containsExactlyInAnyOrder("body-parser", "debug", "ms", "bytes");
        }

        @Test
        void fromLeaf() {
            var deps = analyzer.getAllTransitiveDependencies(sampleGraph, "ms@2.0.0");
            assertThat(deps).isEmpty();
        }

        @Test
        void fromDebug() {
            var deps = analyzer.getAllTransitiveDependencies(sampleGraph, "debug@2.6.9");
            assertThat(deps).hasSize(1);
            assertThat(deps.get(0).name()).isEqualTo("ms");
        }

        @Test
        void cycleHandling() {
            Component cA = new Component("a", "1.0", "a@1.0");
            Component cB = new Component("b", "1.0", "b@1.0");
            DependencyGraph graph = new DependencyGraph(
                    List.of(cA, cB),
                    List.of(
                            new DependencyEdge("a@1.0", "b@1.0"),
                            new DependencyEdge("b@1.0", "a@1.0")
                    ),
                    null
            );
            var deps = analyzer.getAllTransitiveDependencies(graph, "a@1.0");
            assertThat(deps).hasSize(1);
            assertThat(deps.get(0).name()).isEqualTo("b");
        }

        @Test
        void resultIsSorted() {
            var deps = analyzer.getAllTransitiveDependencies(sampleGraph, "my-npm-app@1.0.0");
            var names = deps.stream().map(Component::name).toList();
            assertThat(names).isSorted();
        }
    }

    @Nested
    class FindRootComponents {
        @Test
        void sampleGraphRoots() {
            // All 5 components are targets of edges from my-npm-app (metadata root),
            // so no component in the components list is a root.
            var roots = analyzer.findRootComponents(sampleGraph);
            assertThat(roots).isEmpty();
        }

        @Test
        void simpleGraphWithRoot() {
            Component cA = new Component("a", "1.0", "a@1.0");
            Component cB = new Component("b", "1.0", "b@1.0");
            DependencyGraph graph = new DependencyGraph(
                    List.of(cA, cB),
                    List.of(new DependencyEdge("a@1.0", "b@1.0")),
                    null
            );
            var roots = analyzer.findRootComponents(graph);
            assertThat(roots).hasSize(1);
            assertThat(roots.get(0).name()).isEqualTo("a");
        }
    }

    @Nested
    class Summary {
        @Test
        void sampleGraphSummary() {
            Map<String, Integer> s = analyzer.summary(sampleGraph);
            assertThat(s.get("total_components")).isEqualTo(5);
            assertThat(s.get("total_edges")).isEqualTo(6);
            assertThat(s.get("root_components")).isEqualTo(0);
        }
    }
}
