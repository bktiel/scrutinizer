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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Trigger a posture evaluation",
               description = "Upload a CycloneDX SBOM JSON file. Provide either a policyId to evaluate "
                       + "against a specific policy, or a repositoryUrl to auto-resolve the project and "
                       + "its assigned policy. Policy management is controlled from the dashboard.")
    @ApiResponse(responseCode = "201", description = "Evaluation completed and persisted")
    @ApiResponse(responseCode = "400", description = "Invalid SBOM or missing parameters")
    @ApiResponse(responseCode = "404", description = "No project registered for the given repository URL")
    public ResponseEntity<PostureRunSummaryDto> createRun(
            @Parameter(description = "CycloneDX SBOM JSON file") @RequestPart("sbom") MultipartFile sbomFile,
            @Parameter(description = "Application name") @RequestParam(required = false) String applicationName,
            @Parameter(description = "Policy ID to evaluate against (provide this OR repositoryUrl)") @RequestParam(required = false) UUID policyId,
            @Parameter(description = "Repository URL to resolve project and policy (provide this OR policyId)") @RequestParam(required = false) String repositoryUrl,
            @Parameter(description = "Optional project ID for exception scoping") @RequestParam(required = false) UUID projectId) {
        try {
            String sbomJson = new String(sbomFile.getBytes(), StandardCharsets.UTF_8);
            PostureRunEntity run;

            if (repositoryUrl != null && !repositoryUrl.isBlank()) {
                run = postureRunService.executeForRepositoryUrl(repositoryUrl, applicationName, sbomJson);
            } else if (policyId != null) {
                String appName = (applicationName != null && !applicationName.isBlank())
                        ? applicationName : "unnamed";
                run = postureRunService.executeWithUpload(appName, sbomJson, policyId, projectId);
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Provide either 'policyId' or 'repositoryUrl'");
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toSummaryDto(run));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("No project registered")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
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
    @Transactional(readOnly = true)
    public PostureRunDetailDto getRunDetail(@PathVariable UUID id) {
        PostureRunEntity entity = postureRunRepository.findByIdWithComponents(id)
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
    @Transactional
    public PostureRunDetailDto reviewRun(@PathVariable UUID id, @RequestBody ReviewRequest request) {
        PostureRunEntity entity = postureRunRepository.findByIdWithComponents(id)
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

    @GetMapping(value = "/{id}/export", produces = "application/zip")
    @Operation(summary = "Export audit bundle",
               description = "Download a ZIP containing posture-report.json, findings.csv, and evidence-manifest.json for compliance auditing.")
    @ApiResponse(responseCode = "200", description = "Audit bundle ZIP")
    @ApiResponse(responseCode = "404", description = "Run not found")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exportAuditBundle(@PathVariable UUID id) {
        PostureRunEntity entity = postureRunRepository.findByIdWithComponents(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Run not found"));

        try {
            com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper();
            json.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
            json.findAndRegisterModules();

            // Build posture-report.json from entity directly
            var reportMap = new java.util.LinkedHashMap<String, Object>();
            reportMap.put("id", entity.getId().toString());
            reportMap.put("applicationName", entity.getApplicationName());
            reportMap.put("policyName", entity.getPolicyName());
            reportMap.put("policyVersion", entity.getPolicyVersion());
            reportMap.put("overallDecision", entity.getOverallDecision());
            reportMap.put("postureScore", entity.getPostureScore());
            reportMap.put("runTimestamp", entity.getRunTimestamp().toString());
            reportMap.put("componentCount", entity.getComponentResults().size());
            String reportJson = json.writeValueAsString(reportMap);

            // Build findings.csv
            StringBuilder csv = new StringBuilder();
            csv.append("ruleId,componentName,componentRef,decision,severity,field,actualValue,expectedValue,description,remediation\n");
            for (var cr : entity.getComponentResults()) {
                for (var f : cr.getFindings()) {
                    csv.append(escapeCsv(f.getRuleId())).append(',');
                    csv.append(escapeCsv(cr.getComponentName())).append(',');
                    csv.append(escapeCsv(cr.getComponentRef())).append(',');
                    csv.append(escapeCsv(f.getDecision())).append(',');
                    csv.append(escapeCsv(f.getSeverity())).append(',');
                    csv.append(escapeCsv(f.getField())).append(',');
                    csv.append(escapeCsv(f.getActualValue())).append(',');
                    csv.append(escapeCsv(f.getExpectedValue())).append(',');
                    csv.append(escapeCsv(f.getDescription())).append(',');
                    csv.append(escapeCsv(f.getRemediation())).append('\n');
                }
            }

            // Build evidence-manifest.json
            var manifest = new java.util.LinkedHashMap<String, Object>();
            manifest.put("schemaVersion", "1.0");
            manifest.put("generatedAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
            manifest.put("runId", id.toString());
            manifest.put("applicationName", entity.getApplicationName());
            manifest.put("policyName", entity.getPolicyName());
            manifest.put("overallDecision", entity.getOverallDecision());
            String manifestJson = json.writeValueAsString(manifest);

            // Build ZIP
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                zos.putNextEntry(new ZipEntry("posture-report.json"));
                zos.write(reportJson.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();

                zos.putNextEntry(new ZipEntry("findings.csv"));
                zos.write(csv.toString().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();

                zos.putNextEntry(new ZipEntry("evidence-manifest.json"));
                zos.write(manifestJson.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            String filename = "scrutinizer-audit-" + entity.getApplicationName() + "-"
                    + entity.getRunTimestamp().toString().substring(0, 10) + ".zip";

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .header("Content-Type", "application/zip")
                    .body(baos.toByteArray());

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to generate audit bundle: " + e.getMessage());
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
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
