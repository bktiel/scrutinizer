package com.scrutinizer.policy;

import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses YAML policy definition files into PolicyDefinition objects.
 * Validates required fields, operator names, and severity levels.
 */
@Service
public class PolicyParser {

    private final Yaml yaml;

    public PolicyParser() {
        this.yaml = new Yaml();
    }

    /**
     * Parse a policy from a YAML file on disk.
     */
    public PolicyDefinition parseFile(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            return parse(is);
        }
    }

    /**
     * Parse a policy from a YAML input stream.
     */
    public PolicyDefinition parse(InputStream input) {
        Map<String, Object> data = yaml.load(input);
        if (data == null) {
            throw new PolicyParseException("Empty policy file");
        }
        return parseMap(data);
    }

    /**
     * Parse a policy from an already-loaded map (useful for testing).
     */
    @SuppressWarnings("unchecked")
    public PolicyDefinition parseMap(Map<String, Object> data) {
        // Required top-level fields
        String apiVersion = requireString(data, "apiVersion");
        if (!apiVersion.startsWith("scrutinizer/")) {
            throw new PolicyParseException(
                    "Unsupported apiVersion: '" + apiVersion + "', expected 'scrutinizer/v1'");
        }

        Map<String, Object> metadata = requireMap(data, "metadata");
        String name = requireString(metadata, "name");
        String version = requireString(metadata, "version");

        // Parse rules
        List<Map<String, Object>> rawRules = (List<Map<String, Object>>) data.get("rules");
        if (rawRules == null || rawRules.isEmpty()) {
            throw new PolicyParseException("Policy must contain at least one rule");
        }
        List<Rule> rules = new ArrayList<>();
        for (Map<String, Object> rawRule : rawRules) {
            rules.add(parseRule(rawRule));
        }

        // Parse optional scoring config
        ScoringConfig scoring = null;
        if (data.containsKey("scoring")) {
            scoring = parseScoringConfig((Map<String, Object>) data.get("scoring"));
        }

        return new PolicyDefinition(apiVersion, name, version, rules, scoring);
    }

    private Rule parseRule(Map<String, Object> raw) {
        String id = requireString(raw, "id");
        String description = (String) raw.get("description");
        String field = requireString(raw, "field");

        String operatorStr = requireString(raw, "operator").toUpperCase();
        Rule.Operator operator;
        try {
            operator = Rule.Operator.valueOf(operatorStr);
        } catch (IllegalArgumentException e) {
            throw new PolicyParseException(
                    "Invalid operator '" + operatorStr + "' in rule '" + id + "'. "
                    + "Valid operators: EQ, NEQ, GT, GTE, LT, LTE, IN, NOT_IN, EXISTS");
        }

        // Value can come from 'value' or 'threshold' field
        Object valueObj = raw.get("value");
        if (valueObj == null) valueObj = raw.get("threshold");
        String value = valueObj != null ? String.valueOf(valueObj) : null;

        // Severity can come from 'severity' or 'action' field
        String severityStr = (String) raw.get("severity");
        if (severityStr == null) severityStr = (String) raw.get("action");
        if (severityStr == null) severityStr = "FAIL";

        Rule.Severity severity;
        try {
            severity = Rule.Severity.valueOf(severityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PolicyParseException(
                    "Invalid severity '" + severityStr + "' in rule '" + id + "'. "
                    + "Valid severities: FAIL, WARN, INFO, SKIP");
        }

        String targetStr = (String) raw.get("target");
        Rule.Target target = Rule.Target.ALL;
        if (targetStr != null) {
            try {
                target = Rule.Target.valueOf(targetStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new PolicyParseException(
                        "Invalid target '" + targetStr + "' in rule '" + id + "'. "
                        + "Valid targets: ALL, DIRECT, TRANSITIVE");
            }
        }

        String ecosystem = (String) raw.get("ecosystem");

        return new Rule(id, description, field, operator, value, severity, target, ecosystem);
    }

    @SuppressWarnings("unchecked")
    private ScoringConfig parseScoringConfig(Map<String, Object> raw) {
        String methodStr = (String) raw.getOrDefault("method", "pass_fail");
        ScoringConfig.Method method;
        try {
            method = ScoringConfig.Method.valueOf(methodStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PolicyParseException(
                    "Invalid scoring method '" + methodStr + "'. "
                    + "Valid methods: WEIGHTED_AVERAGE, PASS_FAIL, WORST_CASE");
        }

        Map<String, Double> weights = new LinkedHashMap<>();
        Object weightsObj = raw.get("weights");
        if (weightsObj instanceof Map) {
            Map<String, Object> rawWeights = (Map<String, Object>) weightsObj;
            for (Map.Entry<String, Object> entry : rawWeights.entrySet()) {
                weights.put(entry.getKey(), toDouble(entry.getValue()));
            }
        }

        Map<String, Object> thresholds = (Map<String, Object>) raw.getOrDefault(
                "thresholds", Map.of());
        double passThreshold = toDouble(thresholds.getOrDefault("PASS", 7.0));
        double warnThreshold = toDouble(thresholds.getOrDefault("WARN", 4.0));

        return new ScoringConfig(method, weights, passThreshold, warnThreshold);
    }

    // --- Helpers ---

    @SuppressWarnings("unchecked")
    private Map<String, Object> requireMap(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (!(value instanceof Map)) {
            throw new PolicyParseException("Missing required section: '" + key + "'");
        }
        return (Map<String, Object>) value;
    }

    private String requireString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            throw new PolicyParseException("Missing required field: '" + key + "'");
        }
        return String.valueOf(value);
    }

    private double toDouble(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        return Double.parseDouble(String.valueOf(value));
    }
}
