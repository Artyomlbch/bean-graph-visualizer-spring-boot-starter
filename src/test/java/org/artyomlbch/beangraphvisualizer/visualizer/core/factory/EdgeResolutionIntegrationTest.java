package org.artyomlbch.beangraphvisualizer.visualizer.core.factory;

import org.artyomlbch.beangraphvisualizer.testapp.edges.EdgesTestApplication;
import org.artyomlbch.beangraphvisualizer.visualizer.core.repository.GraphRepository;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanEdge;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.model.EdgeKind;
import org.artyomlbch.beangraphvisualizer.visualizer.model.InjectionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.artyomlbch.beangraphvisualizer.visualizer.support.GraphLookup.edge;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет, что означает каждое ребро и каким способом сделано внедрение.
 * Каждый случай описан в комментарии к классу фикстуры в пакете testapp.edges.
 */
@SpringBootTest(classes = EdgesTestApplication.class, properties = "ioc-visualizer.enabled=true")
class EdgeResolutionIntegrationTest {

    @Autowired
    private GraphRepository graphRepository;

    private BeanEdge edgeOf(String from, String to) {
        BeanGraph graph = graphRepository.getGraph();
        return edge(graph, from, to);
    }

    private void assertInjection(String from, String to, InjectionType expected) {
        BeanEdge edge = edgeOf(from, to);
        assertThat(edge.kind()).as(from + " -> " + to + " kind").isEqualTo(EdgeKind.INJECTION);
        assertThat(edge.injectionType()).as(from + " -> " + to + " injection").isEqualTo(expected);
    }

    // Лучшая точка внедрения

    @Test
    void exactFieldWinsOverListInConstructor() {
        assertInjection("reportService", "pdfExporter", InjectionType.FIELD);
        assertInjection("reportService", "csvExporter", InjectionType.CONSTRUCTOR);
    }

    @Test
    void exactConstructorParameterWinsOverListInField() {
        assertInjection("archiveService", "pdfExporter", InjectionType.CONSTRUCTOR);
        assertInjection("archiveService", "csvExporter", InjectionType.FIELD);
    }

    // @Bean-методы

    @Test
    void parameterOfBeanMethodIsFactoryMethodInjection() {
        assertInjection("reportGenerator", "invoiceRepository", InjectionType.FACTORY_METHOD);
    }

    @Test
    void configurationThatDeclaresBeanIsNotInjection() {
        BeanEdge edge = edgeOf("reportGenerator", "reportingConfig");

        assertThat(edge.kind()).isEqualTo(EdgeKind.DECLARED_IN);
        assertThat(edge.injectionType()).isNull();
    }

    // Остальные случаи

    @Test
    void fieldInheritedFromParentIsDetected() {
        assertInjection("orderHandler", "auditTrail", InjectionType.FIELD);
    }

    @Test
    void dependsOnIsNotInjection() {
        BeanEdge edge = edgeOf("catalogService", "cacheWarmer");

        assertThat(edge.kind()).isEqualTo(EdgeKind.DEPENDS_ON);
        assertThat(edge.injectionType()).isNull();
    }

    @Test
    void wildcardCollectionIsDetected() {
        assertInjection("notificationHub", "emailChannel", InjectionType.CONSTRUCTOR);
    }

    @Test
    void autowiredMethodWithAnyNameIsDetected() {
        assertInjection("metricsConsumer", "metricsRegistry", InjectionType.SETTER);
    }
}
