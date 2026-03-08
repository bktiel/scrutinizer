package com.scrutinizer.viz;

import com.scrutinizer.model.Component;
import com.scrutinizer.model.DependencyEdge;
import com.scrutinizer.model.DependencyGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GraphExporterTest {

    private DependencyGraph testGraph;

    @BeforeEach
    void setUp() {
        List<Component> components = List.of(
                new Component("spring-boot", "3.3.0", "spring-boot-ref", "framework",
                        "org.springframework.boot", "pkg:maven/org.springframework.boot/spring-boot@3.3.0",
                        null, "required"),
                new Component("jackson-core", "2.15.3", "jackson-core-ref", "library",
                        "com.fasterxml.jackson", "pkg:maven/com.fasterxml.jackson/jackson-core@2.15.3",
                        null, "required"),
                new Component("slf4j-api", "2.0.9", "slf4j-api-ref", "library",
                        "org.slf4j", null,
                        null, "optional")
        );
        List<DependencyEdge> edges = List.of(
                new DependencyEdge("spring-boot-ref", "jackson-core-ref"),
                new DependencyEdge("spring-boot-ref", "slf4j-api-ref")
        );
        testGraph = new DependencyGraph(components, edges, null);
    }

    @Nested
    class DotExport {

        @Test
        void producesValidDotFormat() {
            String dot = DotExporter.export(testGraph);

            assertThat(dot).startsWith("digraph dependencies {");
            assertThat(dot).endsWith("}\n");
            assertThat(dot).contains("rankdir=LR");
        }

        @Test
        void containsAllNodes() {
            String dot = DotExporter.export(testGraph);

            assertThat(dot).contains("spring-boot-ref");
            assertThat(dot).contains("jackson-core-ref");
            assertThat(dot).contains("slf4j-api-ref");
        }

        @Test
        void containsAllEdges() {
            String dot = DotExporter.export(testGraph);

            assertThat(dot).contains("\"spring-boot-ref\" -> \"jackson-core-ref\"");
            assertThat(dot).contains("\"spring-boot-ref\" -> \"slf4j-api-ref\"");
        }

        @Test
        void includesVersionInLabels() {
            String dot = DotExporter.export(testGraph);

            assertThat(dot).contains("3.3.0");
            assertThat(dot).contains("2.15.3");
        }

        @Test
        void rootNodeGetsDifferentColor() {
            String dot = DotExporter.export(testGraph);

            // spring-boot is root (not a target of any edge)
            // Root color is #4A90D9 by default
            assertThat(dot).contains("\"spring-boot-ref\" [label=\"");
            assertThat(dot).contains("#4A90D9");
        }

        @Test
        void optionalScopeGetsDifferentColor() {
            String dot = DotExporter.export(testGraph);

            // slf4j-api is optional scope, should use optional color #F4D03F
            assertThat(dot).contains("#F4D03F");
        }

        @Test
        void isDeterministic() {
            String dot1 = DotExporter.export(testGraph);
            String dot2 = DotExporter.export(testGraph);

            assertThat(dot1).isEqualTo(dot2);
        }
    }

    @Nested
    class HtmlExport {

        @Test
        void producesValidHtml() {
            String html = HtmlGraphExporter.export(testGraph);

            assertThat(html).contains("<!DOCTYPE html>");
            assertThat(html).contains("</html>");
            assertThat(html).contains("<script");
        }

        @Test
        void containsD3Import() {
            String html = HtmlGraphExporter.export(testGraph);

            assertThat(html).contains("d3.min.js");
        }

        @Test
        void embedsNodeData() {
            String html = HtmlGraphExporter.export(testGraph);

            assertThat(html).contains("spring-boot");
            assertThat(html).contains("jackson-core");
            assertThat(html).contains("slf4j-api");
        }

        @Test
        void embedsEdgeData() {
            String html = HtmlGraphExporter.export(testGraph);

            assertThat(html).contains("spring-boot-ref");
            assertThat(html).contains("jackson-core-ref");
        }

        @Test
        void includesComponentCounts() {
            String html = HtmlGraphExporter.export(testGraph);

            assertThat(html).contains("3 components");
            assertThat(html).contains("2 dependencies");
        }

        @Test
        void customTitleApplied() {
            String html = HtmlGraphExporter.export(testGraph, "My Custom Title");

            assertThat(html).contains("My Custom Title");
        }

        @Test
        void isDeterministic() {
            String html1 = HtmlGraphExporter.export(testGraph);
            String html2 = HtmlGraphExporter.export(testGraph);

            assertThat(html1).isEqualTo(html2);
        }

        @Test
        void includesInteractiveFeatures() {
            String html = HtmlGraphExporter.export(testGraph);

            assertThat(html).contains("forceSimulation");
            assertThat(html).contains("tooltip");
            assertThat(html).contains("zoom");
            assertThat(html).contains("drag");
        }
    }
}
