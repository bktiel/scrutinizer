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

class ScorecardServiceTest {

    @Nested
    class CacheTests {
        private ScorecardService service;

        @BeforeEach
        void setUp() {
            service = new ScorecardService(createMockHttpClient(200, """
                    {"score": 7.5, "checks": [
                        {"name": "Maintained", "score": 10},
                        {"name": "Vulnerabilities", "score": 8}
                    ]}"""));
        }

        @Test
        void cacheHitReturnsResult() {
            String repo = "github.com/axios/axios";
            Optional<ScorecardResult> first = service.getScorecard(repo);
            Optional<ScorecardResult> second = service.getScorecard(repo);

            assertThat(first).isPresent();
            assertThat(second).isPresent();
            assertThat(second).isEqualTo(first);
            assertThat(service.cacheSize()).isEqualTo(1);
        }

        @Test
        void cacheMissIncrementsCacheSize() {
            service.getScorecard("github.com/axios/axios");
            service.getScorecard("github.com/lodash/lodash");

            assertThat(service.cacheSize()).isEqualTo(2);
        }

        @Test
        void clearCacheEmptiesCache() {
            service.getScorecard("github.com/axios/axios");
            assertThat(service.cacheSize()).isEqualTo(1);

            service.clearCache();

            assertThat(service.cacheSize()).isEqualTo(0);
        }

        @Test
        void cacheEntryMarkedAsAbsentOnHttpError() {
            String repo = "github.com/missing/repo";
            service.getScorecard(repo);

            assertThat(service.cacheSize()).isEqualTo(1);
            Optional<ScorecardResult> cached = service.getScorecard(repo);
            assertThat(cached).isEmpty();
        }
    }

    @Nested
    class HttpErrorHandlingTests {
        @Test
        void nonSuccessStatusReturnsEmpty() {
            ScorecardService service = new ScorecardService(
                    createMockHttpClient(404, "Not found"));
            Optional<ScorecardResult> result = service.getScorecard("github.com/test/test");

            assertThat(result).isEmpty();
        }

