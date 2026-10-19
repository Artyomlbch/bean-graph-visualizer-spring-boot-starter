package org.artyomlbch.beangraphvisualizer.visualizer.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.artyomlbch.beangraphvisualizer.visualizer.support.GraphAssertions.assertConsistent;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.GraphLookup.edgeKeys;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.GraphLookup.ids;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.TestGraphs.graph;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.TestGraphs.user;
import static org.assertj.core.api.Assertions.assertThat;

class BeanGraphTest {

    @Test
    void beanWithOnlyIncomingEdgesIsNotSolo() {
        BeanGraph graph = graph()
                .with(user("service"), user("repository"))
                .edge("service", "repository")
                .build();

        assertThat(ids(graph.getNodes())).containsExactly("service", "repository");
        assertThat(graph.getSoloNodes()).isEmpty();
    }

    @Test
    void beanWithNoEdgesIsSolo() {
        BeanGraph graph = graph()
                .with(user("a"), user("b"), user("lonely"))
                .edge("a", "b")
                .build();

        assertThat(ids(graph.getSoloNodes())).containsExactly("lonely");
    }

    @Test
    void duplicateBeansAndEdgesAreMerged() {
        BeanNode a = user("a");
        BeanNode b = user("b");
        BeanEdge ab = BeanEdge.injection(a, b, InjectionType.CONSTRUCTOR);

        BeanGraph graph = BeanGraph.of(List.of(a, b, a), List.of(ab, ab));

        assertThat(ids(graph.getNodes())).containsExactly("a", "b");
        assertThat(graph.getEdges()).hasSize(1);
    }

    @Test
    void edgesToMissingBeansAreDropped() {
        BeanNode a = user("a");
        BeanEdge toMissing = BeanEdge.injection(a, user("missing"), InjectionType.CONSTRUCTOR);

        BeanGraph graph = BeanGraph.of(List.of(a), List.of(toMissing));

        assertThat(graph.getEdges()).isEmpty();
        assertThat(ids(graph.getSoloNodes())).containsExactly("a");
    }

    @Test
    void subgraphKeepsOnlyEdgesWithBothEnds() {
        BeanGraph graph = graph()
                .with(user("a"), user("b"), user("c"))
                .edge("a", "b")
                .edge("b", "c")
                .build();

        BeanGraph subgraph = graph.subgraph(node -> !node.id().equals("c"));

        assertThat(edgeKeys(subgraph)).containsExactly("a -> b");
        assertConsistent(subgraph);
    }

    @Test
    void subgraphRecomputesSoloBeans() {
        BeanGraph graph = graph()
                .with(user("a"), user("b"))
                .edge("a", "b")
                .build();

        BeanGraph subgraph = graph.subgraph(node -> node.id().equals("a"));

        assertThat(subgraph.getNodes()).isEmpty();
        assertThat(ids(subgraph.getSoloNodes())).containsExactly("a");
    }
}
