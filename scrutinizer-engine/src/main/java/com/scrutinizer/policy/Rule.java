package com.scrutinizer.policy;

import java.util.Objects;

/**
 * A single policy rule that evaluates a field on an enriched component
 * against an expected value using an operator.
 */
public final class Rule {

    public enum Operator {
        EQ, NEQ, GT, GTE, LT, LTE, IN, NOT_IN, EXISTS
    }

    public enum Severity {
        FAIL, WARN, INFO, SKIP
    }

    public enum Target {
        ALL, DIRECT, TRANSITIVE
    }

    private final String id;
    private final String description;
    private final String field;
    private final Operator operator;
    private final String value;
    private final Severity severity;
    private final Target target;
    private final String ecosystem;

    public Rule(String id, String description, String field,
                Operator operator, String value, Severity severity) {
        this(id, description, field, operator, value, severity, Target.ALL, null);
    }

    public Rule(String id, String description, String field,
                Operator operator, String value, Severity severity, Target target) {
        this(id, description, field, operator, value, severity, target, null);
    }

    public Rule(String id, String description, String field,
                Operator operator, String value, Severity severity, Target target, String ecosystem) {
        this.id = Objects.requireNonNull(id);
        this.description = description != null ? description : "";
        this.field = Objects.requireNonNull(field);
        this.operator = Objects.requireNonNull(operator);
        this.value = value;
        this.severity = Objects.requireNonNull(severity);
        this.target = target != null ? target : Target.ALL;
        this.ecosystem = ecosystem;
    }

    public String id() { return id; }
    public String description() { return description; }
    public String field() { return field; }
    public Operator operator() { return operator; }
    public String value() { return value; }
    public Severity severity() { return severity; }
    public Target target() { return target; }
    public String ecosystem() { return ecosystem; }

    @Override
    public String toString() {
        return "Rule{id='" + id + "', field='" + field + "' " + operator + " '" + value + "'}";
    }
}
