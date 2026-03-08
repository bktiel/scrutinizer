package com.scrutinizer.model;

import java.util.Comparator;
import java.util.Objects;

/**
 * A directed edge: source depends on target.
 * Immutable and naturally ordered by sourceRef, then targetRef.
 */
public final class DependencyEdge implements Comparable<DependencyEdge> {

    private static final Comparator<DependencyEdge> NATURAL_ORDER = Comparator
            .comparing(DependencyEdge::sourceRef)
            .thenComparing(DependencyEdge::targetRef);

    private final String sourceRef;
    private final String targetRef;

    public DependencyEdge(String sourceRef, String targetRef) {
        this.sourceRef = Objects.requireNonNull(sourceRef, "sourceRef must not be null");
        this.targetRef = Objects.requireNonNull(targetRef, "targetRef must not be null");
    }

    public String sourceRef() { return sourceRef; }
    public String targetRef() { return targetRef; }

    @Override
    public int compareTo(DependencyEdge other) {
        return NATURAL_ORDER.compare(this, other);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DependencyEdge e)) return false;
        return sourceRef.equals(e.sourceRef) && targetRef.equals(e.targetRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceRef, targetRef);
    }

    @Override
    public String toString() {
        return "DependencyEdge{" + sourceRef + " -> " + targetRef + "}";
    }
}
