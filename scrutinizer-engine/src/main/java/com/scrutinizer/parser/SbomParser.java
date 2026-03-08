package com.scrutinizer.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scrutinizer.model.Component;
import com.scrutinizer.model.DependencyEdge;
import com.scrutinizer.model.DependencyGraph;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses CycloneDX JSON SBOMs into normalized DependencyGraph objects.
 */
@Service
public class SbomParser {

    private final ObjectMapper objectMapper;

    public SbomParser() {
        this.objectMapper = new ObjectMapper();
    }

    public SbomParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parse a CycloneDX JSON file from disk.
     */
    public DependencyGraph parseFile(Path path) throws IOException {
        String content = Files.readString(path);
        JsonNode root = objectMapper.readTree(content);
        return parse(root);
    }

    /**
     * Parse a CycloneDX JSON node into a normalized DependencyGraph.
     */
    public DependencyGraph parse(JsonNode root) {
        validateFormat(root);
        String rootRef = extractRootRef(root);
        List<Component> components = parseComponents(root);
        List<DependencyEdge> edges = parseDependencies(root);
        return new DependencyGraph(components, edges, rootRef);
    }

    private void validateFormat(JsonNode root) {
        JsonNode bomFormatNode = root.get("bomFormat");
        String bomFormat = bomFormatNode != null ? bomFormatNode.asText() : null;
        if (!"CycloneDX".equals(bomFormat)) {
            throw new SbomParseException(
                    "Expected bomFormat 'CycloneDX', got '" + bomFormat + "'");
        }

        JsonNode specVersionNode = root.get("specVersion");
        String specVersion = specVersionNode != null ? specVersionNode.asText() : "";
        if (!specVersion.startsWith("1.")) {
            throw new SbomParseException(
                    "Unsupported specVersion '" + specVersion + "'");
        }
    }

    private String extractRootRef(JsonNode root) {
        JsonNode metadata = root.get("metadata");
        if (metadata == null) return null;
        JsonNode component = metadata.get("component");
        if (component == null) return null;
        JsonNode bomRef = component.get("bom-ref");
        return bomRef != null ? bomRef.asText() : null;
    }

    private List<Component> parseComponents(JsonNode root) {
        JsonNode componentsNode = root.get("components");
        if (componentsNode == null || !componentsNode.isArray()) {
            return List.of();
        }

        List<Component> components = new ArrayList<>();
        for (JsonNode raw : componentsNode) {
            JsonNode bomRefNode = raw.get("bom-ref");
            JsonNode nameNode = raw.get("name");

            if (bomRefNode == null || nameNode == null) {
                throw new SbomParseException(
                        "Component missing required field 'bom-ref' or 'name': " + raw);
            }

            components.add(new Component(
                    nameNode.asText(),
                    raw.has("version") ? raw.get("version").asText() : "",
                    bomRefNode.asText(),
                    raw.has("type") ? raw.get("type").asText() : "library",
                    raw.has("group") ? raw.get("group").asText() : null,
                    raw.has("purl") ? raw.get("purl").asText() : null,
                    raw.has("description") ? raw.get("description").asText() : null,
                    raw.has("scope") ? raw.get("scope").asText() : "required"
            ));
        }
        return components;
    }

    private List<DependencyEdge> parseDependencies(JsonNode root) {
        JsonNode depsNode = root.get("dependencies");
        if (depsNode == null || !depsNode.isArray()) {
            return List.of();
        }

        List<DependencyEdge> edges = new ArrayList<>();
        for (JsonNode dep : depsNode) {
            JsonNode refNode = dep.get("ref");
            if (refNode == null) {
                throw new SbomParseException(
                        "Dependency entry missing 'ref': " + dep);
            }
            String sourceRef = refNode.asText();

            JsonNode dependsOn = dep.get("dependsOn");
            if (dependsOn != null && dependsOn.isArray()) {
                for (JsonNode target : dependsOn) {
                    edges.add(new DependencyEdge(sourceRef, target.asText()));
                }
            }
        }
        return edges;
    }
}
