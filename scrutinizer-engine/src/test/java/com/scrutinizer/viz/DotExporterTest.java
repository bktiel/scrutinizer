package com.scrutinizer.viz;

import com.scrutinizer.model.Component;
import com.scrutinizer.model.DependencyEdge;
import com.scrutinizer.model.DependencyGraph;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DotExporterTest {

    @Nested
    class BasicExportTests {
        @Test
        void exportsEmptyGraph() {
            DependencyGraph graph = new DependencyGraph(List.of(), List.of(), null);

            String dot = DotExporter.export(graph);

            assertThat(dot).contains("digraph dependencies");
            assertThat(dot).contains("rankdir=LR");
        }

        @Test
        void exportsSingleNode() {
            Component c = new Component("pkg", "1.0", "pkg@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String dot = DotExporter.export(graph);

            assertThat(dot).contains("pkg@1.0");
            assertThat(dot).contains("pkg");
            assertThat(dot).contains("1.0");
        }

        @Test
        void exportsMultipleNodes() {
            Component c1 = new Component("pkg1", "1.0", "pkg1@1.0");
            Component c2 = new Component("pkg2", "2.0", "pkg2@2.0");
            DependencyGraph graph = new DependencyGraph(List.of(c1, c2), List.of(), null);

            String dot = DotExporter.export(graph);

            assertThat(dot).contains("pkg1");
            assertThat(dot).contains("pkg2");
            assertThat(dot).contains("1.0");
            assertThat(dot).contains("2.0");
        }

        @Test
        void exportsEdge() {
            Component c1 = new Component("a", "1.0", "a@1.0");
            Component c2 = new Component("b", "1.0", "b@1.0");
            DependencyEdge edge = new DependencyEdge("a@1.0", "b@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c1, c2), List.of(edge), null);

            String dot = DotExporter.export(graph);

            assertThat(dot).contains("a@1.0").contains("b@1.0");
            assertThat(dot).contains("->");
        }

        @Test
        void exportsMultipleEdges() {
            Component c1 = new Component("a", "1.0", "a@1.0");
            Component c2 = new Component("b", "1.0", "b@1.0");
            Component c3 = new Component("c", "1.0", "c@1.0");
            DependencyEdge e1 = new DependencyEdge("a@1.0", "b@1.0");
            DependencyEdge e2 = new DependencyEdge("b@1.0", "c@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c1, c2, c3), List.of(e1, e2), null);

            String dot = DotExporter.export(graph);

            assertThat(dot).contains("a@1.0").contains("b@1.0").contains("c@1.0");
        }
    }

    @Nested
    class FormattingTests {
        @Test
        void startsWithDigraphDeclaration() {
            DependencyGraph graph = new DependencyGraph(List.of(), List.of(), null);

            String dot = DotExporter.export(graph);

            assertThat(dot).startsWith("digraph dependencies");
        }

        @Test
        void endsWithClosingBrace() {
            DependencyGraph graph = new DependencyGraph(List.of(), List.of(), null);

            String dot = DotExporter.export(graph);

            assertThat(dot).endsWith("}\n");
        }

        @Test
        void hasRankdirLr() {
            DependencyGraph graph = new DependencyGraph(List.of(), List.of(), null);

            String dot = DotExporter.export(graph);

            assertThat(dot).contains("rankdir=LR");
        }

        @Test
        void hasNodeShapeBox() {
            DependencyGraph graph = new DependencyGraph(List.of(), List.of(), null);

            String dot = DotExporter.export(graph);

            assertThat(dot).contains("shape=box");
        }

        @Test
        void hasNodeStyleFilled() {
            DependencyGraph graph = new DependencyGraph(List.of(), List.of(), null);

            String dot = DotExporter.export(graph);

            assertThat(dot).contains("style=filled");
        }

        @Test
        void includesNodeLabels() {
            Component c = new Component("axios", "1.0", "axios@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String dot = DotExporter.export(graph);

            assertThat(dot).contains("label=");
            assertThat(dot).contains("axios");
        }

        @Test
        void nodeLabelsIncludeVersionWithNewline() {
            Component c = new Component("axios", "1.0", "axios@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String dot = DotExporter.export(graph);

            assertThat(dot).contains("\\n");
        }
    }

    @Nested
    class NodeColoringTests {
        @Test
        void rootNodesHaveRootColor() {
            Component c = new Component("root", "1.0", "root@1.0", "library",
                    null, null, null, "required");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String dot = DotExporter.export(graph);

            assertThat(dot).contains("#4A90D9"); // root color
        }

        @Test
        void requiredScopeHasRequiredColor() {
            Component root = new Component("root", "1.0", "root@1.0");
            Component dep = new Component("dep", "1.0", "dep@1.0", "library",
                    null, null, null, "required");
            DependencyEdge edge = new DependencyEdge("root@1.0", "dep@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(root, dep), List.of(edge), null);

            String dot = DotExporter.export(graph);

            assertThat(dot).contains("#A8D08D"); // required color
        }

        @Test
        void optionalScopeHasOptionalColor() {
            Component root = new Component("root", "1.0", "root@1.0");
            Component dep = new Component("dep", "1.0", "dep@1.0", "library",
                    null, null, null, "optional");
            DependencyEdge edge = new DependencyEdge("root@1.0", "dep@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(root, dep), List.of(edge), null);

            String dot = DotExporter.export(graph);

            assertThat(dot).contains("#F4D03F"); // optional color
        }

        @Test
        void excludedScopeHasExcludedColor() {
            Component root = new Component("root", "1.0", "root@1.0");
            Component dep = new Component("dep", "1.0", "dep@1.0", "library",
                    null, null, null, "excluded");
            DependencyEdge edge = new DependencyEdge("root@1.0", "dep@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(root, dep), List.of(edge), null);

            String dot = DotExporter.export(graph);

            assertThat(dot).contains("#E74C3C"); // excluded color
        }
    }

    @Nested
    class CustomOptionsTests {
        @Test
        void exportsWithCustomOptions() {
            Component c = new Component("pkg", "1.0", "pkg@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);
            DotExporter.DotOptions options = new DotExporter.DotOptions(
                    "#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#FF00FF");

            String dot = DotExporter.export(graph, options);

            assertThat(dot).contains("#FF0000");
            assertThat(dot).contains("#00FF00");
        }

        @Test
        void defaultOptionsAreApplied() {
            Component c = new Component("pkg", "1.0", "pkg@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String dot1 = DotExporter.export(graph);
            String dot2 = DotExporter.export(graph, DotExporter.DotOptions.defaults());

            assertThat(dot1).isEqualTo(dot2);
        }

        @Test
        void customColorOverridesDefaults() {
            Component root = new Component("root", "1.0", "root@1.0");
            Component dep = new Component("dep", "1.0", "dep@1.0", "library",
                    null, null, null, "required");
            DependencyEdge edge = new DependencyEdge("root@1.0", "dep@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(root, dep), List.of(edge), null);

            DotExporter.DotOptions custom = new DotExporter.DotOptions(
                    "#CUSTOM1", "#CUSTOM2", "#CUSTOM3", "#CUSTOM4", "#CUSTOM5");
            String dot = DotExporter.export(graph, custom);

            assertThat(dot).contains("#CUSTOM2"); // required color
            assertThat(dot).doesNotContain("#A8D08D"); // default required color
        }
    }

    @Nested
    class SanitizationTests {
        @Test
        void sanitizesSpecialCharactersInBomRef() {
            Component c = new Component("pkg", "1.0", "pkg\"with\"quotes@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String dot = DotExporter.export(graph);

            assertThat(dot).contains("\\\"");
        }

        @Test
        void quotesNodeIdentifiers() {
            Component c = new Component("pkg", "1.0", "pkg@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String dot = DotExporter.export(graph);

            // Node IDs should be quoted
            assertThat(dot).contains("\"");
        }

        @Test
        void escapesQuotesInLabels() {
            Component c = new Component("pkg\"name", "1.0", "pkg@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String dot = DotExporter.export(graph);

            assertThat(dot).isNotNull();
        }
    }

    @Nested
    class EdgeCaseTests {
        @Test
        void handlesComponentsWithGroup() {
            Component c = new Component("utils", "1.0", "utils@1.0", "library",
                    "@scope", null, null, "required");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String dot = DotExporter.export(graph);

            assertThat(dot).contains("@scope/utils");
        }

        @Test
        void skipsEdgesWithMissingSourceNode() {
            Component c = new Component("b", "1.0", "b@1.0");
            DependencyEdge edge = new DependencyEdge("a@1.0", "b@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(edge), null);

            String dot = DotExporter.export(graph);

            // Should not contain the invalid edge
            assertThat(dot).contains("b@1.0");
        }

        @Test
        void skipsEdgesWithMissingTargetNode() {
            Component c = new Component("a", "1.0", "a@1.0");
            DependencyEdge edge = new DependencyEdge("a@1.0", "b@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(edge), null);

            String dot = DotExporter.export(graph);

            assertThat(dot).contains("a@1.0");
        }

        @Test
        void handlesLargeGraph() {
            List<Component> components = new java.util.ArrayList<>();
            for (int i = 0; i < 100; i++) {
                components.add(new Component("pkg" + i, "1.0", "pkg" + i + "@1.0"));
            }
            DependencyGraph graph = new DependencyGraph(components, List.of(), null);

            String dot = DotExporter.export(graph);

            assertThat(dot).contains("digraph");
            for (int i = 0; i < 100; i++) {
                assertThat(dot).contains("pkg" + i);
            }
        }
    }

    @Nested
    class DisplayNameTests {
        @Test
        void usesDisplayNameForNodeLabel() {
            Component c = new Component("lib", "1.0", "lib@1.0", "library",
                    "@org", null, null, "required");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String dot = DotExporter.export(graph);

            assertThat(dot).contains("@org/lib");
        }

        @Test
        void usesNameWhenNoGroup() {
            Component c = new Component("lib", "1.0", "lib@1.0");
            DependencyGraph graph = new DependencyGraph(List.of(c), List.of(), null);

            String dot = DotExporter.export(graph);

            assertThat(dot).contains("lib");
            assertThat(dot).doesNotContain("/lib");
        }
    }
}
