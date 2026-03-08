package com.scrutinizer.enrichment;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves Package URLs (purls) to source repository URLs for
 * OpenSSF Scorecard lookups. Supports npm and Maven ecosystems.
 */
public final class PurlResolver {

    // pkg:npm/%40scope/name@version or pkg:npm/name@version
    private static final Pattern NPM_PURL = Pattern.compile(
            "pkg:npm/(?:%40([^/]+)/)?([^@]+)@(.+)");

    // pkg:maven/group/artifact@version
    private static final Pattern MAVEN_PURL = Pattern.compile(
            "pkg:maven/([^/]+)/([^@]+)@(.+)");

    private PurlResolver() {}

    /**
     * Attempt to resolve a purl to a GitHub repository URL.
     * Returns empty if the purl format is unrecognized or the
     * mapping to a source repo is not deterministic.
     *
     * For npm: maps to github.com/scope/name or github.com/name/name
     * For Maven: maps to github.com/group/artifact (best-effort)
     */
    public static Optional<String> toRepoUrl(String purl) {
        if (purl == null) return Optional.empty();

        Matcher npmMatcher = NPM_PURL.matcher(purl);
        if (npmMatcher.matches()) {
            String scope = npmMatcher.group(1); // nullable
            String name = npmMatcher.group(2);
            return Optional.of(resolveNpmRepo(scope, name));
        }

        Matcher mavenMatcher = MAVEN_PURL.matcher(purl);
        if (mavenMatcher.matches()) {
            String group = mavenMatcher.group(1);
            String artifact = mavenMatcher.group(2);
            return Optional.of(resolveMavenRepo(group, artifact));
        }

        return Optional.empty();
    }

    private static String resolveNpmRepo(String scope, String name) {
        // Common npm scope-to-org mappings
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
                default -> scope;
            };
            return "github.com/" + org + "/" + name;
        }
        // Unscoped: assume github.com/name/name (common convention)
        return "github.com/" + name + "/" + name;
    }

    private static String resolveMavenRepo(String group, String artifact) {
        // Common Maven group-to-org mappings
        if (group.startsWith("org.springframework")) {
            return "github.com/spring-projects/" + artifact;
        }
        if (group.startsWith("com.fasterxml.jackson")) {
            return "github.com/FasterXML/" + artifact;
        }
        if (group.equals("org.hibernate.orm")) {
            return "github.com/hibernate/hibernate-orm";
        }
        if (group.equals("org.hibernate.validator")) {
            return "github.com/hibernate/hibernate-validator";
        }
        if (group.equals("org.flywaydb")) {
            return "github.com/flyway/flyway";
        }
        if (group.equals("org.postgresql")) {
            return "github.com/pgjdbc/pgjdbc";
        }
        if (group.equals("ch.qos.logback")) {
            return "github.com/qos-ch/logback";
        }
        if (group.equals("org.slf4j")) {
            return "github.com/qos-ch/slf4j";
        }
        if (group.equals("org.apache.tomcat.embed")) {
            return "github.com/apache/tomcat";
        }
        if (group.equals("com.zaxxer")) {
            return "github.com/brettwooldridge/HikariCP";
        }
        if (group.equals("org.yaml")) {
            return "github.com/snakeyaml/snakeyaml";
        }
        if (group.equals("org.projectlombok")) {
            return "github.com/projectlombok/lombok";
        }
        // Fallback: use group segments as org/repo
        String[] parts = group.split("\\.");
        String org = parts[parts.length - 1];
        return "github.com/" + org + "/" + artifact;
    }
}
