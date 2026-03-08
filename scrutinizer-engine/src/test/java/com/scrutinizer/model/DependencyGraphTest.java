package com.scrutinizer.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyGraphTest {

    private DependencyGraph simpleGraph() {
        Component cA = new Component("a", "1.0", "a@1.0");
        Component cB = new Component("b", "1.0", "b@1.0");
        Component cC = new Component("c", "1.0", "c@1.0");
        DependencyEdge e1 = new DependencyEdge("a@1.0", "b@1.0");
        DependencyEdge e2 = new DependencyEdge("a@1.0", "c@1.0");
        return new DependencyGraph(List.of(cA, cB, cC), List.of(e1, e2), "a@1.0");
    }

    @Test
    void componentCount() {
        assertThat(simpleGraph().componentCount()).isEqualTo(3);
    }

    @Test
    void edgeCount() {
        assertThat(simpleGraph().edgeCount()).isEqualTo(2);
    }

    @Test
    void getComponentByRefFound() {
        var c = simpleGraph().getComponentByRef("b@1.0");
        assertThat(c).isPresent();
        assertThat(c.get().name()).isEqualTo("b");
    }

    @Test
    void getComponentByRefNotFound() {
        assertThat(simpleGraph().getComponentByRef("z@1.0")).isEmpty();
    }

    @Test
    void getDirectDependencies() {
        List<Component> deps = simpleGraph().getDirectDependencies("a@1.0");
        assertThat(deps).hasSize(2);
        assertThat(deps.get(0).name()).isEqualTo("b");
        assertThat(deps.get(1).name()).isEqualTo("c");
    }

    @Test
    void getDirectDependenciesLeaf() {
        List<Component> deps = simpleGraph().getDirectDependencies("b@1.0");
        assertThat(deps).isEmpty();
    }

    @Test
    void getDependents() {
        List<Component> deps = simpleGraph().getDependents("b@1.0");
        assertThat(deps).hasSize(1);
        assertThat(deps.get(0).name()).isEqualTo("a");
    }

    @Test
    void emptyGraph() {
        DependencyGraph g = new DependencyGraph(List.of(), List.of(), null);
        assertThat(g.componentCount()).isEqualTo(0);
        assertThat(g.edgeCount()).isEqualTo(0);
        assertThat(g.rootRef()).isEmpty();
    }

    @Test
    void componentsAreSorted() {
        Component cZ = new Component("z", "1.0", "z@1.0");
        Component cA = new Component("a", "1.0", "a@1.0");
        DependencyGraph g = new DependencyGraph(List.of(cZ, cA), List.of(), null);
        assertThat(g.components().get(0).name()).isEqualTo("a");
        assertThat(g.components().get(1).name()).isEqualTo("z");
    }
}
