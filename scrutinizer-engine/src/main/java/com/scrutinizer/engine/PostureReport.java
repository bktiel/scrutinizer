package com.scrutinizer.engine;

import com.scrutinizer.policy.PolicyDefinition;
import com.scrutinizer.policy.ScoringConfig;

import java.time.Instant;
import java.util.*;

/**
 * Structured evidence bundle capturing the full evaluation of an SBOM
 * against a policy. Designed for JSON serialization and audit trails.
 */
public final class PostureReport {

    private final String timestamp;
    private final String policyName;
    private final String policyVersion;
    private final String sbomHash;
    private final RuleResult.Decision overallDecision;
    private final double postureScore;
    private final Summary summary;
    private final List<ComponentReport> componentReports;
    private final List<Finding> findings;

    private PostureReport(Builder builder) {
        this.timestamp = builder.timestamp;
        this.policyName = builder.policyName;
        this.policyVersion = builder.policyVersion;
        this.sbomHash = builder.sbomHash;
        this.overallDecision = builder.overallDecision;
        this.postureScore = builder.postureScore;
        this.summary = builder.summary;
        this.componentReports = Collections.unmodifiableList(new ArrayList<>(builder.componentReports));
        this.findings = Collections.unmodifiableList(new ArrayList<>(builder.findings));
    }

    public String timestamp() { return timestamp; }
    public String policyName() { return policyName; }
    public String policyVersion() { return policyVersion; }
    public String sbomHash() { return sbomHash; }
    public RuleResult.Decision overallDecision() { return overallDecision; }
    public double postureScore() { return postureScore; }
    public Summary summary() { return summary; }
    public List<ComponentReport> componentReports() { return componentReports; }
    public List<Finding> findings() { return findings; }

    /**
     * Build a PostureReport from evaluation results.
     */
    public static PostureReport create(
            PolicyDefinition policy,
            String sbomHash,
            Map<String, List<RuleResult>> resultsByComponent) {
        return create(policy, sbomHash, resultsByComponent, List.of());
    }

    public static PostureReport create(
            PolicyDefinition policy,
            String sbomHash,
            Map<String, List<RuleResult>> resultsByComponent,
            List<Finding> findings) {

        List<RuleResult> allResults = resultsByComponent.values().stream()
                .flatMap(List::stream)
                .toList();

        RuleResult.Decision overall = PostureScorer.computeOverallDecision(allResults, policy.scoring(), policy.rules());
        double score = PostureScorer.computeScore(allResults);

        // Count decisions
        int pass = 0, warn = 0, fail = 0, info = 0, skip = 0;
        for (RuleResult r : allResults) {
            switch (r.decision()) {
                case PASS -> pass++;
                case WARN -> warn++;
                case FAIL -> fail++;
                case INFO -> info++;
                case SKIP -> skip++;
            }
        }

        Summary summary = new Summary(pass, warn, fail, info, skip, allResults.size());

        // Build per-component reports (sorted for determinism)
        List<ComponentReport> compReports = resultsByComponent.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new ComponentReport(entry.getKey(),
                        Collections.unmodifiableList(new ArrayList<>(entry.getValue()))))
                .toList();

        return new Builder()
                .timestamp(Instant.now().toString())
                .policyName(policy.name())
                .policyVersion(policy.version())
                .sbomHash(sbomHash)
                .overallDecision(overall)
                .postureScore(score)
                .summary(summary)
                .componentReports(compReports)
                .findings(findings != null ? findings : List.of())
                .build();
    }

    /**
     * Convert the report to a serializable map for JSON output.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("timestamp", timestamp);
        map.put("policy", Map.of("name", policyName, "version", policyVersion));
        map.put("sbomHash", sbomHash);
        map.put("overallDecision", overallDecision.name());
        map.put("postureScore", Math.round(postureScore * 100.0) / 100.0);
        map.put("summary", summary.toMap());

        List<Map<String, Object>> components = new ArrayList<>();
        for (ComponentReport cr : componentReports) {
            Map<String, Object> comp = new LinkedHashMap<>();
            comp.put("componentRef", cr.componentRef());
            List<Map<String, Object>> rules = new ArrayList<>();
            for (RuleResult rr : cr.ruleResults()) {
                Map<String, Object> rule = new LinkedHashMap<>();
                rule.put("ruleId", rr.ruleId());
                rule.put("decision", rr.decision().name());
                rule.put("actual", rr.actualValue());
                rule.put("expected", rr.expectedValue());
                rule.put("description", rr.description());
                rules.add(rule);
            }
            comp.put("rules", rules);
            components.add(comp);
        }
        map.put("components", components);

        List<Map<String, Object>> findingMaps = new ArrayList<>();
        for (Finding f : findings) {
            findingMaps.add(f.toMap());
        }
        map.put("findings", findingMaps);

        return map;
    }

    /** Counts of each decision type. */
    public record Summary(int pass, int warn, int fail, int info, int skip, int total) {
        Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("total", total);
            m.put("pass", pass);
            m.put("warn", warn);
            m.put("fail", fail);
            m.put("info", info);
            m.put("skip", skip);
            return m;
        }
    }

    /** All rule results for a single component. */
    public record ComponentReport(String componentRef, List<RuleResult> ruleResults) {}

    /** Builder for testing and custom report construction. */
    public static final class Builder {
        private String timestamp = Instant.now().toString();
        private String policyName = "";
        private String policyVersion = "";
        private String sbomHash = "";
        private RuleResult.Decision overallDecision = RuleResult.Decision.PASS;
        private double postureScore = 10.0;
        private Summary summary = new Summary(0, 0, 0, 0, 0, 0);
        private List<ComponentReport> componentReports = List.of();
        private List<Finding> findings = List.of();

        public Builder timestamp(String v) { this.timestamp = v; return this; }
        public Builder policyName(String v) { this.policyName = v; return this; }
        public Builder policyVersion(String v) { this.policyVersion = v; return this; }
        public Builder sbomHash(String v) { this.sbomHash = v; return this; }
        public Builder overallDecision(RuleResult.Decision v) { this.overallDecision = v; return this; }
        public Builder postureScore(double v) { this.postureScore = v; return this; }
        public Builder summary(Summary v) { this.summary = v; return this; }
        public Builder componentReports(List<ComponentReport> v) { this.componentReports = v; return this; }
        public Builder findings(List<Finding> v) { this.findings = v; return this; }
        public PostureReport build() { return new PostureReport(this); }
    }
}
