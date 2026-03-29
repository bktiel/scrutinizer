package com.scrutinizer.engine;

import com.scrutinizer.policy.Rule;
import com.scrutinizer.policy.ScoringConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PostureScorer {

    private PostureScorer() {}

    public static RuleResult.Decision computeOverallDecision(
            List<RuleResult> results, ScoringConfig config) {
        return computeOverallDecision(results, config, List.of());
    }

    public static RuleResult.Decision computeOverallDecision(
            List<RuleResult> results, ScoringConfig config, List<Rule> rules) {

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
            case WEIGHTED_AVERAGE -> computeWeightedAverage(actionable, config, rules);
        };
    }

    private static RuleResult.Decision computePassFail(List<RuleResult> results) {
        boolean hasFail = results.stream().anyMatch(RuleResult::isFailing);
        if (hasFail) return RuleResult.Decision.FAIL;

        boolean hasWarn = results.stream().anyMatch(RuleResult::isWarning);
        if (hasWarn) return RuleResult.Decision.WARN;

        return RuleResult.Decision.PASS;
    }

    private static RuleResult.Decision computeWorstCase(List<RuleResult> results) {
        return computePassFail(results);
    }

    private static RuleResult.Decision computeWeightedAverage(
            List<RuleResult> results, ScoringConfig config, List<Rule> rules) {

        Map<String, String> ruleIdToField = new HashMap<>();
        for (Rule rule : rules) {
            ruleIdToField.put(rule.id(), rule.field());
        }

        Map<String, Double> weights = config.weights();
        boolean hasWeights = !weights.isEmpty();

        double weightedTotal = 0;
        double totalWeight = 0;
        double unweightedTotal = 0;
        int unweightedCount = 0;

        for (RuleResult r : results) {
            double decisionScore = decisionToScore(r.decision());

            if (hasWeights) {
                String field = ruleIdToField.get(r.ruleId());
                Double weight = field != null ? weights.get(field) : null;

                if (weight != null) {
                    weightedTotal += decisionScore * weight;
                    totalWeight += weight;
                } else {
                    unweightedTotal += decisionScore;
                    unweightedCount++;
                }
            } else {
                unweightedTotal += decisionScore;
                unweightedCount++;
            }
        }

        double avg;
        if (hasWeights && totalWeight > 0) {
            double combinedTotal = weightedTotal;
            double combinedWeight = totalWeight;
            if (unweightedCount > 0) {
                double remainingWeight = Math.max(0, 1.0 - totalWeight);
                double perItemWeight = unweightedCount > 0 ? remainingWeight / unweightedCount : 0;
                for (int i = 0; i < unweightedCount; i++) {
                    combinedWeight += perItemWeight;
                }
                combinedTotal += (unweightedTotal / unweightedCount) * remainingWeight;
            }
            avg = combinedWeight > 0 ? combinedTotal / combinedWeight : 0;
        } else if (unweightedCount > 0) {
            avg = unweightedTotal / unweightedCount;
        } else {
            return RuleResult.Decision.PASS;
        }

        if (avg >= config.passThreshold()) return RuleResult.Decision.PASS;
        if (avg >= config.warnThreshold()) return RuleResult.Decision.WARN;
        return RuleResult.Decision.FAIL;
    }

    public static double computeScore(List<RuleResult> results) {
        List<RuleResult> actionable = results.stream()
                .filter(r -> r.decision() != RuleResult.Decision.SKIP
                        && r.decision() != RuleResult.Decision.INFO)
                .toList();

        if (actionable.isEmpty()) return 10.0;

        double total = 0;
        for (RuleResult r : actionable) {
            total += decisionToScore(r.decision());
        }
        return total / actionable.size();
    }

    private static double decisionToScore(RuleResult.Decision decision) {
        return switch (decision) {
            case PASS -> 10.0;
            case WARN -> 5.0;
            case FAIL -> 0.0;
            default -> 5.0;
        };
    }
}
