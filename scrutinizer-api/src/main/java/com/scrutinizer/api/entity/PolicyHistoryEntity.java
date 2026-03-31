package com.scrutinizer.api.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "policy_history")
public class PolicyHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID policyId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String policyYaml;

    @Column
    private String changedBy;

    @Column(nullable = false)
    private Instant changedAt;

    @PrePersist
    void prePersist() {
        if (changedAt == null) changedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPolicyId() { return policyId; }
    public void setPolicyId(UUID policyId) { this.policyId = policyId; }
    public String getPolicyYaml() { return policyYaml; }
    public void setPolicyYaml(String policyYaml) { this.policyYaml = policyYaml; }
    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
    public Instant getChangedAt() { return changedAt; }
    public void setChangedAt(Instant changedAt) { this.changedAt = changedAt; }
}
