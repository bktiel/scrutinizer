package com.scrutinizer.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrutinizer.api.dto.AssignPolicyRequest;
import com.scrutinizer.api.dto.CreateProjectRequest;
import com.scrutinizer.api.dto.PostureRunSummaryDto;
import com.scrutinizer.api.dto.ProjectDto;
import com.scrutinizer.api.dto.ProjectStatsDto;
import com.scrutinizer.api.dto.TrendDataPoint;
import com.scrutinizer.api.entity.ComponentResultEntity;
import com.scrutinizer.api.entity.PolicyEntity;
import com.scrutinizer.api.entity.PostureRunEntity;
import com.scrutinizer.api.entity.ProjectEntity;
import com.scrutinizer.api.repository.ComponentResultRepository;
import com.scrutinizer.api.repository.PolicyRepository;
import com.scrutinizer.api.repository.PostureRunRepository;
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
import java.util.ArrayList;
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

@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectRepository projectRepository;

    @MockBean
    private PostureRunRepository postureRunRepository;

    @MockBean
    private ComponentResultRepository componentResultRepository;

    @MockBean
    private PolicyRepository policyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID projectId;
    private UUID policyId;
    private ProjectEntity projectEntity;

    @BeforeEach
    void setup() {
        projectId = UUID.randomUUID();
        policyId = UUID.randomUUID();
        projectEntity = new ProjectEntity();
        projectEntity.setId(projectId);
        projectEntity.setName("test-project");
        projectEntity.setDescription("Test project");
        projectEntity.setRepositoryUrl("https://github.com/example/repo.git");
        projectEntity.setPolicyId(policyId);
        projectEntity.setCreatedAt(Instant.now());
        projectEntity.setUpdatedAt(Instant.now());
    }

    @Nested
    class CreateProject {
        @Test
        void shouldCreateProject() throws Exception {
            CreateProjectRequest request = new CreateProjectRequest(
                    "new-project", "A new project", "https://github.com/example/new",
                    "gitlab-id", "main", policyId);

            ProjectEntity newProject = new ProjectEntity();
            newProject.setId(UUID.randomUUID());
            newProject.setName("new-project");
            newProject.setDescription("A new project");
            newProject.setRepositoryUrl("https://github.com/example/new");
            newProject.setPolicyId(policyId);
            newProject.setCreatedAt(Instant.now());
            newProject.setUpdatedAt(Instant.now());

            when(projectRepository.findByName("new-project")).thenReturn(Optional.empty());
            when(projectRepository.save(any(ProjectEntity.class))).thenReturn(newProject);
            when(policyRepository.findById(policyId)).thenReturn(Optional.of(new PolicyEntity()));
            when(postureRunRepository.countByProjectId(newProject.getId())).thenReturn(0L);

            String requestBody = objectMapper.writeValueAsString(request);
            mockMvc.perform(post("/api/v1/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name", is("new-project")));

            verify(projectRepository).save(any(ProjectEntity.class));
        }

        @Test
        void shouldFailWhenProjectNameExists() throws Exception {
            CreateProjectRequest request = new CreateProjectRequest(
                    "test-project", "desc", "url", null, "main", null);

            when(projectRepository.findByName("test-project")).thenReturn(Optional.of(projectEntity));

            String requestBody = objectMapper.writeValueAsString(request);
            mockMvc.perform(post("/api/v1/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    class ListProjects {
        @Test
        void shouldListProjects() throws Exception {
            ProjectEntity project2 = new ProjectEntity();
            project2.setId(UUID.randomUUID());
            project2.setName("another-project");
            project2.setCreatedAt(Instant.now());
            project2.setUpdatedAt(Instant.now());

            Page<ProjectEntity> page = new PageImpl<>(List.of(projectEntity, project2));
            when(projectRepository.findAll(PageRequest.of(0, 20))).thenReturn(page);
            when(postureRunRepository.countByProjectId(projectEntity.getId())).thenReturn(5L);
            when(postureRunRepository.countByProjectId(project2.getId())).thenReturn(0L);
            when(postureRunRepository.findFirstByProjectIdOrderByRunTimestampDesc(projectEntity.getId()))
                    .thenReturn(Optional.empty());
            when(postureRunRepository.findFirstByProjectIdOrderByRunTimestampDesc(project2.getId()))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/projects"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].name", is("test-project")))
                    .andExpect(jsonPath("$.content[1].name", is("another-project")));

            verify(projectRepository).findAll(PageRequest.of(0, 20));
        }
    }

    @Nested
    class GetProject {
        @Test
        void shouldReturnProjectDetail() throws Exception {
            when(projectRepository.findById(projectId)).thenReturn(Optional.of(projectEntity));
            when(postureRunRepository.countByProjectId(projectId)).thenReturn(3L);
            when(postureRunRepository.findFirstByProjectIdOrderByRunTimestampDesc(projectId))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/projects/{id}", projectId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(projectId.toString())))
                    .andExpect(jsonPath("$.name", is("test-project")));

            verify(projectRepository).findById(projectId);
        }

        @Test
        void shouldReturn404WhenProjectNotFound() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(projectRepository.findById(unknownId)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/projects/{id}", unknownId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class UpdateProject {
        @Test
        void shouldUpdateProject() throws Exception {
            CreateProjectRequest request = new CreateProjectRequest(
                    "updated-project", "Updated desc", "https://github.com/new/url",
                    "new-gitlab", "develop", policyId);

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(projectEntity));
            when(projectRepository.findByName("updated-project")).thenReturn(Optional.empty());
            when(projectRepository.save(any(ProjectEntity.class))).thenReturn(projectEntity);
            when(postureRunRepository.countByProjectId(projectId)).thenReturn(0L);
            when(postureRunRepository.findFirstByProjectIdOrderByRunTimestampDesc(projectId))
                    .thenReturn(Optional.empty());

            String requestBody = objectMapper.writeValueAsString(request);
            mockMvc.perform(put("/api/v1/projects/{id}", projectId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk());

            verify(projectRepository).save(any(ProjectEntity.class));
        }

        @Test
        void shouldReturn404WhenProjectNotFound() throws Exception {
            UUID unknownId = UUID.randomUUID();
            CreateProjectRequest request = new CreateProjectRequest("name", "desc", "url", null, "main", null);

            when(projectRepository.findById(unknownId)).thenReturn(Optional.empty());

            String requestBody = objectMapper.writeValueAsString(request);
            mockMvc.perform(put("/api/v1/projects/{id}", unknownId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class DeleteProject {
        @Test
        void shouldDeleteProject() throws Exception {
            when(projectRepository.existsById(projectId)).thenReturn(true);

            mockMvc.perform(delete("/api/v1/projects/{id}", projectId))
                    .andExpect(status().isNoContent());

            verify(projectRepository).deleteById(projectId);
        }

        @Test
        void shouldReturn404WhenProjectNotFound() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(projectRepository.existsById(unknownId)).thenReturn(false);

            mockMvc.perform(delete("/api/v1/projects/{id}", unknownId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class AssignPolicy {
        @Test
        void shouldAssignPolicyToProject() throws Exception {
            AssignPolicyRequest request = new AssignPolicyRequest(policyId);

            PolicyEntity policy = new PolicyEntity();
            policy.setId(policyId);
            policy.setName("assigned-policy");

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(projectEntity));
            when(policyRepository.existsById(policyId)).thenReturn(true);
            when(projectRepository.save(any(ProjectEntity.class))).thenReturn(projectEntity);
            when(postureRunRepository.countByProjectId(projectId)).thenReturn(0L);
            when(postureRunRepository.findFirstByProjectIdOrderByRunTimestampDesc(projectId))
                    .thenReturn(Optional.empty());

            String requestBody = objectMapper.writeValueAsString(request);
            mockMvc.perform(put("/api/v1/projects/{id}/policy", projectId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk());

            verify(projectRepository).save(any(ProjectEntity.class));
        }

        @Test
        void shouldReturn404WhenProjectNotFound() throws Exception {
            UUID unknownId = UUID.randomUUID();
            AssignPolicyRequest request = new AssignPolicyRequest(policyId);

            when(projectRepository.findById(unknownId)).thenReturn(Optional.empty());

            String requestBody = objectMapper.writeValueAsString(request);
            mockMvc.perform(put("/api/v1/projects/{id}/policy", unknownId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturn404WhenPolicyNotFound() throws Exception {
            UUID unknownPolicyId = UUID.randomUUID();
            AssignPolicyRequest request = new AssignPolicyRequest(unknownPolicyId);

            when(projectRepository.findById(projectId)).thenReturn(Optional.of(projectEntity));
            when(policyRepository.existsById(unknownPolicyId)).thenReturn(false);

            String requestBody = objectMapper.writeValueAsString(request);
            mockMvc.perform(put("/api/v1/projects/{id}/policy", projectId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class GetProjectRuns {
        @Test
        void shouldReturnProjectRuns() throws Exception {
            PostureRunEntity run = new PostureRunEntity();
            run.setId(UUID.randomUUID());
            run.setApplicationName("test-app");
            run.setPostureScore(95.0);
            run.setOverallDecision("PASS");
            run.setRunTimestamp(Instant.now());

            Page<PostureRunEntity> page = new PageImpl<>(List.of(run));
            when(projectRepository.existsById(projectId)).thenReturn(true);
            when(postureRunRepository.findByProjectIdOrderByRunTimestampDesc(projectId, PageRequest.of(0, 20)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/v1/projects/{id}/runs", projectId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)));

            verify(postureRunRepository).findByProjectIdOrderByRunTimestampDesc(projectId, PageRequest.of(0, 20));
        }

        @Test
        void shouldReturn404WhenProjectNotFound() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(projectRepository.existsById(unknownId)).thenReturn(false);

            mockMvc.perform(get("/api/v1/projects/{id}/runs", unknownId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class GetProjectComponents {
        @Test
        void shouldReturnLatestComponentResults() throws Exception {
            PostureRunEntity run = new PostureRunEntity();
            run.setId(UUID.randomUUID());

            ComponentResultEntity comp = new ComponentResultEntity();
            comp.setId(UUID.randomUUID());
            comp.setComponentRef("pkg:npm/test@1.0");
            comp.setComponentName("test");
            comp.setDecision("PASS");

            run.getComponentResults().add(comp);

            when(projectRepository.existsById(projectId)).thenReturn(true);
            when(postureRunRepository.findFirstByProjectIdOrderByRunTimestampDesc(projectId))
                    .thenReturn(Optional.of(run));

            mockMvc.perform(get("/api/v1/projects/{id}/components", projectId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].componentName", is("test")));

            verify(postureRunRepository).findFirstByProjectIdOrderByRunTimestampDesc(projectId);
        }

        @Test
        void shouldReturn404WhenProjectNotFound() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(projectRepository.existsById(unknownId)).thenReturn(false);

            mockMvc.perform(get("/api/v1/projects/{id}/components", unknownId))
                    .andExpect(status().isNotFound());
        }

        @Test
        void shouldReturn404WhenNoRunsFound() throws Exception {
            when(projectRepository.existsById(projectId)).thenReturn(true);
            when(postureRunRepository.findFirstByProjectIdOrderByRunTimestampDesc(projectId))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/projects/{id}/components", projectId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class GetProjectTrends {
        @Test
        void shouldReturnTrendData() throws Exception {
            PostureRunEntity run1 = new PostureRunEntity();
            run1.setPostureScore(90.0);
            run1.setOverallDecision("PASS");
            run1.setPolicyVersion("1.0");
            run1.setRunTimestamp(Instant.now());

            PostureRunEntity run2 = new PostureRunEntity();
            run2.setPostureScore(92.0);
            run2.setOverallDecision("PASS");
            run2.setPolicyVersion("1.0");
            run2.setRunTimestamp(Instant.now().plusSeconds(3600));

            when(projectRepository.existsById(projectId)).thenReturn(true);
            when(postureRunRepository.findByProjectIdOrderByRunTimestampAsc(projectId))
                    .thenReturn(List.of(run1, run2));

            mockMvc.perform(get("/api/v1/projects/{id}/trends", projectId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));

            verify(postureRunRepository).findByProjectIdOrderByRunTimestampAsc(projectId);
        }

        @Test
        void shouldReturn404WhenProjectNotFound() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(projectRepository.existsById(unknownId)).thenReturn(false);

            mockMvc.perform(get("/api/v1/projects/{id}/trends", unknownId))
                    .andExpect(status().isNotFound());
        }
    }
}
