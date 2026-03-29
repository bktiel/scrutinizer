package com.scrutinizer.engine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Finding {

    private final String id;
    private final String componentRef;
    private final String componentName;
    private final String ruleId;
    private final RuleResult.Decision decision;
    private final String severity;
    private final String field;
    private final String actualValue;
    private final String expectedValue;
    private final String description;
    private final String remediation;
    private final boolean isDirect;
    private final int depthFromRoot;
    private final List<String> evidenceChain;

    public Finding(String id, String componentRef, String componentName, String ruleId,
                   RuleResult.Decision decision, String severity, String field,
                   String actualValue, String expectedValue, String description,
                   String remediation, boolean isDirect, int depthFromRoot,
                   List<String> evidenceChain) {
        this.id = Objects.requireNonNull(id);
        this.componentRef = Objects.requireNonNull(componentRef);
        this.componentName = Objects.requireNonNull(componentName);
        this.ruleId = Objects.requireNonNull(ruleId);
        this.decision = Objects.requireNonNull(decision);
        this.severity = Objects.requireNonNull(severity);
        this.field = Objects.requireNonNull(field);
        this.actualValue = actualValue;
        this.expectedValue = expectedValue;
        this.description = description != null ? description : "";
        this.remediation = remediation != null ? remediation : "";
        this.isDirect = isDirect;
        this.depthFromRoot = depthFromRoot;
        this.evidenceChain = evidenceChain != null
                ? Collections.unmodifiableList(evidenceChain) : List.of();
    }

    public String id() { return id; }
    public String componentRef() { return componentRef; }
    public String componentName() { return componentName; }
    public String ruleId() { return ruleId; }
    public RuleResult.Decision decision() { return decision; }
    public String severity() { return severity; }
    public String field() { return field; }
    public String actualValue() { return actualValue; }
    public String expectedValue() { return expectedValue; }
    public String description() { return description; }
    public String remediation() { return remediation; }
    public boolean isDirect() { return isDirect; }
    public int depthFromRoot() { return depthFromRoot; }
    public List<String> evidenceChain() { return evidenceChain; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("componentRef", componentRef);
        map.put("componentName", componentName);
        map.put("ruleId", ruleId);
        map.put("decision", decision.name());
        map.put("severity", severity);
        map.put("field", field);
        map.put("actualValue", actualValue);
        map.put("expectedValue", expectedValue);
        map.put("description", description);
        map.put("remediation", remediation);
        map.put("isDirect", isDirect);
        map.put("depthFromRoot", depthFromRoot);
        map.put("evidenceChain", evidenceChain);
        return map;
    }
}
