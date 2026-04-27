package com.scrutinizer.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrutinizer.api.entity.ComponentResultEntity;
import com.scrutinizer.api.entity.FindingEntity;
import com.scrutinizer.api.entity.PolicyEntity;
import com.scrutinizer.api.entity.PolicyExceptionEntity;
import com.scrutinizer.api.entity.PostureRunEntity;
import com.scrutinizer.api.entity.ProjectEntity;
import com.scrutinizer.api.repository.PolicyRepository;
import com.scrutinizer.api.repository.PolicyExceptionRepository;
import com.scrutinizer.api.repository.PostureRunRepository;
import com.scrutinizer.api.repository.ProjectRepository;
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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
public class PostureRunService {

    private final SbomParser sbomParser;
    private final PolicyParser policyParser;
    private final EnrichmentPipeline enrichmentPipeline;
    private final PostureEvaluator postureEvaluator;
    private final PostureRunRepository postureRunRepository;
    private final PolicyRepository policyRepository;
    private final PolicyExceptionRepository policyExceptionRepository;
    private final ProjectRepository projectRepository;
    private final ObjectMapper objectMapper;

    public PostureRunService(SbomParser sbomParser, PolicyParser policyParser,
                              EnrichmentPipeline enrichmentPipeline,
                              PostureEvaluator postureEvaluator,
                              PostureRunRepository postureRunRepository,
                              PolicyRepository policyRepository,
                              PolicyExceptionRepository policyExceptionRepository,
                              ProjectRepository projectRepository) {
        this.sbomParser = sbomParser;
        this.policyParser = policyParser;
        this.enrichmentPipeline = enrichmentPipeline;
        this.postureEvaluator = postureEvaluator;
        this.postureRunRepository = postureRunRepository;
        this.policyRepository = policyRepository;
        this.policyExceptionRepository = policyExceptionRepository;
        this.projectRepository = projectRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public PostureRunEntity executeWithUpload(String applicationName,
                                               String sbomJson,
                                               UUID policyId) {
        return executeWithUpload(applicationName, sbomJson, policyId, null);
    }

    @Transactional
    public PostureRunEntity executeWithUpload(String applicationName,
                                               String sbomJson,
                                               UUID policyId,
                                               UUID projectId) {
        PolicyEntity policyEntity = policyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));

        JsonNode sbomRoot;
        try {
            sbomRoot = objectMapper.readTree(sbomJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid SBOM JSON: " + e.getMessage());
        }

        DependencyGraph graph = sbomParser.parse(sbomRoot);

        PolicyDefinition policy = policyParser.parse(
                new ByteArrayInputStream(policyEntity.getPolicyYaml().getBytes(StandardCharsets.UTF_8)));

        EnrichedDependencyGraph enrichedGraph = enrichmentPipeline.enrich(graph);
        PostureReport report = postureEvaluator.evaluate(enrichedGraph, policy, sbomJson);

        return persistReport(applicationName, report, enrichedGraph, policyEntity.getId(), projectId);
    }

    @Transactional
    public PostureRunEntity executeForProject(UUID projectId, String sbomJson) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (project.getPolicyId() == null) {
            throw new IllegalArgumentException("Project does not have a policy assigned: " + projectId);
        }

