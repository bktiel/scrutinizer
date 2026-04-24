package com.scrutinizer.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrutinizer.api.dto.FindingDto;
import com.scrutinizer.api.dto.PostureRunDetailDto;
import com.scrutinizer.api.dto.PostureRunSummaryDto;
import com.scrutinizer.api.dto.ReviewRequest;
import com.scrutinizer.api.dto.TrendDataPoint;
import com.scrutinizer.api.entity.ComponentResultEntity;
import com.scrutinizer.api.entity.FindingEntity;
import com.scrutinizer.api.entity.PostureRunEntity;
import com.scrutinizer.api.mapper.PostureRunMapper;
import com.scrutinizer.api.repository.FindingRepository;
import com.scrutinizer.api.repository.PostureRunRepository;
import com.scrutinizer.api.service.PostureRunService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostureRunController.class)
class PostureRunControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostureRunService postureRunService;

    @MockBean
    private PostureRunRepository postureRunRepository;

    @MockBean
    private FindingRepository findingRepository;

    @MockBean
    private PostureRunMapper mapper;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID runId;
    private PostureRunEntity postureRunEntity;
    private PostureRunSummaryDto summaryDto;

    @BeforeEach
    void setup() {
        runId = UUID.randomUUID();
        postureRunEntity = new PostureRunEntity();
        postureRunEntity.setId(runId);
        postureRunEntity.setApplicationName("test-app");
        postureRunEntity.setPolicyName("security-policy");
        postureRunEntity.setPolicyVersion("1.0");
        postureRunEntity.setOverallDecision("PASS");
        postureRunEntity.setPostureScore(95.0);
        postureRunEntity.setRunTimestamp(Instant.now());
        postureRunEntity.setSbomHash("abc123");

        summaryDto = new PostureRunSummaryDto(
                runId,
                "test-app",
                "security-policy",
                "1.0",
                "PASS",
                95.0,
                postureRunEntity.getRunTimestamp()
        );
    }

    @Nested
    class CreateRun {
        @Test
        void shouldCreateRunWithPolicyId() throws Exception {
            UUID policyId = UUID.randomUUID();
            String sbomJson = "{\"bomFormat\": \"CycloneDX\"}";
            MockMultipartFile sbomFile = new MockMultipartFile(
                    "sbom", "sbom.json", MediaType.APPLICATION_JSON_VALUE, sbomJson.getBytes());

            when(postureRunService.executeWithUpload("test-app", sbomJson, policyId, null))
                    .thenReturn(postureRunEntity);
            when(mapper.toSummaryDto(postureRunEntity)).thenReturn(summaryDto);

            mockMvc.perform(multipart("/api/v1/runs")
                    .file(sbomFile)
                    .param("applicationName", "test-app")
                    .param("policyId", policyId.toString())
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(runId.toString())))
                    .andExpect(jsonPath("$.applicationName", is("test-app")))
                    .andExpect(jsonPath("$.overallDecision", is("PASS")));

            verify(postureRunService).executeWithUpload("test-app", sbomJson, policyId, null);
        }

        @Test
        void shouldCreateRunWithRepositoryUrl() throws Exception {
            String repositoryUrl = "https://github.com/example/repo.git";
            String sbomJson = "{\"bomFormat\": \"CycloneDX\"}";
            MockMultipartFile sbomFile = new MockMultipartFile(
                    "sbom", "sbom.json", MediaType.APPLICATION_JSON_VALUE, sbomJson.getBytes());

            when(postureRunService.executeForRepositoryUrl(repositoryUrl, null, sbomJson))
                    .thenReturn(postureRunEntity);
            when(mapper.toSummaryDto(postureRunEntity)).thenReturn(summaryDto);

            mockMvc.perform(multipart("/api/v1/runs")
                    .file(sbomFile)
                    .param("repositoryUrl", repositoryUrl)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(runId.toString())));

            verify(postureRunService).executeForRepositoryUrl(repositoryUrl, null, sbomJson);
        }

        @Test
        void shouldFailWhenBothPolicyIdAndRepositoryUrlMissing() throws Exception {
            String sbomJson = "{\"bomFormat\": \"CycloneDX\"}";
            MockMultipartFile sbomFile = new MockMultipartFile(
                    "sbom", "sbom.json", MediaType.APPLICATION_JSON_VALUE, sbomJson.getBytes());

            mockMvc.perform(multipart("/api/v1/runs")
                    .file(sbomFile)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturnNotFoundWhenRepositoryUrlNotRegistered() throws Exception {
            String repositoryUrl = "https://github.com/unknown/repo.git";
            String sbomJson = "{\"bomFormat\": \"CycloneDX\"}";
            MockMultipartFile sbomFile = new MockMultipartFile(
                    "sbom", "sbom.json", MediaType.APPLICATION_JSON_VALUE, sbomJson.getBytes());

            when(postureRunService.executeForRepositoryUrl(repositoryUrl, null, sbomJson))
                    .thenThrow(new IllegalArgumentException("No project registered"));

            mockMvc.perform(multipart("/api/v1/runs")
                    .file(sbomFile)
                    .param("repositoryUrl", repositoryUrl)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class ListRuns {
        @Test
        void shouldListAllRuns() throws Exception {
            PostureRunEntity run2 = new PostureRunEntity();
            run2.setId(UUID.randomUUID());
            run2.setApplicationName("app2");

            PostureRunSummaryDto summaryDto2 = new PostureRunSummaryDto(
                    run2.getId(), "app2", "policy", "1.0", "PASS", 90.0, Instant.now());

            Page<PostureRunEntity> page = new PageImpl<>(List.of(postureRunEntity, run2));
            when(postureRunRepository.findAllByOrderByRunTimestampDesc(any(Pageable.class)))
                    .thenReturn(page);
            when(mapper.toSummaryDto(postureRunEntity)).thenReturn(summaryDto);
            when(mapper.toSummaryDto(run2)).thenReturn(summaryDto2);

            mockMvc.perform(get("/api/v1/runs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements", is(2)));

            verify(postureRunRepository).findAllByOrderByRunTimestampDesc(any(Pageable.class));
        }

        @Test
        void shouldListRunsFilteredByApplicationName() throws Exception {
            Page<PostureRunEntity> page = new PageImpl<>(List.of(postureRunEntity));
            when(postureRunRepository.findByApplicationNameOrderByRunTimestampDesc("test-app", PageRequest.of(0, 20)))
                    .thenReturn(page);
            when(mapper.toSummaryDto(postureRunEntity)).thenReturn(summaryDto);

            mockMvc.perform(get("/api/v1/runs")
                    .param("applicationName", "test-app"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].applicationName", is("test-app")));

            verify(postureRunRepository).findByApplicationNameOrderByRunTimestampDesc("test-app", PageRequest.of(0, 20));
        }

        @Test
        void shouldSupportPagination() throws Exception {
            Page<PostureRunEntity> page = new PageImpl<>(List.of(postureRunEntity), PageRequest.of(1, 10), 25);
            when(postureRunRepository.findAllByOrderByRunTimestampDesc(PageRequest.of(1, 10)))
                    .thenReturn(page);
            when(mapper.toSummaryDto(postureRunEntity)).thenReturn(summaryDto);

            mockMvc.perform(get("/api/v1/runs")
                    .param("page", "1")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.number", is(1)))
                    .andExpect(jsonPath("$.size", is(10)));
        }
    }

    @Nested
    class GetRunDetail {
        @Test
        void shouldReturnRunDetailWhenFound() throws Exception {
            ComponentResultEntity comp = new ComponentResultEntity();
            comp.setId(UUID.randomUUID());
            comp.setComponentRef("pkg:npm/lodash@4.17.21");
            comp.setComponentName("lodash");
            comp.setDecision("PASS");

            postureRunEntity.getComponentResults().add(comp);

            PostureRunDetailDto detailDto = new PostureRunDetailDto(
                    runId, "test-app", "hash", "policy", "1.0", "PASS", 95.0,
                    "{}", Instant.now(), Instant.now(),
                    "PENDING", null, null, null, new ArrayList<>());

            when(postureRunRepository.findByIdWithComponents(runId))
                    .thenReturn(Optional.of(postureRunEntity));
            when(mapper.toDetailDto(postureRunEntity)).thenReturn(detailDto);

            mockMvc.perform(get("/api/v1/runs/{id}", runId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(runId.toString())))
                    .andExpect(jsonPath("$.applicationName", is("test-app")));

            verify(postureRunRepository).findByIdWithComponents(runId);
        }

        @Test
        void shouldReturn404WhenRunNotFound() throws Exception {
            when(postureRunRepository.findByIdWithComponents(runId))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/runs/{id}", runId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class GetRunFindings {
        @Test
        void shouldReturnFindingsForRun() throws Exception {
            FindingEntity finding = new FindingEntity();
            finding.setId(UUID.randomUUID());
            finding.setRuleId("RULE-001");
            finding.setDecision("PASS");
            finding.setSeverity("HIGH");

            FindingDto findingDto = new FindingDto(
                    finding.getId(), "RULE-001", "PASS", "HIGH", "field",
                    "actual", "expected", "description", "remediation", "ref", "name");

            Page<FindingEntity> page = new PageImpl<>(List.of(finding));
            when(findingRepository.findByPostureRunId(runId, PageRequest.of(0, 50)))
                    .thenReturn(page);
            when(mapper.toFindingDto(finding)).thenReturn(findingDto);

            mockMvc.perform(get("/api/v1/runs/{id}/findings", runId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].ruleId", is("RULE-001")));

            verify(findingRepository).findByPostureRunId(runId, PageRequest.of(0, 50));
        }

        @Test
        void shouldFilterFindingsByDecision() throws Exception {
            FindingEntity finding = new FindingEntity();
            finding.setId(UUID.randomUUID());
            finding.setRuleId("RULE-001");
            finding.setDecision("FAIL");

            FindingDto findingDto = new FindingDto(
                    finding.getId(), "RULE-001", "FAIL", "HIGH", "field",
                    "actual", "expected", "description", "remediation", "ref", "name");

            Page<FindingEntity> page = new PageImpl<>(List.of(finding));
            when(findingRepository.findByPostureRunIdAndDecision(runId, "FAIL", PageRequest.of(0, 50)))
                    .thenReturn(page);
            when(mapper.toFindingDto(finding)).thenReturn(findingDto);

            mockMvc.perform(get("/api/v1/runs/{id}/findings", runId)
                    .param("decision", "FAIL"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].decision", is("FAIL")));

            verify(findingRepository).findByPostureRunIdAndDecision(runId, "FAIL", PageRequest.of(0, 50));
        }

        @Test
        void shouldHandleDecisionFilterCaseInsensitive() throws Exception {
            FindingEntity finding = new FindingEntity();
            finding.setId(UUID.randomUUID());
            finding.setDecision("WARN");

            FindingDto findingDto = new FindingDto(
                    finding.getId(), "RULE-001", "WARN", "MEDIUM", "field",
                    "actual", "expected", "description", "remediation", "ref", "name");

            Page<FindingEntity> page = new PageImpl<>(List.of(finding));
            when(findingRepository.findByPostureRunIdAndDecision(runId, "WARN", PageRequest.of(0, 50)))
                    .thenReturn(page);
            when(mapper.toFindingDto(finding)).thenReturn(findingDto);

            mockMvc.perform(get("/api/v1/runs/{id}/findings", runId)
                    .param("decision", "warn"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].decision", is("WARN")));
        }
    }

    @Nested
    class ReviewRun {
        @Test
        void shouldUpdateReviewStatus() throws Exception {
            postureRunEntity.setReviewStatus("PENDING");
            PostureRunDetailDto detailDto = new PostureRunDetailDto(
                    runId, "test-app", "hash", "policy", "1.0", "PASS", 95.0,
                    "{}", Instant.now(), Instant.now(),
                    "APPROVED", "Looks good", Instant.now(), null, new ArrayList<>());

            when(postureRunRepository.findByIdWithComponents(runId))
                    .thenReturn(Optional.of(postureRunEntity));
            when(postureRunRepository.save(any(PostureRunEntity.class)))
                    .thenReturn(postureRunEntity);
            when(mapper.toDetailDto(postureRunEntity)).thenReturn(detailDto);

            ReviewRequest request = new ReviewRequest("APPROVED", "Looks good");
            String requestBody = objectMapper.writeValueAsString(request);

            mockMvc.perform(patch("/api/v1/runs/{id}/review", runId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(runId.toString())));

            verify(postureRunRepository).save(any(PostureRunEntity.class));
        }

        @Test
        void shouldRejectInvalidReviewStatus() throws Exception {
            when(postureRunRepository.findByIdWithComponents(runId))
                    .thenReturn(Optional.of(postureRunEntity));

            ReviewRequest request = new ReviewRequest("INVALID_STATUS", null);
            String requestBody = objectMapper.writeValueAsString(request);

            mockMvc.perform(patch("/api/v1/runs/{id}/review", runId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturn404WhenRunNotFound() throws Exception {
            when(postureRunRepository.findByIdWithComponents(runId))
                    .thenReturn(Optional.empty());

            ReviewRequest request = new ReviewRequest("APPROVED", null);
            String requestBody = objectMapper.writeValueAsString(request);

            mockMvc.perform(patch("/api/v1/runs/{id}/review", runId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldAcceptValidStatuses() throws Exception {
            when(postureRunRepository.findByIdWithComponents(runId))
                    .thenReturn(Optional.of(postureRunEntity));
            when(postureRunRepository.save(any(PostureRunEntity.class)))
                    .thenReturn(postureRunEntity);
            when(mapper.toDetailDto(postureRunEntity))
                    .thenReturn(new PostureRunDetailDto(runId, "test-app", "hash", "policy", "1.0",
                            "PASS", 95.0, "{}", Instant.now(), Instant.now(),
                            "REJECTED", "Not approved", Instant.now(), null, new ArrayList<>()));

            for (String status : List.of("PENDING", "APPROVED", "REJECTED", "NEEDS_REVIEW")) {
                ReviewRequest request = new ReviewRequest(status, null);
                String requestBody = objectMapper.writeValueAsString(request);

                mockMvc.perform(patch("/api/v1/runs/{id}/review", runId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                        .andExpect(status().isOk());
            }
        }
    }

    @Nested
    class ExportAuditBundle {
        @Test
        void shouldExportAuditBundle() throws Exception {
            ComponentResultEntity comp = new ComponentResultEntity();
            comp.setComponentRef("pkg:npm/test@1.0");
            comp.setComponentName("test");
            comp.setComponentVersion("1.0");
            comp.setDecision("PASS");

            FindingEntity finding = new FindingEntity();
            finding.setRuleId("RULE-001");
            finding.setDecision("PASS");
            finding.setSeverity("HIGH");
            finding.setField("field");
            finding.setActualValue("actual");
            finding.setExpectedValue("expected");
            finding.setDescription("description");
            finding.setRemediation("remediation");

            comp.getFindings().add(finding);
            postureRunEntity.getComponentResults().add(comp);

            when(postureRunRepository.findByIdWithComponents(runId))
                    .thenReturn(Optional.of(postureRunEntity));

            mockMvc.perform(get("/api/v1/runs/{id}/export", runId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("application/zip"));

            verify(postureRunRepository).findByIdWithComponents(runId);
        }

        @Test
        void shouldReturn404WhenRunNotFound() throws Exception {
            when(postureRunRepository.findByIdWithComponents(runId))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/runs/{id}/export", runId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class GetTrends {
        @Test
        void shouldReturnTrendPoints() throws Exception {
            PostureRunEntity run2 = new PostureRunEntity();
            run2.setId(UUID.randomUUID());
            run2.setApplicationName("test-app");
            run2.setPostureScore(90.0);
            run2.setOverallDecision("WARN");
            run2.setPolicyVersion("1.0");
            run2.setRunTimestamp(Instant.now().plusSeconds(3600));

            TrendDataPoint point1 = new TrendDataPoint(
                    postureRunEntity.getRunTimestamp(), 95.0, "PASS", "1.0");
            TrendDataPoint point2 = new TrendDataPoint(
                    run2.getRunTimestamp(), 90.0, "WARN", "1.0");

            when(postureRunRepository.findByApplicationNameOrderByRunTimestampAsc("test-app"))
                    .thenReturn(List.of(postureRunEntity, run2));
            when(mapper.toTrendPoint(postureRunEntity)).thenReturn(point1);
            when(mapper.toTrendPoint(run2)).thenReturn(point2);

            mockMvc.perform(get("/api/v1/runs/trends")
                    .param("applicationName", "test-app"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].overallDecision", is("PASS")))
                    .andExpect(jsonPath("$[1].overallDecision", is("WARN")));

            verify(postureRunRepository).findByApplicationNameOrderByRunTimestampAsc("test-app");
        }

        @Test
        void shouldReturnEmptyListWhenNoRuns() throws Exception {
            when(postureRunRepository.findByApplicationNameOrderByRunTimestampAsc("unknown-app"))
                    .thenReturn(new ArrayList<>());

            mockMvc.perform(get("/api/v1/runs/trends")
                    .param("applicationName", "unknown-app"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }
}
