package com.scrutinizer.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrutinizer.enrichment.EnrichmentPipeline;
import com.scrutinizer.model.DependencyGraph;
import com.scrutinizer.parser.SbomParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Administrative operations: cache management, diagnostics")
public class AdminController {

    private final EnrichmentPipeline enrichmentPipeline;
    private final SbomParser sbomParser;
    private final ObjectMapper objectMapper;

    public AdminController(EnrichmentPipeline enrichmentPipeline, SbomParser sbomParser) {
        this.enrichmentPipeline = enrichmentPipeline;
        this.sbomParser = sbomParser;
        this.objectMapper = new ObjectMapper();
    }

    @PostMapping(value = "/warm-cache", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Warm enrichment caches",
               description = "Upload a CycloneDX SBOM JSON file. The engine will pre-fetch "
                       + "OpenSSF Scorecard and provenance data for every component, populating "
                       + "the in-memory caches. Useful before demos or offline presentations.")
    @ApiResponse(responseCode = "200", description = "Cache warming completed")
    @ApiResponse(responseCode = "400", description = "Invalid SBOM")
    public ResponseEntity<Map<String, Object>> warmCache(
            @Parameter(description = "CycloneDX SBOM JSON file") @RequestPart("sbom") MultipartFile sbomFile) {
        try {
            String sbomJson = new String(sbomFile.getBytes(), StandardCharsets.UTF_8);
            JsonNode sbomRoot = objectMapper.readTree(sbomJson);
            DependencyGraph graph = sbomParser.parse(sbomRoot);
            Map<String, Object> result = enrichmentPipeline.warmCache(graph);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid SBOM: " + e.getMessage());
        }
    }

    @GetMapping("/cache-stats")
    @Operation(summary = "Get cache statistics",
               description = "Returns the current size of the scorecard and provenance caches.")
    public ResponseEntity<Map<String, Object>> cacheStats() {
        return ResponseEntity.ok(enrichmentPipeline.cacheStats());
    }

    @DeleteMapping("/cache")
    @Operation(summary = "Clear enrichment caches",
               description = "Clears all in-memory enrichment caches. Next enrichment calls will fetch fresh data.")
    public ResponseEntity<Map<String, String>> clearCache() {
        enrichmentPipeline.clearCaches();
        return ResponseEntity.ok(Map.of("status", "cleared"));
    }
}
