package com.scrutinizer.api.controller;

import com.scrutinizer.api.dto.*;
import com.scrutinizer.api.entity.ProjectEntity;
import com.scrutinizer.api.repository.ComponentResultRepository;
import com.scrutinizer.api.repository.PolicyRepository;
import com.scrutinizer.api.repository.PostureRunRepository;
import com.scrutinizer.api.repository.ProjectRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projects", description = "Manage projects and view their posture evaluation history")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final PostureRunRepository postureRunRepository;
    private final ComponentResultRepository componentResultRepository;
    private final PolicyRepository policyRepository;

    public ProjectController(ProjectRepository projectRepository,
                            PostureRunRepository postureRunRepository,
                            ComponentResultRepository componentResultRepository,
                            PolicyRepository policyRepository) {
        this.projectRepository = projectRepository;
        this.postureRunRepository = postureRunRepository;
        this.componentResultRepository = componentResultRepository;
        this.policyRepository = policyRepository;
    }

    @PostMapping
    @Operation(summary = "Create a new project",
               description = "Register a new project in Scrutinizer. Optionally assign a policy.")
    @ApiResponse(responseCode = "201", description = "Project created")
    @ApiResponse(responseCode = "409", description = "Project name already exists")
    public ResponseEntity<ProjectDto> createProject(@RequestBody CreateProjectRequest request) {
        if (projectRepository.findByName(request.name()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Project name already exists: " + request.name());
        }

        ProjectEntity project = new ProjectEntity();
        project.setName(request.name());
        project.setDescription(request.description());
        project.setRepositoryUrl(request.repositoryUrl());
        project.setGitlabProjectId(request.gitlabProjectId());
        if (request.defaultBranch() != null) {
            project.setDefaultBranch(request.defaultBranch());
        }
        project.setPolicyId(request.policyId());

        ProjectEntity saved = projectRepository.save(project);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @GetMapping
    @Operation(summary = "List all projects",
               description = "Get paginated list of projects with their stats.")
    public Page<ProjectDto> listProjects(
            @Parameter(description = "Page number (zero-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return projectRepository.findAll(pageable)
                .map(this::toDto);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project detail",
               description = "Retrieve full project details including stats and policy assignment.")
    @ApiResponse(responseCode = "200", description = "Project found")
    @ApiResponse(responseCode = "404", description = "Project not found")
    public ProjectDto getProject(@PathVariable UUID id) {
        ProjectEntity project = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
        return toDto(project);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update project",
               description = "Update project metadata (name, description, repository URL, etc.).")
    @ApiResponse(responseCode = "200", description = "Project updated")
    @ApiResponse(responseCode = "404", description = "Project not found")
    @Transactional
    public ProjectDto updateProject(@PathVariable UUID id, @RequestBody CreateProjectRequest request) {
        ProjectEntity project = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        if (!project.getName().equals(request.name()) &&
            projectRepository.findByName(request.name()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Project name already exists: " + request.name());
        }

        project.setName(request.name());
        project.setDescription(request.description());
        project.setRepositoryUrl(request.repositoryUrl());
        project.setGitlabProjectId(request.gitlabProjectId());
        if (request.defaultBranch() != null) {
            project.setDefaultBranch(request.defaultBranch());
        }
        project.setPolicyId(request.policyId());

        ProjectEntity updated = projectRepository.save(project);
        return toDto(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete project",
               description = "Delete a project. Associated posture runs are retained but delinked.")
    @ApiResponse(responseCode = "204", description = "Project deleted")
    @ApiResponse(responseCode = "404", description = "Project not found")
    @Transactional
    public ResponseEntity<Void> deleteProject(@PathVariable UUID id) {
        if (!projectRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
        projectRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/policy")
    @Operation(summary = "Assign policy to project",
               description = "Associate a policy with the project for future evaluations.")
    @ApiResponse(responseCode = "200", description = "Policy assigned")
    @ApiResponse(responseCode = "404", description = "Project or policy not found")
    @Transactional
    public ProjectDto assignPolicy(@PathVariable UUID id, @RequestBody AssignPolicyRequest request) {
        ProjectEntity project = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));

        if (request.policyId() != null && !policyRepository.existsById(request.policyId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Policy not found");
        }

        project.setPolicyId(request.policyId());
        ProjectEntity updated = projectRepository.save(project);
        return toDto(updated);
    }

    @GetMapping("/{id}/runs")
    @Operation(summary = "Get runs for project",
               description = "Paginated list of posture runs associated with this project.")
    public Page<PostureRunSummaryDto> getProjectRuns(
            @PathVariable UUID id,
            @Parameter(description = "Page number (zero-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        if (!projectRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }

        Pageable pageable = PageRequest.of(page, size);
        return postureRunRepository.findByProjectIdOrderByRunTimestampDesc(id, pageable)
                .map(this::toPostureRunSummaryDto);
    }

    @GetMapping("/{id}/components")
    @Operation(summary = "Get latest component results",
               description = "Component results from the latest posture run of this project.")
    @ApiResponse(responseCode = "200", description = "Component results found")
    @ApiResponse(responseCode = "404", description = "Project or run not found")
    @Transactional(readOnly = true)
    public List<ComponentResultDto> getProjectComponents(@PathVariable UUID id) {
        if (!projectRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }

        return postureRunRepository.findFirstByProjectIdOrderByRunTimestampDesc(id)
                .map(run -> run.getComponentResults().stream()
                        .map(this::toComponentResultDto)
                        .collect(Collectors.toList()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No runs found for project"));
    }

    @GetMapping("/{id}/trends")
    @Operation(summary = "Get trend data",
               description = "Historical trend data (scores, decisions) for the project.")
    public List<TrendDataPoint> getProjectTrends(@PathVariable UUID id) {
        if (!projectRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }

        return postureRunRepository.findByProjectIdOrderByRunTimestampAsc(id)
                .stream()
                .map(this::toTrendPoint)
                .collect(Collectors.toList());
    }

    private ProjectDto toDto(ProjectEntity entity) {
        String policyName = null;
        if (entity.getPolicyId() != null) {
            policyName = policyRepository.findById(entity.getPolicyId())
                    .map(p -> p.getName())
                    .orElse(null);
        }

        ProjectStatsDto stats = computeStats(entity.getId());

        return new ProjectDto(
            entity.getId(),
            entity.getName(),
            entity.getDescription(),
            entity.getRepositoryUrl(),
            entity.getGitlabProjectId(),
            entity.getDefaultBranch(),
            entity.getPolicyId(),
            policyName,
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            stats
        );
    }

    private ProjectStatsDto computeStats(UUID projectId) {
        long totalRuns = postureRunRepository.countByProjectId(projectId);

        if (totalRuns == 0) {
            return new ProjectStatsDto(0, 0, 0, 0, 0, 0.0, null, null, 0.0, 0.0);
        }

        var latestRun = postureRunRepository.findFirstByProjectIdOrderByRunTimestampDesc(projectId);

        long totalComponents = 0;
        long passCount = 0;
        long failCount = 0;
        long warnCount = 0;
        double latestScore = 0.0;
        String latestDecision = null;
        Instant lastRunAt = null;

        if (latestRun.isPresent()) {
            var run = latestRun.get();
            latestScore = run.getPostureScore();
            latestDecision = run.getOverallDecision();
            lastRunAt = run.getRunTimestamp();

            totalComponents = run.getComponentResults().size();
            for (var comp : run.getComponentResults()) {
                switch (comp.getDecision()) {
                    case "PASS":
                        passCount++;
                        break;
                    case "FAIL":
                        failCount++;
                        break;
                    case "WARN":
                        warnCount++;
                        break;
                }
            }
        }

        return new ProjectStatsDto(
            totalRuns,
            totalComponents,
            passCount,
            failCount,
            warnCount,
            latestScore,
            latestDecision,
            lastRunAt,
            0.0,
            0.0
        );
    }

    private PostureRunSummaryDto toPostureRunSummaryDto(com.scrutinizer.api.entity.PostureRunEntity entity) {
        return new PostureRunSummaryDto(
            entity.getId(),
            entity.getApplicationName(),
            entity.getPolicyName(),
            entity.getPolicyVersion(),
            entity.getOverallDecision(),
            entity.getPostureScore(),
            entity.getRunTimestamp(),
            entity.getCreatedAt(),
            entity.getReviewStatus()
        );
    }

    private ComponentResultDto toComponentResultDto(com.scrutinizer.api.entity.ComponentResultEntity entity) {
        return new ComponentResultDto(
            entity.getId(),
            entity.getComponentRef(),
            entity.getComponentName(),
            entity.getComponentVersion(),
            entity.getPurl(),
            entity.isDirect(),
            entity.getDecision()
        );
    }

    private TrendDataPoint toTrendPoint(com.scrutinizer.api.entity.PostureRunEntity entity) {
        return new TrendDataPoint(
            entity.getRunTimestamp(),
            entity.getPostureScore(),
            entity.getOverallDecision()
        );
    }
}
