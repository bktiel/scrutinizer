package com.scrutinizer.enrichment;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves Package URLs (purls) to source repository URLs for
 * OpenSSF Scorecard lookups. Supports npm, Maven, PyPI, and Go ecosystems.
 *
 * Resolution strategy:
 * 1. Parse the purl to extract ecosystem, namespace, and artifact
 * 2. Look up known group/scope → GitHub org mappings
 * 3. Fall back to convention-based resolution
 */
public final class PurlResolver {

    // pkg:npm/%40scope/name@version or pkg:npm/name@version
    private static final Pattern NPM_PURL = Pattern.compile(
            "pkg:npm/(?:%40([^/]+)/)?([^@]+)@(.+)");

    // pkg:maven/group/artifact@version
    private static final Pattern MAVEN_PURL = Pattern.compile(
            "pkg:maven/([^/]+)/([^@]+)@(.+)");

    // pkg:pypi/name@version
    private static final Pattern PYPI_PURL = Pattern.compile(
            "pkg:pypi/([^@]+)@(.+)");

    // pkg:golang/module@version
    private static final Pattern GOLANG_PURL = Pattern.compile(
            "pkg:golang/([^@]+)@(.+)");

    /**
     * Maven group prefix → GitHub org/repo-prefix mappings.
     *
     * Maps the most common Java ecosystem groups to their correct GitHub locations.
     * Entries are checked by prefix match (startsWith) so "org.springframework"
     * catches org.springframework.boot, org.springframework.data, etc.
     */
    private static final Map<String, String> MAVEN_GROUP_MAPPINGS = Map.ofEntries(
            // Spring ecosystem
            Map.entry("org.springframework.boot", "spring-projects/spring-boot"),
            Map.entry("org.springframework.data", "spring-projects/spring-data"),
            Map.entry("org.springframework.security", "spring-projects/spring-security"),
            Map.entry("org.springframework.cloud", "spring-cloud"),
            Map.entry("org.springframework.kafka", "spring-projects/spring-kafka"),
            Map.entry("org.springframework.amqp", "spring-projects/spring-amqp"),
            Map.entry("org.springframework", "spring-projects"),

            // Jackson
            Map.entry("com.fasterxml.jackson", "FasterXML"),

            // Hibernate / JPA
            Map.entry("org.hibernate.orm", "hibernate/hibernate-orm"),
            Map.entry("org.hibernate.validator", "hibernate/hibernate-validator"),
            Map.entry("org.hibernate.search", "hibernate/hibernate-search"),

            // Jakarta EE (Eclipse EE4J)
            Map.entry("jakarta.persistence", "jakartaee/persistence"),
            Map.entry("jakarta.transaction", "jakartaee/transactions"),
            Map.entry("jakarta.validation", "jakartaee/validation"),
            Map.entry("jakarta.servlet", "jakartaee/servlet"),
            Map.entry("jakarta.annotation", "jakartaee/common-annotations-api"),
            Map.entry("jakarta.ws.rs", "jakartaee/rest"),
            Map.entry("jakarta.inject", "jakartaee/inject"),
            Map.entry("jakarta.json", "jakartaee/jsonp"),
            Map.entry("jakarta.mail", "jakartaee/mail-api"),

            // Apache projects
            Map.entry("org.apache.tomcat.embed", "apache/tomcat"),
            Map.entry("org.apache.tomcat", "apache/tomcat"),
            Map.entry("org.apache.logging.log4j", "apache/logging-log4j2"),
            Map.entry("org.apache.commons", "apache"),
            Map.entry("org.apache.httpcomponents", "apache/httpcomponents-client"),
            Map.entry("org.apache.maven", "apache/maven"),
            Map.entry("org.apache.kafka", "apache/kafka"),
            Map.entry("org.apache.camel", "apache/camel"),

            // Logging
            Map.entry("ch.qos.logback", "qos-ch/logback"),
            Map.entry("org.slf4j", "qos-ch/slf4j"),

            // Database drivers & pools
            Map.entry("org.postgresql", "pgjdbc/pgjdbc"),
            Map.entry("com.mysql", "mysql/mysql-connector-j"),
            Map.entry("com.oracle.database.jdbc", "oracle/ojdbc"),
            Map.entry("com.zaxxer", "brettwooldridge/HikariCP"),
            Map.entry("org.flywaydb", "flyway/flyway"),
            Map.entry("org.liquibase", "liquibase/liquibase"),

            // Testing
            Map.entry("org.junit.jupiter", "junit-team/junit5"),
            Map.entry("org.junit.platform", "junit-team/junit5"),
            Map.entry("org.junit", "junit-team/junit5"),
            Map.entry("org.mockito", "mockito/mockito"),
            Map.entry("org.assertj", "assertj/assertj"),
            Map.entry("org.hamcrest", "hamcrest/JavaHamcrest"),
            Map.entry("org.testcontainers", "testcontainers/testcontainers-java"),

            // Observability
            Map.entry("io.micrometer", "micrometer-metrics/micrometer"),
            Map.entry("io.opentelemetry", "open-telemetry/opentelemetry-java"),
            Map.entry("io.prometheus", "prometheus/client_java"),

            // Serialization & data
            Map.entry("org.yaml", "snakeyaml/snakeyaml"),
            Map.entry("com.google.code.gson", "google/gson"),
            Map.entry("com.google.protobuf", "protocolbuffers/protobuf"),
            Map.entry("com.google.guava", "google/guava"),
            Map.entry("com.google.errorprone", "google/error-prone"),

            // Reactive / async
            Map.entry("io.projectreactor", "reactor/reactor-core"),
            Map.entry("io.reactivex.rxjava3", "ReactiveX/RxJava"),
            Map.entry("io.netty", "netty/netty"),

            // Build tools & code generation
            Map.entry("org.projectlombok", "projectlombok/lombok"),
            Map.entry("org.mapstruct", "mapstruct/mapstruct"),
            Map.entry("io.swagger", "swagger-api/swagger-core"),
            Map.entry("org.springdoc", "springdoc/springdoc-openapi"),

            // Security
            Map.entry("org.bouncycastle", "bcgit/bc-java"),
            Map.entry("com.auth0", "auth0/java-jwt"),
            Map.entry("io.jsonwebtoken", "jwtk/jjwt"),

            // Cloud SDKs
            Map.entry("software.amazon.awssdk", "aws/aws-sdk-java-v2"),
            Map.entry("com.amazonaws", "aws/aws-sdk-java"),
            Map.entry("com.azure", "Azure/azure-sdk-for-java"),
            Map.entry("com.google.cloud", "googleapis/java-cloud")
    );

