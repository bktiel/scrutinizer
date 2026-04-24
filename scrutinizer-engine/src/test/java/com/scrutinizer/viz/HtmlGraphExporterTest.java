package com.scrutinizer.viz;

import com.scrutinizer.model.Component;
import com.scrutinizer.model.DependencyEdge;
import com.scrutinizer.model.DependencyGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlGraphExporterTest {

    @Nested
    class BasicExportTests {
        @Test
        void exportsEmptyGraph() {
            DependencyGraph graph = new DependencyGraph(List.of(), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("<!DOCTYPE html>");
            assertThat(html).contains("</html>");
        }

        @Test
        void exportsWithDefaultTitle() {
            DependencyGraph graph = new DependencyGraph(List.of(), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("Dependency Graph");
        }

        @Test
        void exportsWithCustomTitle() {
            DependencyGraph graph = new DependencyGraph(List.of(), List.of(), null);

            String html = HtmlGraphExporter.export(graph, "My Custom Title");

            assertThat(html).contains("My Custom Title");
            assertThat(html).doesNotContain("Dependency Graph");
        }

        @Test
        void exportsSingleComponent() {
            Component c = new Component("axios", "1.7.2", "axios@1.7.2");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("\"axios\"");
            assertThat(html).contains("\"1.7.2\"");
        }

        @Test
        void exportsMultipleComponents() {
            Component c1 = new Component("pkg1", "1.0", "pkg1@1.0");
            Component c2 = new Component("pkg2", "2.0", "pkg2@2.0");
            DependencyGraph graph = new DependencyGraph(List.of(c1, c2), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("\"pkg1\"");
            assertThat(html).contains("\"pkg2\"");
            assertThat(html).contains("\"1.0\"");
            assertThat(html).contains("\"2.0\"");
        }

        @Test
        void exportsDependencyEdges() {
            Component c1 = new Component("a", "1.0", "a@1.0");
            Component c2 = new Component("b", "1.0", "b@1.0");
            DependencyEdge edge = new DependencyEdge("a@1.0", "b@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c1, c2), List.of(edge), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("\"source\":");
            assertThat(html).contains("\"target\":");
        }
    }

    @Nested
    class HtmlStructureTests {
        @Test
        void containsHead() {
            DependencyGraph graph = new DependencyGraph(List.of(), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("<head>").contains("</head>");
        }

        @Test
        void containsBody() {
            DependencyGraph graph = new DependencyGraph(List.of(), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("<body>").contains("</body>");
        }

        @Test
        void containsMetaCharset() {
            DependencyGraph graph = new DependencyGraph(List.of(), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("charset=UTF-8");
        }

        @Test
        void containsViewportMeta() {
            DependencyGraph graph = new DependencyGraph(List.of(), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("viewport");
        }

        @Test
        void containsStyleTag() {
            DependencyGraph graph = new DependencyGraph(List.of(), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("<style>").contains("</style>");
        }

        @Test
        void containsScriptTag() {
            DependencyGraph graph = new DependencyGraph(List.of(), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("<script>").contains("</script>");
        }

        @Test
        void loadsD3FromCdn() {
            DependencyGraph graph = new DependencyGraph(List.of(), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("cdnjs.cloudflare.com/ajax/libs/d3");
        }
    }

    @Nested
    class DataEmbeddingTests {
        @Test
        void embedsNodesAsJson() {
            Component c = new Component("pkg", "1.0", "pkg@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("const nodes = ");
            assertThat(html).contains("[");
            assertThat(html).contains("]");
        }

        @Test
        void embedsLinksAsJson() {
            Component c1 = new Component("a", "1.0", "a@1.0");
            Component c2 = new Component("b", "1.0", "b@1.0");
            DependencyEdge edge = new DependencyEdge("a@1.0", "b@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c1, c2), List.of(edge), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("const links = ");
            assertThat(html).contains("source");
            assertThat(html).contains("target");
        }

        @Test
        void nodeJsonIncludesId() {
            Component c = new Component("pkg", "1.0", "pkg@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("\"id\":");
        }

        @Test
        void nodeJsonIncludesName() {
            Component c = new Component("pkg", "1.0", "pkg@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("\"name\":");
        }

        @Test
        void nodeJsonIncludesVersion() {
            Component c = new Component("pkg", "1.0", "pkg@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("\"version\":");
        }

        @Test
        void nodeJsonIncludesType() {
            Component c = new Component("pkg", "1.0", "pkg@1.0", "framework", null, null, null, null);
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("\"type\":");
            assertThat(html).contains("framework");
        }

        @Test
        void nodeJsonIncludesScope() {
            Component c = new Component("pkg", "1.0", "pkg@1.0", "library", null, null, null, "optional");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("\"scope\":");
            assertThat(html).contains("optional");
        }

        @Test
        void nodeJsonIncludesGroup() {
            Component c = new Component("lib", "1.0", "lib@1.0", "library", "@scope", null, null, "required");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("\"group\":");
            assertThat(html).contains("@scope");
        }

        @Test
        void nodeJsonIncludesPurl() {
            Component c = new Component("pkg", "1.0", "pkg@1.0", "library", null, "pkg:npm/pkg@1.0", null, "required");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("\"purl\":");
            assertThat(html).contains("pkg:npm/pkg@1.0");
        }

        @Test
        void nodeJsonIncludesIsRoot() {
            Component c = new Component("pkg", "1.0", "pkg@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("\"isRoot\":");
        }
    }

    @Nested
    class StatsDisplayTests {
        @Test
        void displaysComponentCount() {
            Component c1 = new Component("a", "1.0", "a@1.0");
            Component c2 = new Component("b", "1.0", "b@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c1, c2), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("2 components");
        }

        @Test
        void displaysDependencyCount() {
            Component c1 = new Component("a", "1.0", "a@1.0");
            Component c2 = new Component("b", "1.0", "b@1.0");
            DependencyEdge e = new DependencyEdge("a@1.0", "b@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c1, c2), List.of(e), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("1 dependencies");
        }

        @Test
        void displaysZeroComponentsWhenEmpty() {
            DependencyGraph graph = new DependencyGraph(List.of(), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("0 components");
        }

        @Test
        void displaysZeroDependenciesWhenNone() {
            Component c = new Component("pkg", "1.0", "pkg@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("0 dependencies");
        }
    }

    @Nested
    class EscapingTests {
        @Test
        void escapesJsonSpecialCharactersInId() {
            Component c = new Component("pkg", "1.0", "pkg\"with\"quotes@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("\\\"");
        }

        @Test
        void escapesNewlinesInJson() {
            Component c = new Component("pkg\nwith\nnewlines", "1.0", "pkg@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("\\n");
        }

        @Test
        void escapesBackslashesInJson() {
            Component c = new Component("pkg\\with\\backslash", "1.0", "pkg@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("\\\\");
        }

        @Test
        void escapesHtmlSpecialCharsInTitle() {
            DependencyGraph graph = new DependencyGraph(List.of(), List.of(), null);

            String html = HtmlGraphExporter.export(graph, "<script>alert('xss')</script>");

            assertThat(html).contains("&lt;script&gt;");
            assertThat(html).doesNotContain("<script>");
        }

        @Test
        void escapesAmpersandInTitle() {
            DependencyGraph graph = new DependencyGraph(List.of(), List.of(), null);

            String html = HtmlGraphExporter.export(graph, "Foo & Bar");

            assertThat(html).contains("Foo &amp; Bar");
        }
    }

    @Nested
    class NodeColoringTests {
        @Test
        void rootComponentsHaveDifferentColor() {
            Component root = new Component("root", "1.0", "root@1.0");
            Component dep = new Component("dep", "1.0", "dep@1.0");
            DependencyEdge edge = new DependencyEdge("root@1.0", "dep@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(root, dep), List.of(edge), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("#4A90D9");
        }

        @Test
        void optionalScopeComponentsHaveOtherColor() {
            Component root = new Component("root", "1.0", "root@1.0");
            Component optional = new Component("opt", "1.0", "opt@1.0", "library",
                    null, null, null, "optional");
            DependencyEdge edge = new DependencyEdge("root@1.0", "opt@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(root, optional), List.of(edge), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("#F4D03F");
        }

        @Test
        void requiredComponentsHaveBaseColor() {
            Component root = new Component("root", "1.0", "root@1.0");
            Component required = new Component("req", "1.0", "req@1.0", "library",
                    null, null, null, "required");
            DependencyEdge edge = new DependencyEdge("root@1.0", "req@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(root, required), List.of(edge), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("#A8D08D");
        }
    }

    @Nested
    class SpecialFieldsTests {
        @Test
        void nullGroupIsRenderedAsNull() {
            Component c = new Component("pkg", "1.0", "pkg@1.0", "library", null, null, null, "required");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("\"group\":null");
        }

        @Test
        void nullPurlIsRenderedAsNull() {
            Component c = new Component("pkg", "1.0", "pkg@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("\"purl\":null");
        }

        @Test
        void presentGroupIsQuoted() {
            Component c = new Component("pkg", "1.0", "pkg@1.0", "library", "@org", null, null, "required");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("\"group\":\"@org\"");
        }

        @Test
        void presentPurlIsQuoted() {
            Component c = new Component("pkg", "1.0", "pkg@1.0", "library", null, "pkg:npm/pkg@1.0", null, "required");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("\"purl\":\"pkg:npm/pkg@1.0\"");
        }
    }

    @Nested
    class InteractiveElementsTests {
        @Test
        void includesLegend() {
            DependencyGraph graph = new DependencyGraph(List.of(), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("legend");
        }

        @Test
        void includesControlButtons() {
            DependencyGraph graph = new DependencyGraph(List.of(), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("Reset Zoom");
        }

        @Test
        void includesToolTip() {
            DependencyGraph graph = new DependencyGraph(List.of(), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("tooltip");
        }

        @Test
        void includesD3ForceSimulation() {
            DependencyGraph graph = new DependencyGraph(List.of(), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("forceSimulation");
            assertThat(html).contains("forceLink");
            assertThat(html).contains("forceManyBody");
        }

        @Test
        void includesD3Zoom() {
            DependencyGraph graph = new DependencyGraph(List.of(), List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("d3.zoom()");
        }
    }

    @Nested
    class EdgeCaseTests {
        @Test
        void skipsEdgesWithMissingNodes() {
            Component c = new Component("a", "1.0", "a@1.0");
            DependencyEdge edge = new DependencyEdge("a@1.0", "b@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(edge), null);

            String html = HtmlGraphExporter.export(graph);

            // Should still produce valid HTML
            assertThat(html).contains("<!DOCTYPE html>");
            assertThat(html).contains("const links = ");
        }

        @Test
        void handlesLargeGraph() {
            List<Component> components = new java.util.ArrayList<>();
            for (int i = 0; i < 100; i++) {
                components.add(new Component("pkg" + i, "1.0", "pkg" + i + "@1.0"));
            }
            DependencyGraph graph = new DependencyGraph(components, List.of(), null);

            String html = HtmlGraphExporter.export(graph);

            assertThat(html).contains("100 components");
        }
    }
}
