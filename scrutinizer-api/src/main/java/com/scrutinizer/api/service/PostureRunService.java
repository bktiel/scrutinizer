package com.scrutinizer.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrutinizer.api.entity.ComponentResultEntity;
import com.scrutinizer.api.entity.FindingEntity;
import com.scrutinizer.api.entity.PostureRunEntity;
import com.scrutinizer.api.repository.PostureRunRepository;
import com.scrutinizer.engine.Finding;
import com.scrutinizer.engine.PostureEvaluator;
import com.scrutinizer.engine.PostureReport;
import com.scrutinizer.engine.RuleResult;
import com.scrutinizer.enrichment.EnrichedComponent;
import com.scrutinizer.enrichment.EnrichedDependencyGraph;
import com.scrutinizer.enrichment.EnrichmentPipeline;
import com.scrutinizer.model.DependencyGraph;
import com.scrutinizer.parser.SbomParser;
import com.scrutinizer.policy.PolicyDefinition;
import com.scrutinizer.policy.PolicyParser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class PostureRunService {

    private final SbomParser sbomParser;
    private final PolicyParser policyParser;
    private final EnrichmentPipeline enrichmentPipeline;
    private final PostureEvaluator postureEvaluator;
    private final PostureRunRepository postureRunRepository;
    private final ObjectMapper objectMapper;

    public PostureRunService(SbomParser sbomParser, PolicyParser policyParser,
                              EnrichmentPipeline enrichmentPipeline,
                              PostureEvaluator postureEvaluator,
                              PostureRunRepository postureRunRepository) {
        this.sbomParser = sbomParser;
        this.policyParser = policyParser;
        this.enrichmentPipeline = enrichmentPipeline;
        this.postureEvaluator = postureEvaluator;
        this.postureRunRepository = postureRunRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public PostureRunEntity executeAndPersist(String applicationName,
                                               Path sbomPath,
                                               Path policyPath) throws IOException {
        String sbomJson = Files.readString(sbomPath);
        DependencyGraph graph = sbomParser.parseFile(sbomPath);

        PolicyDefinition policy;
        try (FileInputStream fis = new FileInputStream(policyPath.toFile())) {
            policy = policyParser.parse(fis);
        }

        EnrichedDependencyGraph enrichedGraph = enrichmentPipeline.enrich(graph);
        PostureReport report = postureEvaluator.evaluate(enrichedGraph, policy, sbomJson);

        return persistReport(applicationName, report, enrichedGraph);
    }

    private PostureRunEntity persistReport(String applicationName,
                                            PostureReport report,
                                            EnrichedDependencyGraph graph) {
        PostureRunEntity run = new PostureRunEntity();
        run.setApplicationName(applicationName);
        run.setSbomHash(report.sbomHash());
        run.setPolicyName(report.policyName());
        run.setPolicyVersion(report.policyVersion());
        run.setOverallDecision(report.overallDecision().name());
        run.setPostureScore(report.postureScore());

        try {
            run.setSummaryJson(objectMapper.writeValueAsString(report.summary().toMap()));
        } catch (Exception e) {
            run.setSummaryJson("{}");
        }

        Set<String> directRefs = computeDirectRefs(graph);

        Map<String, List<Finding>> findingsByComponent = new LinkedHashMap<>();
        for (Finding f : report.findings()) {
            findingsByComponent.computeIfAbsent(f.componentRef(), k -> new ArrayList<>()).add(f);
        }

        for (PostureReport.ComponentReport cr : report.componentReports()) {
            ComponentResultEntity compEntity = new ComponentResultEntity();
            compEntity.setPostureRun(run);
            compEntity.setComponentRef(cr.componentRef());

            Optional<EnrichedComponent> ecOpt = graph.getComponentByRef(cr.componentRef());
            ecOpt.ifPresent(ec -> {
                compEntity.setComponentName(ec.component().displayName());
                compEntity.setComponentVersion(ec.component().version());
                compEntity.setPurl(ec.component().purl().orElse(null));
            });

            compEntity.setDirect(directRefs.contains(cr.componentRef()));

            RuleResult.Decision worst = RuleResult.Decision.PASS;
            for (RuleResult rr : cr.ruleResults()) {
                if (rr.decision() == RuleResult.Decision.FAIL) { worst = RuleResult.Decision.FAIL; break; }
                if (rr.decision() == RuleResult.Decision.WARN) worst = RuleResult.Decision.WARN;
            }
            compEntity.setDecision(worst.name());

            List<Finding> componentFindings = findingsByComponent.getOrDefault(cr.componentRef(), List.of());
            for (Finding f : componentFindings) {
                FindingEntity findingEntity = new FindingEntity();
                findingEntity.setComponentResult(compEntity);
                findingEntity.setRuleId(f.ruleId());
                findingEntity.setDecision(f.decision().name());
                findingEntity.setSeverity(f.severity());
                findingEntity.setField(f.field());
                findingEntity.setActualValue(f.actualValue());
                findingEntity.setExpectedValue(f.expectedValue());
                findingEntity.setDescription(f.description());
                findingEntity.setRemediation(f.remediation());
                compEntity.getFindings().add(findingEntity);
            }

            run.getComponentResults().add(compEntity);
        }

        return postureRunRepository.save(run);
    }

    private Set<String> computeDirectRefs(EnrichedDependencyGraph graph) {
        Optional<String> rootRef = graph.rootRef();
        if (rootRef.isEmpty()) return Set.of();
        Set<String> refs = new HashSet<>();
        graph.edges().stream()
                .filter(e -> e.sourceRef().equals(rootRef.get()))
                .forEach(e -> refs.add(e.targetRef()));
        return refs;
    }
}
