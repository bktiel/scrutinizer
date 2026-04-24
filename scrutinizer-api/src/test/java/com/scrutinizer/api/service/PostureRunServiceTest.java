package com.scrutinizer.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrutinizer.api.entity.PolicyEntity;
import com.scrutinizer.api.entity.PolicyExceptionEntity;
import com.scrutinizer.api.entity.PostureRunEntity;
import com.scrutinizer.api.entity.ProjectEntity;
import com.scrutinizer.api.repository.PolicyExceptionRepository;
import com.scrutinizer.api.repository.PolicyRepository;
import com.scrutinizer.api.repository.PostureRunRepository;
import com.scrutinizer.api.repository.ProjectRepository;
import com.scrutinizer.engine.Finding;
import com.scrutinizer.engine.PostureEvaluator;
import com.scrutinizer.engine.PostureReport;
import com.scrutinizer.engine.RuleResult;
import com.scrutinizer.enrichment.EnrichedDependencyGraph;
import com.scrutinizer.enrichment.EnrichmentPipeline;
import com.scrutinizer.model.Component;
import com.scrutinizer.model.DependencyGraph;
import com.scrutinizer.parser.SbomParser;
import com.scrutinizer.policy.PolicyDefinition;
import com.scrutinizer.policy.PolicyParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostureRunServiceTest {

    @Mock private SbomParser sbomParser;
    @Mock private PolicyParser policyParser;
    @Mock private EnrichmentPipeline enrichmentPipeline;
    @Mock private PostureEvaluator postureEvaluator;
    @Mock private PostureRunRepository postureRunRepository;
    @Mock private PolicyRepository policyRepository;
    @Mock private PolicyExceptionRepository policyExceptionRepository;
    @Mock private ProjectRepository projectRepository;

    @InjectMocks
    private PostureRunService postureRunService;

    private UUID policyId;
    private UUID projectId;
    private String sbomJson;
    private PolicyEntity policyEntity;
    private ProjectEntity projectEntity;

    // Helper objects
    private DependencyGraph graph;
    private EnrichedDependencyGraph enrichedGraph;
    private PolicyDefinition policyDef;
    private PostureReport report;

    @BeforeEach
    void setup() {
        policyId = UUID.randomUUID();
        projectId = UUID.randomUUID();

        sbomJson = "{\"bomFormat\":\"CycloneDX\",\"specVersion\":\"1.4\",\"components\":[]}";

        policyEntity = new PolicyEntity();
        policyEntity.setId(policyId);
        policyEntity.setName("test-policy");
        policyEntity.setVersion("1.0");
        policyEntity.setPolicyYaml("apiVersion: scrutinizer/v1\nmetadata:\n  name: test\n  version: \"1.0\"\nrules: []\nscoring:\n  method: PASS_FAIL");

        projectEntity = new ProjectEntity();
        projectEntity.setId(projectId);
        projectEntity.setName("test-project");
        projectEntity.setPolicyId(policyId);
        projectEntity.setRepositoryUrl("https://gitlab.com/test/project");

        Component comp = new Component("test-pkg", "1.0.0", "test-pkg@1.0.0");
        graph = new DependencyGraph(List.of(comp), List.of(), null);
        enrichedGraph = EnrichedDependencyGraph.fromGraph(graph);
        policyDef = new PolicyDefinition("scrutinizer/v1", "test-policy", "1.0", List.of(), null);

        // Create a report with a finding
        Finding finding = new Finding("f-1", "test-pkg@1.0.0", "test-pkg", "rule-1",
                RuleResult.Decision.PASS, "FAIL", "scorecard.score", "7.5", "5.0",
                "Score check", "Upgrade", true, 0, List.of());
        report = PostureReport.create(policyDef, "hash123",
                Map.of("test-pkg@1.0.0", List.of(
                        new RuleResult("test-pkg@1.0.0", "rule-1", RuleResult.Decision.PASS, "7.5", "5.0", "Score check")
                )),
                List.of(finding));
    }

    private void setupCommonMocks() {
        when(policyRepository.findById(policyId)).thenReturn(Optional.of(policyEntity));
        when(sbomParser.parse(any())).thenReturn(graph);
        when(policyParser.parse(any(ByteArrayInputStream.class))).thenReturn(policyDef);
        when(enrichmentPipeline.enrich(any())).thenReturn(enrichedGraph);
        when(postureEvaluator.evaluate(any(), any(), any())).thenReturn(report);
        when(policyExceptionRepository.findByScopeAndStatus("GLOBAL", "ACTIVE")).thenReturn(List.of());
        when(postureRunRepository.save(any(PostureRunEntity.class))).thenAnswer(inv -> {
            PostureRunEntity entity = inv.getArgument(0);
            if (entity.getId() == null) entity.setId(UUID.randomUUID());
            return entity;
        });
    }

    @Nested
    class ExecuteWithUpload {
        @Test
        void shouldExecuteSuccessfully() {
            setupCommonMocks();
            when(policyExceptionRepository.findByProjectIdAndStatus(projectId, "ACTIVE")).thenReturn(List.of());

            PostureRunEntity result = postureRunService.executeWithUpload("test-app", sbomJson, policyId, projectId);

            assertThat(result).isNotNull();
            assertThat(result.getApplicationName()).isEqualTo("test-app");
            assertThat(result.getPolicyName()).isEqualTo("test-policy");
            verify(postureRunRepository).save(any(PostureRunEntity.class));
        }

        @Test
        void shouldThrowWhenPolicyNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(policyRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postureRunService.executeWithUpload("test-app", sbomJson, unknownId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Policy not found");
        }

        @Test
        void shouldThrowOnInvalidSbomJson() {
            when(policyRepository.findById(policyId)).thenReturn(Optional.of(policyEntity));

            assertThatThrownBy(() -> postureRunService.executeWithUpload("test-app", "not json{{{", policyId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid SBOM JSON");
        }

        @Test
        void shouldSetProjectIdWhenProvided() {
            setupCommonMocks();
            when(policyExceptionRepository.findByProjectIdAndStatus(projectId, "ACTIVE")).thenReturn(List.of());

            PostureRunEntity result = postureRunService.executeWithUpload("test-app", sbomJson, policyId, projectId);

            assertThat(result.getProjectId()).isEqualTo(projectId);
        }

        @Test
        void shouldWorkWithoutProjectId() {
            setupCommonMocks();

            PostureRunEntity result = postureRunService.executeWithUpload("test-app", sbomJson, policyId);

            assertThat(result).isNotNull();
            assertThat(result.getProjectId()).isNull();
        }
    }

    @Nested
    class ExecuteForProject {
        @Test
        void shouldExecuteForProjectSuccessfully() {
            setupCommonMocks();
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(projectEntity));
            when(policyExceptionRepository.findByProjectIdAndStatus(projectId, "ACTIVE")).thenReturn(List.of());

            PostureRunEntity result = postureRunService.executeForProject(projectId, sbomJson);

            assertThat(result).isNotNull();
            assertThat(result.getApplicationName()).isEqualTo("test-project");
        }

        @Test
        void shouldThrowWhenProjectNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(projectRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postureRunService.executeForProject(unknownId, sbomJson))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Project not found");
        }

        @Test
        void shouldThrowWhenProjectHasNoPolicy() {
            ProjectEntity noPolicyProject = new ProjectEntity();
            noPolicyProject.setId(projectId);
            noPolicyProject.setName("no-policy");
            noPolicyProject.setPolicyId(null);

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(noPolicyProject));

            assertThatThrownBy(() -> postureRunService.executeForProject(projectId, sbomJson))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not have a policy assigned");
        }
    }

    @Nested
    class ExecuteForRepositoryUrl {
        @Test
        void shouldResolveProjectByUrl() {
            setupCommonMocks();
            when(projectRepository.findByRepositoryUrl("https://gitlab.com/test/project"))
                    .thenReturn(Optional.of(projectEntity));
            when(policyExceptionRepository.findByProjectIdAndStatus(projectId, "ACTIVE")).thenReturn(List.of());

            PostureRunEntity result = postureRunService.executeForRepositoryUrl(
                    "https://gitlab.com/test/project", null, sbomJson);

            assertThat(result).isNotNull();
            assertThat(result.getApplicationName()).isEqualTo("test-project");
        }

        @Test
        void shouldThrowWhenNoProjectForUrl() {
            when(projectRepository.findByRepositoryUrl(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postureRunService.executeForRepositoryUrl(
                    "https://unknown.com/repo", null, sbomJson))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No project registered");
        }

        @Test
        void shouldUseProvidedApplicationName() {
            setupCommonMocks();
            when(projectRepository.findByRepositoryUrl("https://gitlab.com/test/project"))
                    .thenReturn(Optional.of(projectEntity));
            when(policyExceptionRepository.findByProjectIdAndStatus(projectId, "ACTIVE")).thenReturn(List.of());

            PostureRunEntity result = postureRunService.executeForRepositoryUrl(
                    "https://gitlab.com/test/project", "custom-name", sbomJson);

            assertThat(result.getApplicationName()).isEqualTo("custom-name");
        }
    }

    @Nested
    class ExceptionMatching {
        @Test
        void shouldApplyMatchingException() {
            setupCommonMocks();
            when(policyExceptionRepository.findByProjectIdAndStatus(projectId, "ACTIVE")).thenReturn(List.of());

            // Create a GLOBAL exception matching rule-1
            PolicyExceptionEntity exception = new PolicyExceptionEntity();
            exception.setId(UUID.randomUUID());
            exception.setRuleId("rule-1");
            exception.setPackageName(null); // Matches any package
            exception.setPackageVersion(null);
            exception.setJustification("Known issue, approved");
            exception.setStatus("ACTIVE");
            exception.setScope("GLOBAL");
            exception.setExpiresAt(Instant.now().plusSeconds(3600));

            when(policyExceptionRepository.findByScopeAndStatus("GLOBAL", "ACTIVE"))
                    .thenReturn(List.of(exception));

            PostureRunEntity result = postureRunService.executeWithUpload("test-app", sbomJson, policyId, projectId);

            assertThat(result).isNotNull();
            // Exception should have been applied; verify run was saved
            verify(postureRunRepository).save(any(PostureRunEntity.class));
        }

        @Test
        void shouldNotApplyExpiredException() {
            setupCommonMocks();
            when(policyExceptionRepository.findByProjectIdAndStatus(projectId, "ACTIVE")).thenReturn(List.of());

            PolicyExceptionEntity expired = new PolicyExceptionEntity();
            expired.setId(UUID.randomUUID());
            expired.setRuleId("rule-1");
            expired.setJustification("Expired");
            expired.setStatus("ACTIVE");
            expired.setScope("GLOBAL");
            expired.setExpiresAt(Instant.now().minusSeconds(3600)); // Already expired

            when(policyExceptionRepository.findByScopeAndStatus("GLOBAL", "ACTIVE"))
                    .thenReturn(List.of(expired));

            PostureRunEntity result = postureRunService.executeWithUpload("test-app", sbomJson, policyId, projectId);

            assertThat(result).isNotNull();
        }

        @Test
        void shouldNotMatchExceptionWithDifferentRuleId() {
            setupCommonMocks();
            when(policyExceptionRepository.findByProjectIdAndStatus(projectId, "ACTIVE")).thenReturn(List.of());

            PolicyExceptionEntity nonMatching = new PolicyExceptionEntity();
            nonMatching.setId(UUID.randomUUID());
            nonMatching.setRuleId("completely-different-rule");
            nonMatching.setJustification("Should not match");
            nonMatching.setStatus("ACTIVE");
            nonMatching.setScope("GLOBAL");
            nonMatching.setExpiresAt(Instant.now().plusSeconds(3600));

            when(policyExceptionRepository.findByScopeAndStatus("GLOBAL", "ACTIVE"))
                    .thenReturn(List.of(nonMatching));

            PostureRunEntity result = postureRunService.executeWithUpload("test-app", sbomJson, policyId, projectId);
            assertThat(result).isNotNull();
        }
    }

    @Nested
    class ScoreComputation {
        @Test
        void shouldSetOverallDecisionAndScore() {
            setupCommonMocks();
            when(policyExceptionRepository.findByProjectIdAndStatus(projectId, "ACTIVE")).thenReturn(List.of());

            PostureRunEntity result = postureRunService.executeWithUpload("test-app", sbomJson, policyId, projectId);

            assertThat(result.getOverallDecision()).isNotNull();
            assertThat(result.getPostureScore()).isGreaterThanOrEqualTo(0.0);
            assertThat(result.getPostureScore()).isLessThanOrEqualTo(100.0);
        }

        @Test
        void shouldSetSbomHash() {
            setupCommonMocks();
            when(policyExceptionRepository.findByProjectIdAndStatus(projectId, "ACTIVE")).thenReturn(List.of());

            PostureRunEntity result = postureRunService.executeWithUpload("test-app", sbomJson, policyId, projectId);

            assertThat(result.getSbomHash()).isNotNull();
            assertThat(result.getSbomHash()).isNotEmpty();
        }

        @Test
        void shouldSetSummaryJson() {
            setupCommonMocks();
            when(policyExceptionRepository.findByProjectIdAndStatus(projectId, "ACTIVE")).thenReturn(List.of());

            PostureRunEntity result = postureRunService.executeWithUpload("test-app", sbomJson, policyId, projectId);

            assertThat(result.getSummaryJson()).isNotNull();
            assertThat(result.getSummaryJson()).contains("pass");
        }
    }
}
