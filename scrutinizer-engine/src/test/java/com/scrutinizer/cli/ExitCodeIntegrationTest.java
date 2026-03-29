package com.scrutinizer.cli;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExitCodeIntegrationTest {

    @Test
    void parseArgsWithNoFileFlag() {
        ScrutinizerCommand.CliArgs args = ScrutinizerCommand.parseArgs(new String[]{
                "--sbom", "test.json", "--policy", "policy.yaml", "--no-file"
        });
        assertThat(args.noFile()).isTrue();
        assertThat(args.outputPath()).isNull();
    }

    @Test
    void parseArgsDefaultsOutputWhenPolicyProvided() {
        ScrutinizerCommand.CliArgs args = ScrutinizerCommand.parseArgs(new String[]{
                "--sbom", "test.json", "--policy", "policy.yaml"
        });
        assertThat(args.noFile()).isFalse();
        assertThat(args.outputPath()).isNotNull();
        assertThat(args.outputPath().toString()).contains("posture-report.json");
    }

    @Test
    void parseArgsNoDefaultOutputWithoutPolicy() {
        ScrutinizerCommand.CliArgs args = ScrutinizerCommand.parseArgs(new String[]{
                "--sbom", "test.json"
        });
        assertThat(args.outputPath()).isNull();
    }

    @Test
    void parseArgsExplicitOutputOverridesDefault() {
        ScrutinizerCommand.CliArgs args = ScrutinizerCommand.parseArgs(new String[]{
                "--sbom", "test.json", "--policy", "policy.yaml", "--output", "custom.json"
        });
        assertThat(args.outputPath().toString()).isEqualTo("custom.json");
    }

    @Test
    void missingRequiredSbomThrows() {
        try {
            ScrutinizerCommand.parseArgs(new String[]{"--policy", "p.yaml"});
            assertThat(false).as("Should have thrown").isTrue();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("--sbom is required");
        }
    }

    @Test
    void missingFileReturnsExitCode1() {
        ScrutinizerCommand command = createMinimalCommand();
        command.run("--sbom", "/nonexistent/file.json", "--policy", "/nonexistent/policy.yaml", "--no-file");
        assertThat(command.getExitCode()).isEqualTo(1);
    }

    private ScrutinizerCommand createMinimalCommand() {
        return new ScrutinizerCommand(
                new com.scrutinizer.parser.SbomParser(),
                new com.scrutinizer.graph.GraphAnalyzer(),
                new com.scrutinizer.policy.PolicyParser(),
                new com.scrutinizer.enrichment.EnrichmentPipeline(
                        new com.scrutinizer.enrichment.ScorecardService(),
                        new com.scrutinizer.enrichment.ProvenanceService()
                ),
                new com.scrutinizer.engine.PostureEvaluator(
                        new com.scrutinizer.engine.RuleEvaluator()
                ),
                new com.scrutinizer.viz.GraphExportCommand(
                        new com.scrutinizer.parser.SbomParser()
                )
        );
    }
}
