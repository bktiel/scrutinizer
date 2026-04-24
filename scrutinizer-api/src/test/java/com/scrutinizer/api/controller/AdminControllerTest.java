package com.scrutinizer.api.controller;

import com.scrutinizer.enrichment.EnrichmentPipeline;
import com.scrutinizer.model.DependencyGraph;
import com.scrutinizer.parser.SbomParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EnrichmentPipeline enrichmentPipeline;

    @MockBean
    private SbomParser sbomParser;

    private String validSbomJson;

    @BeforeEach
    void setup() {
        validSbomJson = """
                {
                    "bomFormat": "CycloneDX",
                    "specVersion": "1.4",
                    "version": 1,
                    "components": [
                        {
                            "type": "library",
                            "name": "lodash",
                            "version": "4.17.21"
                        }
                    ]
                }
                """;
    }

    @Nested
    class WarmCache {
        @Test
        void shouldWarmCacheSuccessfully() throws Exception {
            MockMultipartFile sbomFile = new MockMultipartFile(
                    "sbom", "sbom.json", MediaType.APPLICATION_JSON_VALUE, validSbomJson.getBytes());

            DependencyGraph mockGraph = new DependencyGraph(List.of(), List.of(), null);
            Map<String, Object> resultMap = Map.of(
                    "scorecards_cached", 5,
                    "provenance_cached", 3,
                    "status", "complete"
            );

            when(sbomParser.parse(any())).thenReturn(mockGraph);
            when(enrichmentPipeline.warmCache(mockGraph)).thenReturn(resultMap);

            mockMvc.perform(multipart("/api/v1/admin/warm-cache")
                    .file(sbomFile)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("complete")))
                    .andExpect(jsonPath("$.scorecards_cached", is(5)));

            verify(sbomParser).parse(any());
            verify(enrichmentPipeline).warmCache(mockGraph);
        }

        @Test
        void shouldFailWithInvalidSbom() throws Exception {
            String invalidSbom = "not valid json {]";
            MockMultipartFile sbomFile = new MockMultipartFile(
                    "sbom", "sbom.json", MediaType.APPLICATION_JSON_VALUE, invalidSbom.getBytes());

            mockMvc.perform(multipart("/api/v1/admin/warm-cache")
                    .file(sbomFile)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldFailWhenSbomParsingFails() throws Exception {
            MockMultipartFile sbomFile = new MockMultipartFile(
                    "sbom", "sbom.json", MediaType.APPLICATION_JSON_VALUE, validSbomJson.getBytes());

            when(sbomParser.parse(any())).thenThrow(new RuntimeException("Parse error"));

            mockMvc.perform(multipart("/api/v1/admin/warm-cache")
                    .file(sbomFile)
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class CacheStats {
        @Test
        void shouldReturnCacheStatistics() throws Exception {
            Map<String, Object> statsMap = Map.of(
                    "scorecard_cache_size", 150,
                    "provenance_cache_size", 75,
                    "last_updated", "2024-03-29T12:00:00Z"
            );

            when(enrichmentPipeline.cacheStats()).thenReturn(statsMap);

            mockMvc.perform(get("/api/v1/admin/cache-stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scorecard_cache_size", is(150)))
                    .andExpect(jsonPath("$.provenance_cache_size", is(75)));

            verify(enrichmentPipeline).cacheStats();
        }

        @Test
        void shouldHandleEmptyCaches() throws Exception {
            Map<String, Object> statsMap = Map.of(
                    "scorecard_cache_size", 0,
                    "provenance_cache_size", 0
            );

            when(enrichmentPipeline.cacheStats()).thenReturn(statsMap);

            mockMvc.perform(get("/api/v1/admin/cache-stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scorecard_cache_size", is(0)))
                    .andExpect(jsonPath("$.provenance_cache_size", is(0)));
        }
    }

    @Nested
    class ClearCache {
        @Test
        void shouldClearCaches() throws Exception {
            mockMvc.perform(delete("/api/v1/admin/cache"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("cleared")));

            verify(enrichmentPipeline).clearCaches();
        }
    }
}
