package org.artyomlbch.beangraphvisualizer.visualizer.analysis.cycle;

import org.artyomlbch.beangraphvisualizer.visualizer.graph.build.ApplicationPackages;
import org.artyomlbch.beangraphvisualizer.visualizer.graph.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.graph.model.InjectionType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.artyomlbch.beangraphvisualizer.visualizer.graph.model.Stereotype.CONFIGURATION;
import static org.artyomlbch.beangraphvisualizer.visualizer.graph.model.Stereotype.REPOSITORY;
import static org.artyomlbch.beangraphvisualizer.visualizer.graph.model.Stereotype.SERVICE;
import static org.artyomlbch.beangraphvisualizer.visualizer.graph.model.Stereotype.UNKNOWN;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.TestGraphs.bean;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.TestGraphs.graph;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.TestGraphs.system;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ModuleCycleFinderTest {

    private static final String APP = "com.example.app.";

    // класс прямо в com.example.app к модулям не относится, модули - его подпакеты
    private final ModuleCycleFinder finder = new ModuleCycleFinder(new ApplicationPackages(List.of("com.example.app")));

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

        List<ModuleCycle> cycles = finder.find(graph);

        assertThat(new BeanCycleFinder().find(graph)).isEmpty();
        assertThat(cycles).hasSize(1);
        ModuleCycle cycle = cycles.getFirst();
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

        assertThat(finder.find(graph)).isEmpty();
    }

    // Если тип из кода неизвестен (точка внедрения не найдена), берётся класс внедренного бина
    @Test
    void withoutDeclaredTypeClassOfInjectedBeanIsUsed() {
        BeanGraph graph = shippingGraph(null);

        assertThat(finder.find(graph))
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

        assertThat(finder.find(graph)).isEmpty();
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

        assertThat(finder.find(graph)).isEmpty();
    }
}