        return executeWithUpload(project.getName(), sbomJson, project.getPolicyId(), projectId);
    }

    @Transactional
    public PostureRunEntity executeForRepositoryUrl(String repositoryUrl, String applicationName, String sbomJson) {
        // Normalize URL: strip trailing "/" and ".git"
        String normalized = repositoryUrl.replaceAll("(\\.git)?/*$", "");

        ProjectEntity project = projectRepository.findByRepositoryUrl(normalized)
                .or(() -> projectRepository.findByRepositoryUrl(normalized + ".git"))
                .or(() -> projectRepository.findByRepositoryUrl(repositoryUrl))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No project registered for repository URL: " + repositoryUrl
                        + ". Register it in the Scrutinizer dashboard first."));

        if (project.getPolicyId() == null) {
            throw new IllegalArgumentException(
                    "Project '" + project.getName() + "' has no policy assigned. "
                    + "Assign a policy in the Scrutinizer dashboard.");
        }

        String appName = (applicationName != null && !applicationName.isBlank())
                ? applicationName : project.getName();

        return executeWithUpload(appName, sbomJson, project.getPolicyId(), project.getId());
    }

    private PostureRunEntity persistReport(String applicationName,
                                            PostureReport report,
                                            EnrichedDependencyGraph graph,
                                            UUID policyId,
                                            UUID projectId) {
        // Load active exceptions for project (if provided) and global exceptions
        List<PolicyExceptionEntity> activeExceptions = collectActiveExceptions(projectId);

        PostureRunEntity run = new PostureRunEntity();
        run.setApplicationName(applicationName);
        run.setSbomHash(report.sbomHash());
        run.setPolicyName(report.policyName());
        run.setPolicyVersion(report.policyVersion());
        run.setPolicyId(policyId);
        run.setProjectId(projectId);

        Set<String> directRefs = computeDirectRefs(graph);

        Map<String, List<Finding>> findingsByComponent = new LinkedHashMap<>();
        for (Finding f : report.findings()) {
            findingsByComponent.computeIfAbsent(f.componentRef(), k -> new ArrayList<>()).add(f);
        }

        Map<String, RuleResult.Decision> componentDecisions = new LinkedHashMap<>();

        for (PostureReport.ComponentReport cr : report.componentReports()) {
            ComponentResultEntity compEntity = new ComponentResultEntity();
            compEntity.setPostureRun(run);
            compEntity.setComponentRef(cr.componentRef());

            Optional<EnrichedComponent> ecOpt = graph.getComponentByRef(cr.componentRef());
            String componentName = null;
            String componentVersion = null;
            if (ecOpt.isPresent()) {
                EnrichedComponent ec = ecOpt.get();
                componentName = ec.component().displayName();
                componentVersion = ec.component().version();
                compEntity.setComponentName(componentName);
                compEntity.setComponentVersion(componentVersion);
                compEntity.setPurl(ec.component().purl().orElse(null));
            }

            compEntity.setDirect(directRefs.contains(cr.componentRef()));

            RuleResult.Decision worst = RuleResult.Decision.PASS;
            List<Finding> componentFindings = findingsByComponent.getOrDefault(cr.componentRef(), List.of());

            for (Finding f : componentFindings) {
                FindingEntity findingEntity = new FindingEntity();
                findingEntity.setComponentResult(compEntity);
                findingEntity.setRuleId(f.ruleId());

                RuleResult.Decision findingDecision = f.decision();
                String remediation = f.remediation();

                // Check if this finding matches an active exception
                PolicyExceptionEntity matchingException = findMatchingException(
                    f.ruleId(),
                    componentName,
                    componentVersion,
                    activeExceptions
                );

                if (matchingException != null) {
                    // Override decision to PASS
                    findingDecision = RuleResult.Decision.PASS;
                    // Mark the finding as excepted in remediation
                    remediation = remediation + " [EXCEPTED: " + matchingException.getJustification() + "]";
                }

                findingEntity.setDecision(findingDecision.name());
                findingEntity.setSeverity(f.severity());
                findingEntity.setField(f.field());
                findingEntity.setActualValue(f.actualValue());
                findingEntity.setExpectedValue(f.expectedValue());
                findingEntity.setDescription(f.description());
                findingEntity.setRemediation(remediation);
                compEntity.getFindings().add(findingEntity);

                // Track worst decision for component (after applying exceptions)
                if (findingDecision == RuleResult.Decision.FAIL) {
                    worst = RuleResult.Decision.FAIL;
                } else if (findingDecision == RuleResult.Decision.WARN && worst != RuleResult.Decision.FAIL) {
                    worst = RuleResult.Decision.WARN;
                }
            }

            compEntity.setDecision(worst.name());
            componentDecisions.put(cr.componentRef(), worst);
            run.getComponentResults().add(compEntity);
        }

        // Recompute overall decision and score after applying exceptions
        RuleResult.Decision overallDecision = computeOverallDecision(componentDecisions.values());
        double overallScore = computeOverallScore(run.getComponentResults());

        run.setOverallDecision(overallDecision.name());
        run.setPostureScore(overallScore);

        // Update summary counts
        updateSummary(run);

        try {
            run.setSummaryJson(objectMapper.writeValueAsString(buildSummaryMap(run)));
        } catch (Exception e) {
            run.setSummaryJson("{}");
        }

        return postureRunRepository.save(run);
    }

    private List<PolicyExceptionEntity> collectActiveExceptions(UUID projectId) {
        List<PolicyExceptionEntity> exceptions = new ArrayList<>();
        Instant now = Instant.now();

        // Load project-scoped exceptions if projectId is provided
        if (projectId != null) {
            List<PolicyExceptionEntity> projectExceptions = policyExceptionRepository
                    .findByProjectIdAndStatus(projectId, "ACTIVE");
            for (PolicyExceptionEntity ex : projectExceptions) {
                if (!isExpired(ex, now)) {
                    exceptions.add(ex);
                }
            }
        }

        // Load global exceptions
        List<PolicyExceptionEntity> globalExceptions = policyExceptionRepository
                .findByScopeAndStatus("GLOBAL", "ACTIVE");
        for (PolicyExceptionEntity ex : globalExceptions) {
            if (!isExpired(ex, now)) {
                exceptions.add(ex);
            }
        }

        return exceptions;
    }

    private boolean isExpired(PolicyExceptionEntity exception, Instant now) {
        return exception.getExpiresAt() != null && exception.getExpiresAt().isBefore(now);
    }

    private PolicyExceptionEntity findMatchingException(String ruleId, String componentName,
                                                        String componentVersion,
                                                        List<PolicyExceptionEntity> exceptions) {
        for (PolicyExceptionEntity ex : exceptions) {
            // Check ruleId match
            if (ex.getRuleId() != null && !ex.getRuleId().equals(ruleId)) {
                continue; // Rule ID doesn't match
            }

            // Check packageName match
            if (ex.getPackageName() != null && componentName != null) {
                if (!ex.getPackageName().equals(componentName)) {
                    continue; // Package name doesn't match
                }
            }

            // Check packageVersion match
            if (ex.getPackageVersion() != null && componentVersion != null) {
                if (!ex.getPackageVersion().equals(componentVersion)) {
                    continue; // Version doesn't match
                }
            }

            // All checks passed - this exception matches
            return ex;
        }
        return null;
    }

    private RuleResult.Decision computeOverallDecision(Collection<RuleResult.Decision> decisions) {
        for (RuleResult.Decision d : decisions) {
            if (d == RuleResult.Decision.FAIL) {
                return RuleResult.Decision.FAIL;
            }
        }
        for (RuleResult.Decision d : decisions) {
            if (d == RuleResult.Decision.WARN) {
                return RuleResult.Decision.WARN;
            }
        }
        return RuleResult.Decision.PASS;
    }

    private double computeOverallScore(List<ComponentResultEntity> componentResults) {
        if (componentResults.isEmpty()) {
            return 100.0;
        }

        int totalFindings = 0;
        int passedFindings = 0;

        for (ComponentResultEntity comp : componentResults) {
            for (FindingEntity finding : comp.getFindings()) {
                totalFindings++;
                if ("PASS".equals(finding.getDecision())) {
                    passedFindings++;
                }
            }
        }

        if (totalFindings == 0) {
            return 100.0;
        }

        return (passedFindings * 100.0) / totalFindings;
    }

    private void updateSummary(PostureRunEntity run) {
        int pass = 0, warn = 0, fail = 0;
        for (ComponentResultEntity comp : run.getComponentResults()) {
            for (FindingEntity finding : comp.getFindings()) {
                String decision = finding.getDecision();
                if ("PASS".equals(decision)) {
                    pass++;
                } else if ("WARN".equals(decision)) {
                    warn++;
                } else if ("FAIL".equals(decision)) {
                    fail++;
                }
            }
        }

        Map<String, Object> summaryMap = new LinkedHashMap<>();
        summaryMap.put("pass", pass);
        summaryMap.put("warn", warn);
        summaryMap.put("fail", fail);
        summaryMap.put("total", pass + warn + fail);

        try {
            run.setSummaryJson(objectMapper.writeValueAsString(summaryMap));
        } catch (Exception e) {
            run.setSummaryJson("{}");
        }
    }

    private Map<String, Object> buildSummaryMap(PostureRunEntity run) {
        int pass = 0, warn = 0, fail = 0;
        for (ComponentResultEntity comp : run.getComponentResults()) {
            for (FindingEntity finding : comp.getFindings()) {
                String decision = finding.getDecision();
                if ("PASS".equals(decision)) {
                    pass++;
                } else if ("WARN".equals(decision)) {
                    warn++;
                } else if ("FAIL".equals(decision)) {
                    fail++;
                }
            }
        }

        Map<String, Object> summaryMap = new LinkedHashMap<>();
        summaryMap.put("pass", pass);
        summaryMap.put("warn", warn);
        summaryMap.put("fail", fail);
        summaryMap.put("total", pass + warn + fail);
        return summaryMap;
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
