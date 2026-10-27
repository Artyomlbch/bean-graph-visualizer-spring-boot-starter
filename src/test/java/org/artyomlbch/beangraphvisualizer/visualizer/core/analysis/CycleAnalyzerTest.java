package org.artyomlbch.beangraphvisualizer.visualizer.core.analysis;

import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanEdge;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.model.InjectionType;
import org.artyomlbch.beangraphvisualizer.visualizer.model.analysis.BeanCycle;
import org.artyomlbch.beangraphvisualizer.visualizer.model.analysis.CycleReport;
import org.artyomlbch.beangraphvisualizer.visualizer.model.analysis.ModuleCycle;
import org.artyomlbch.beangraphvisualizer.visualizer.model.analysis.ModuleLink;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.artyomlbch.beangraphvisualizer.visualizer.model.filter.Stereotype.CONFIGURATION;
import static org.artyomlbch.beangraphvisualizer.visualizer.model.filter.Stereotype.REPOSITORY;
import static org.artyomlbch.beangraphvisualizer.visualizer.model.filter.Stereotype.SERVICE;
import static org.artyomlbch.beangraphvisualizer.visualizer.model.filter.Stereotype.UNKNOWN;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.TestGraphs.bean;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.TestGraphs.graph;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.TestGraphs.system;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.TestGraphs.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class CycleAnalyzerTest {

    private static final String APP = "com.example.app.";

    // бины из user(...) лежат прямо в com.example.app и к модулям не относятся
    private final CycleAnalyzer analyzer = new CycleAnalyzer(new ModuleResolver(List.of("com.example.app")));

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

    // Циклы между модулями
    @Test
    void moduleCycleWithoutBeanCycle() {
        BeanGraph graph = graph()
                .with(bean("orderService", APP + "orders.OrderService", SERVICE),
                        bean("orderRepository", APP + "orders.OrderRepository", REPOSITORY),
                        bean("billingService", APP + "billing.BillingService", SERVICE),
                        bean("invoiceService", APP + "billing.InvoiceService", SERVICE))
                .edge("orderService", "billingService")
                .edge("orderService", "invoiceService")
                .edge("invoiceService", "orderRepository")
                .build();

        CycleReport report = analyzer.analyze(graph);

        assertThat(report.beanCycles()).isEmpty();
        assertThat(report.moduleCycles()).hasSize(1);
        ModuleCycle cycle = report.moduleCycles().getFirst();
        assertThat(cycle.modules()).containsExactly(APP + "billing", APP + "orders");
        // первой идёт самая слабая связь: её, скорее всего, и стоит разорвать
        assertThat(cycle.links())
                .extracting(ModuleLink::from, ModuleLink::to, ModuleLink::count)
                .containsExactly(
                        tuple(APP + "billing", APP + "orders", 1),
                        tuple(APP + "orders", APP + "billing", 2));
    }

    @Test
    void dependencyOnInterfaceOfOwnModuleIsNotCycle() {
        BeanGraph graph = shippingGraph(APP + "shipping.ShipmentListener");

        assertThat(analyzer.analyze(graph).moduleCycles()).isEmpty();
    }

    // Если тип из кода неизвестен (точка внедрения не найдена), берётся класс внедренного бина
    @Test
    void withoutDeclaredTypeClassOfInjectedBeanIsUsed() {
        BeanGraph graph = shippingGraph(null);

        assertThat(analyzer.analyze(graph).moduleCycles())
                .extracting(ModuleCycle::modules)
                .containsExactly(List.of(APP + "orders", APP + "shipping"));
    }

    private static BeanGraph shippingGraph(String declaredType) {
        InjectionType type = declaredType != null ? InjectionType.CONSTRUCTOR : InjectionType.UNKNOWN;
        return graph()
                .with(bean("orderService", APP + "orders.OrderService", SERVICE),
                        bean("orderShipmentHandler", APP + "orders.OrderShipmentHandler", UNKNOWN),
                        bean("shippingService", APP + "shipping.ShippingService", SERVICE))
                .edge("orderService", "shippingService")
                .injection("shippingService", "orderShipmentHandler", type, declaredType)
                .build();
    }

    @Test
    void beanMethodParameterBelongsToConfiguration() {
        BeanGraph graph = graph()
                .with(bean("reportingConfig", APP + "reporting.ReportingConfig", CONFIGURATION),
                        bean("reportPrinter", APP + "printing.ReportPrinter", UNKNOWN),
                        bean("orderRepository", APP + "orders.OrderRepository", REPOSITORY),
                        bean("orderService", APP + "orders.OrderService", SERVICE))
                .declaredIn("reportPrinter", "reportingConfig")
                .injection("reportPrinter", "orderRepository", InjectionType.FACTORY_METHOD,
                        APP + "orders.OrderRepository")
                .edge("orderService", "reportPrinter")
                .build();

        assertThat(analyzer.analyze(graph).moduleCycles()).isEmpty();
    }

    // Класс приложения лежит прямо в основном пакете, а системные бины вне его — к модулям они не относятся
    @Test
    void beansOutsideModulesAreIgnored() {
        BeanGraph graph = graph()
                .with(bean("application", APP + "Application", CONFIGURATION),
                        bean("orderService", APP + "orders.OrderService", SERVICE),
                        system("dataSource"))
                .edge("application", "orderService")
                .edge("orderService", "application")
                .edge("orderService", "dataSource")
                .build();

        assertThat(analyzer.analyze(graph).moduleCycles()).isEmpty();
    }
}
