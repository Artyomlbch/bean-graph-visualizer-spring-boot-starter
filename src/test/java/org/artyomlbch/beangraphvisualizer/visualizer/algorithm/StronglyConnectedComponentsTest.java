package org.artyomlbch.beangraphvisualizer.visualizer.algorithm;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.artyomlbch.beangraphvisualizer.visualizer.algorithm.StronglyConnectedComponents.find;
import static org.assertj.core.api.Assertions.assertThat;

class StronglyConnectedComponentsTest {

    // граф из рёбер вида "a>b"
    private static Map<String, List<String>> graph(String... edges) {
        Map<String, List<String>> graph = new LinkedHashMap<>();
        for (String edge : edges) {
            String[] ends = edge.split(">");
            graph.computeIfAbsent(ends[0], vertex -> new ArrayList<>()).add(ends[1]);
        }
        return graph;
    }

    // b и c есть только как концы рёбер, ключами графа они не указаны, но компоненты у них есть
    @Test
    void chainSplitsIntoSingleVertices() {
        assertThat(find(graph("a>b", "b>c")))
                .containsExactlyInAnyOrder(Set.of("a"), Set.of("b"), Set.of("c"));
    }

    @Test
    void twoVerticesDependingOnEachOtherAreOneComponent() {
        assertThat(find(graph("a>b", "b>a"))).containsExactly(Set.of("a", "b"));
    }

    // d ведёт в цикл, но из цикла в d не вернуться, поэтому d - отдельная компонента
    @Test
    void vertexThatOnlyLeadsIntoCycleIsSeparate() {
        assertThat(find(graph("d>a", "a>b", "b>c", "c>a")))
                .containsExactlyInAnyOrder(Set.of("a", "b", "c"), Set.of("d"));
    }

    // из {a, b} есть ребро в {c, d}, обратного нет - это две разные компоненты
    @Test
    void separateCyclesAreSeparateComponents() {
        assertThat(find(graph("a>b", "b>a", "b>c", "c>d", "d>c")))
                .containsExactlyInAnyOrder(Set.of("a", "b"), Set.of("c", "d"));
    }

    // a ⇄ b и b ⇄ c: из любой вершины можно дойти до любой, это одна компонента
    @Test
    void intertwinedCyclesAreOneComponent() {
        assertThat(find(graph("a>b", "b>a", "b>c", "c>b"))).containsExactly(Set.of("a", "b", "c"));
    }

    // петля не добавляет вершине соседей: a остаётся одиночной компонентой
    @Test
    void selfLoopKeepsVertexAlone() {
        assertThat(find(graph("a>a", "a>b"))).containsExactlyInAnyOrder(Set.of("a"), Set.of("b"));
    }

    @Test
    void emptyGraphHasNoComponents() {
        assertThat(find(graph())).isEmpty();
    }
}
