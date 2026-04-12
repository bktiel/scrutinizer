package com.scrutinizer.api.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "component_result")
public class ComponentResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posture_run_id", nullable = false)
    private PostureRunEntity postureRun;

    @Column(nullable = false)
    private String componentRef;

    private String componentName;
    private String componentVersion;
    private String purl;
    private boolean isDirect;

    @Column(nullable = false, length = 10)
    private String decision;

    @OneToMany(mappedBy = "componentResult", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("ruleId ASC")
    private List<FindingEntity> findings = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public PostureRunEntity getPostureRun() { return postureRun; }
    public void setPostureRun(PostureRunEntity postureRun) { this.postureRun = postureRun; }
    public String getComponentRef() { return componentRef; }
    public void setComponentRef(String componentRef) { this.componentRef = componentRef; }
    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }
    public String getComponentVersion() { return componentVersion; }
    public void setComponentVersion(String componentVersion) { this.componentVersion = componentVersion; }
    public String getPurl() { return purl; }
    public void setPurl(String purl) { this.purl = purl; }
    public boolean isDirect() { return isDirect; }
    public void setDirect(boolean direct) { isDirect = direct; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public List<FindingEntity> getFindings() { return findings; }
    public void setFindings(List<FindingEntity> findings) { this.findings = findings; }
}
