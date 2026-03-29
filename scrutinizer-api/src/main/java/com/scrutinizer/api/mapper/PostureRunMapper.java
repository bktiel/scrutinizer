package com.scrutinizer.api.mapper;

import com.scrutinizer.api.dto.FindingDto;
import com.scrutinizer.api.dto.PostureRunDetailDto;
import com.scrutinizer.api.dto.PostureRunSummaryDto;
import com.scrutinizer.api.dto.TrendDataPoint;
import com.scrutinizer.api.entity.ComponentResultEntity;
import com.scrutinizer.api.entity.FindingEntity;
import com.scrutinizer.api.entity.PostureRunEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PostureRunMapper {

    public PostureRunSummaryDto toSummaryDto(PostureRunEntity entity) {
        return new PostureRunSummaryDto(
                entity.getId(),
                entity.getApplicationName(),
                entity.getPolicyName(),
                entity.getPolicyVersion(),
                entity.getOverallDecision(),
                entity.getPostureScore(),
                entity.getRunTimestamp()
        );
    }

    public PostureRunDetailDto toDetailDto(PostureRunEntity entity) {
        List<PostureRunDetailDto.ComponentResultDto> componentDtos = entity.getComponentResults().stream()
                .map(this::toComponentDto)
                .toList();

        return new PostureRunDetailDto(
                entity.getId(),
                entity.getApplicationName(),
                entity.getSbomHash(),
                entity.getPolicyName(),
                entity.getPolicyVersion(),
                entity.getOverallDecision(),
                entity.getPostureScore(),
                entity.getSummaryJson(),
                entity.getRunTimestamp(),
                entity.getCreatedAt(),
                entity.getReviewStatus(),
                entity.getReviewerNotes(),
                entity.getReviewedAt(),
                entity.getMetricsJson(),
                componentDtos
        );
    }

    public FindingDto toFindingDto(FindingEntity entity) {
        ComponentResultEntity cr = entity.getComponentResult();
        return new FindingDto(
                entity.getId(),
                entity.getRuleId(),
                entity.getDecision(),
                entity.getSeverity(),
                entity.getField(),
                entity.getActualValue(),
                entity.getExpectedValue(),
                entity.getDescription(),
                entity.getRemediation(),
                cr != null ? cr.getComponentRef() : null,
                cr != null ? cr.getComponentName() : null
        );
    }

    public TrendDataPoint toTrendPoint(PostureRunEntity entity) {
        return new TrendDataPoint(
                entity.getRunTimestamp(),
                entity.getPostureScore(),
                entity.getOverallDecision(),
                entity.getPolicyVersion()
        );
    }

    private PostureRunDetailDto.ComponentResultDto toComponentDto(ComponentResultEntity entity) {
        List<FindingDto> findingDtos = entity.getFindings().stream()
                .map(this::toFindingDto)
                .toList();

        return new PostureRunDetailDto.ComponentResultDto(
                entity.getId(),
                entity.getComponentRef(),
                entity.getComponentName(),
                entity.getComponentVersion(),
                entity.getPurl(),
                entity.isDirect(),
                entity.getDecision(),
                findingDtos
        );
    }
}
