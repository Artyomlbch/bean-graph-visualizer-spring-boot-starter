package org.artyomlbch.beangraphvisualizer.visualizer.support;

import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanEdge;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanNode;

import java.util.List;
import java.util.stream.Stream;

public final class GraphLookup {

    private GraphLookup() {}

    public static List<BeanNode> allNodes(BeanGraph graph) {
        return Stream.concat(graph.getNodes().stream(), graph.getSoloNodes().stream()).toList();
    }

    public static BeanNode node(BeanGraph graph, String id) {
        return allNodes(graph).stream()
                .filter(node -> node.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Graph has no node " + id));
    }

    public static BeanEdge edge(BeanGraph graph, String from, String to) {
        return graph.getEdges().stream()
                .filter(edge -> edge.source().id().equals(from) && edge.target().id().equals(to))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Graph has no edge " + from + " -> " + to));
    }
}
