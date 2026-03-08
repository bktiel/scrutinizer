package com.scrutinizer.cli;

import com.scrutinizer.cli.ScrutinizerCommand.CliArgs;
import com.scrutinizer.engine.PostureEvaluator;
import com.scrutinizer.engine.RuleEvaluator;
import com.scrutinizer.enrichment.EnrichmentPipeline;
import com.scrutinizer.enrichment.ProvenanceService;
import com.scrutinizer.enrichment.ScorecardService;
import com.scrutinizer.graph.GraphAnalyzer;
import com.scrutinizer.parser.SbomParser;
import com.scrutinizer.policy.PolicyParser;
import com.scrutinizer.viz.GraphExportCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScrutinizerCommandTest {

    private static final String SAMPLE_SBOM = "src/test/resources/fixtures/sample_npm_sbom.json";

    @Nested
    class ArgParsing {
        @Test
        void hasSbomArgument() {
            CliArgs args = ScrutinizerCommand.parseArgs(new String[]{"--sbom", "test.json"});
            assertThat(args.sbomPath()).isEqualTo(Path.of("test.json"));
        }

        @Test
        void defaultFormatIsTable() {
            CliArgs args = ScrutinizerCommand.parseArgs(new String[]{"--sbom", "test.json"});
            assertThat(args.format()).isEqualTo("table");
        }

        @Test
        void jsonFormat() {
            CliArgs args = ScrutinizerCommand.parseArgs(
                    new String[]{"--sbom", "test.json", "--format", "json"});
            assertThat(args.format()).isEqualTo("json");
        }

        @Test
        void missingRequiredSbom() {
            assertThatThrownBy(() -> ScrutinizerCommand.parseArgs(new String[]{}))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void invalidFormat() {
            assertThatThrownBy(() -> ScrutinizerCommand.parseArgs(
                    new String[]{"--sbom", "test.json", "--format", "xml"}))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class TableOutput {
        private ScrutinizerCommand command;
        private ByteArrayOutputStream stdout;
        private ByteArrayOutputStream stderr;

        @BeforeEach
        void setUp() {
            command = new ScrutinizerCommand(new SbomParser(), new GraphAnalyzer(),
                    new PolicyParser(),
                    new EnrichmentPipeline(new ScorecardService(), new ProvenanceService()),
                    new PostureEvaluator(new RuleEvaluator()),
                    new GraphExportCommand(new SbomParser()));
            stdout = new ByteArrayOutputStream();
            stderr = new ByteArrayOutputStream();
            System.setOut(new PrintStream(stdout));
            System.setErr(new PrintStream(stderr));
        }

        @Test
        void exitCodeZero() {
            command.run("--sbom", SAMPLE_SBOM);
            assertThat(command.getExitCode()).isEqualTo(0);
        }

        @Test
        void printsInventoryHeader() {
            command.run("--sbom", SAMPLE_SBOM);
            String output = stdout.toString();
            assertThat(output).contains("SBOM Inventory: 5 components");
            assertThat(output).contains("6 dependency edges");
        }

        @Test
        void printsColumnHeaders() {
            command.run("--sbom", SAMPLE_SBOM);
            String output = stdout.toString();
            assertThat(output).contains("Name");
            assertThat(output).contains("Version");
            assertThat(output).contains("Type");
            assertThat(output).contains("Scope");
        }

        @Test
        void printsComponentNames() {
            command.run("--sbom", SAMPLE_SBOM);
            String output = stdout.toString();
            assertThat(output).contains("express");
            assertThat(output).contains("body-parser");
            assertThat(output).contains("debug");
            assertThat(output).contains("ms");
            assertThat(output).contains("bytes");
        }
    }

    @Nested
    class JsonOutput {
        private ScrutinizerCommand command;
        private ByteArrayOutputStream stdout;

        @BeforeEach
        void setUp() {
            command = new ScrutinizerCommand(new SbomParser(), new GraphAnalyzer(),
                    new PolicyParser(),
                    new EnrichmentPipeline(new ScorecardService(), new ProvenanceService()),
                    new PostureEvaluator(new RuleEvaluator()),
                    new GraphExportCommand(new SbomParser()));
            stdout = new ByteArrayOutputStream();
            System.setOut(new PrintStream(stdout));
        }

        @Test
        void validJson() {
            command.run("--sbom", SAMPLE_SBOM, "--format", "json");
            String output = stdout.toString();
            assertThat(output).contains("\"components\"");
            assertThat(output).contains("\"edges\"");
            assertThat(output).contains("\"summary\"");
        }

        @Test
        void jsonComponentCount() {
            command.run("--sbom", SAMPLE_SBOM, "--format", "json");
            String output = stdout.toString();
            assertThat(output).contains("\"total_components\" : 5");
        }
    }

    @Nested
    class Errors {
        private ScrutinizerCommand command;
        private ByteArrayOutputStream stderr;

        @BeforeEach
        void setUp() {
            command = new ScrutinizerCommand(new SbomParser(), new GraphAnalyzer(),
                    new PolicyParser(),
                    new EnrichmentPipeline(new ScorecardService(), new ProvenanceService()),
                    new PostureEvaluator(new RuleEvaluator()),
                    new GraphExportCommand(new SbomParser()));
            stderr = new ByteArrayOutputStream();
            System.setErr(new PrintStream(stderr));
        }

        @Test
        void fileNotFound() {
            command.run("--sbom", "nonexistent.json");
            assertThat(command.getExitCode()).isEqualTo(1);
            assertThat(stderr.toString()).contains("File not found");
        }

        @Test
        void invalidSbom() throws Exception {
            // Create a temp file with invalid SBOM
            Path tmpFile = Path.of(System.getProperty("java.io.tmpdir"), "bad_sbom.json");
            java.nio.file.Files.writeString(tmpFile, "{\"bomFormat\": \"SPDX\"}");
            command.run("--sbom", tmpFile.toString());
            assertThat(command.getExitCode()).isEqualTo(1);
            assertThat(stderr.toString()).contains("Error");
            java.nio.file.Files.deleteIfExists(tmpFile);
        }
    }
}
