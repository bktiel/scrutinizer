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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects SLSA provenance attestations for packages.
 *
 * Detection strategies:
 * - npm: Checks the npm registry for Sigstore-based provenance attestations
 * - Maven: Best-effort check (adoption is minimal on Maven Central)
 */
@Service
public class ProvenanceService {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final Pattern NPM_PURL = Pattern.compile(
            "pkg:npm/(?:%40([^/]+)/)?([^@]+)@(.+)");

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final Map<String, ProvenanceResult> cache;

    public ProvenanceService() {
        this(HttpClient.newBuilder().connectTimeout(TIMEOUT).build());
    }

    public ProvenanceService(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.mapper = new ObjectMapper();
        this.cache = new ConcurrentHashMap<>();
    }

    /**
     * Check for SLSA provenance on a package identified by its purl.
     */
    public ProvenanceResult checkProvenance(String purl) {
        if (purl == null) return ProvenanceResult.absent();

        ProvenanceResult cached = cache.get(purl);
        if (cached != null) return cached;

        ProvenanceResult result;
        if (purl.startsWith("pkg:npm/")) {
            result = checkNpmProvenance(purl);
        } else if (purl.startsWith("pkg:maven/")) {
            // Maven Central has minimal SLSA adoption
            result = ProvenanceResult.absent();
        } else {
            result = ProvenanceResult.absent();
        }

        cache.put(purl, result);
        return result;
    }

    private ProvenanceResult checkNpmProvenance(String purl) {
        Matcher m = NPM_PURL.matcher(purl);
        if (!m.matches()) return ProvenanceResult.absent();

        String scope = m.group(1);
        String name = m.group(2);
        String version = m.group(3);

        // Build npm registry URL
        String packageName = scope != null ? "@" + scope + "/" + name : name;
        String registryUrl = "https://registry.npmjs.org/" + packageName + "/" + version;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(registryUrl))
                    .timeout(TIMEOUT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return ProvenanceResult.absent();
            }

            return parseNpmProvenance(response.body());

        } catch (IOException | InterruptedException e) {
            return ProvenanceResult.absent();
        }
    }

    private ProvenanceResult parseNpmProvenance(String body) throws IOException {
        JsonNode root = mapper.readTree(body);
        JsonNode dist = root.path("dist");

        // Check for Sigstore attestation (npm's provenance mechanism)
        if (dist.has("attestations")) {
            // npm uses Sigstore/Fulcio for provenance — maps to SLSA L2+
            return ProvenanceResult.detected(
                    ProvenanceResult.SlsaLevel.SLSA_L2,
                    "npm-sigstore"
            );
        }

        // Check for signature (older mechanism, weaker than full provenance)
        if (dist.has("signatures")) {
            JsonNode signatures = dist.get("signatures");
            if (signatures.isArray() && signatures.size() > 0) {
                return ProvenanceResult.detected(
                        ProvenanceResult.SlsaLevel.SLSA_L1,
                        "npm-signature"
                );
            }
        }

        return ProvenanceResult.absent();
    }

    public void clearCache() {
        cache.clear();
    }

    public int cacheSize() {
        return cache.size();
    }
}
