package com.scrutinizer.api.controller;

import com.scrutinizer.api.dto.*;
import com.scrutinizer.api.entity.PostureRunEntity;
import com.scrutinizer.api.mapper.PostureRunMapper;
import com.scrutinizer.api.repository.FindingRepository;
import com.scrutinizer.api.repository.PostureRunRepository;
import com.scrutinizer.api.service.PostureRunService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/runs")
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
    public Page<PostureRunSummaryDto> listRuns(
            @RequestParam(required = false) String applicationName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

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
    public PostureRunDetailDto getRunDetail(@PathVariable UUID id) {
        PostureRunEntity entity = postureRunRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Run not found"));
        return mapper.toDetailDto(entity);
    }

    @GetMapping("/{id}/findings")
    public Page<FindingDto> getRunFindings(
            @PathVariable UUID id,
            @RequestParam(required = false) String decision,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size);

        if (decision != null && !decision.isBlank()) {
            return findingRepository.findByPostureRunIdAndDecision(id, decision.toUpperCase(), pageable)
                    .map(mapper::toFindingDto);
        }

        return findingRepository.findByPostureRunId(id, pageable)
                .map(mapper::toFindingDto);
    }

    @GetMapping("/trends")
    public List<TrendDataPoint> getTrends(@RequestParam String applicationName) {
        return postureRunRepository
                .findByApplicationNameOrderByRunTimestampAsc(applicationName)
                .stream()
                .map(mapper::toTrendPoint)
                .toList();
    }
}
