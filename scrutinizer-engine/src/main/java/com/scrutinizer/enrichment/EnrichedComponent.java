package com.scrutinizer.enrichment;

import com.scrutinizer.model.Component;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

/**
 * A Component enriched with supply-chain signals: OpenSSF Scorecard
 * and SLSA provenance data. Wraps a Component via composition (not inheritance)
 * to preserve the original immutable model.
 */
public final class EnrichedComponent implements Comparable<EnrichedComponent> {

    private static final Comparator<EnrichedComponent> NATURAL_ORDER =
            Comparator.comparing(ec -> ec.component);

    private final Component component;
    private final ScorecardResult scorecardResult;   // nullable
    private final ProvenanceResult provenanceResult;  // nullable

    public EnrichedComponent(Component component, ScorecardResult scorecardResult,
                             ProvenanceResult provenanceResult) {
        this.component = Objects.requireNonNull(component);
        this.scorecardResult = scorecardResult;
        this.provenanceResult = provenanceResult;
    }

    /** Create an enriched component with no signals (not yet enriched). */
    public static EnrichedComponent unenriched(Component component) {
        return new EnrichedComponent(component, null, null);
    }

    /** Create a new EnrichedComponent with updated scorecard data. */
    public EnrichedComponent withScorecard(ScorecardResult scorecard) {
        return new EnrichedComponent(this.component, scorecard, this.provenanceResult);
    }

    /** Create a new EnrichedComponent with updated provenance data. */
    public EnrichedComponent withProvenance(ProvenanceResult provenance) {
        return new EnrichedComponent(this.component, this.scorecardResult, provenance);
    }

    public Component component() { return component; }
    public Optional<ScorecardResult> scorecardResult() { return Optional.ofNullable(scorecardResult); }
    public Optional<ProvenanceResult> provenanceResult() { return Optional.ofNullable(provenanceResult); }

    /** Convenience: does this component have any enrichment data? */
    public boolean hasEnrichment() {
        return scorecardResult != null || provenanceResult != null;
    }

    @Override
    public int compareTo(EnrichedComponent other) {
        return NATURAL_ORDER.compare(this, other);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EnrichedComponent that)) return false;
        return component.equals(that.component)
                && Objects.equals(scorecardResult, that.scorecardResult)
                && Objects.equals(provenanceResult, that.provenanceResult);
    }

    @Override
    public int hashCode() {
        return Objects.hash(component, scorecardResult, provenanceResult);
    }
}
