package com.scrutinizer.engine;

import com.scrutinizer.enrichment.EnrichedComponent;
import com.scrutinizer.enrichment.EnrichedDependencyGraph;
import com.scrutinizer.model.DependencyEdge;
import com.scrutinizer.policy.PolicyDefinition;
import com.scrutinizer.policy.Rule;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostureEvaluator {

    private final RuleEvaluator ruleEvaluator;

    public PostureEvaluator(RuleEvaluator ruleEvaluator) {
        this.ruleEvaluator = ruleEvaluator;
    }

    public PostureReport evaluate(EnrichedDependencyGraph graph,
                                   PolicyDefinition policy,
                                   String sbomJson) {
        String sbomHash = computeSha256(sbomJson);

        Set<String> directRefs = computeDirectRefs(graph);

        Map<String, List<RuleResult>> resultsByComponent = new LinkedHashMap<>();

        List<EnrichedComponent> sortedComponents = graph.components().stream()
                .sorted(Comparator.comparing(ec -> ec.component().bomRef()))
                .toList();

        for (EnrichedComponent ec : sortedComponents) {
            String ref = ec.component().bomRef();
            boolean isDirect = directRefs.contains(ref);
            List<RuleResult> componentResults = new ArrayList<>();

            for (Rule rule : policy.rules()) {
                if (!matchesTarget(rule.target(), isDirect)) {
                    componentResults.add(new RuleResult(ref, rule.id(), RuleResult.Decision.PASS,
                            "(skipped-target)", rule.value(), rule.description()));
                    continue;
                }

                RuleResult result = ruleEvaluator.evaluate(rule, ec);

                if (result.decision() == RuleResult.Decision.SKIP) {
                    componentResults.clear();
                    componentResults.add(result);
                    break;
                }
                componentResults.add(result);
            }

            resultsByComponent.put(ref, componentResults);
        }

        List<Finding> findings = FindingFactory.createFindings(resultsByComponent, policy.rules(), graph);

        return PostureReport.create(policy, sbomHash, resultsByComponent, findings);
    }

    private Set<String> computeDirectRefs(EnrichedDependencyGraph graph) {
        Optional<String> rootRef = graph.rootRef();
        if (rootRef.isEmpty()) {
            return Set.of();
        }
        return graph.edges().stream()
                .filter(edge -> edge.sourceRef().equals(rootRef.get()))
                .map(DependencyEdge::targetRef)
                .collect(Collectors.toSet());
    }

    private boolean matchesTarget(Rule.Target target, boolean isDirect) {
        return switch (target) {
            case ALL -> true;
            case DIRECT -> isDirect;
            case TRANSITIVE -> !isDirect;
        };
    }

    private static String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return "sha256-unavailable";
        }
    }
}
