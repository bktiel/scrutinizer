package com.scrutinizer.model;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

/**
 * A single software component extracted from an SBOM.
 * Immutable and naturally ordered by name, then version, then bomRef
 * to guarantee deterministic iteration.
 */
public final class Component implements Comparable<Component> {

    private static final Comparator<Component> NATURAL_ORDER = Comparator
            .comparing(Component::name)
            .thenComparing(Component::version)
            .thenComparing(Component::bomRef);

    private final String name;
    private final String version;
    private final String bomRef;
    private final String type;
    private final String group;   // nullable
    private final String purl;    // nullable
    private final String description; // nullable
    private final String scope;

    public Component(String name, String version, String bomRef, String type,
                     String group, String purl, String description, String scope) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.version = Objects.requireNonNull(version, "version must not be null");
        this.bomRef = Objects.requireNonNull(bomRef, "bomRef must not be null");
        this.type = type != null ? type : "library";
        this.group = group;
        this.purl = purl;
        this.description = description;
        this.scope = scope != null ? scope : "required";
    }

    public Component(String name, String version, String bomRef) {
        this(name, version, bomRef, "library", null, null, null, "required");
    }

    public String name() { return name; }
    public String version() { return version; }
    public String bomRef() { return bomRef; }
    public String type() { return type; }
    public Optional<String> group() { return Optional.ofNullable(group); }
    public Optional<String> purl() { return Optional.ofNullable(purl); }
    public Optional<String> description() { return Optional.ofNullable(description); }
    public String scope() { return scope; }

    /** Returns display name: "group/name" if group is present, otherwise just "name". */
    public String displayName() {
        return group != null ? group + "/" + name : name;
    }

    @Override
    public int compareTo(Component other) {
        return NATURAL_ORDER.compare(this, other);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Component c)) return false;
        return name.equals(c.name) && version.equals(c.version) && bomRef.equals(c.bomRef)
                && type.equals(c.type) && Objects.equals(group, c.group)
                && Objects.equals(purl, c.purl) && Objects.equals(description, c.description)
                && scope.equals(c.scope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version, bomRef, type, group, purl, description, scope);
    }

    @Override
    public String toString() {
        return "Component{name='" + name + "', version='" + version + "', bomRef='" + bomRef + "'}";
    }
}