    /**
     * Common PyPI package → GitHub repo mappings.
     */
    private static final Map<String, String> PYPI_MAPPINGS = Map.ofEntries(
            Map.entry("django", "django/django"),
            Map.entry("flask", "pallets/flask"),
            Map.entry("fastapi", "tiangolo/fastapi"),
            Map.entry("requests", "psf/requests"),
            Map.entry("numpy", "numpy/numpy"),
            Map.entry("pandas", "pandas-dev/pandas"),
            Map.entry("pytest", "pytest-dev/pytest"),
            Map.entry("sqlalchemy", "sqlalchemy/sqlalchemy"),
            Map.entry("pydantic", "pydantic/pydantic"),
            Map.entry("celery", "celery/celery")
    );

    private PurlResolver() {}

    /**
     * Attempt to resolve a purl to a GitHub repository URL.
     * Returns empty if the purl format is unrecognized or the
     * mapping to a source repo is not deterministic.
     */
    public static Optional<String> toRepoUrl(String purl) {
        if (purl == null) return Optional.empty();

        Matcher npmMatcher = NPM_PURL.matcher(purl);
        if (npmMatcher.matches()) {
            String scope = npmMatcher.group(1);
            String name = npmMatcher.group(2);
            return Optional.of(resolveNpmRepo(scope, name));
        }

        Matcher mavenMatcher = MAVEN_PURL.matcher(purl);
        if (mavenMatcher.matches()) {
            String group = mavenMatcher.group(1);
            String artifact = mavenMatcher.group(2);
            return Optional.of(resolveMavenRepo(group, artifact));
        }

        Matcher pypiMatcher = PYPI_PURL.matcher(purl);
        if (pypiMatcher.matches()) {
            String name = pypiMatcher.group(1);
            return resolvePypiRepo(name);
        }

        Matcher goMatcher = GOLANG_PURL.matcher(purl);
        if (goMatcher.matches()) {
            String module = goMatcher.group(1);
            return resolveGoRepo(module);
        }

        return Optional.empty();
    }

    /**
     * Extract the ecosystem type from a purl (npm, maven, pypi, golang, etc.).
     */
    public static Optional<String> extractEcosystem(String purl) {
        if (purl == null) return Optional.empty();
        if (purl.startsWith("pkg:npm/")) return Optional.of("npm");
        if (purl.startsWith("pkg:maven/")) return Optional.of("maven");
        if (purl.startsWith("pkg:pypi/")) return Optional.of("pypi");
        if (purl.startsWith("pkg:golang/")) return Optional.of("golang");
        if (purl.startsWith("pkg:nuget/")) return Optional.of("nuget");
        if (purl.startsWith("pkg:cargo/")) return Optional.of("cargo");
        return Optional.empty();
    }

