package com.scrutinizer.api.controller;

import com.scrutinizer.api.dto.CreateExceptionRequest;
import com.scrutinizer.api.dto.PolicyExceptionDto;
import com.scrutinizer.api.entity.PolicyExceptionEntity;
import com.scrutinizer.api.repository.PolicyExceptionRepository;
import com.scrutinizer.api.repository.PolicyRepository;
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
@RequestMapping("/api/v1/exceptions")
@Tag(name = "Policy Exceptions", description = "Manage temporary exceptions for rejected packages/rules")
public class PolicyExceptionController {

    private final PolicyExceptionRepository exceptionRepository;
    private final ProjectRepository projectRepository;
    private final PolicyRepository policyRepository;

    public PolicyExceptionController(PolicyExceptionRepository exceptionRepository,
                                     ProjectRepository projectRepository,
                                     PolicyRepository policyRepository) {
        this.exceptionRepository = exceptionRepository;
        this.projectRepository = projectRepository;
        this.policyRepository = policyRepository;
    }

    @PostMapping
    @Operation(summary = "Create a new exception",
               description = "Create a temporary exception for a rejected package or rule.")
    @ApiResponse(responseCode = "201", description = "Exception created")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "404", description = "Project or policy not found")
    public ResponseEntity<PolicyExceptionDto> createException(@RequestBody CreateExceptionRequest request) {
        if (request.projectId() != null && !projectRepository.existsById(request.projectId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }

        if (request.policyId() != null && !policyRepository.existsById(request.policyId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Policy not found");
        }

        PolicyExceptionEntity exception = new PolicyExceptionEntity();
        exception.setProjectId(request.projectId());
        exception.setPolicyId(request.policyId());
        exception.setRuleId(request.ruleId());
        exception.setPackageName(request.packageName());
        exception.setPackageVersion(request.packageVersion());
        exception.setJustification(request.justification());
        if (request.scope() != null) {
            exception.setScope(request.scope());
        }
        exception.setExpiresAt(request.expiresAt());

        PolicyExceptionEntity saved = exceptionRepository.save(exception);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @GetMapping
    @Operation(summary = "List all exceptions",
               description = "Get paginated list of exceptions, optionally filtered by project or status.")
    public Page<PolicyExceptionDto> listExceptions(
            @Parameter(description = "Filter by project ID") @RequestParam(required = false) UUID projectId,
            @Parameter(description = "Filter by status (ACTIVE, REVOKED, EXPIRED)") @RequestParam(required = false) String status,
            @Parameter(description = "Page number (zero-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);

        if (projectId != null && status != null) {
            return exceptionRepository.findByProjectIdAndStatus(projectId, status.toUpperCase(), pageable)
                    .map(this::toDto);
        }

        return exceptionRepository.findAll(pageable)
                .map(this::toDto);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get exception detail",
               description = "Retrieve a single exception by ID.")
    @ApiResponse(responseCode = "200", description = "Exception found")
    @ApiResponse(responseCode = "404", description = "Exception not found")
    public PolicyExceptionDto getException(@PathVariable UUID id) {
        PolicyExceptionEntity exception = exceptionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exception not found"));
        return toDto(exception);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update exception",
               description = "Update exception status or other fields (e.g., change to REVOKED).")
    @ApiResponse(responseCode = "200", description = "Exception updated")
    @ApiResponse(responseCode = "404", description = "Exception not found")
    @Transactional
    public PolicyExceptionDto updateException(@PathVariable UUID id,
                                             @RequestBody UpdateExceptionRequest request) {
        PolicyExceptionEntity exception = exceptionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exception not found"));

        if (request.status() != null) {
            exception.setStatus(request.status().toUpperCase());
        }

        if (request.approvedBy() != null) {
            exception.setApprovedBy(request.approvedBy());
        }

        if (request.expiresAt() != null) {
            exception.setExpiresAt(request.expiresAt());
        }

        PolicyExceptionEntity updated = exceptionRepository.save(exception);
        return toDto(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete exception",
               description = "Remove an exception from the system.")
    @ApiResponse(responseCode = "204", description = "Exception deleted")
    @ApiResponse(responseCode = "404", description = "Exception not found")
    @Transactional
    public ResponseEntity<Void> deleteException(@PathVariable UUID id) {
        if (!exceptionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Exception not found");
        }
        exceptionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get active exceptions for project",
               description = "List all active exceptions for a specific project.")
    @ApiResponse(responseCode = "200", description = "Exceptions found")
    public List<PolicyExceptionDto> getProjectExceptions(
            @PathVariable UUID projectId,
            @Parameter(description = "Include expired exceptions") @RequestParam(defaultValue = "false") boolean includeExpired) {

        if (!projectRepository.existsById(projectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }

        if (includeExpired) {
            return exceptionRepository.findByProjectIdAndStatus(projectId, "ACTIVE")
                    .stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        } else {
            return exceptionRepository.findByProjectIdAndStatusAndExpiresAtAfter(projectId, "ACTIVE", Instant.now())
                    .stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }
    }

    private PolicyExceptionDto toDto(PolicyExceptionEntity entity) {
        return new PolicyExceptionDto(
            entity.getId(),
            entity.getProjectId(),
            entity.getPolicyId(),
            entity.getRuleId(),
            entity.getPackageName(),
            entity.getPackageVersion(),
            entity.getJustification(),
            entity.getCreatedBy(),
            entity.getApprovedBy(),
            entity.getStatus(),
            entity.getScope(),
            entity.getExpiresAt(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
