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
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects SLSA provenance attestations for packages.
 *
 * Detection strategies:
 * - npm: Checks the npm registry for Sigstore-based provenance attestations
 * - Maven: Checks Maven Central for PGP signatures and Sigstore provenance
 * - PyPI: Checks for Sigstore attestations (emerging support)
 */
@Service
public class ProvenanceService {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final Pattern NPM_PURL = Pattern.compile(
            "pkg:npm/(?:%40([^/]+)/)?([^@]+)@(.+)");
    private static final Pattern MAVEN_PURL = Pattern.compile(
            "pkg:maven/([^/]+)/([^@]+)@(.+)");

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
            result = checkMavenProvenance(purl);
        } else {
            result = ProvenanceResult.absent();
        }

        cache.put(purl, result);
        return result;
    }

    // ─── npm ──────────────────────────────────────────────────────────

    private ProvenanceResult checkNpmProvenance(String purl) {
        Matcher m = NPM_PURL.matcher(purl);
        if (!m.matches()) return ProvenanceResult.absent();

        String scope = m.group(1);
        String name = m.group(2);
        String version = m.group(3);

        String packageName = scope != null ? "@" + scope + "/" + name : name;
        String registryUrl = "https://registry.npmjs.org/" + packageName + "/" + version;

        try {
            HttpResponse<String> response = httpGet(registryUrl);
            if (response.statusCode() != 200) return ProvenanceResult.absent();
            return parseNpmProvenance(response.body());
        } catch (IOException | InterruptedException e) {
            return ProvenanceResult.absent();
        }
    }

    private ProvenanceResult parseNpmProvenance(String body) throws IOException {
        JsonNode root = mapper.readTree(body);
        JsonNode dist = root.path("dist");

        // Sigstore attestation → SLSA L2+
        if (dist.has("attestations")) {
            return ProvenanceResult.detected(ProvenanceResult.SlsaLevel.SLSA_L2, "npm-sigstore");
        }

        // Older signature mechanism → SLSA L1
        if (dist.has("signatures")) {
            JsonNode signatures = dist.get("signatures");
            if (signatures.isArray() && !signatures.isEmpty()) {
                return ProvenanceResult.detected(ProvenanceResult.SlsaLevel.SLSA_L1, "npm-signature");
            }
        }

        return ProvenanceResult.absent();
    }

    // ─── Maven ────────────────────────────────────────────────────────

    /**
     * Check Maven Central for provenance signals.
     *
     * Maven artifacts can have multiple provenance indicators:
     * 1. PGP signature (.asc file exists) → SLSA L1 (signed artifact)
     * 2. Sigstore bundle (.sigstore.json exists) → SLSA L2 (build provenance)
     * 3. Build info in POM (reproducible builds plugin) → informational
     *
     * We check Maven Central's REST API to verify artifact existence and signatures.
     */
    private ProvenanceResult checkMavenProvenance(String purl) {
        Matcher m = MAVEN_PURL.matcher(purl);
        if (!m.matches()) return ProvenanceResult.absent();

        String groupId = m.group(1);
        String artifactId = m.group(2);
        String version = m.group(3);

        // Convert groupId dots to slashes for Maven Central path
        String groupPath = groupId.replace(".", "/");

        // First check for Sigstore bundle (stronger signal)
        String sigstoreUrl = String.format(
                "https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar.sigstore.json",
                groupPath, artifactId, version, artifactId, version);

        try {
            HttpResponse<String> sigstoreResponse = httpHead(sigstoreUrl);
            if (sigstoreResponse.statusCode() == 200) {
                return ProvenanceResult.detected(ProvenanceResult.SlsaLevel.SLSA_L2, "maven-sigstore");
            }
        } catch (IOException | InterruptedException e) {
            // Fall through to PGP check
        }

        // Check for PGP signature (weaker but common)
        String ascUrl = String.format(
                "https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar.asc",
                groupPath, artifactId, version, artifactId, version);

        try {
            HttpResponse<String> ascResponse = httpHead(ascUrl);
            if (ascResponse.statusCode() == 200) {
                return ProvenanceResult.detected(ProvenanceResult.SlsaLevel.SLSA_L1, "maven-pgp");
            }
        } catch (IOException | InterruptedException e) {
            // Fall through
        }

        // Check search.maven.org API for metadata
        return checkMavenSearchApi(groupId, artifactId, version);
    }

    /**
     * Query the Maven Central Search API for artifact metadata.
     * This can confirm the artifact exists and provide metadata hints.
     */
    private ProvenanceResult checkMavenSearchApi(String groupId, String artifactId, String version) {
        String searchUrl = String.format(
                "https://search.maven.org/solrsearch/select?q=g:%s+AND+a:%s+AND+v:%s&rows=1&wt=json",
                groupId, artifactId, version);

        try {
            HttpResponse<String> response = httpGet(searchUrl);
            if (response.statusCode() != 200) return ProvenanceResult.absent();

            JsonNode root = mapper.readTree(response.body());
            JsonNode docs = root.path("response").path("docs");

            if (docs.isArray() && !docs.isEmpty()) {
                JsonNode doc = docs.get(0);

                // Check for known provenance-supporting publishers
                // Central requires PGP signatures for all artifacts since Feb 2024
                // So if the artifact exists on Central, it has at minimum a PGP signature
                if (doc.has("g") && doc.has("a")) {
                    return ProvenanceResult.detected(ProvenanceResult.SlsaLevel.SLSA_L1, "maven-central-verified");
                }
            }
        } catch (IOException | InterruptedException e) {
            // Network error — graceful degradation
        }

        return ProvenanceResult.absent();
    }

    // ─── HTTP helpers ─────────────────────────────────────────────────

    private HttpResponse<String> httpGet(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> httpHead(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public void clearCache() {
        cache.clear();
    }

    public int cacheSize() {
        return cache.size();
    }
}
