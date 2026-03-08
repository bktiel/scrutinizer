package com.scrutinizer.viz;

import com.scrutinizer.model.DependencyGraph;
import com.scrutinizer.parser.SbomParser;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * CLI sub-command for exporting dependency graph visualizations.
 *
 * Usage:
 *   scrutinizer --sbom &lt;path&gt; --viz dot --output graph.dot
 *   scrutinizer --sbom &lt;path&gt; --viz html --output graph.html
 */
@Service
public class GraphExportCommand {

    private final SbomParser parser;

    public GraphExportCommand(SbomParser parser) {
        this.parser = parser;
    }

    /**
     * Export a visualization of the SBOM dependency graph.
     *
     * @param sbomPath   path to the CycloneDX SBOM JSON
     * @param vizFormat  "dot" or "html"
     * @param outputPath path to write the output file
     * @param title      optional title for HTML export
     */
    public void export(Path sbomPath, String vizFormat, Path outputPath, String title)
            throws IOException {

        DependencyGraph graph = parser.parseFile(sbomPath);

        String content = switch (vizFormat.toLowerCase()) {
            case "dot" -> DotExporter.export(graph);
            case "html" -> HtmlGraphExporter.export(graph,
                    title != null ? title : sbomPath.getFileName().toString());
            default -> throw new IllegalArgumentException(
                    "Unsupported viz format: " + vizFormat + " (use 'dot' or 'html')");
        };

        Files.writeString(outputPath, content);
        System.out.println("Graph exported to: " + outputPath);
    }
}
