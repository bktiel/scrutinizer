package com.scrutinizer.api.controller;

import com.scrutinizer.api.dto.PolicyDto;
import com.scrutinizer.api.dto.PolicyHistoryDto;
import com.scrutinizer.api.entity.PolicyEntity;
import com.scrutinizer.api.entity.PolicyHistoryEntity;
import com.scrutinizer.api.repository.PolicyHistoryRepository;
import com.scrutinizer.api.repository.PolicyRepository;
import com.scrutinizer.policy.PolicyDefinition;
import com.scrutinizer.policy.PolicyParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PolicyController.class)
class PolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PolicyRepository policyRepository;

    @MockBean
    private PolicyHistoryRepository policyHistoryRepository;

    @MockBean
    private PolicyParser policyParser;

    private UUID policyId;
    private PolicyEntity policyEntity;
    private String validYaml = "name: test-policy\nversion: 1.0";

    @BeforeEach
    void setup() {
        policyId = UUID.randomUUID();
        policyEntity = new PolicyEntity();
        policyEntity.setId(policyId);
        policyEntity.setName("test-policy");
        policyEntity.setVersion("1.0");
        policyEntity.setDescription("Test policy");
        policyEntity.setPolicyYaml(validYaml);
        policyEntity.setCreatedAt(Instant.now());
        policyEntity.setUpdatedAt(Instant.now());
    }

    @Nested
    class CreatePolicy {
        @Test
        void shouldCreatePolicyWithValidYaml() throws Exception {
            String yamlContent = "name: new-policy\nversion: 1.0";
            MockMultipartFile yamlFile = new MockMultipartFile(
                    "file", "policy.yaml", MediaType.TEXT_PLAIN_VALUE, yamlContent.getBytes());

            PolicyDefinition parsed = new PolicyDefinition("scrutinizer/v1", "new-policy", "1.0", List.of(), null);
            when(policyParser.parse(any())).thenReturn(parsed);
            when(policyRepository.findByName("new-policy")).thenReturn(Optional.empty());
            when(policyRepository.save(any(PolicyEntity.class))).thenReturn(policyEntity);

            mockMvc.perform(multipart("/api/v1/policies")
                    .file(yamlFile)
                    .param("description", "A test policy")
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name", is("test-policy")))
                    .andExpect(jsonPath("$.version", is("1.0")));

            verify(policyRepository).save(any(PolicyEntity.class));
        }

        @Test
        void shouldFailWhenPolicyNameAlreadyExists() throws Exception {
            String yamlContent = "name: test-policy\nversion: 1.0";
            MockMultipartFile yamlFile = new MockMultipartFile(
                    "file", "policy.yaml", MediaType.TEXT_PLAIN_VALUE, yamlContent.getBytes());

            PolicyDefinition parsed = new PolicyDefinition("scrutinizer/v1", "test-policy", "1.0", List.of(), null);
            when(policyParser.parse(any())).thenReturn(parsed);
            when(policyRepository.findByName("test-policy")).thenReturn(Optional.of(policyEntity));

            mockMvc.perform(multipart("/api/v1/policies")
                    .file(yamlFile)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isConflict());
        }

        @Test
        void shouldFailWithInvalidYaml() throws Exception {
            String invalidYaml = "this is not valid yaml: [ }";
            MockMultipartFile yamlFile = new MockMultipartFile(
                    "file", "policy.yaml", MediaType.TEXT_PLAIN_VALUE, invalidYaml.getBytes());

            when(policyParser.parse(any())).thenThrow(new RuntimeException("Invalid YAML"));

            mockMvc.perform(multipart("/api/v1/policies")
                    .file(yamlFile)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class UpdatePolicy {
        @Test
        void shouldUpdatePolicy() throws Exception {
            String newYaml = "name: test-policy\nversion: 2.0";
            MockMultipartFile yamlFile = new MockMultipartFile(
                    "file", "policy.yaml", MediaType.TEXT_PLAIN_VALUE, newYaml.getBytes());

            PolicyDefinition parsed = new PolicyDefinition("scrutinizer/v1", "test-policy", "2.0", List.of(), null);
            when(policyRepository.findById(policyId)).thenReturn(Optional.of(policyEntity));
            when(policyParser.parse(any())).thenReturn(parsed);
            when(policyRepository.save(any(PolicyEntity.class))).thenReturn(policyEntity);

            mockMvc.perform(multipart("/api/v1/policies/{id}", policyId)
                    .file(yamlFile)
                    .param("description", "Updated description")
                    .with(request -> { request.setMethod("PUT"); return request; })
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(policyId.toString())));

            verify(policyRepository).save(any(PolicyEntity.class));
        }

        @Test
        void shouldReturn404WhenPolicyNotFound() throws Exception {
            UUID unknownId = UUID.randomUUID();
            String yamlContent = "name: test-policy\nversion: 1.0";
            MockMultipartFile yamlFile = new MockMultipartFile(
                    "file", "policy.yaml", MediaType.TEXT_PLAIN_VALUE, yamlContent.getBytes());

            when(policyRepository.findById(unknownId)).thenReturn(Optional.empty());

            mockMvc.perform(multipart("/api/v1/policies/{id}", unknownId)
                    .file(yamlFile)
                    .with(request -> { request.setMethod("PUT"); return request; })
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class ListPolicies {
        @Test
        void shouldListAllPolicies() throws Exception {
            PolicyEntity policy2 = new PolicyEntity();
            policy2.setId(UUID.randomUUID());
            policy2.setName("another-policy");
            policy2.setVersion("2.0");
            policy2.setPolicyYaml("yaml");
            policy2.setCreatedAt(Instant.now());
            policy2.setUpdatedAt(Instant.now());

            when(policyRepository.findAll()).thenReturn(List.of(policyEntity, policy2));

            mockMvc.perform(get("/api/v1/policies"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].name", is("test-policy")))
                    .andExpect(jsonPath("$[1].name", is("another-policy")));

            verify(policyRepository).findAll();
        }

        @Test
        void shouldReturnEmptyListWhenNoPolicies() throws Exception {
            when(policyRepository.findAll()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/policies"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    class GetPolicy {
        @Test
        void shouldReturnPolicyDetail() throws Exception {
            when(policyRepository.findById(policyId)).thenReturn(Optional.of(policyEntity));

            mockMvc.perform(get("/api/v1/policies/{id}", policyId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(policyId.toString())))
                    .andExpect(jsonPath("$.name", is("test-policy")))
                    .andExpect(jsonPath("$.version", is("1.0")))
                    .andExpect(jsonPath("$.description", is("Test policy")));

            verify(policyRepository).findById(policyId);
        }

        @Test
        void shouldReturn404WhenPolicyNotFound() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(policyRepository.findById(unknownId)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/policies/{id}", unknownId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class DeletePolicy {
        @Test
        void shouldDeletePolicy() throws Exception {
            when(policyRepository.existsById(policyId)).thenReturn(true);

            mockMvc.perform(delete("/api/v1/policies/{id}", policyId))
                    .andExpect(status().isNoContent());

            verify(policyRepository).deleteById(policyId);
        }

        @Test
        void shouldReturn404WhenDeletingNonExistentPolicy() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(policyRepository.existsById(unknownId)).thenReturn(false);

            mockMvc.perform(delete("/api/v1/policies/{id}", unknownId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class GetPolicyHistory {
        @Test
        void shouldReturnPolicyHistory() throws Exception {
            PolicyHistoryEntity history1 = new PolicyHistoryEntity();
            history1.setId(UUID.randomUUID());
            history1.setPolicyId(policyId);
            history1.setPolicyYaml("version: 1.0");
            history1.setChangedBy("user1");
            history1.setChangedAt(Instant.now());

            PolicyHistoryEntity history2 = new PolicyHistoryEntity();
            history2.setId(UUID.randomUUID());
            history2.setPolicyId(policyId);
            history2.setPolicyYaml("version: 1.1");
            history2.setChangedBy("user2");
            history2.setChangedAt(Instant.now().plusSeconds(100));

            when(policyRepository.existsById(policyId)).thenReturn(true);
            when(policyHistoryRepository.findByPolicyIdOrderByChangedAtDesc(policyId))
                    .thenReturn(List.of(history2, history1));

            mockMvc.perform(get("/api/v1/policies/{id}/history", policyId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].changedBy", is("user2")))
                    .andExpect(jsonPath("$[1].changedBy", is("user1")));

            verify(policyHistoryRepository).findByPolicyIdOrderByChangedAtDesc(policyId);
        }

        @Test
        void shouldReturn404WhenPolicyNotFound() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(policyRepository.existsById(unknownId)).thenReturn(false);

            mockMvc.perform(get("/api/v1/policies/{id}/history", unknownId))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturnEmptyHistoryWhenNone() throws Exception {
            when(policyRepository.existsById(policyId)).thenReturn(true);
            when(policyHistoryRepository.findByPolicyIdOrderByChangedAtDesc(policyId))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/v1/policies/{id}/history", policyId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }
}