    /**
     * Extract the Maven groupId from a Maven purl.
     */
    public static Optional<String> extractMavenGroupId(String purl) {
        if (purl == null) return Optional.empty();
        Matcher m = MAVEN_PURL.matcher(purl);
        if (m.matches()) return Optional.of(m.group(1));
        return Optional.empty();
    }

    /**
     * Extract the Maven artifactId from a Maven purl.
     */
    public static Optional<String> extractMavenArtifactId(String purl) {
        if (purl == null) return Optional.empty();
        Matcher m = MAVEN_PURL.matcher(purl);
        if (m.matches()) return Optional.of(m.group(2));
        return Optional.empty();
    }

    private static String resolveNpmRepo(String scope, String name) {
        if (scope != null) {
            String org = switch (scope) {
                case "mui" -> "mui";
                case "emotion" -> "emotion-js";
                case "hookform" -> "react-hook-form";
                case "testing-library" -> "testing-library";
                case "typescript-eslint" -> "typescript-eslint";
                case "vitejs" -> "vitejs";
                case "remix-run" -> "remix-run";
                case "types" -> "DefinitelyTyped";
                case "babel" -> "babel";
                case "eslint" -> "eslint";
                case "angular" -> "angular";
                case "nestjs" -> "nestjs";
                case "prisma" -> "prisma";
                case "reduxjs" -> "reduxjs";
                case "tanstack" -> "TanStack";
                default -> scope;
            };
            return "github.com/" + org + "/" + name;
        }
        return "github.com/" + name + "/" + name;
    }

    static String resolveMavenRepo(String group, String artifact) {
        // Try exact and prefix matches against known mappings
        // Check longest prefix first for specificity (e.g., org.springframework.boot before org.springframework)
        String bestMatch = null;
        int bestLen = 0;

        for (Map.Entry<String, String> entry : MAVEN_GROUP_MAPPINGS.entrySet()) {
            String prefix = entry.getKey();
            if (group.startsWith(prefix) && prefix.length() > bestLen) {
                bestMatch = entry.getValue();
                bestLen = prefix.length();
            }
        }

        if (bestMatch != null) {
            // If the mapping is "org/repo" (contains /), it's a full repo path
            if (bestMatch.contains("/")) {
                // Some are org/repo (fixed), others are org/artifact-prefix
                // If the mapping ends with the org only, append artifact
                return "github.com/" + bestMatch;
            }
            // Otherwise it's just an org name — append the artifact
            return "github.com/" + bestMatch + "/" + artifact;
        }

        // Fallback: derive org from group ID segments
        // Strategy: use the second-level domain as org, artifact as repo
        // e.g., "io.micrometer" → "micrometer", "com.example" → "example"
        return fallbackMavenRepo(group, artifact);
    }

    /**
     * Fallback heuristic for unknown Maven groups.
     * Uses the most meaningful segment of the group ID as the GitHub org.
     */
    static String fallbackMavenRepo(String group, String artifact) {
        String[] parts = group.split("\\.");

        // For "org.xxx" or "com.xxx" or "io.xxx", use the third segment if available,
        // otherwise the second
        if (parts.length >= 3 && isTopLevelDomain(parts[0])) {
            // e.g., "org.apache.commons" → "apache", with artifact = "commons-lang3"
            // e.g., "com.example.project" → "example"
            return "github.com/" + parts[1] + "/" + artifact;
        }
        if (parts.length >= 2) {
            return "github.com/" + parts[parts.length - 1] + "/" + artifact;
        }
        return "github.com/" + group + "/" + artifact;
    }

    private static boolean isTopLevelDomain(String segment) {
        return segment.equals("org") || segment.equals("com") || segment.equals("io")
                || segment.equals("net") || segment.equals("de") || segment.equals("ch")
                || segment.equals("software") || segment.equals("jakarta");
    }

    private static Optional<String> resolvePypiRepo(String name) {
        String normalized = name.toLowerCase().replace("-", "").replace("_", "");
        String repo = PYPI_MAPPINGS.get(name.toLowerCase());
        if (repo != null) {
            return Optional.of("github.com/" + repo);
        }
        // PyPI packages often don't map cleanly to GitHub — return empty
        // rather than guessing wrong
        return Optional.empty();
    }

    private static Optional<String> resolveGoRepo(String module) {
        // Go modules often include the host: github.com/foo/bar
        if (module.startsWith("github.com/")) {
            // Strip to org/repo (first two path segments after github.com)
            String path = module.substring("github.com/".length());
            String[] parts = path.split("/");
            if (parts.length >= 2) {
                return Optional.of("github.com/" + parts[0] + "/" + parts[1]);
            }
        }
        return Optional.empty();
    }
}
