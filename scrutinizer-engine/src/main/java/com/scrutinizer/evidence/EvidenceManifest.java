package com.scrutinizer.evidence;

import java.time.Instant;
import java.util.*;

public final class EvidenceManifest {

    private final String generatedAt;
    private final String engineVersion;
    private final String schemaVersion;
    private final List<ArtifactEntry> artifacts;

    public EvidenceManifest(String engineVersion, String schemaVersion, List<ArtifactEntry> artifacts) {
        this.generatedAt = Instant.now().toString();
        this.engineVersion = Objects.requireNonNull(engineVersion);
        this.schemaVersion = Objects.requireNonNull(schemaVersion);
        this.artifacts = Collections.unmodifiableList(new ArrayList<>(artifacts));
    }

    public String generatedAt() { return generatedAt; }
    public String engineVersion() { return engineVersion; }
    public String schemaVersion() { return schemaVersion; }
    public List<ArtifactEntry> artifacts() { return artifacts; }

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
}
