package com.scrutinizer.policy;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A complete policy definition parsed from YAML.
 * Contains metadata, rules, and scoring configuration.
 */
public final class PolicyDefinition {

    private final String apiVersion;
    private final String name;
    private final String version;
    private final List<Rule> rules;
    private final ScoringConfig scoring;

    public PolicyDefinition(String apiVersion, String name, String version,
                            List<Rule> rules, ScoringConfig scoring) {
        this.apiVersion = Objects.requireNonNull(apiVersion);
        this.name = Objects.requireNonNull(name);
        this.version = Objects.requireNonNull(version);
        this.rules = Collections.unmodifiableList(Objects.requireNonNull(rules));
        this.scoring = scoring != null ? scoring : ScoringConfig.defaultConfig();
    }

    public String apiVersion() { return apiVersion; }
    public String name() { return name; }
    public String version() { return version; }
    public List<Rule> rules() { return rules; }
    public ScoringConfig scoring() { return scoring; }

    @Override
    public String toString() {
        return "PolicyDefinition{name='" + name + "', version='" + version
                + "', rules=" + rules.size() + "}";
    }
}
