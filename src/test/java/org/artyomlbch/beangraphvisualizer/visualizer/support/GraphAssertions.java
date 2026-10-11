package org.artyomlbch.beangraphvisualizer.visualizer.support;

import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanNode;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.artyomlbch.beangraphvisualizer.visualizer.support.GraphLookup.allNodes;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.GraphLookup.ids;
import static org.assertj.core.api.Assertions.assertThat;

public final class GraphAssertions {

    private GraphAssertions() {}

    public static void assertConsistent(BeanGraph graph) {
        assertThat(ids(allNodes(graph)))
                .as("every bean appears in the graph only once")
                .doesNotHaveDuplicates();

        Set<String> connectedIds = graph.getNodes().stream()
                .map(BeanNode::id)
                .collect(Collectors.toSet());

        Set<String> edgeEndIds = graph.getEdges().stream()
                .flatMap(edge -> Stream.of(edge.source().id(), edge.target().id()))
                .collect(Collectors.toSet());

        assertThat(edgeEndIds)
                .as("edge ends must be exactly the connected nodes")
                .isEqualTo(connectedIds);
    }

}
