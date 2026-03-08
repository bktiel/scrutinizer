package com.scrutinizer.enrichment;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Holds OpenSSF Scorecard results for a repository.
 * Immutable value object.
 */
public final class ScorecardResult {

    private final double overallScore;
    private final Map<String, Double> checkScores;
    private final String repoUrl;
    private final Instant retrievedAt;

    public ScorecardResult(double overallScore, Map<String, Double> checkScores,
                           String repoUrl, Instant retrievedAt) {
        this.overallScore = overallScore;
        this.checkScores = Collections.unmodifiableMap(Objects.requireNonNull(checkScores));
        this.repoUrl = Objects.requireNonNull(repoUrl);
        this.retrievedAt = Objects.requireNonNull(retrievedAt);
    }

    public double overallScore() { return overallScore; }
    public Map<String, Double> checkScores() { return checkScores; }
    public String repoUrl() { return repoUrl; }
    public Instant retrievedAt() { return retrievedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScorecardResult that)) return false;
        return Double.compare(overallScore, that.overallScore) == 0
                && repoUrl.equals(that.repoUrl)
                && checkScores.equals(that.checkScores);
    }

    @Override
    public int hashCode() {
        return Objects.hash(overallScore, repoUrl, checkScores);
    }

    @Override
    public String toString() {
        return "ScorecardResult{score=" + overallScore + ", repo='" + repoUrl + "'}";
    }
}
