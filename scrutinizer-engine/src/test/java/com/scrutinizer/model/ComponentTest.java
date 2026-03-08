package com.scrutinizer.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComponentTest {

    @Test
    void creationWithDefaults() {
        Component c = new Component("lodash", "4.17.21", "lodash@4.17.21");
        assertThat(c.name()).isEqualTo("lodash");
        assertThat(c.version()).isEqualTo("4.17.21");
        assertThat(c.bomRef()).isEqualTo("lodash@4.17.21");
        assertThat(c.type()).isEqualTo("library");
        assertThat(c.group()).isEmpty();
        assertThat(c.purl()).isEmpty();
        assertThat(c.description()).isEmpty();
        assertThat(c.scope()).isEqualTo("required");
    }

    @Test
    void creationWithAllFields() {
        Component c = new Component("utils", "1.0.0", "@scope/utils@1.0.0",
                "framework", "@scope", "pkg:npm/%40scope/utils@1.0.0",
                "Utility library", "optional");
        assertThat(c.group()).hasValue("@scope");
        assertThat(c.type()).isEqualTo("framework");
        assertThat(c.description()).hasValue("Utility library");
        assertThat(c.scope()).isEqualTo("optional");
    }

    @Test
    void ordering() {
        Component cB = new Component("b-lib", "1.0", "b@1.0");
        Component cA = new Component("a-lib", "1.0", "a@1.0");
        List<Component> sorted = Arrays.asList(cB, cA);
        sorted.sort(null);
        assertThat(sorted).containsExactly(cA, cB);
    }

    @Test
    void equalityAndHashing() {
        Component c1 = new Component("x", "1.0", "x@1.0");
        Component c2 = new Component("x", "1.0", "x@1.0");
        assertThat(c1).isEqualTo(c2);
        assertThat(c1.hashCode()).isEqualTo(c2.hashCode());
    }

    @Test
    void inequality() {
        Component c1 = new Component("x", "1.0", "x@1.0");
        Component c2 = new Component("x", "2.0", "x@2.0");
        assertThat(c1).isNotEqualTo(c2);
    }

    @Test
    void displayNameWithGroup() {
        Component c = new Component("utils", "1.0", "ref",
                "library", "@scope", null, null, "required");
        assertThat(c.displayName()).isEqualTo("@scope/utils");
    }

    @Test
    void displayNameWithoutGroup() {
        Component c = new Component("utils", "1.0", "ref");
        assertThat(c.displayName()).isEqualTo("utils");
    }
}
