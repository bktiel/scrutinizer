package com.scrutinizer.api.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "finding")
public class FindingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_result_id", nullable = false)
    private ComponentResultEntity componentResult;

    @Column(nullable = false)
    private String ruleId;

    @Column(nullable = false, length = 10)
    private String decision;

    @Column(nullable = false, length = 10)
    private String severity;

    @Column(nullable = false)
    private String field;

    private String actualValue;
    private String expectedValue;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String remediation;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public ComponentResultEntity getComponentResult() { return componentResult; }
    public void setComponentResult(ComponentResultEntity componentResult) { this.componentResult = componentResult; }
    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getField() { return field; }
    public void setField(String field) { this.field = field; }
    public String getActualValue() { return actualValue; }
    public void setActualValue(String actualValue) { this.actualValue = actualValue; }
    public String getExpectedValue() { return expectedValue; }
    public void setExpectedValue(String expectedValue) { this.expectedValue = expectedValue; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRemediation() { return remediation; }
    public void setRemediation(String remediation) { this.remediation = remediation; }
}
