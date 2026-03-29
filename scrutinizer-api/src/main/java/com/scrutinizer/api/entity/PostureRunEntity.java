package com.scrutinizer.api.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "posture_run")
public class PostureRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String applicationName;

    @Column(nullable = false, length = 64)
    private String sbomHash;

    @Column(nullable = false)
    private String policyName;

    @Column(nullable = false)
    private String policyVersion;

    @Column(nullable = false, length = 10)
    private String overallDecision;

    @Column(nullable = false)
    private double postureScore;

    @Column(columnDefinition = "jsonb")
    private String summaryJson;

    @Column(nullable = false)
    private Instant runTimestamp;

    @Column(nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "postureRun", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("componentRef ASC")
    private List<ComponentResultEntity> componentResults = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (runTimestamp == null) runTimestamp = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getApplicationName() { return applicationName; }
    public void setApplicationName(String applicationName) { this.applicationName = applicationName; }
    public String getSbomHash() { return sbomHash; }
    public void setSbomHash(String sbomHash) { this.sbomHash = sbomHash; }
    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }
    public String getPolicyVersion() { return policyVersion; }
    public void setPolicyVersion(String policyVersion) { this.policyVersion = policyVersion; }
    public String getOverallDecision() { return overallDecision; }
    public void setOverallDecision(String overallDecision) { this.overallDecision = overallDecision; }
    public double getPostureScore() { return postureScore; }
    public void setPostureScore(double postureScore) { this.postureScore = postureScore; }
    public String getSummaryJson() { return summaryJson; }
    public void setSummaryJson(String summaryJson) { this.summaryJson = summaryJson; }
    public Instant getRunTimestamp() { return runTimestamp; }
    public void setRunTimestamp(Instant runTimestamp) { this.runTimestamp = runTimestamp; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public List<ComponentResultEntity> getComponentResults() { return componentResults; }
    public void setComponentResults(List<ComponentResultEntity> componentResults) { this.componentResults = componentResults; }
}