        @Test
        void networkFailureReturnsEmpty() throws IOException, InterruptedException {
            HttpClient mockClient = new HttpClient() {
                @Override
                public <T> HttpResponse<T> send(HttpRequest request,
                                               HttpResponse.BodyHandler<T> responseBodyHandler)
                        throws IOException, InterruptedException {
                    throw new IOException("Network unreachable");
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

            ScorecardService service = new ScorecardService(mockClient);
            Optional<ScorecardResult> result = service.getScorecard("github.com/test/test");

            assertThat(result).isEmpty();
        }

        @Test
        void http500ErrorReturnsEmpty() {
            ScorecardService service = new ScorecardService(
                    createMockHttpClient(500, "Internal Server Error"));
            Optional<ScorecardResult> result = service.getScorecard("github.com/test/test");

            assertThat(result).isEmpty();
        }

        @Test
        void interruptedThreadReturnsEmpty() throws IOException, InterruptedException {
            HttpClient mockClient = new HttpClient() {
                @Override
                public <T> HttpResponse<T> send(HttpRequest request,
                                               HttpResponse.BodyHandler<T> responseBodyHandler)
                        throws IOException, InterruptedException {
                    throw new InterruptedException("Interrupted");
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

            ScorecardService service = new ScorecardService(mockClient);
            Optional<ScorecardResult> result = service.getScorecard("github.com/test/test");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class JsonParsingTests {
        @Test
        void parsesValidScorecardResponse() {
            String json = """
                    {"score": 7.5, "checks": [
                        {"name": "Maintained", "score": 10},
                        {"name": "Vulnerabilities", "score": 8}
                    ]}""";
            ScorecardService service = new ScorecardService(
                    createMockHttpClient(200, json));
            Optional<ScorecardResult> result = service.getScorecard("github.com/test/test");

            assertThat(result).isPresent();
            ScorecardResult sr = result.get();
            assertThat(sr.overallScore()).isEqualTo(7.5);
            assertThat(sr.checkScores()).containsEntry("Maintained", 10.0);
            assertThat(sr.checkScores()).containsEntry("Vulnerabilities", 8.0);
        }

        @Test
        void parsesResponseWithoutChecks() {
            String json = """
                    {"score": 5.0}""";
            ScorecardService service = new ScorecardService(
                    createMockHttpClient(200, json));
            Optional<ScorecardResult> result = service.getScorecard("github.com/test/test");

            assertThat(result).isPresent();
            assertThat(result.get().overallScore()).isEqualTo(5.0);
            assertThat(result.get().checkScores()).isEmpty();
        }

        @Test
        void parsesResponseWithEmptyChecks() {
            String json = """
                    {"score": 6.0, "checks": []}""";
            ScorecardService service = new ScorecardService(
                    createMockHttpClient(200, json));
            Optional<ScorecardResult> result = service.getScorecard("github.com/test/test");

            assertThat(result).isPresent();
            assertThat(result.get().checkScores()).isEmpty();
        }

        @Test
        void skipsInvalidChecks() {
            String json = """
                    {"score": 7.0, "checks": [
                        {"name": "Maintained", "score": 10},
                        {"name": "", "score": 5},
                        {"name": "Invalid", "score": -1}
                    ]}""";
            ScorecardService service = new ScorecardService(
                    createMockHttpClient(200, json));
            Optional<ScorecardResult> result = service.getScorecard("github.com/test/test");

            assertThat(result).isPresent();
            assertThat(result.get().checkScores()).containsOnlyKeys("Maintained");
        }

        @Test
        void parsesZeroScore() {
            String json = """
                    {"score": 0.0, "checks": []}""";
            ScorecardService service = new ScorecardService(
                    createMockHttpClient(200, json));
            Optional<ScorecardResult> result = service.getScorecard("github.com/test/test");

            assertThat(result).isPresent();
            assertThat(result.get().overallScore()).isEqualTo(0.0);
        }

        @Test
        void parsesMaxScore() {
            String json = """
                    {"score": 10.0, "checks": []}""";
            ScorecardService service = new ScorecardService(
                    createMockHttpClient(200, json));
            Optional<ScorecardResult> result = service.getScorecard("github.com/test/test");

            assertThat(result).isPresent();
            assertThat(result.get().overallScore()).isEqualTo(10.0);
        }
    }

    @Nested
    class RepoUrlTests {
        @Test
        void cachesResultByRepoUrl() {
            String json = """
                    {"score": 7.5, "checks": []}""";
            ScorecardService service = new ScorecardService(
                    createMockHttpClient(200, json));

            service.getScorecard("github.com/org1/repo1");
            service.getScorecard("github.com/org2/repo2");

            assertThat(service.cacheSize()).isEqualTo(2);
        }

        @Test
        void storesRepoUrlInResult() {
            String json = """
                    {"score": 7.5, "checks": []}""";
            ScorecardService service = new ScorecardService(
                    createMockHttpClient(200, json));
            String repoUrl = "github.com/axios/axios";

            Optional<ScorecardResult> result = service.getScorecard(repoUrl);

            assertThat(result).isPresent();
            assertThat(result.get().repoUrl()).isEqualTo(repoUrl);
        }
    }

    // Helper to create a mock HttpClient
    private HttpClient createMockHttpClient(int statusCode, String responseBody) {
        return new HttpClient() {
            @Override
            public <T> HttpResponse<T> send(HttpRequest request,
                                           HttpResponse.BodyHandler<T> responseBodyHandler)
                    throws IOException, InterruptedException {
                HttpResponse.ResponseInfo info = new HttpResponse.ResponseInfo() {
                    @Override
                    public int statusCode() { return statusCode; }

                    @Override
                    public HttpRequest request() { return request; }

                    @Override
                    public java.net.http.HttpHeaders headers() {
                        return java.net.http.HttpHeaders.of(Map.of(), (a, b) -> true);
                    }
                };
                T body = responseBodyHandler.apply(info);
                if (body instanceof String) {
                    return (HttpResponse<T>) new MockHttpResponse<>(statusCode, responseBody);
                }
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
