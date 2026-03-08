package com.scrutinizer.enrichment;

import com.scrutinizer.model.Component;
import com.scrutinizer.model.DependencyGraph;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates the enrichment of a DependencyGraph with supply-chain signals.
 * Transforms DependencyGraph → EnrichedDependencyGraph by:
 * 1. Resolving package URLs to source repository URLs
 * 2. Fetching OpenSSF Scorecard data per repository
 * 3. Checking SLSA provenance attestations per package
 */
@Service
public class EnrichmentPipeline {

    private final ScorecardService scorecardService;
    private final ProvenanceService provenanceService;

    public EnrichmentPipeline(ScorecardService scorecardService,
                               ProvenanceService provenanceService) {
        this.scorecardService = scorecardService;
        this.provenanceService = provenanceService;
    }

    /**
     * Enrich all components in the graph with scorecard and provenance data.
     * Components without a purl or without available signal data will have
     * empty enrichment fields (graceful degradation).
     */
    public EnrichedDependencyGraph enrich(DependencyGraph graph) {
        List<EnrichedComponent> enrichedComponents = new ArrayList<>();

        for (Component component : graph.components()) {
            EnrichedComponent enriched = enrichComponent(component);
            enrichedComponents.add(enriched);
        }

        return new EnrichedDependencyGraph(
                enrichedComponents,
                graph.edges(),
                graph.rootRef().orElse(null)
        );
    }

    /**
     * Enrich a single component with available signals.
     */
    private EnrichedComponent enrichComponent(Component component) {
        EnrichedComponent enriched = EnrichedComponent.unenriched(component);

        String purl = component.purl().orElse(null);

        // Scorecard: resolve purl -> repo URL -> scorecard
        Optional<String> repoUrl = PurlResolver.toRepoUrl(purl);
        if (repoUrl.isPresent()) {
            Optional<ScorecardResult> scorecard = scorecardService.getScorecard(repoUrl.get());
            if (scorecard.isPresent()) {
                enriched = enriched.withScorecard(scorecard.get());
            }
        }

        // Provenance: check directly via purl
        if (purl != null) {
            ProvenanceResult provenance = provenanceService.checkProvenance(purl);
            enriched = enriched.withProvenance(provenance);
        }

        return enriched;
    }
}
