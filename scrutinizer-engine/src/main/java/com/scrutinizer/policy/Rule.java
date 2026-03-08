package com.scrutinizer.policy;

import java.util.Objects;

/**
 * A single policy rule that evaluates a field on an enriched component
 * against an expected value using an operator.
 */
public final class Rule {

    /** Supported comparison operators. */
    public enum Operator {
        EQ, NEQ, GT, GTE, LT, LTE, IN, NOT_IN, EXISTS
    }

    /** Severity / action when a rule triggers. */
    public enum Severity {
        FAIL, WARN, INFO, SKIP
    }

    private final String id;
    private final String description;
    private final String field;
    private final Operator operator;
    private final String value;    // string representation of threshold/expected value
    private final Severity severity;

    public Rule(String id, String description, String field,
                Operator operator, String value, Severity severity) {
        this.id = Objects.requireNonNull(id);
        this.description = description != null ? description : "";
        this.field = Objects.requireNonNull(field);
        this.operator = Objects.requireNonNull(operator);
        this.value = value; // can be null for EXISTS operator
        this.severity = Objects.requireNonNull(severity);
    }

    public String id() { return id; }
    public String description() { return description; }
    public String field() { return field; }
    public Operator operator() { return operator; }
    public String value() { return value; }
    public Severity severity() { return severity; }

    @Override
    public String toString() {
        return "Rule{id='" + id + "', field='" + field + "' " + operator + " '" + value + "'}";
    }
}
