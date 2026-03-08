package com.scrutinizer.enrichment;

import java.util.Objects;
import java.util.Optional;

/**
 * SLSA provenance detection result for a package.
 * Immutable value object.
 */
public final class ProvenanceResult {

    /** SLSA provenance levels. */
    public enum SlsaLevel {
        SLSA_L1, SLSA_L2, SLSA_L3, SLSA_L4
    }

    private final boolean present;
    private final SlsaLevel level;  // nullable
    private final String source;    // e.g., "npm-sigstore", "in-toto", "github-actions"

    public ProvenanceResult(boolean present, SlsaLevel level, String source) {
        this.present = present;
        this.level = level;
        this.source = source;
    }

    /** Factory for absent provenance. */
    public static ProvenanceResult absent() {
        return new ProvenanceResult(false, null, null);
    }

    /** Factory for detected provenance. */
    public static ProvenanceResult detected(SlsaLevel level, String source) {
        return new ProvenanceResult(true, level, Objects.requireNonNull(source));
    }

    public boolean isPresent() { return present; }
    public Optional<SlsaLevel> level() { return Optional.ofNullable(level); }
    public Optional<String> source() { return Optional.ofNullable(source); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProvenanceResult that)) return false;
        return present == that.present && level == that.level
                && Objects.equals(source, that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(present, level, source);
    }

    @Override
    public String toString() {
        if (!present) return "ProvenanceResult{absent}";
        return "ProvenanceResult{level=" + level + ", source='" + source + "'}";
    }
}
