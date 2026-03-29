package com.scrutinizer.viz;

import com.scrutinizer.model.Component;
import com.scrutinizer.model.DependencyEdge;
import com.scrutinizer.model.DependencyGraph;

import java.util.*;

/**
 * Exports a DependencyGraph to an interactive HTML visualization
 * using D3.js force-directed layout. Self-contained single-file output
 * with no external dependencies (D3 is loaded from CDN).
 */
public final class HtmlGraphExporter {

    private HtmlGraphExporter() {}

    /**
     * Generate a complete standalone HTML file with interactive dependency graph.
     */
    public static String export(DependencyGraph graph) {
        return export(graph, "Dependency Graph");
    }

    /**
     * Generate with a custom title.
     */
    public static String export(DependencyGraph graph, String title) {
        // Build nodes JSON
        Map<String, Component> componentMap = new LinkedHashMap<>();
        for (Component c : graph.components()) {
            componentMap.put(c.bomRef(), c);
        }

        // Find roots
        Set<String> depTargets = new HashSet<>();
        for (DependencyEdge edge : graph.edges()) {
            depTargets.add(edge.targetRef());
        }

        StringBuilder nodesJson = new StringBuilder("[");
        boolean first = true;
        for (Component c : graph.components()) {
            if (!first) nodesJson.append(",");
            boolean isRoot = !depTargets.contains(c.bomRef());
            nodesJson.append(String.format(
                    "{\"id\":\"%s\",\"name\":\"%s\",\"version\":\"%s\",\"type\":\"%s\",\"scope\":\"%s\",\"group\":%s,\"purl\":%s,\"isRoot\":%b}",
                    escapeJson(c.bomRef()),
                    escapeJson(c.name()),
                    escapeJson(c.version()),
                    escapeJson(c.type()),
                    escapeJson(c.scope()),
                    c.group().map(g -> "\"" + escapeJson(g) + "\"").orElse("null"),
                    c.purl().map(p -> "\"" + escapeJson(p) + "\"").orElse("null"),
                    isRoot));
            first = false;
        }
        nodesJson.append("]");

        StringBuilder linksJson = new StringBuilder("[");
        first = true;
        for (DependencyEdge edge : graph.edges()) {
            // Skip edges referencing nodes not in the components list (e.g. root metadata component)
            if (!componentMap.containsKey(edge.sourceRef()) || !componentMap.containsKey(edge.targetRef())) {
                continue;
            }
            if (!first) linksJson.append(",");
            linksJson.append(String.format(
                    "{\"source\":\"%s\",\"target\":\"%s\"}",
                    escapeJson(edge.sourceRef()),
                    escapeJson(edge.targetRef())));
            first = false;
        }
        linksJson.append("]");

        return HTML_TEMPLATE
                .replace("{{TITLE}}", escapeHtml(title))
                .replace("{{NODES_JSON}}", nodesJson.toString())
                .replace("{{LINKS_JSON}}", linksJson.toString())
                .replace("{{NODE_COUNT}}", String.valueOf(graph.components().size()))
                .replace("{{EDGE_COUNT}}", String.valueOf(graph.edges().size()));
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static final String HTML_TEMPLATE = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>{{TITLE}} — Scrutinizer</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: #1a1a2e;
            color: #e0e0e0;
            overflow: hidden;
        }
        #header {
            position: fixed; top: 0; left: 0; right: 0; z-index: 10;
            background: rgba(26, 26, 46, 0.95);
            padding: 12px 24px;
            border-bottom: 1px solid #333;
            display: flex; align-items: center; justify-content: space-between;
        }
        #header h1 { font-size: 18px; font-weight: 600; color: #4A90D9; }
        #stats { font-size: 13px; color: #888; }
        #graph { width: 100vw; height: 100vh; }
        .node-label {
            font-size: 10px; fill: #e0e0e0;
            pointer-events: none; text-anchor: middle;
        }
        #tooltip {
            position: fixed; display: none;
            background: rgba(30, 30, 50, 0.95);
            border: 1px solid #4A90D9;
            border-radius: 6px; padding: 12px;
            font-size: 12px; max-width: 320px;
            z-index: 100; pointer-events: none;
        }
        #tooltip .field { color: #888; margin-right: 4px; }
        #tooltip .value { color: #e0e0e0; }
        #tooltip .title { font-weight: 600; color: #4A90D9; margin-bottom: 6px; font-size: 14px; }
        #controls {
            position: fixed; bottom: 16px; right: 16px; z-index: 10;
            display: flex; gap: 8px;
        }
        .ctrl-btn {
            background: rgba(74, 144, 217, 0.2);
            border: 1px solid #4A90D9; color: #4A90D9;
            padding: 6px 12px; border-radius: 4px;
            cursor: pointer; font-size: 12px;
        }
        .ctrl-btn:hover { background: rgba(74, 144, 217, 0.4); }
        #legend {
            position: fixed; bottom: 16px; left: 16px; z-index: 10;
            background: rgba(26, 26, 46, 0.9); border: 1px solid #333;
            border-radius: 6px; padding: 10px; font-size: 11px;
        }
        .legend-item { display: flex; align-items: center; gap: 6px; margin: 3px 0; }
        .legend-dot { width: 10px; height: 10px; border-radius: 50%; }
    </style>
