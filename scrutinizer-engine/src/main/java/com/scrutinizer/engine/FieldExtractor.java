package com.scrutinizer.engine;

import com.scrutinizer.enrichment.EnrichedComponent;

import java.util.Optional;

/**
 * Extracts field values from an EnrichedComponent using dot-notation paths.
 *
 * Supported fields:
 * - name, version, type, scope, group, purl, bomRef
 * - scorecard.score
 * - scorecard.checks.{CheckName}
 * - provenance.present
 * - provenance.level
 * - provenance.source
 */
public final class FieldExtractor {

    private FieldExtractor() {}

    /**
     * Extract a field value as a string from an enriched component.
     * Returns empty if the field is not present or the path is invalid.
     */
    public static Optional<String> extract(EnrichedComponent ec, String fieldPath) {
        if (fieldPath == null) return Optional.empty();

        // Component-level fields
        switch (fieldPath) {
            case "name":
                return Optional.of(ec.component().name());
            case "version":
                return Optional.of(ec.component().version());
            case "type":
                return Optional.of(ec.component().type());
            case "scope":
                return Optional.of(ec.component().scope());
            case "group":
                return ec.component().group();
            case "purl":
                return ec.component().purl();
            case "bomRef":
                return Optional.of(ec.component().bomRef());
        }

        // Scorecard fields
        if (fieldPath.equals("scorecard.score")) {
            return ec.scorecardResult()
                    .map(sr -> String.valueOf(sr.overallScore()));
        }
        if (fieldPath.startsWith("scorecard.checks.")) {
            String checkName = fieldPath.substring("scorecard.checks.".length());
            return ec.scorecardResult()
                    .flatMap(sr -> Optional.ofNullable(sr.checkScores().get(checkName)))
                    .map(String::valueOf);
        }

        // Provenance fields
        if (fieldPath.equals("provenance.present")) {
            return ec.provenanceResult()
                    .map(pr -> String.valueOf(pr.isPresent()));
        }
        if (fieldPath.equals("provenance.level")) {
            return ec.provenanceResult()
                    .flatMap(pr -> pr.level())
                    .map(Enum::name);
        }
        if (fieldPath.equals("provenance.source")) {
            return ec.provenanceResult()
                    .flatMap(pr -> pr.source());
        }

        return Optional.empty();
    }
}
