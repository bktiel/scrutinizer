package com.scrutinizer.api.mapper;

import com.scrutinizer.api.dto.FindingDto;
import com.scrutinizer.api.dto.PostureRunDetailDto;
import com.scrutinizer.api.dto.PostureRunSummaryDto;
import com.scrutinizer.api.dto.TrendDataPoint;
import com.scrutinizer.api.entity.ComponentResultEntity;
import com.scrutinizer.api.entity.FindingEntity;
import com.scrutinizer.api.entity.PostureRunEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PostureRunMapperTest {

    private PostureRunMapper mapper;
    private UUID runId;
    private PostureRunEntity postureRunEntity;

    @BeforeEach
    void setup() {
        mapper = new PostureRunMapper();
        runId = UUID.randomUUID();
        postureRunEntity = new PostureRunEntity();
        postureRunEntity.setId(runId);
        postureRunEntity.setApplicationName("test-app");
        postureRunEntity.setSbomHash("hash-abc123");
        postureRunEntity.setPolicyName("security-policy");
        postureRunEntity.setPolicyVersion("1.0");
        postureRunEntity.setOverallDecision("PASS");
        postureRunEntity.setPostureScore(95.5);
        postureRunEntity.setSummaryJson("{\"pass\": 10, \"warn\": 2, \"fail\": 0}");
        postureRunEntity.setRunTimestamp(Instant.parse("2024-03-29T12:00:00Z"));
        postureRunEntity.setCreatedAt(Instant.parse("2024-03-29T11:50:00Z"));
        postureRunEntity.setReviewStatus("PENDING");
        postureRunEntity.setReviewerNotes(null);
        postureRunEntity.setReviewedAt(null);
        postureRunEntity.setMetricsJson("{\"metric1\": 100}");
    }

    @Nested
    class ToSummaryDto {
        @Test
        void shouldMapPostureRunEntityToSummaryDto() {
            PostureRunSummaryDto result = mapper.toSummaryDto(postureRunEntity);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(runId);
            assertThat(result.applicationName()).isEqualTo("test-app");
            assertThat(result.policyName()).isEqualTo("security-policy");
            assertThat(result.policyVersion()).isEqualTo("1.0");
            assertThat(result.overallDecision()).isEqualTo("PASS");
            assertThat(result.postureScore()).isEqualTo(95.5);
            assertThat(result.runTimestamp()).isEqualTo(Instant.parse("2024-03-29T12:00:00Z"));
        }

        @Test
        void shouldHandleNullFieldsInSummaryDto() {
            PostureRunEntity entity = new PostureRunEntity();
            entity.setId(UUID.randomUUID());
            entity.setApplicationName(null);
            entity.setPolicyName(null);
            entity.setPostureScore(0.0);

            PostureRunSummaryDto result = mapper.toSummaryDto(entity);

            assertThat(result).isNotNull();
            assertThat(result.applicationName()).isNull();
            assertThat(result.policyName()).isNull();
            assertThat(result.postureScore()).isEqualTo(0.0);
        }

        @Test
        void shouldMapAllFieldsCorrectly() {
            PostureRunSummaryDto result = mapper.toSummaryDto(postureRunEntity);

            assertThat(result.id()).isNotNull();
            assertThat(result.applicationName()).isNotNull();
            assertThat(result.policyName()).isNotNull();
            assertThat(result.policyVersion()).isNotNull();
            assertThat(result.overallDecision()).isNotNull();
            assertThat(result.postureScore()).isNotNaN();
            assertThat(result.runTimestamp()).isNotNull();
        }
    }

    @Nested
    class ToDetailDto {
        @Test
        void shouldMapPostureRunEntityToDetailDto() {
            PostureRunDetailDto result = mapper.toDetailDto(postureRunEntity);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(runId);
            assertThat(result.applicationName()).isEqualTo("test-app");
            assertThat(result.sbomHash()).isEqualTo("hash-abc123");
            assertThat(result.policyName()).isEqualTo("security-policy");
            assertThat(result.policyVersion()).isEqualTo("1.0");
            assertThat(result.overallDecision()).isEqualTo("PASS");
            assertThat(result.postureScore()).isEqualTo(95.5);
            assertThat(result.summaryJson()).isEqualTo("{\"pass\": 10, \"warn\": 2, \"fail\": 0}");
            assertThat(result.runTimestamp()).isEqualTo(Instant.parse("2024-03-29T12:00:00Z"));
            assertThat(result.createdAt()).isEqualTo(Instant.parse("2024-03-29T11:50:00Z"));
            assertThat(result.reviewStatus()).isEqualTo("PENDING");
            assertThat(result.reviewerNotes()).isNull();
            assertThat(result.reviewedAt()).isNull();
            assertThat(result.metricsJson()).isEqualTo("{\"metric1\": 100}");
        }

        @Test
        void shouldIncludeComponentResults() {
            ComponentResultEntity comp1 = new ComponentResultEntity();
            comp1.setId(UUID.randomUUID());
            comp1.setComponentRef("pkg:npm/lodash@4.17.21");
            comp1.setComponentName("lodash");
            comp1.setComponentVersion("4.17.21");
            comp1.setDecision("PASS");
            comp1.setDirect(true);

            ComponentResultEntity comp2 = new ComponentResultEntity();
            comp2.setId(UUID.randomUUID());
            comp2.setComponentRef("pkg:npm/express@4.18.0");
            comp2.setComponentName("express");
            comp2.setComponentVersion("4.18.0");
            comp2.setDecision("FAIL");
            comp2.setDirect(false);

            postureRunEntity.getComponentResults().add(comp1);
            postureRunEntity.getComponentResults().add(comp2);

            PostureRunDetailDto result = mapper.toDetailDto(postureRunEntity);

            assertThat(result.componentResults()).hasSize(2);
            assertThat(result.componentResults().get(0).componentName()).isEqualTo("lodash");
            assertThat(result.componentResults().get(1).componentName()).isEqualTo("express");
        }

        @Test
        void shouldHandleEmptyComponentResults() {
            PostureRunDetailDto result = mapper.toDetailDto(postureRunEntity);

            assertThat(result.componentResults()).isNotNull();
            assertThat(result.componentResults()).isEmpty();
        }

        @Test
        void shouldMapReviewStatus() {
            postureRunEntity.setReviewStatus("APPROVED");
            postureRunEntity.setReviewerNotes("Looks good");
            postureRunEntity.setReviewedAt(Instant.parse("2024-03-29T13:00:00Z"));

            PostureRunDetailDto result = mapper.toDetailDto(postureRunEntity);

            assertThat(result.reviewStatus()).isEqualTo("APPROVED");
            assertThat(result.reviewerNotes()).isEqualTo("Looks good");
            assertThat(result.reviewedAt()).isEqualTo(Instant.parse("2024-03-29T13:00:00Z"));
        }
    }

    @Nested
    class ToFindingDto {
        @Test
        void shouldMapFindingEntityToFindingDto() {
            ComponentResultEntity comp = new ComponentResultEntity();
            comp.setComponentRef("pkg:npm/lodash@4.17.21");
            comp.setComponentName("lodash");

            FindingEntity finding = new FindingEntity();
            finding.setId(UUID.randomUUID());
            finding.setRuleId("RULE-001");
            finding.setDecision("FAIL");
            finding.setSeverity("HIGH");
            finding.setField("metadata");
            finding.setActualValue("oldValue");
            finding.setExpectedValue("newValue");
            finding.setDescription("Test description");
            finding.setRemediation("Test remediation");
            finding.setComponentResult(comp);

            FindingDto result = mapper.toFindingDto(finding);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(finding.getId());
            assertThat(result.ruleId()).isEqualTo("RULE-001");
            assertThat(result.decision()).isEqualTo("FAIL");
            assertThat(result.severity()).isEqualTo("HIGH");
            assertThat(result.field()).isEqualTo("metadata");
            assertThat(result.actualValue()).isEqualTo("oldValue");
            assertThat(result.expectedValue()).isEqualTo("newValue");
            assertThat(result.description()).isEqualTo("Test description");
            assertThat(result.remediation()).isEqualTo("Test remediation");
            assertThat(result.componentRef()).isEqualTo("pkg:npm/lodash@4.17.21");
            assertThat(result.componentName()).isEqualTo("lodash");
        }

        @Test
        void shouldHandleNullComponentResult() {
            FindingEntity finding = new FindingEntity();
            finding.setId(UUID.randomUUID());
            finding.setRuleId("RULE-002");
            finding.setDecision("WARN");
            finding.setSeverity("MEDIUM");
            finding.setField("field");
            finding.setActualValue("actual");
            finding.setExpectedValue("expected");
            finding.setDescription("desc");
            finding.setRemediation("rem");
            finding.setComponentResult(null);

            FindingDto result = mapper.toFindingDto(finding);

            assertThat(result).isNotNull();
            assertThat(result.ruleId()).isEqualTo("RULE-002");
            assertThat(result.componentRef()).isNull();
            assertThat(result.componentName()).isNull();
        }

        @Test
        void shouldHandleNullFieldsInFinding() {
            FindingEntity finding = new FindingEntity();
            finding.setId(UUID.randomUUID());
            finding.setRuleId("RULE-003");
            finding.setDecision("PASS");
            finding.setSeverity("LOW");
            finding.setField("field");
            finding.setActualValue(null);
            finding.setExpectedValue(null);
            finding.setDescription(null);
            finding.setRemediation(null);
            finding.setComponentResult(null);

            FindingDto result = mapper.toFindingDto(finding);

            assertThat(result).isNotNull();
            assertThat(result.actualValue()).isNull();
            assertThat(result.expectedValue()).isNull();
            assertThat(result.description()).isNull();
            assertThat(result.remediation()).isNull();
        }

        @Test
        void shouldMapAllDecisionTypes() {
            ComponentResultEntity comp = new ComponentResultEntity();
            comp.setComponentName("test");

            for (String decision : List.of("PASS", "WARN", "FAIL")) {
                FindingEntity finding = new FindingEntity();
                finding.setId(UUID.randomUUID());
                finding.setRuleId("RULE-" + decision);
                finding.setDecision(decision);
                finding.setSeverity("HIGH");
                finding.setField("field");
                finding.setDescription("desc");
                finding.setComponentResult(comp);

                FindingDto result = mapper.toFindingDto(finding);

                assertThat(result.decision()).isEqualTo(decision);
            }
        }
    }

    @Nested
    class ToTrendPoint {
        @Test
        void shouldMapPostureRunEntityToTrendDataPoint() {
            TrendDataPoint result = mapper.toTrendPoint(postureRunEntity);

            assertThat(result).isNotNull();
            assertThat(result.timestamp()).isEqualTo(Instant.parse("2024-03-29T12:00:00Z"));
            assertThat(result.postureScore()).isEqualTo(95.5);
            assertThat(result.overallDecision()).isEqualTo("PASS");
            assertThat(result.policyVersion()).isEqualTo("1.0");
        }

        @Test
        void shouldHandleVariousScores() {
            PostureRunEntity entity100 = new PostureRunEntity();
            entity100.setPostureScore(100.0);
            entity100.setOverallDecision("PASS");
            entity100.setPolicyVersion("1.0");
            entity100.setRunTimestamp(Instant.now());

            TrendDataPoint result100 = mapper.toTrendPoint(entity100);
            assertThat(result100.postureScore()).isEqualTo(100.0);

            PostureRunEntity entity0 = new PostureRunEntity();
            entity0.setPostureScore(0.0);
            entity0.setOverallDecision("FAIL");
            entity0.setPolicyVersion("1.0");
            entity0.setRunTimestamp(Instant.now());

            TrendDataPoint result0 = mapper.toTrendPoint(entity0);
            assertThat(result0.postureScore()).isEqualTo(0.0);
        }

        @Test
        void shouldHandleVariousDecisions() {
            for (String decision : List.of("PASS", "WARN", "FAIL")) {
                PostureRunEntity entity = new PostureRunEntity();
                entity.setPostureScore(85.0);
                entity.setOverallDecision(decision);
                entity.setPolicyVersion("1.0");
                entity.setRunTimestamp(Instant.now());

                TrendDataPoint result = mapper.toTrendPoint(entity);

                assertThat(result.overallDecision()).isEqualTo(decision);
            }
        }

        @Test
        void shouldMapMultipleVersions() {
            for (String version : List.of("1.0", "1.1", "2.0", "2.1.0")) {
                PostureRunEntity entity = new PostureRunEntity();
                entity.setPostureScore(90.0);
                entity.setOverallDecision("PASS");
                entity.setPolicyVersion(version);
                entity.setRunTimestamp(Instant.now());

                TrendDataPoint result = mapper.toTrendPoint(entity);

                assertThat(result.policyVersion()).isEqualTo(version);
            }
        }
    }

    @Nested
    class ComponentResultMapping {
        @Test
        void shouldMapComponentResultWithFindings() {
            ComponentResultEntity comp = new ComponentResultEntity();
            comp.setId(UUID.randomUUID());
            comp.setComponentRef("pkg:npm/test@1.0");
            comp.setComponentName("test");
            comp.setComponentVersion("1.0");
            comp.setPurl("pkg:npm/test@1.0");
            comp.setDirect(true);
            comp.setDecision("PASS");

            FindingEntity finding = new FindingEntity();
            finding.setId(UUID.randomUUID());
            finding.setRuleId("RULE-001");
            finding.setDecision("PASS");
            finding.setSeverity("HIGH");
            finding.setField("field");
            finding.setActualValue("actual");
            finding.setExpectedValue("expected");
            finding.setDescription("desc");
            finding.setRemediation("rem");

            comp.getFindings().add(finding);
            postureRunEntity.getComponentResults().add(comp);

            PostureRunDetailDto result = mapper.toDetailDto(postureRunEntity);

            assertThat(result.componentResults()).hasSize(1);
            PostureRunDetailDto.ComponentResultDto compDto = result.componentResults().get(0);
            assertThat(compDto.id()).isEqualTo(comp.getId());
            assertThat(compDto.componentRef()).isEqualTo("pkg:npm/test@1.0");
            assertThat(compDto.componentName()).isEqualTo("test");
            assertThat(compDto.componentVersion()).isEqualTo("1.0");
            assertThat(compDto.purl()).isEqualTo("pkg:npm/test@1.0");
            assertThat(compDto.isDirect()).isTrue();
            assertThat(compDto.decision()).isEqualTo("PASS");
            assertThat(compDto.findings()).hasSize(1);
        }

        @Test
        void shouldHandleComponentWithoutFindings() {
            ComponentResultEntity comp = new ComponentResultEntity();
            comp.setId(UUID.randomUUID());
            comp.setComponentRef("pkg:npm/empty@1.0");
            comp.setComponentName("empty");
            comp.setComponentVersion("1.0");
            comp.setDecision("PASS");

            postureRunEntity.getComponentResults().add(comp);

            PostureRunDetailDto result = mapper.toDetailDto(postureRunEntity);

            assertThat(result.componentResults()).hasSize(1);
            assertThat(result.componentResults().get(0).findings()).isEmpty();
        }
    }
}
