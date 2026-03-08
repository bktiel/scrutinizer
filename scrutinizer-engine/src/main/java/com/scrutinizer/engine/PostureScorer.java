package com.scrutinizer.engine;

import com.scrutinizer.policy.ScoringConfig;

import java.util.List;

/**
 * Computes aggregate posture scores and overall decisions from rule results.
 */
public final class PostureScorer {

    private PostureScorer() {}

    /**
     * Compute the overall decision for a set of rule results using the scoring config.
     */
    public static RuleResult.Decision computeOverallDecision(
            List<RuleResult> results, ScoringConfig config) {

        // Filter out SKIP and INFO — only evaluate PASS/WARN/FAIL
        List<RuleResult> actionable = results.stream()
                .filter(r -> r.decision() != RuleResult.Decision.SKIP
                        && r.decision() != RuleResult.Decision.INFO)
                .toList();

        if (actionable.isEmpty()) {
            return RuleResult.Decision.PASS;
        }

        return switch (config.method()) {
            case PASS_FAIL -> computePassFail(actionable);
            case WORST_CASE -> computeWorstCase(actionable);
            case WEIGHTED_AVERAGE -> computeWeightedAverage(actionable, config);
        };
    }

    /**
     * PASS_FAIL: Any FAIL -> overall FAIL; any WARN -> overall WARN; else PASS.
     */
    private static RuleResult.Decision computePassFail(List<RuleResult> results) {
        boolean hasFail = results.stream().anyMatch(RuleResult::isFailing);
        if (hasFail) return RuleResult.Decision.FAIL;

        boolean hasWarn = results.stream().anyMatch(RuleResult::isWarning);
        if (hasWarn) return RuleResult.Decision.WARN;

        return RuleResult.Decision.PASS;
    }

    /**
     * WORST_CASE: Overall decision is the worst decision across all results.
     */
    private static RuleResult.Decision computeWorstCase(List<RuleResult> results) {
        return computePassFail(results); // Same logic for WORST_CASE
    }

    /**
     * WEIGHTED_AVERAGE: Compute a numeric score and compare to thresholds.
     * Components with PASS score 10, WARN scores 5, FAIL scores 0.
     */
    private static RuleResult.Decision computeWeightedAverage(
            List<RuleResult> results, ScoringConfig config) {

        double totalScore = 0;
        int count = 0;
        for (RuleResult r : results) {
            double componentScore = switch (r.decision()) {
                case PASS -> 10.0;
                case WARN -> 5.0;
                case FAIL -> 0.0;
                default -> 5.0; // INFO
            };
            totalScore += componentScore;
            count++;
        }

        if (count == 0) return RuleResult.Decision.PASS;

        double avg = totalScore / count;
        if (avg >= config.passThreshold()) return RuleResult.Decision.PASS;
        if (avg >= config.warnThreshold()) return RuleResult.Decision.WARN;
        return RuleResult.Decision.FAIL;
    }

    /**
     * Compute a numeric posture score (0-10 scale) from rule results.
     */
    public static double computeScore(List<RuleResult> results) {
        List<RuleResult> actionable = results.stream()
                .filter(r -> r.decision() != RuleResult.Decision.SKIP
                        && r.decision() != RuleResult.Decision.INFO)
                .toList();

        if (actionable.isEmpty()) return 10.0;

        double total = 0;
        for (RuleResult r : actionable) {
            total += switch (r.decision()) {
                case PASS -> 10.0;
                case WARN -> 5.0;
                case FAIL -> 0.0;
                default -> 5.0;
            };
        }
        return total / actionable.size();
    }
}
