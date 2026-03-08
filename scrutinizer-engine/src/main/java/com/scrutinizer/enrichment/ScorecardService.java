package com.scrutinizer.enrichment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Retrieves OpenSSF Scorecard results from the public API.
 * Includes an in-memory TTL cache to avoid redundant API calls.
 *
 * API docs: https://api.securityscorecards.dev
 */
@Service
public class ScorecardService {

    private static final String API_BASE = "https://api.securityscorecards.dev";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final Map<String, CacheEntry> cache;

    public ScorecardService() {
        this(HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build());
    }

    public ScorecardService(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.mapper = new ObjectMapper();
        this.cache = new ConcurrentHashMap<>();
    }

    /**
     * Fetch scorecard for a repository URL (e.g., "github.com/axios/axios").
     * Returns empty if the API call fails or the repo has no scorecard.
     */
    public Optional<ScorecardResult> getScorecard(String repoUrl) {
        // Check cache
        CacheEntry cached = cache.get(repoUrl);
        if (cached != null && !cached.isExpired()) {
            return cached.result;
        }

        try {
            String url = API_BASE + "/projects/" + repoUrl;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                cache.put(repoUrl, new CacheEntry(Optional.empty()));
                return Optional.empty();
            }

            ScorecardResult result = parseResponse(response.body(), repoUrl);
            Optional<ScorecardResult> opt = Optional.of(result);
            cache.put(repoUrl, new CacheEntry(opt));
            return opt;

        } catch (IOException | InterruptedException e) {
            // Network error — cache as absent to avoid hammering
            cache.put(repoUrl, new CacheEntry(Optional.empty()));
            return Optional.empty();
        }
    }

    private ScorecardResult parseResponse(String body, String repoUrl) throws IOException {
        JsonNode root = mapper.readTree(body);
        double overallScore = root.path("score").asDouble(0.0);

        Map<String, Double> checkScores = new LinkedHashMap<>();
        JsonNode checks = root.path("checks");
        if (checks.isArray()) {
            for (JsonNode check : checks) {
                String name = check.path("name").asText();
                double score = check.path("score").asDouble(-1);
                if (!name.isEmpty() && score >= 0) {
                    checkScores.put(name, score);
                }
            }
        }

        return new ScorecardResult(overallScore, checkScores, repoUrl, Instant.now());
    }

    /** Clear the in-memory cache. */
    public void clearCache() {
        cache.clear();
    }

    /** Returns cache size (for testing/monitoring). */
    public int cacheSize() {
        return cache.size();
    }

    private static class CacheEntry {
        final Optional<ScorecardResult> result;
        final Instant createdAt;

        CacheEntry(Optional<ScorecardResult> result) {
            this.result = result;
            this.createdAt = Instant.now();
        }

        boolean isExpired() {
            return Duration.between(createdAt, Instant.now()).compareTo(CACHE_TTL) > 0;
        }
    }
}
