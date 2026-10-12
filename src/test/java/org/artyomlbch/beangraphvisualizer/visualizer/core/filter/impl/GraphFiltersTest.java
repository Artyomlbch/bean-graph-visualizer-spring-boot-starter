package org.artyomlbch.beangraphvisualizer.visualizer.core.filter.impl;

import org.artyomlbch.beangraphvisualizer.visualizer.core.filter.Filter;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.model.filter.DetailLevel;
import org.artyomlbch.beangraphvisualizer.visualizer.model.filter.GraphType;
import org.artyomlbch.beangraphvisualizer.visualizer.model.filter.ScopeType;
import org.artyomlbch.beangraphvisualizer.visualizer.model.filter.Stereotype;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.artyomlbch.beangraphvisualizer.visualizer.model.filter.Stereotype.CONTROLLER;
import static org.artyomlbch.beangraphvisualizer.visualizer.model.filter.Stereotype.REPOSITORY;
import static org.artyomlbch.beangraphvisualizer.visualizer.model.filter.Stereotype.SERVICE;
import static org.artyomlbch.beangraphvisualizer.visualizer.model.filter.Stereotype.UNKNOWN;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.GraphAssertions.assertConsistent;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.GraphLookup.allNodes;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.GraphLookup.edgeKeys;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.GraphLookup.ids;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.TestGraphs.bean;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.TestGraphs.graph;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.TestGraphs.prototype;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.TestGraphs.system;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Юнит-тесты фильтров на собранном вручную графе
 * TODO: доделать комментарии
 */
class GraphFiltersTest {

    private static final BeanGraph GRAPH = graph()
            .with(
                    bean("orderController", "com.example.orders.OrderController", CONTROLLER),
                    bean("reportController", "com.example.reports.ReportController", CONTROLLER),
                    bean("orderService", "com.example.orders.OrderService", SERVICE),
                    bean("orderRepository", "com.example.orders.OrderRepository", REPOSITORY),
                    prototype("exportTask", "com.example.reports.ExportTask"),
                    bean("taskRunner", "com.example.reports.TaskRunner", UNKNOWN),
                    bean("lonelyHelper", "com.example.util.LonelyHelper", UNKNOWN),
                    system("dataSource"),
                    system("springHelper"))
            .edge("orderController", "orderService")
            .edge("reportController", "orderService")
            .edge("orderService", "orderRepository")
            .edge("orderService", "exportTask")
            .edge("orderRepository", "dataSource")
            .edge("taskRunner", "exportTask")
            .build();

    static Stream<Arguments> allFilters() {
        return Stream.of(
                arguments(named("TYPE=USER", new TypeGraphFilter(GraphType.USER))),
                arguments(named("TYPE=SYSTEM", new TypeGraphFilter(GraphType.SYSTEM))),
                arguments(named("PACKAGE=com.example.orders", new PackageGraphFilter("com.example.orders"))),
                arguments(named("SCOPE=SINGLETON_ONLY", new BeanScopeGraphFilter(ScopeType.SINGLETON_ONLY))),
                arguments(named("SCOPE=PROTOTYPE_ONLY", new BeanScopeGraphFilter(ScopeType.PROTOTYPE_ONLY))),
                arguments(named("STEREOTYPE=CONTROLLER", new BeanStereotypeGraphFilter(Stereotype.CONTROLLER))),
                arguments(named("DETAIL_LEVEL=CONNECTED_ONLY", new DetailLevelGraphFilter(DetailLevel.CONNECTED_ONLY))),
                arguments(named("DETAIL_LEVEL=SOLO_ONLY", new DetailLevelGraphFilter(DetailLevel.SOLO_ONLY))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allFilters")
    void everyFilterProducesConsistentGraph(Filter filter) {
        assertConsistent(filter.apply(GRAPH));
    }

    @Nested
    class TypeFilter {

        @Test
        void userKeepsUserBeansAndDropsEdgesToSystemBeans() {
            BeanGraph result = new TypeGraphFilter(GraphType.USER).apply(GRAPH);

            assertThat(ids(allNodes(result))).doesNotContain("dataSource", "springHelper");
            assertThat(edgeKeys(result)).doesNotContain("orderRepository -> dataSource");
            assertThat(ids(result.getNodes())).contains("orderRepository");
        }

        @Test
        void systemKeepsOnlySystemBeans() {
            BeanGraph result = new TypeGraphFilter(GraphType.SYSTEM).apply(GRAPH);

            assertThat(ids(allNodes(result))).containsExactlyInAnyOrder("dataSource", "springHelper");
            assertThat(result.getEdges()).isEmpty();
        }
    }

    @Nested
    class PackageFilter {

        @Test
        void keepsBeansOfPackageAndEdgesBetweenThem() {
            BeanGraph result = new PackageGraphFilter("com.example.orders").apply(GRAPH);

            assertThat(ids(allNodes(result)))
                    .containsExactlyInAnyOrder("orderController", "orderService", "orderRepository");
            assertThat(edgeKeys(result))
                    .containsExactlyInAnyOrder("orderController -> orderService", "orderService -> orderRepository");
        }
    }

    @Nested
    class ScopeFilter {

        @Test
        void singletonOnlyMakesBeanSoloWhenItsOnlyNeighbourIsRemoved() {
            BeanGraph result = new BeanScopeGraphFilter(ScopeType.SINGLETON_ONLY).apply(GRAPH);

            assertThat(ids(allNodes(result))).doesNotContain("exportTask");
            assertThat(ids(result.getSoloNodes())).contains("taskRunner");
        }

        @Test
        void prototypeOnlyKeepsPrototypeBeans() {
            BeanGraph result = new BeanScopeGraphFilter(ScopeType.PROTOTYPE_ONLY).apply(GRAPH);

            assertThat(ids(allNodes(result))).containsExactly("exportTask");
        }
    }

    @Nested
    class StereotypeFilter {

        @Test
        void keepsControllersAndTheirDependenciesWithoutDuplicates() {
            BeanGraph result = new BeanStereotypeGraphFilter(Stereotype.CONTROLLER).apply(GRAPH);

            assertThat(ids(allNodes(result)))
                    .containsExactlyInAnyOrder("orderController", "reportController", "orderService");
            assertThat(edgeKeys(result))
                    .containsExactlyInAnyOrder("orderController -> orderService", "reportController -> orderService");
        }

        @Test
        void keepsRepositoriesAndTheirDependencies() {
            BeanGraph result = new BeanStereotypeGraphFilter(Stereotype.REPOSITORY).apply(GRAPH);

            assertThat(ids(allNodes(result))).containsExactlyInAnyOrder("orderRepository", "dataSource");
            assertThat(edgeKeys(result)).containsExactly("orderRepository -> dataSource");
        }
    }

    @Nested
    class DetailLevelFilter {

        @Test
        void connectedOnlyKeepsEdges() {
            BeanGraph result = new DetailLevelGraphFilter(DetailLevel.CONNECTED_ONLY).apply(GRAPH);

            assertThat(result.getSoloNodes()).isEmpty();
            assertThat(result.getEdges()).hasSameSizeAs(GRAPH.getEdges());
        }

        @Test
        void soloOnlyKeepsSoloBeans() {
            BeanGraph result = new DetailLevelGraphFilter(DetailLevel.SOLO_ONLY).apply(GRAPH);

            assertThat(ids(allNodes(result))).containsExactlyInAnyOrder("lonelyHelper", "springHelper");
            assertThat(result.getEdges()).isEmpty();
        }
    }
}
