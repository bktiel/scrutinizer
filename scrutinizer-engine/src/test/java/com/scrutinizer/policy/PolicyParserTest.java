package com.scrutinizer.policy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolicyParserTest {

    private PolicyParser parser;

    @BeforeEach
    void setUp() {
        parser = new PolicyParser();
    }

    @Nested
    class ValidPolicies {
        @Test
        void parsesValidPolicyFile() throws IOException {
            try (InputStream is = getClass().getResourceAsStream("/policies/valid-policy.yaml")) {
                PolicyDefinition policy = parser.parse(is);
                assertThat(policy.name()).isEqualTo("weighted-posture");
                assertThat(policy.version()).isEqualTo("1.0");
                assertThat(policy.apiVersion()).isEqualTo("scrutinizer/v1");
                assertThat(policy.rules()).hasSize(3);
            }
        }

        @Test
        void parsesRuleFields() throws IOException {
            try (InputStream is = getClass().getResourceAsStream("/policies/valid-policy.yaml")) {
                PolicyDefinition policy = parser.parse(is);
                Rule first = policy.rules().get(0);
                assertThat(first.id()).isEqualTo("scorecard-baseline");
                assertThat(first.field()).isEqualTo("scorecard.score");
                assertThat(first.operator()).isEqualTo(Rule.Operator.GTE);
                assertThat(first.value()).isEqualTo("4.0");
                assertThat(first.severity()).isEqualTo(Rule.Severity.WARN);
            }
        }

        @Test
        void parsesScoringConfig() throws IOException {
            try (InputStream is = getClass().getResourceAsStream("/policies/valid-policy.yaml")) {
                PolicyDefinition policy = parser.parse(is);
                ScoringConfig scoring = policy.scoring();
                assertThat(scoring.method()).isEqualTo(ScoringConfig.Method.WEIGHTED_AVERAGE);
                assertThat(scoring.weights()).containsEntry("scorecard.score", 0.6);
                assertThat(scoring.weights()).containsEntry("provenance.present", 0.4);
                assertThat(scoring.passThreshold()).isEqualTo(6.0);
                assertThat(scoring.warnThreshold()).isEqualTo(3.0);
            }
        }

        @Test
        void parsesSkipAction() throws IOException {
            try (InputStream is = getClass().getResourceAsStream("/policies/valid-policy.yaml")) {
                PolicyDefinition policy = parser.parse(is);
                Rule skipRule = policy.rules().get(2);
                assertThat(skipRule.id()).isEqualTo("exclude-optional");
                assertThat(skipRule.severity()).isEqualTo(Rule.Severity.SKIP);
            }
        }

        @Test
        void parsesMinimalValidPolicy() {
            Map<String, Object> data = Map.of(
                    "apiVersion", "scrutinizer/v1",
                    "metadata", Map.of("name", "test", "version", "1.0"),
                    "rules", List.of(Map.of(
                            "id", "r1",
                            "field", "scorecard.score",
                            "operator", "gte",
                            "threshold", 5.0,
                            "severity", "WARN"
                    ))
            );
            PolicyDefinition policy = parser.parseMap(data);
            assertThat(policy.rules()).hasSize(1);
            assertThat(policy.scoring().method()).isEqualTo(ScoringConfig.Method.PASS_FAIL);
        }
    }

    @Nested
    class Validation {
        @Test
        void rejectsUnsupportedApiVersion() {
            Map<String, Object> data = Map.of(
                    "apiVersion", "other/v1",
                    "metadata", Map.of("name", "test", "version", "1.0"),
                    "rules", List.of(Map.of(
                            "id", "r1", "field", "f", "operator", "eq",
                            "value", "x", "severity", "FAIL"
                    ))
            );
            assertThatThrownBy(() -> parser.parseMap(data))
                    .isInstanceOf(PolicyParseException.class)
                    .hasMessageContaining("apiVersion");
        }

        @Test
        void rejectsMissingMetadata() {
            Map<String, Object> data = Map.of(
                    "apiVersion", "scrutinizer/v1",
                    "rules", List.of(Map.of(
                            "id", "r1", "field", "f", "operator", "eq",
                            "value", "x", "severity", "FAIL"
                    ))
            );
            assertThatThrownBy(() -> parser.parseMap(data))
                    .isInstanceOf(PolicyParseException.class)
                    .hasMessageContaining("metadata");
        }

        @Test
        void rejectsEmptyRules() {
            Map<String, Object> data = Map.of(
                    "apiVersion", "scrutinizer/v1",
                    "metadata", Map.of("name", "test", "version", "1.0"),
                    "rules", List.of()
            );
            assertThatThrownBy(() -> parser.parseMap(data))
                    .isInstanceOf(PolicyParseException.class)
                    .hasMessageContaining("rule");
        }

        @Test
        void rejectsInvalidOperator() {
            Map<String, Object> data = Map.of(
                    "apiVersion", "scrutinizer/v1",
                    "metadata", Map.of("name", "test", "version", "1.0"),
                    "rules", List.of(Map.of(
                            "id", "r1", "field", "f", "operator", "INVALID",
                            "value", "x", "severity", "FAIL"
                    ))
            );
            assertThatThrownBy(() -> parser.parseMap(data))
                    .isInstanceOf(PolicyParseException.class)
                    .hasMessageContaining("operator");
        }

        @Test
        void rejectsInvalidSeverity() {
            Map<String, Object> data = Map.of(
                    "apiVersion", "scrutinizer/v1",
                    "metadata", Map.of("name", "test", "version", "1.0"),
                    "rules", List.of(Map.of(
                            "id", "r1", "field", "f", "operator", "eq",
                            "value", "x", "severity", "CRITICAL"
                    ))
            );
            assertThatThrownBy(() -> parser.parseMap(data))
                    .isInstanceOf(PolicyParseException.class)
                    .hasMessageContaining("severity");
        }

        @Test
        void rejectsInvalidScoringMethod() {
            Map<String, Object> data = Map.of(
                    "apiVersion", "scrutinizer/v1",
                    "metadata", Map.of("name", "test", "version", "1.0"),
                    "rules", List.of(Map.of(
                            "id", "r1", "field", "f", "operator", "eq",
                            "value", "x", "severity", "FAIL"
                    )),
                    "scoring", Map.of("method", "invalid_method")
            );
            assertThatThrownBy(() -> parser.parseMap(data))
                    .isInstanceOf(PolicyParseException.class)
                    .hasMessageContaining("scoring method");
        }
    }
}
