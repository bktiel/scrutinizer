package com.scrutinizer.enrichment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ProvenanceServiceTest {

    @Nested
    class NpmProvenanceTests {
        @Test
        void detectsNpmSignstoreAttestation() {
            String json = """
                    {"name": "axios", "version": "1.7.2", "dist": {
                        "attestations": {"url": "https://..."}
                    }}""";
            ProvenanceService service = new ProvenanceService(
                    createMockHttpClient(200, json));

            ProvenanceResult result = service.checkProvenance("pkg:npm/axios@1.7.2");

            assertThat(result.isPresent()).isTrue();
            assertThat(result.level()).hasValue(ProvenanceResult.SlsaLevel.SLSA_L2);
            assertThat(result.source()).hasValue("npm-sigstore");
        }

        @Test
        void detectsNpmSignatures() {
            String json = """
                    {"name": "lodash", "version": "4.17.21", "dist": {
                        "signatures": [{"sig": "abc123", "keyid": "key1"}]
                    }}""";
            ProvenanceService service = new ProvenanceService(
                    createMockHttpClient(200, json));

            ProvenanceResult result = service.checkProvenance("pkg:npm/lodash@4.17.21");

            assertThat(result.isPresent()).isTrue();
            assertThat(result.level()).hasValue(ProvenanceResult.SlsaLevel.SLSA_L1);
            assertThat(result.source()).hasValue("npm-signature");
        }

        @Test
        void prefersSignstoreOverSignatures() {
            String json = """
                    {"name": "pkg", "version": "1.0", "dist": {
                        "attestations": {"url": "https://..."},
                        "signatures": [{"sig": "xyz"}]
                    }}""";
            ProvenanceService service = new ProvenanceService(
                    createMockHttpClient(200, json));

            ProvenanceResult result = service.checkProvenance("pkg:npm/pkg@1.0");

            assertThat(result.level()).hasValue(ProvenanceResult.SlsaLevel.SLSA_L2);
        }

        @Test
        void nosProvenanceReturnsAbsent() {
            String json = """
                    {"name": "pkg", "version": "1.0", "dist": {"integrity": "sha512-..."}}""";
            ProvenanceService service = new ProvenanceService(
                    createMockHttpClient(200, json));

            ProvenanceResult result = service.checkProvenance("pkg:npm/pkg@1.0");

            assertThat(result.isPresent()).isFalse();
        }

        @Test
        void emptySignaturesArrayReturnsAbsent() {
            String json = """
                    {"name": "pkg", "version": "1.0", "dist": {"signatures": []}}""";
            ProvenanceService service = new ProvenanceService(
                    createMockHttpClient(200, json));

            ProvenanceResult result = service.checkProvenance("pkg:npm/pkg@1.0");

            assertThat(result.isPresent()).isFalse();
        }

        @Test
        void handlesScopedPackages() {
            String json = """
                    {"name": "@scope/pkg", "version": "1.0", "dist": {
                        "attestations": {"url": "https://..."}
                    }}""";
            ProvenanceService service = new ProvenanceService(
                    createMockHttpClient(200, json));

            ProvenanceResult result = service.checkProvenance("pkg:npm/%40scope/pkg@1.0");

            assertThat(result.isPresent()).isTrue();
            assertThat(result.source()).hasValue("npm-sigstore");
        }

        @Test
        void npmRegistryErrorReturnsAbsent() {
            ProvenanceService service = new ProvenanceService(
                    createMockHttpClient(404, "Not found"));

            ProvenanceResult result = service.checkProvenance("pkg:npm/nonexistent@1.0");

            assertThat(result.isPresent()).isFalse();
        }
    }

    @Nested
    class MavenProvenanceTests {
        @Test
        void detectsSigstoreBundleSignal() {
            ProvenanceService service = new ProvenanceService(
                    createHeadMockHttpClient(200)); // HEAD returns 200 for .sigstore.json

            ProvenanceResult result = service.checkProvenance(
                    "pkg:maven/org.springframework.boot/spring-boot@3.0.0");

            assertThat(result.isPresent()).isTrue();
            assertThat(result.level()).hasValue(ProvenanceResult.SlsaLevel.SLSA_L2);
            assertThat(result.source()).hasValue("maven-sigstore");
        }

        @Test
        void detectsPgpSignatureSignal() {
            ProvenanceService service = new ProvenanceService(
                    createHeadMockHttpClientForPgp()); // 404 for sigstore, 200 for .asc

            ProvenanceResult result = service.checkProvenance(
                    "pkg:maven/org.springframework.boot/spring-boot@3.0.0");

            assertThat(result.isPresent()).isTrue();
            assertThat(result.level()).hasValue(ProvenanceResult.SlsaLevel.SLSA_L1);
            assertThat(result.source()).hasValue("maven-pgp");
        }

        @Test
        void prefersSignstoreOverPgp() {
            // Both exist, should prefer sigstore (SLSA L2)
            ProvenanceService service = new ProvenanceService(
                    createHeadMockHttpClient(200));

            ProvenanceResult result = service.checkProvenance(
                    "pkg:maven/org.springframework.boot/spring-boot@3.0.0");

            assertThat(result.level()).hasValue(ProvenanceResult.SlsaLevel.SLSA_L2);
        }

        @Test
        void fallsBackToSearchApiWhenNoSignatures() {
            ProvenanceService service = new ProvenanceService(
                    createMavenSearchMockHttpClient());

            ProvenanceResult result = service.checkProvenance(
                    "pkg:maven/org.apache.commons/commons-lang3@3.12.0");

            assertThat(result.isPresent()).isTrue();
            assertThat(result.level()).hasValue(ProvenanceResult.SlsaLevel.SLSA_L1);
            assertThat(result.source()).hasValue("maven-central-verified");
        }

        @Test
        void handlesGroupIdWithMultipleDots() {
            ProvenanceService service = new ProvenanceService(
                    createHeadMockHttpClient(200));

            ProvenanceResult result = service.checkProvenance(
                    "pkg:maven/com.google.guava/guava@31.0.0");

            assertThat(result.isPresent()).isTrue();
        }

        @Test
        void returnAbsentForUnknownArtifact() {
            ProvenanceService service = new ProvenanceService(
                    createHeadMockHttpClient(404));

            ProvenanceResult result = service.checkProvenance(
                    "pkg:maven/org.unknowngroup/unknownartifact@1.0.0");

            assertThat(result.isPresent()).isFalse();
        }

        @Test
        void handlesInvalidMavenPurl() {
            ProvenanceService service = new ProvenanceService(
                    HttpClient.newBuilder().build());

            ProvenanceResult result = service.checkProvenance(
                    "pkg:maven/invalid-purl");

            assertThat(result.isPresent()).isFalse();
        }
    }

    @Nested
    class CacheTests {
        @Test
        void cacheHitReturnsResult() {
            String json = """
                    {"name": "axios", "version": "1.7.2", "dist": {
                        "attestations": {"url": "https://..."}
                    }}""";
            ProvenanceService service = new ProvenanceService(
                    createMockHttpClient(200, json));
            String purl = "pkg:npm/axios@1.7.2";

            ProvenanceResult first = service.checkProvenance(purl);
            ProvenanceResult second = service.checkProvenance(purl);

            assertThat(first).isEqualTo(second);
            assertThat(service.cacheSize()).isEqualTo(1);
        }

        @Test
        void multiplePurlsIncreaseCacheSize() {
            ProvenanceService service = new ProvenanceService(
                    createMockHttpClient(200, "{}"));

            service.checkProvenance("pkg:npm/pkg1@1.0");
            service.checkProvenance("pkg:npm/pkg2@1.0");
            service.checkProvenance("pkg:npm/pkg3@1.0");

            assertThat(service.cacheSize()).isEqualTo(3);
        }

        @Test
        void clearCacheRemovesAllEntries() {
            ProvenanceService service = new ProvenanceService(
                    createMockHttpClient(200, "{}"));

            service.checkProvenance("pkg:npm/pkg1@1.0");
            service.checkProvenance("pkg:npm/pkg2@1.0");
            service.clearCache();

            assertThat(service.cacheSize()).isEqualTo(0);
        }
    }

    @Nested
    class EdgeCaseTests {
        @Test
        void handleNullPurl() {
            ProvenanceService service = new ProvenanceService(
                    HttpClient.newBuilder().build());

            ProvenanceResult result = service.checkProvenance(null);

            assertThat(result.isPresent()).isFalse();
        }

        @Test
        void handleUnknownEcosystem() {
            ProvenanceService service = new ProvenanceService(
                    HttpClient.newBuilder().build());

            ProvenanceResult result = service.checkProvenance("pkg:nuget/SomePackage@1.0");

            assertThat(result.isPresent()).isFalse();
        }

        @Test
        void handleNetworkError() {
            HttpClient mockClient = new HttpClient() {
                @Override
                public <T> HttpResponse<T> send(HttpRequest request,
                                               HttpResponse.BodyHandler<T> responseBodyHandler)
                        throws IOException, InterruptedException {
                    throw new IOException("Network error");
                }

                @Override
                public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
                        HttpRequest request,
                        HttpResponse.BodyHandler<T> responseBodyHandler) {
                    return null;
                }

                @Override
                public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
                        HttpRequest request,
                        HttpResponse.BodyHandler<T> responseBodyHandler,
                        HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
                    return null;
                }

                @Override
                public void close() {
                }
            };

            ProvenanceService service = new ProvenanceService(mockClient);
            ProvenanceResult result = service.checkProvenance("pkg:npm/axios@1.0");

            assertThat(result.isPresent()).isFalse();
        }
    }

    // Helpers
    private HttpClient createMockHttpClient(int statusCode, String responseBody) {
        return new HttpClient() {
            @Override
            public <T> HttpResponse<T> send(HttpRequest request,
                                           HttpResponse.BodyHandler<T> responseBodyHandler)
                    throws IOException, InterruptedException {
                return (HttpResponse<T>) new MockHttpResponse<>(statusCode, responseBody);
            }

            @Override
            public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
                    HttpRequest request,
                    HttpResponse.BodyHandler<T> responseBodyHandler) {
                return null;
            }

            @Override
            public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
                    HttpRequest request,
                    HttpResponse.BodyHandler<T> responseBodyHandler,
                    HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
                return null;
            }

            @Override
            public void close() {
            }
        };
    }

    private HttpClient createHeadMockHttpClient(int statusCode) {
        return new HttpClient() {
            @Override
            public <T> HttpResponse<T> send(HttpRequest request,
                                           HttpResponse.BodyHandler<T> responseBodyHandler)
                    throws IOException, InterruptedException {
                return (HttpResponse<T>) new MockHttpResponse<>(statusCode, "");
            }

            @Override
            public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
                    HttpRequest request,
                    HttpResponse.BodyHandler<T> responseBodyHandler) {
                return null;
            }

            @Override
            public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
                    HttpRequest request,
                    HttpResponse.BodyHandler<T> responseBodyHandler,
                    HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
                return null;
            }

            @Override
            public void close() {
            }
        };
    }

    private HttpClient createHeadMockHttpClientForPgp() {
        return new HttpClient() {
            @Override
            public <T> HttpResponse<T> send(HttpRequest request,
                                           HttpResponse.BodyHandler<T> responseBodyHandler)
                    throws IOException, InterruptedException {
                String uri = request.uri().toString();
                int status = uri.contains(".asc") ? 200 : 404;
                return (HttpResponse<T>) new MockHttpResponse<>(status, "");
            }

            @Override
            public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
                    HttpRequest request,
                    HttpResponse.BodyHandler<T> responseBodyHandler) {
                return null;
            }

            @Override
            public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
                    HttpRequest request,
                    HttpResponse.BodyHandler<T> responseBodyHandler,
                    HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
                return null;
            }

            @Override
            public void close() {
            }
        };
    }

    private HttpClient createMavenSearchMockHttpClient() {
        return new HttpClient() {
            @Override
            public <T> HttpResponse<T> send(HttpRequest request,
                                           HttpResponse.BodyHandler<T> responseBodyHandler)
                    throws IOException, InterruptedException {
                String json = """
                        {"response": {"docs": [{"g": "org.apache.commons", "a": "commons-lang3"}]}}""";
                return (HttpResponse<T>) new MockHttpResponse<>(200, json);
            }

            @Override
            public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
                    HttpRequest request,
                    HttpResponse.BodyHandler<T> responseBodyHandler) {
                return null;
            }

            @Override
            public <T> java.util.concurrent.CompletableFuture<HttpResponse<T>> sendAsync(
                    HttpRequest request,
                    HttpResponse.BodyHandler<T> responseBodyHandler,
                    HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
                return null;
            }

            @Override
            public void close() {
            }
        };
    }

    private static class MockHttpResponse<T> implements HttpResponse<T> {
        private final int statusCode;
        private final String body;

        MockHttpResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        @Override
        public int statusCode() { return statusCode; }

        @Override
        public HttpRequest request() { return null; }

        @Override
        public Optional<HttpResponse<T>> previousResponse() { return Optional.empty(); }

        @Override
        public java.net.http.HttpHeaders headers() {
            return java.net.http.HttpHeaders.of(Map.of(), (a, b) -> true);
        }

        @Override
        public T body() { return (T) body; }

        @Override
        public Optional<javax.net.ssl.SSLSession> sslSession() { return Optional.empty(); }

        @Override
        public java.net.URI uri() { return null; }

        @Override
        public java.net.http.HttpClient.Version version() { return null; }
    }
}
