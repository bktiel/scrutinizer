package com.scrutinizer.engine;

import com.scrutinizer.enrichment.EnrichedComponent;
import com.scrutinizer.policy.Rule;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Evaluates a single Rule against an EnrichedComponent, producing a RuleResult.
 */
@Service
public class RuleEvaluator {

    /**
     * Evaluate a rule against a component. Returns the result with
     * decision, actual value, and expected value.
     */
    public RuleResult evaluate(Rule rule, EnrichedComponent component) {
        String bomRef = component.component().bomRef();

        // SKIP rules: if the field matches the value, skip this component
        if (rule.severity() == Rule.Severity.SKIP) {
            Optional<String> actual = FieldExtractor.extract(component, rule.field());
            if (actual.isPresent() && actual.get().equals(rule.value())) {
                return new RuleResult(bomRef, rule.id(), RuleResult.Decision.SKIP,
                        actual.orElse(null), rule.value(), rule.description());
            }
            // Field doesn't match SKIP condition — treat as pass
            return new RuleResult(bomRef, rule.id(), RuleResult.Decision.PASS,
                    actual.orElse(null), rule.value(), rule.description());
        }

        // EXISTS operator: just check if field is present
        if (rule.operator() == Rule.Operator.EXISTS) {
            Optional<String> actual = FieldExtractor.extract(component, rule.field());
            RuleResult.Decision decision = actual.isPresent()
                    ? RuleResult.Decision.PASS
                    : mapSeverity(rule.severity());
            return new RuleResult(bomRef, rule.id(), decision,
                    actual.orElse("(absent)"), "(exists)", rule.description());
        }

        // Extract field value
        Optional<String> actualOpt = FieldExtractor.extract(component, rule.field());
        if (actualOpt.isEmpty()) {
            // Field not available — can't evaluate, return INFO
            return new RuleResult(bomRef, rule.id(), RuleResult.Decision.INFO,
                    "(no data)", rule.value(), rule.description());
        }

        String actual = actualOpt.get();
        boolean matches = compareValues(actual, rule.value(), rule.operator());

        // Ban-style rules: field=name with EQ/IN operators define a blocklist.
        // A match means the component IS the banned package → FAIL.
        // A non-match means the component is fine → PASS.
        boolean isBanRule = "name".equals(rule.field())
                && (rule.operator() == Rule.Operator.EQ || rule.operator() == Rule.Operator.IN);

        boolean passes;
        if (isBanRule) {
            passes = !matches;  // match = violation, non-match = pass
        } else {
            passes = matches;   // standard: match = pass, non-match = violation
        }

        RuleResult.Decision decision = passes
                ? RuleResult.Decision.PASS
                : mapSeverity(rule.severity());

        return new RuleResult(bomRef, rule.id(), decision,
                actual, rule.value(), rule.description());
    }

    private boolean compareValues(String actual, String expected, Rule.Operator operator) {
        switch (operator) {
            case EQ:
                return actual.equalsIgnoreCase(expected);
            case NEQ:
                return !actual.equalsIgnoreCase(expected);
            case GT:
            case GTE:
            case LT:
            case LTE:
                return compareNumeric(actual, expected, operator);
            case IN:
                // expected is comma-separated list
                for (String val : expected.split(",")) {
                    if (actual.equalsIgnoreCase(val.trim())) return true;
                }
                return false;
            case NOT_IN:
                for (String val : expected.split(",")) {
                    if (actual.equalsIgnoreCase(val.trim())) return false;
                }
                return true;
            default:
                return false;
        }
    }

    private boolean compareNumeric(String actual, String expected, Rule.Operator operator) {
        try {
            double a = Double.parseDouble(actual);
            double e = Double.parseDouble(expected);
            return switch (operator) {
                case GT -> a > e;
                case GTE -> a >= e;
                case LT -> a < e;
                case LTE -> a <= e;
                default -> false;
            };
        } catch (NumberFormatException ex) {
            // Non-numeric comparison falls back to string comparison
            int cmp = actual.compareTo(expected);
            return switch (operator) {
                case GT -> cmp > 0;
                case GTE -> cmp >= 0;
                case LT -> cmp < 0;
                case LTE -> cmp <= 0;
                default -> false;
            };
        }
    }

    private RuleResult.Decision mapSeverity(Rule.Severity severity) {
        return switch (severity) {
            case FAIL -> RuleResult.Decision.FAIL;
            case WARN -> RuleResult.Decision.WARN;
            case INFO -> RuleResult.Decision.INFO;
            case SKIP -> RuleResult.Decision.SKIP;
        };
    }
}
