package com.scrutinizer.engine;

import java.util.Objects;

/**
 * The result of evaluating a single rule against a single component.
 */
public final class RuleResult {

    /** Outcome of rule evaluation. */
    public enum Decision {
        PASS, WARN, FAIL, INFO, SKIP
    }

    private final String componentRef;
    private final String ruleId;
    private final Decision decision;
    private final String actualValue;
    private final String expectedValue;
    private final String description;

    public RuleResult(String componentRef, String ruleId, Decision decision,
                      String actualValue, String expectedValue, String description) {
        this.componentRef = Objects.requireNonNull(componentRef);
        this.ruleId = Objects.requireNonNull(ruleId);
        this.decision = Objects.requireNonNull(decision);
        this.actualValue = actualValue;
        this.expectedValue = expectedValue;
        this.description = description != null ? description : "";
    }

    public String componentRef() { return componentRef; }
    public String ruleId() { return ruleId; }
    public Decision decision() { return decision; }
    public String actualValue() { return actualValue; }
    public String expectedValue() { return expectedValue; }
    public String description() { return description; }

    public boolean isFailing() {
        return decision == Decision.FAIL;
    }

    public boolean isWarning() {
        return decision == Decision.WARN;
    }

    @Override
    public String toString() {
        return "[" + decision + "] " + ruleId + " on " + componentRef
                + " (actual=" + actualValue + ", expected=" + expectedValue + ")";
    }
}
