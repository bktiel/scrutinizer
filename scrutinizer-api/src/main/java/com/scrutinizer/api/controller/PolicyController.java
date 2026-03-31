package com.scrutinizer.api.controller;

import com.scrutinizer.api.dto.PolicyDto;
import com.scrutinizer.api.dto.PolicyHistoryDto;
import com.scrutinizer.api.entity.PolicyEntity;
import com.scrutinizer.api.entity.PolicyHistoryEntity;
import com.scrutinizer.api.repository.PolicyHistoryRepository;
import com.scrutinizer.api.repository.PolicyRepository;
import com.scrutinizer.policy.PolicyParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/policies")
@Tag(name = "Policies", description = "Manage posture evaluation policies")
public class PolicyController {

    private final PolicyRepository policyRepository;
    private final PolicyHistoryRepository policyHistoryRepository;
    private final PolicyParser policyParser;

    public PolicyController(PolicyRepository policyRepository,
                            PolicyHistoryRepository policyHistoryRepository,
                            PolicyParser policyParser) {
        this.policyRepository = policyRepository;
        this.policyHistoryRepository = policyHistoryRepository;
        this.policyParser = policyParser;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a new policy",
               description = "Upload a YAML policy file. The file is validated by the policy parser before storage.")
    @Transactional
    public ResponseEntity<PolicyDto> createPolicy(
            @Parameter(description = "Policy YAML file") @RequestPart("file") MultipartFile file,
            @Parameter(description = "Optional description") @RequestParam(required = false) String description) {

        String yaml = readFile(file);
        var parsed = validateYaml(yaml);

        if (policyRepository.findByName(parsed.name()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Policy with name '" + parsed.name() + "' already exists. Use PUT to update.");
        }

        PolicyEntity entity = new PolicyEntity();
        entity.setName(parsed.name());
        entity.setVersion(parsed.version());
        entity.setDescription(description);
        entity.setPolicyYaml(yaml);
        policyRepository.save(entity);

        recordHistory(entity, "system");

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(entity));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update an existing policy",
               description = "Upload a new YAML file to replace the current policy. Previous version is saved in history.")
    @Transactional
    public PolicyDto updatePolicy(
            @PathVariable UUID id,
            @Parameter(description = "Policy YAML file") @RequestPart("file") MultipartFile file,
            @Parameter(description = "Optional description") @RequestParam(required = false) String description) {

        PolicyEntity entity = policyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Policy not found"));

        String yaml = readFile(file);
        var parsed = validateYaml(yaml);

        entity.setName(parsed.name());
        entity.setVersion(parsed.version());
        entity.setPolicyYaml(yaml);
        if (description != null) {
            entity.setDescription(description);
        }
        policyRepository.save(entity);

        recordHistory(entity, "system");

        return toDto(entity);
    }

    @GetMapping
    @Operation(summary = "List all policies")
    public List<PolicyDto> listPolicies() {
        return policyRepository.findAll().stream().map(this::toDto).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get policy detail")
    public PolicyDto getPolicy(@PathVariable UUID id) {
        PolicyEntity entity = policyRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Policy not found"));
        return toDto(entity);
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get policy change history")
    public List<PolicyHistoryDto> getPolicyHistory(@PathVariable UUID id) {
        if (!policyRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Policy not found");
        }
        return policyHistoryRepository.findByPolicyIdOrderByChangedAtDesc(id).stream()
                .map(h -> new PolicyHistoryDto(h.getId(), h.getPolicyYaml(), h.getChangedBy(), h.getChangedAt()))
                .toList();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a policy")
    @Transactional
    public ResponseEntity<Void> deletePolicy(@PathVariable UUID id) {
        if (!policyRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Policy not found");
        }
        policyRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private String readFile(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read uploaded file");
        }
    }

    private com.scrutinizer.policy.PolicyDefinition validateYaml(String yaml) {
        try {
            return policyParser.parse(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid policy YAML: " + e.getMessage());
        }
    }

    private void recordHistory(PolicyEntity entity, String changedBy) {
        PolicyHistoryEntity history = new PolicyHistoryEntity();
        history.setPolicyId(entity.getId());
        history.setPolicyYaml(entity.getPolicyYaml());
        history.setChangedBy(changedBy);
        policyHistoryRepository.save(history);
    }

    private PolicyDto toDto(PolicyEntity entity) {
        return new PolicyDto(
                entity.getId(),
                entity.getName(),
                entity.getVersion(),
                entity.getDescription(),
                entity.getPolicyYaml(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
