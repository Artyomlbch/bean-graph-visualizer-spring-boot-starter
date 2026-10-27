package org.artyomlbch.beangraphvisualizer.visualizer.core.analysis;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.artyomlbch.beangraphvisualizer.visualizer.core.analysis.CycleDetector.findCycles;
import static org.assertj.core.api.Assertions.assertThat;

class CycleDetectorTest {

    // граф из рёбер вида "a>b"
    private static Map<String, List<String>> graph(String... edges) {
        Map<String, List<String>> graph = new LinkedHashMap<>();
        for (String edge : edges) {
            String[] ends = edge.split(">");
            graph.computeIfAbsent(ends[0], vertex -> new ArrayList<>()).add(ends[1]);
        }
        return graph;
    }

    // b и c есть только как концы рёбер, ключами графа они не указаны
    @Test
    void chainHasNoCycles() {
        assertThat(findCycles(graph("a>b", "b>c"))).isEmpty();
    }

    @Test
    void twoVerticesDependingOnEachOtherAreCycle() {
        assertThat(findCycles(graph("a>b", "b>a"))).containsExactly(Set.of("a", "b"));
    }

    // d ведёт в цикл, но из цикла в d не вернуться, поэтому d в цикл не входит
    @Test
    void vertexThatOnlyLeadsIntoCycleIsNotPartOfIt() {
        assertThat(findCycles(graph("d>a", "a>b", "b>c", "c>a"))).containsExactly(Set.of("a", "b", "c"));
    }

    // из {a, b} есть ребро в {c, d}, обратного нет — это два разных цикла
    @Test
    void separateCyclesAreFoundSeparately() {
        assertThat(findCycles(graph("a>b", "b>a", "b>c", "c>d", "d>c")))
                .containsExactlyInAnyOrder(Set.of("a", "b"), Set.of("c", "d"));
    }

    // a ⇄ b и b ⇄ c: из любой вершины можно дойти до любой, это одна компонента
    @Test
    void intertwinedCyclesAreOneComponent() {
        assertThat(findCycles(graph("a>b", "b>a", "b>c", "c>b"))).containsExactly(Set.of("a", "b", "c"));
    }

    @Test
    void selfLoopIsNotCycle() {
        assertThat(findCycles(graph("a>a", "a>b"))).isEmpty();
    }
}
