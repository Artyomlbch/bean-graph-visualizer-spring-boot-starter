package org.artyomlbch.beangraphvisualizer.visualizer.core.factory;

import org.artyomlbch.beangraphvisualizer.testapp.layered.LayeredTestApplication;
import org.artyomlbch.beangraphvisualizer.visualizer.core.repository.GraphRepository;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanNode;
import org.artyomlbch.beangraphvisualizer.visualizer.model.InjectionType;
import org.artyomlbch.beangraphvisualizer.visualizer.model.filter.Stereotype;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.artyomlbch.beangraphvisualizer.visualizer.support.GraphLookup.allNodes;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.GraphLookup.edge;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.GraphLookup.node;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = LayeredTestApplication.class, properties = "ioc-visualizer.enabled=true")
class GraphFactoryIntegrationTest {

    @Autowired
    private GraphRepository graphRepository;

    private BeanGraph graph() {
        return graphRepository.getGraph();
    }

    @Test
    void userBeansAreNotSystem() {
        for (String id : List.of("orderController", "orderService", "orderRepository", "injectionShowcase")) {
            assertThat(node(graph(), id).isSystem()).as(id).isFalse();
        }
    }

    @Test
    void springBeansAreSystem() {
        List<BeanNode> springNodes = allNodes(graph()).stream()
                .filter(node -> node.fullClassName().startsWith("org.springframework."))
                .toList();

        assertThat(springNodes).isNotEmpty();
        assertThat(springNodes).allMatch(BeanNode::isSystem);
    }

    @Test
    void layersAreConnectedThroughConstructors() {
        assertThat(edge(graph(), "orderController", "orderService").injectionType())
                .isEqualTo(InjectionType.CONSTRUCTOR);
        assertThat(edge(graph(), "orderService", "orderRepository").injectionType())
                .isEqualTo(InjectionType.CONSTRUCTOR);
    }

    @Test
    void stereotypesOfPlainClassesAreDetected() {
        assertThat(node(graph(), "orderController").stereotype()).isEqualTo(Stereotype.CONTROLLER);
        assertThat(node(graph(), "orderService").stereotype()).isEqualTo(Stereotype.SERVICE);
        assertThat(node(graph(), "orderRepository").stereotype()).isEqualTo(Stereotype.REPOSITORY);
    }

    @Test
    void injectionTypesAreDetectedWhenUnambiguous() {
        assertThat(edge(graph(), "injectionShowcase", "ctorDependency").injectionType())
                .isEqualTo(InjectionType.CONSTRUCTOR);
        assertThat(edge(graph(), "injectionShowcase", "fieldDependency").injectionType())
                .isEqualTo(InjectionType.FIELD);
        assertThat(edge(graph(), "injectionShowcase", "setterDependency").injectionType())
                .isEqualTo(InjectionType.SETTER);
    }
}