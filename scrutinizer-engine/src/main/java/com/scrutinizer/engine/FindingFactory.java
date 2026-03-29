package com.scrutinizer.engine;

import com.scrutinizer.enrichment.EnrichedComponent;
import com.scrutinizer.enrichment.EnrichedDependencyGraph;
import com.scrutinizer.model.DependencyEdge;
import com.scrutinizer.policy.Rule;

import java.util.*;

public final class FindingFactory {

    private static final Map<String, String> REMEDIATION_TEMPLATES = Map.ofEntries(
            Map.entry("scorecard.score", "Upgrade to a version from a repository with a higher OpenSSF Scorecard score, or contribute security improvements upstream."),
            Map.entry("scorecard.checks.Code-Review", "Ensure the upstream project requires code review for all changes."),
            Map.entry("scorecard.checks.Maintained", "Consider switching to an actively maintained alternative if the project is unmaintained."),
            Map.entry("scorecard.checks.Vulnerabilities", "Check for open vulnerability advisories and upgrade to a patched version."),
            Map.entry("provenance.present", "Request SLSA provenance attestations from the package maintainer, or pin to a verified build."),
            Map.entry("provenance.level", "Upgrade to a dependency version published with a higher SLSA provenance level."),
            Map.entry("purl", "Ensure the component has a valid Package URL for traceability."),
            Map.entry("name", "Verify the component name matches expected allowlists or blocklists."),
            Map.entry("version", "Pin to a known-good version that meets policy requirements."),
            Map.entry("scope", "Review whether this dependency's scope is appropriate for production use."),
            Map.entry("type", "Confirm the component type is permitted by your organization's policy.")
    );

    private FindingFactory() {}

    public static List<Finding> createFindings(
            Map<String, List<RuleResult>> resultsByComponent,
            List<Rule> rules,
            EnrichedDependencyGraph graph) {

        Set<String> directRefs = computeDirectRefs(graph);
        Map<String, Integer> depthMap = computeDepthFromRoot(graph);
        Map<String, Rule> ruleIndex = new LinkedHashMap<>();
        for (Rule rule : rules) {
            ruleIndex.put(rule.id(), rule);
        }

        List<Finding> findings = new ArrayList<>();
        int findingCounter = 0;

        List<String> sortedRefs = new ArrayList<>(resultsByComponent.keySet());
        Collections.sort(sortedRefs);

        for (String componentRef : sortedRefs) {
            List<RuleResult> results = resultsByComponent.get(componentRef);

            Optional<EnrichedComponent> ecOpt = graph.getComponentByRef(componentRef);
            String componentName = ecOpt.map(ec -> ec.component().displayName()).orElse(componentRef);

            boolean isDirect = directRefs.contains(componentRef);
            int depth = depthMap.getOrDefault(componentRef, -1);

            for (RuleResult rr : results) {
                if (rr.decision() == RuleResult.Decision.PASS
                        || rr.decision() == RuleResult.Decision.SKIP) {
                    continue;
                }

                Rule rule = ruleIndex.get(rr.ruleId());
                String field = rule != null ? rule.field() : "";
                String severity = rule != null ? rule.severity().name() : rr.decision().name();

                String remediation = resolveRemediation(field);

                List<String> evidenceChain = buildEvidenceChain(componentRef, componentName, rr, isDirect, depth);

                findingCounter++;
                String findingId = "F-" + String.format("%04d", findingCounter);

                findings.add(new Finding(
                        findingId, componentRef, componentName, rr.ruleId(),
                        rr.decision(), severity, field,
                        rr.actualValue(), rr.expectedValue(), rr.description(),
                        remediation, isDirect, depth, evidenceChain
                ));
            }
        }

        return Collections.unmodifiableList(findings);
    }

    private static String resolveRemediation(String field) {
        if (REMEDIATION_TEMPLATES.containsKey(field)) {
            return REMEDIATION_TEMPLATES.get(field);
        }
        for (Map.Entry<String, String> entry : REMEDIATION_TEMPLATES.entrySet()) {
            if (field.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "Review this dependency against your organization's supply-chain security policy.";
    }

    private static List<String> buildEvidenceChain(String componentRef, String componentName,
                                                    RuleResult rr, boolean isDirect, int depth) {
        List<String> chain = new ArrayList<>();
        chain.add("Component: " + componentName + " (" + componentRef + ")");
        chain.add("Position: " + (isDirect ? "direct dependency" : "transitive dependency at depth " + depth));
        chain.add("Rule: " + rr.ruleId() + " evaluated field with result " + rr.decision().name());
        chain.add("Actual: " + rr.actualValue() + ", Expected: " + rr.expectedValue());
        return chain;
    }

    private static Set<String> computeDirectRefs(EnrichedDependencyGraph graph) {
        Optional<String> rootRef = graph.rootRef();
        if (rootRef.isEmpty()) {
            return Set.of();
        }
        Set<String> directRefs = new HashSet<>();
        for (DependencyEdge edge : graph.edges()) {
            if (edge.sourceRef().equals(rootRef.get())) {
                directRefs.add(edge.targetRef());
            }
        }
        return directRefs;
    }

    private static Map<String, Integer> computeDepthFromRoot(EnrichedDependencyGraph graph) {
        Map<String, Integer> depthMap = new HashMap<>();
        Optional<String> rootRef = graph.rootRef();
        if (rootRef.isEmpty()) {
            return depthMap;
        }

        Map<String, List<String>> adjacency = new HashMap<>();
        for (DependencyEdge edge : graph.edges()) {
            adjacency.computeIfAbsent(edge.sourceRef(), k -> new ArrayList<>()).add(edge.targetRef());
        }

        Deque<String> queue = new ArrayDeque<>();
        queue.add(rootRef.get());
        depthMap.put(rootRef.get(), 0);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            int currentDepth = depthMap.get(current);
            List<String> children = adjacency.getOrDefault(current, List.of());
            for (String child : children) {
                if (!depthMap.containsKey(child)) {
                    depthMap.put(child, currentDepth + 1);
                    queue.add(child);
                }
            }
        }

        return depthMap;
    }
}
