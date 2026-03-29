package com.scrutinizer.evidence;

import java.time.Instant;
import java.util.*;

public final class EvidenceManifest {

    private final String generatedAt;
    private final String engineVersion;
    private final String schemaVersion;
    private final List<ArtifactEntry> artifacts;
    private final AuditMetadata auditMetadata;
    private final List<ChainOfCustodyEntry> chainOfCustody;
    private final String reviewerNotes;

    public EvidenceManifest(String engineVersion, String schemaVersion, List<ArtifactEntry> artifacts) {
        this(engineVersion, schemaVersion, artifacts, null, List.of(), null);
    }

    public EvidenceManifest(String engineVersion, String schemaVersion, List<ArtifactEntry> artifacts,
                            AuditMetadata auditMetadata, List<ChainOfCustodyEntry> chainOfCustody,
                            String reviewerNotes) {
        this.generatedAt = Instant.now().toString();
        this.engineVersion = Objects.requireNonNull(engineVersion);
        this.schemaVersion = Objects.requireNonNull(schemaVersion);
        this.artifacts = Collections.unmodifiableList(new ArrayList<>(artifacts));
        this.auditMetadata = auditMetadata;
        this.chainOfCustody = Collections.unmodifiableList(new ArrayList<>(chainOfCustody));
        this.reviewerNotes = reviewerNotes;
    }

    public String generatedAt() { return generatedAt; }
    public String engineVersion() { return engineVersion; }
    public String schemaVersion() { return schemaVersion; }
    public List<ArtifactEntry> artifacts() { return artifacts; }
    public AuditMetadata auditMetadata() { return auditMetadata; }
    public List<ChainOfCustodyEntry> chainOfCustody() { return chainOfCustody; }
    public String reviewerNotes() { return reviewerNotes; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("generatedAt", generatedAt);
        map.put("engineVersion", engineVersion);
        map.put("schemaVersion", schemaVersion);

        List<Map<String, Object>> artifactList = new ArrayList<>();
        for (ArtifactEntry entry : artifacts) {
            artifactList.add(entry.toMap());
        }
        map.put("artifacts", artifactList);

        if (auditMetadata != null) {
            map.put("auditMetadata", auditMetadata.toMap());
        }

        if (!chainOfCustody.isEmpty()) {
            List<Map<String, Object>> custodyList = new ArrayList<>();
            for (ChainOfCustodyEntry entry : chainOfCustody) {
                custodyList.add(entry.toMap());
            }
            map.put("chainOfCustody", custodyList);
        }

        if (reviewerNotes != null && !reviewerNotes.isBlank()) {
            map.put("reviewerNotes", reviewerNotes);
        }

        return map;
    }

    public record ArtifactEntry(String filename, String sha256, long sizeBytes, String contentType) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("filename", filename);
            map.put("sha256", sha256);
            map.put("sizeBytes", sizeBytes);
            map.put("contentType", contentType);
            return map;
        }
    }

    public record AuditMetadata(String evaluatorId, String evaluationContext, String retentionPolicy) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("evaluatorId", evaluatorId);
            map.put("evaluationContext", evaluationContext);
            map.put("retentionPolicy", retentionPolicy);
            return map;
        }
    }

    public record ChainOfCustodyEntry(String actor, String action, String timestamp) {
        public ChainOfCustodyEntry(String actor, String action) {
            this(actor, action, Instant.now().toString());
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("actor", actor);
            map.put("action", action);
            map.put("timestamp", timestamp);
            return map;
        }
    }
}
