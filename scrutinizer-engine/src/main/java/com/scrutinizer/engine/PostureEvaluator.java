package com.scrutinizer.engine;

import com.scrutinizer.enrichment.EnrichedComponent;
import com.scrutinizer.enrichment.EnrichedDependencyGraph;
import com.scrutinizer.policy.PolicyDefinition;
import com.scrutinizer.policy.Rule;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Orchestrates rule evaluation across all components in an enriched graph,
 * producing a complete PostureReport.
 */
@Service
public class PostureEvaluator {

    private final RuleEvaluator ruleEvaluator;

    public PostureEvaluator(RuleEvaluator ruleEvaluator) {
        this.ruleEvaluator = ruleEvaluator;
    }

    /**
     * Evaluate all rules in a policy against every component in the enriched graph.
     *
     * @param graph   the enriched dependency graph
     * @param policy  the policy definition to evaluate
     * @param sbomJson raw SBOM content for hash computation
     * @return a complete PostureReport
     */
    public PostureReport evaluate(EnrichedDependencyGraph graph,
                                   PolicyDefinition policy,
                                   String sbomJson) {
        String sbomHash = computeSha256(sbomJson);

        Map<String, List<RuleResult>> resultsByComponent = new LinkedHashMap<>();

        // Evaluate each component against all rules (sorted for determinism)
        List<EnrichedComponent> sortedComponents = graph.components().stream()
                .sorted(Comparator.comparing(ec -> ec.component().bomRef()))
                .toList();

        for (EnrichedComponent ec : sortedComponents) {
            String ref = ec.component().bomRef();
            List<RuleResult> componentResults = new ArrayList<>();

            for (Rule rule : policy.rules()) {
                RuleResult result = ruleEvaluator.evaluate(rule, ec);

                // If SKIP, don't add to results — component is excluded by this rule
                if (result.decision() == RuleResult.Decision.SKIP) {
                    componentResults.clear();
                    componentResults.add(result);
                    break;
                }
                componentResults.add(result);
            }

            resultsByComponent.put(ref, componentResults);
        }

        return PostureReport.create(policy, sbomHash, resultsByComponent);
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
