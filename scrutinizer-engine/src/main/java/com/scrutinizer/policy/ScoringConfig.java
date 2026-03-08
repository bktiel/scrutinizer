package com.scrutinizer.policy;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for how posture scores are computed from rule results.
 */
public final class ScoringConfig {

    /** Scoring computation methods. */
    public enum Method {
        WEIGHTED_AVERAGE, PASS_FAIL, WORST_CASE
    }

    private final Method method;
    private final Map<String, Double> weights;   // field -> weight
    private final double passThreshold;
    private final double warnThreshold;

    public ScoringConfig(Method method, Map<String, Double> weights,
                         double passThreshold, double warnThreshold) {
        this.method = Objects.requireNonNull(method);
        this.weights = weights != null ? Collections.unmodifiableMap(weights) : Map.of();
        this.passThreshold = passThreshold;
        this.warnThreshold = warnThreshold;
    }

    /** Default scoring: pass_fail with no weights. */
    public static ScoringConfig defaultConfig() {
        return new ScoringConfig(Method.PASS_FAIL, Map.of(), 7.0, 4.0);
    }

    public Method method() { return method; }
    public Map<String, Double> weights() { return weights; }
    public double passThreshold() { return passThreshold; }
    public double warnThreshold() { return warnThreshold; }
}
