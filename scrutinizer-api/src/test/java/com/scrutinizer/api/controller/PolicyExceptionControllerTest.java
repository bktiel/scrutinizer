package com.scrutinizer.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrutinizer.api.dto.CreateExceptionRequest;
import com.scrutinizer.api.dto.PolicyExceptionDto;
import com.scrutinizer.api.dto.UpdateExceptionRequest;
import com.scrutinizer.api.entity.PolicyExceptionEntity;
import com.scrutinizer.api.repository.PolicyExceptionRepository;
import com.scrutinizer.api.repository.PolicyRepository;
import com.scrutinizer.api.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PolicyExceptionController.class)
class PolicyExceptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PolicyExceptionRepository exceptionRepository;

    @MockBean
    private ProjectRepository projectRepository;

    @MockBean
    private PolicyRepository policyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID exceptionId;
    private UUID projectId;
    private UUID policyId;
    private PolicyExceptionEntity exceptionEntity;

    @BeforeEach
    void setup() {
        exceptionId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        policyId = UUID.randomUUID();

        exceptionEntity = new PolicyExceptionEntity();
        exceptionEntity.setId(exceptionId);
        exceptionEntity.setProjectId(projectId);
        exceptionEntity.setPolicyId(policyId);
        exceptionEntity.setRuleId("RULE-001");
        exceptionEntity.setPackageName("lodash");
        exceptionEntity.setPackageVersion("4.17.21");
        exceptionEntity.setJustification("Required for compatibility");
        exceptionEntity.setStatus("ACTIVE");
        exceptionEntity.setScope("PROJECT");
        exceptionEntity.setExpiresAt(Instant.now().plusSeconds(86400));
        exceptionEntity.setCreatedAt(Instant.now());
        exceptionEntity.setUpdatedAt(Instant.now());
    }

    @Nested
    class CreateException {
        @Test
        void shouldCreateException() throws Exception {
            CreateExceptionRequest request = new CreateExceptionRequest(
                    projectId, policyId, "RULE-001", "lodash", "4.17.21",
                    "Required for compatibility", "PROJECT", Instant.now().plusSeconds(86400));

            when(projectRepository.existsById(projectId)).thenReturn(true);
            when(policyRepository.existsById(policyId)).thenReturn(true);
            when(exceptionRepository.save(any(PolicyExceptionEntity.class))).thenReturn(exceptionEntity);

            String requestBody = objectMapper.writeValueAsString(request);
            mockMvc.perform(post("/api/v1/exceptions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.ruleId", is("RULE-001")))
                    .andExpect(jsonPath("$.packageName", is("lodash")));

            verify(exceptionRepository).save(any(PolicyExceptionEntity.class));
        }

        @Test
        void shouldFailWhenProjectNotFound() throws Exception {
            UUID unknownProjectId = UUID.randomUUID();
            CreateExceptionRequest request = new CreateExceptionRequest(
                    unknownProjectId, policyId, "RULE-001", "lodash", "4.17.21",
                    "Justification", "PROJECT", Instant.now().plusSeconds(86400));

            when(projectRepository.existsById(unknownProjectId)).thenReturn(false);

            String requestBody = objectMapper.writeValueAsString(request);
            mockMvc.perform(post("/api/v1/exceptions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldFailWhenPolicyNotFound() throws Exception {
            UUID unknownPolicyId = UUID.randomUUID();
            CreateExceptionRequest request = new CreateExceptionRequest(
                    projectId, unknownPolicyId, "RULE-001", "lodash", "4.17.21",
                    "Justification", "PROJECT", Instant.now().plusSeconds(86400));

            when(projectRepository.existsById(projectId)).thenReturn(true);
            when(policyRepository.existsById(unknownPolicyId)).thenReturn(false);

            String requestBody = objectMapper.writeValueAsString(request);
            mockMvc.perform(post("/api/v1/exceptions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class ListExceptions {
        @Test
        void shouldListAllExceptions() throws Exception {
            PolicyExceptionEntity exception2 = new PolicyExceptionEntity();
            exception2.setId(UUID.randomUUID());
            exception2.setRuleId("RULE-002");
            exception2.setStatus("ACTIVE");

            Page<PolicyExceptionEntity> page = new PageImpl<>(List.of(exceptionEntity, exception2));
            when(exceptionRepository.findAll(PageRequest.of(0, 20))).thenReturn(page);

            mockMvc.perform(get("/api/v1/exceptions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].ruleId", is("RULE-001")));

            verify(exceptionRepository).findAll(PageRequest.of(0, 20));
        }

        @Test
        void shouldFilterByProjectAndStatus() throws Exception {
            Page<PolicyExceptionEntity> page = new PageImpl<>(List.of(exceptionEntity));
            when(exceptionRepository.findByProjectIdAndStatus(projectId, "ACTIVE", PageRequest.of(0, 20)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/v1/exceptions")
                    .param("projectId", projectId.toString())
                    .param("status", "ACTIVE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)));

            verify(exceptionRepository).findByProjectIdAndStatus(projectId, "ACTIVE", PageRequest.of(0, 20));
        }

        @Test
        void shouldSupportPagination() throws Exception {
            Page<PolicyExceptionEntity> page = new PageImpl<>(List.of(exceptionEntity), PageRequest.of(1, 10), 25);
            when(exceptionRepository.findAll(PageRequest.of(1, 10))).thenReturn(page);

            mockMvc.perform(get("/api/v1/exceptions")
                    .param("page", "1")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.number", is(1)));
        }
    }

    @Nested
    class GetException {
        @Test
        void shouldReturnExceptionDetail() throws Exception {
            when(exceptionRepository.findById(exceptionId)).thenReturn(Optional.of(exceptionEntity));

            mockMvc.perform(get("/api/v1/exceptions/{id}", exceptionId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(exceptionId.toString())))
                    .andExpect(jsonPath("$.ruleId", is("RULE-001")))
                    .andExpect(jsonPath("$.status", is("ACTIVE")));

            verify(exceptionRepository).findById(exceptionId);
        }

        @Test
        void shouldReturn404WhenExceptionNotFound() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(exceptionRepository.findById(unknownId)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/exceptions/{id}", unknownId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class UpdateException {
        @Test
        void shouldUpdateExceptionStatus() throws Exception {
            UpdateExceptionRequest request = new UpdateExceptionRequest("REVOKED", "approver", null);

            when(exceptionRepository.findById(exceptionId)).thenReturn(Optional.of(exceptionEntity));
            when(exceptionRepository.save(any(PolicyExceptionEntity.class))).thenReturn(exceptionEntity);

            String requestBody = objectMapper.writeValueAsString(request);
            mockMvc.perform(put("/api/v1/exceptions/{id}", exceptionId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk());

            verify(exceptionRepository).save(any(PolicyExceptionEntity.class));
        }

        @Test
        void shouldUpdateExpiresAt() throws Exception {
            Instant newExpiry = Instant.now().plusSeconds(172800);
            UpdateExceptionRequest request = new UpdateExceptionRequest(null, null, newExpiry);

            when(exceptionRepository.findById(exceptionId)).thenReturn(Optional.of(exceptionEntity));
            when(exceptionRepository.save(any(PolicyExceptionEntity.class))).thenReturn(exceptionEntity);

            String requestBody = objectMapper.writeValueAsString(request);
            mockMvc.perform(put("/api/v1/exceptions/{id}", exceptionId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk());

            verify(exceptionRepository).save(any(PolicyExceptionEntity.class));
        }

        @Test
        void shouldReturn404WhenExceptionNotFound() throws Exception {
            UUID unknownId = UUID.randomUUID();
            UpdateExceptionRequest request = new UpdateExceptionRequest("REVOKED", null, null);

            when(exceptionRepository.findById(unknownId)).thenReturn(Optional.empty());

            String requestBody = objectMapper.writeValueAsString(request);
            mockMvc.perform(put("/api/v1/exceptions/{id}", unknownId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class DeleteException {
        @Test
        void shouldDeleteException() throws Exception {
            when(exceptionRepository.existsById(exceptionId)).thenReturn(true);

            mockMvc.perform(delete("/api/v1/exceptions/{id}", exceptionId))
                    .andExpect(status().isNoContent());

            verify(exceptionRepository).deleteById(exceptionId);
        }

        @Test
        void shouldReturn404WhenExceptionNotFound() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(exceptionRepository.existsById(unknownId)).thenReturn(false);

            mockMvc.perform(delete("/api/v1/exceptions/{id}", unknownId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class GetProjectExceptions {
        @Test
        void shouldReturnActiveExceptionsForProject() throws Exception {
            when(projectRepository.existsById(projectId)).thenReturn(true);
            when(exceptionRepository.findByProjectIdAndStatusAndExpiresAtAfter(projectId, "ACTIVE", any(Instant.class)))
                    .thenReturn(List.of(exceptionEntity));

            mockMvc.perform(get("/api/v1/exceptions/project/{projectId}", projectId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].status", is("ACTIVE")));

            verify(exceptionRepository).findByProjectIdAndStatusAndExpiresAtAfter(projectId, "ACTIVE", any(Instant.class));
        }

        @Test
        void shouldIncludeExpiredExceptions() throws Exception {
            when(projectRepository.existsById(projectId)).thenReturn(true);
            when(exceptionRepository.findByProjectIdAndStatus(projectId, "ACTIVE"))
                    .thenReturn(List.of(exceptionEntity));

            mockMvc.perform(get("/api/v1/exceptions/project/{projectId}", projectId)
                    .param("includeExpired", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)));

            verify(exceptionRepository).findByProjectIdAndStatus(projectId, "ACTIVE");
        }

        @Test
        void shouldReturn404WhenProjectNotFound() throws Exception {
            UUID unknownProjectId = UUID.randomUUID();
            when(projectRepository.existsById(unknownProjectId)).thenReturn(false);

            mockMvc.perform(get("/api/v1/exceptions/project/{projectId}", unknownProjectId))
                    .andExpect(status().isNotFound());
        }
    }
}