</head>
<body>
    <div id="header">
        <h1>{{TITLE}}</h1>
        <div id="stats">{{NODE_COUNT}} components &middot; {{EDGE_COUNT}} dependencies</div>
    </div>
    <svg id="graph"></svg>
    <div id="tooltip"></div>
    <div id="legend">
        <div class="legend-item"><div class="legend-dot" style="background:#4A90D9"></div> Root component</div>
        <div class="legend-item"><div class="legend-dot" style="background:#A8D08D"></div> Required</div>
        <div class="legend-item"><div class="legend-dot" style="background:#F4D03F"></div> Optional</div>
    </div>
    <div id="controls">
        <button class="ctrl-btn" onclick="resetZoom()">Reset Zoom</button>
    </div>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/d3/7.8.5/d3.min.js"></script>
    <script>
        const nodes = {{NODES_JSON}};
        const links = {{LINKS_JSON}};

        const width = window.innerWidth;
        const height = window.innerHeight;

        const svg = d3.select("#graph")
            .attr("width", width)
            .attr("height", height);

        const g = svg.append("g");

        const zoom = d3.zoom()
            .scaleExtent([0.1, 4])
            .on("zoom", (event) => g.attr("transform", event.transform));

        svg.call(zoom);

        const simulation = d3.forceSimulation(nodes)
            .force("link", d3.forceLink(links).id(d => d.id).distance(80))
            .force("charge", d3.forceManyBody().strength(-200))
            .force("center", d3.forceCenter(width / 2, height / 2))
            .force("collision", d3.forceCollide(30));

        // Arrow markers
        svg.append("defs").append("marker")
            .attr("id", "arrow")
            .attr("viewBox", "0 -5 10 10")
            .attr("refX", 20).attr("refY", 0)
            .attr("markerWidth", 6).attr("markerHeight", 6)
            .attr("orient", "auto")
            .append("path")
            .attr("d", "M0,-5L10,0L0,5")
            .attr("fill", "#555");

        const link = g.append("g")
            .selectAll("line")
            .data(links)
            .join("line")
            .attr("stroke", "#555")
            .attr("stroke-opacity", 0.4)
            .attr("stroke-width", 1)
            .attr("marker-end", "url(#arrow)");

        function nodeColor(d) {
            if (d.isRoot) return "#4A90D9";
            if (d.scope === "optional") return "#F4D03F";
            return "#A8D08D";
        }

        const node = g.append("g")
            .selectAll("circle")
            .data(nodes)
            .join("circle")
            .attr("r", d => d.isRoot ? 10 : 7)
            .attr("fill", nodeColor)
            .attr("stroke", "#fff")
            .attr("stroke-width", 1)
            .call(d3.drag()
                .on("start", dragStart)
                .on("drag", dragging)
                .on("end", dragEnd));

        const labels = g.append("g")
            .selectAll("text")
            .data(nodes)
            .join("text")
            .attr("class", "node-label")
            .attr("dy", -14)
            .text(d => d.name);

        // Tooltip
        const tooltip = d3.select("#tooltip");
        node.on("mouseover", (event, d) => {
            tooltip.style("display", "block")
                .html(`<div class="title">${d.name}</div>
                    <div><span class="field">Version:</span><span class="value">${d.version}</span></div>
                    <div><span class="field">Type:</span><span class="value">${d.type}</span></div>
                    <div><span class="field">Scope:</span><span class="value">${d.scope}</span></div>
                    ${d.group ? `<div><span class="field">Group:</span><span class="value">${d.group}</span></div>` : ''}
                    ${d.purl ? `<div><span class="field">PURL:</span><span class="value">${d.purl}</span></div>` : ''}
                    <div><span class="field">Ref:</span><span class="value">${d.id}</span></div>`);
        }).on("mousemove", (event) => {
            tooltip.style("left", (event.clientX + 16) + "px")
                .style("top", (event.clientY - 10) + "px");
        }).on("mouseout", () => tooltip.style("display", "none"));

        // Highlight connections on hover
        node.on("mouseover.highlight", (event, d) => {
            const connected = new Set();
            links.forEach(l => {
                if (l.source.id === d.id) connected.add(l.target.id);
                if (l.target.id === d.id) connected.add(l.source.id);
            });
            connected.add(d.id);
            node.attr("opacity", n => connected.has(n.id) ? 1 : 0.15);
            link.attr("stroke-opacity", l =>
                (l.source.id === d.id || l.target.id === d.id) ? 0.8 : 0.05);
            labels.attr("opacity", n => connected.has(n.id) ? 1 : 0.1);
        }).on("mouseout.highlight", () => {
            node.attr("opacity", 1);
            link.attr("stroke-opacity", 0.4);
            labels.attr("opacity", 1);
        });

        simulation.on("tick", () => {
            link.attr("x1", d => d.source.x).attr("y1", d => d.source.y)
                .attr("x2", d => d.target.x).attr("y2", d => d.target.y);
            node.attr("cx", d => d.x).attr("cy", d => d.y);
            labels.attr("x", d => d.x).attr("y", d => d.y);
        });

        function dragStart(event, d) {
            if (!event.active) simulation.alphaTarget(0.3).restart();
            d.fx = d.x; d.fy = d.y;
        }
        function dragging(event, d) { d.fx = event.x; d.fy = event.y; }
        function dragEnd(event, d) {
            if (!event.active) simulation.alphaTarget(0);
            d.fx = null; d.fy = null;
        }
        function resetZoom() {
            svg.transition().duration(500).call(zoom.transform, d3.zoomIdentity);
        }
    </script>
</body>
</html>
""";
}
