package org.artyomlbch.beangraphvisualizer.visualizer.core.analysis;

import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanEdge;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.model.InjectionType;
import org.artyomlbch.beangraphvisualizer.visualizer.model.analysis.BeanCycle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.artyomlbch.beangraphvisualizer.visualizer.support.TestGraphs.graph;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.TestGraphs.user;
import static org.assertj.core.api.Assertions.assertThat;

class CycleAnalyzerTest {

    private final CycleAnalyzer analyzer = new CycleAnalyzer();

    private static List<String> keys(List<BeanEdge> edges) {
        return edges.stream().map(edge -> edge.source().id() + " -> " + edge.target().id()).toList();
    }

    @Test
    void graphWithoutCyclesHasNoBeanCycles() {
        BeanGraph graph = graph()
                .with(user("controller"), user("service"), user("repository"))
                .edge("controller", "service")
                .edge("service", "repository")
                .build();

        assertThat(analyzer.analyze(graph).beanCycles()).isEmpty();
    }

    @Test
    void beanCycleContainsOnlyEdgesInsideIt() {
        BeanGraph graph = graph()
                .with(user("a"), user("b"), user("c"))
                .edge("a", "b")
                .edge("b", "a")
                .edge("a", "c")
                .build();

        List<BeanCycle> cycles = analyzer.analyze(graph).beanCycles();

        assertThat(cycles).hasSize(1);
        assertThat(cycles.getFirst().beans()).containsExactly("a", "b");
        assertThat(keys(cycles.getFirst().edges())).containsExactlyInAnyOrder("a -> b", "b -> a");
    }

    @Test
    void configurationThatInjectsItsOwnBeanIsCycle() {
        BeanGraph graph = graph()
                .with(user("appConfig"), user("client"))
                .declaredIn("client", "appConfig")
                .injection("appConfig", "client", InjectionType.FIELD, "com.example.app.client")
                .build();

        assertThat(analyzer.analyze(graph).beanCycles())
                .extracting(BeanCycle::beans)
                .containsExactly(List.of("appConfig", "client"));
    }

    @Test
    void dependsOnCountsAsDependency() {
        BeanGraph graph = graph()
                .with(user("cache"), user("warmer"))
                .dependsOn("cache", "warmer")
                .edge("warmer", "cache")
                .build();

        assertThat(analyzer.analyze(graph).beanCycles()).hasSize(1);
    }

    @Test
    void largestCycleComesFirst() {
        BeanGraph graph = graph()
                .with(user("a"), user("b"), user("c"), user("d"), user("e"))
                .edge("a", "b").edge("b", "a")
                .edge("c", "d").edge("d", "e").edge("e", "c")
                .build();

        assertThat(analyzer.analyze(graph).beanCycles())
                .extracting(BeanCycle::beans)
                .containsExactly(List.of("c", "d", "e"), List.of("a", "b"));
    }
}
