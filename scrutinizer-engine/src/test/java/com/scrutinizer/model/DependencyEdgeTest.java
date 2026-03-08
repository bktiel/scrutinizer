package com.scrutinizer.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyEdgeTest {

    @Test
    void creation() {
        DependencyEdge e = new DependencyEdge("a@1.0", "b@1.0");
        assertThat(e.sourceRef()).isEqualTo("a@1.0");
        assertThat(e.targetRef()).isEqualTo("b@1.0");
    }

    @Test
    void ordering() {
        DependencyEdge e1 = new DependencyEdge("b@1.0", "c@1.0");
        DependencyEdge e2 = new DependencyEdge("a@1.0", "d@1.0");
        List<DependencyEdge> sorted = Arrays.asList(e1, e2);
        sorted.sort(null);
        assertThat(sorted).containsExactly(e2, e1);
    }

    @Test
    void equality() {
        DependencyEdge e1 = new DependencyEdge("a", "b");
        DependencyEdge e2 = new DependencyEdge("a", "b");
        assertThat(e1).isEqualTo(e2);
        assertThat(e1.hashCode()).isEqualTo(e2.hashCode());
    }
}
