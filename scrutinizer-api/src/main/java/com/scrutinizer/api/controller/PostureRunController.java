package com.scrutinizer.api.controller;

import com.scrutinizer.api.dto.*;
import com.scrutinizer.api.entity.PostureRunEntity;
import com.scrutinizer.api.mapper.PostureRunMapper;
import com.scrutinizer.api.repository.FindingRepository;
import com.scrutinizer.api.repository.PostureRunRepository;
import com.scrutinizer.api.service.PostureRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/runs")
@Tag(name = "Posture Runs", description = "Evaluate SBOMs against policies and manage posture run results")
public class PostureRunController {

    private final PostureRunService postureRunService;
    private final PostureRunRepository postureRunRepository;
    private final FindingRepository findingRepository;
    private final PostureRunMapper mapper;

    public PostureRunController(PostureRunService postureRunService,
                                 PostureRunRepository postureRunRepository,
                                 FindingRepository findingRepository,
                                 PostureRunMapper mapper) {
        this.postureRunService = postureRunService;
        this.postureRunRepository = postureRunRepository;
        this.findingRepository = findingRepository;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(summary = "Trigger a posture evaluation",
               description = "Parses the SBOM, enriches dependencies with scorecard and provenance signals, "
                       + "evaluates against the policy, and persists the result.")
    @ApiResponse(responseCode = "201", description = "Evaluation completed and persisted")
    @ApiResponse(responseCode = "400", description = "Invalid SBOM, policy, or application name")
    public ResponseEntity<PostureRunSummaryDto> createRun(@RequestBody CreateRunRequest request) {
        try {
            PostureRunEntity run = postureRunService.executeAndPersist(
                    request.applicationName(),
                    Path.of(request.sbomPath()),
                    Path.of(request.policyPath())
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toSummaryDto(run));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "List posture runs", description = "Paginated list of run summaries, optionally filtered by application name.")
    public Page<PostureRunSummaryDto> listRuns(
            @Parameter(description = "Filter by application name") @RequestParam(required = false) String applicationName,
            @Parameter(description = "Page number (zero-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);

        if (applicationName != null && !applicationName.isBlank()) {
            return postureRunRepository
                    .findByApplicationNameOrderByRunTimestampDesc(applicationName, pageable)
                    .map(mapper::toSummaryDto);
        }

        return postureRunRepository.findAllByOrderByRunTimestampDesc(pageable)
                .map(mapper::toSummaryDto);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get run detail", description = "Full detail for a single run including component results and findings.")
    @ApiResponse(responseCode = "200", description = "Run found")
    @ApiResponse(responseCode = "404", description = "Run not found")
    public PostureRunDetailDto getRunDetail(@PathVariable UUID id) {
        PostureRunEntity entity = postureRunRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Run not found"));
        return mapper.toDetailDto(entity);
    }

    @GetMapping("/{id}/findings")
    @Operation(summary = "Get findings for a run", description = "Paginated findings list, optionally filtered by decision (PASS, WARN, FAIL).")
    public Page<FindingDto> getRunFindings(
            @PathVariable UUID id,
            @Parameter(description = "Filter by decision: PASS, WARN, FAIL") @RequestParam(required = false) String decision,
            @Parameter(description = "Page number (zero-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size);

        if (decision != null && !decision.isBlank()) {
            return findingRepository.findByPostureRunIdAndDecision(id, decision.toUpperCase(), pageable)
                    .map(mapper::toFindingDto);
        }

        return findingRepository.findByPostureRunId(id, pageable)
                .map(mapper::toFindingDto);
    }

    @PatchMapping("/{id}/review")
    @Operation(summary = "Review a posture run",
               description = "Set review status and optional reviewer notes. Valid statuses: PENDING, APPROVED, REJECTED, NEEDS_REVIEW.")
    @ApiResponse(responseCode = "200", description = "Review updated")
    @ApiResponse(responseCode = "400", description = "Invalid review status")
    @ApiResponse(responseCode = "404", description = "Run not found")
    public PostureRunDetailDto reviewRun(@PathVariable UUID id, @RequestBody ReviewRequest request) {
        PostureRunEntity entity = postureRunRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Run not found"));

        if (request.reviewStatus() != null && !request.reviewStatus().isBlank()) {
            String status = request.reviewStatus().toUpperCase();
            if (!List.of("PENDING", "APPROVED", "REJECTED", "NEEDS_REVIEW").contains(status)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid review status. Must be one of: PENDING, APPROVED, REJECTED, NEEDS_REVIEW");
            }
            entity.setReviewStatus(status);
        }

        if (request.reviewerNotes() != null) {
            entity.setReviewerNotes(request.reviewerNotes());
        }

        entity.setReviewedAt(Instant.now());
        postureRunRepository.save(entity);

        return mapper.toDetailDto(entity);
    }

    @GetMapping("/trends")
    @Operation(summary = "Get score trends", description = "Posture score trend data for a single application, ordered chronologically.")
    public List<TrendDataPoint> getTrends(
            @Parameter(description = "Application name", required = true) @RequestParam String applicationName) {
        return postureRunRepository
                .findByApplicationNameOrderByRunTimestampAsc(applicationName)
                .stream()
                .map(mapper::toTrendPoint)
                .toList();
    }
}
