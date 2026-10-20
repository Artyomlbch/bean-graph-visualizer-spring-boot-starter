package org.artyomlbch.beangraphvisualizer.visualizer.core.factory;

import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.model.filter.GraphRequestDto;
import org.artyomlbch.beangraphvisualizer.visualizer.model.filter.Stereotype;
import org.junit.jupiter.api.Test;

import static org.artyomlbch.beangraphvisualizer.visualizer.support.GraphLookup.allNodes;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.GraphLookup.ids;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.TestGraphs.bean;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.TestGraphs.graph;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.TestGraphs.system;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.TestGraphs.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilterPipelineFactoryTest {

    private final FilterPipelineFactory factory = new FilterPipelineFactory();

    private final BeanGraph graph = graph()
            .with(user("userBean"), system("springBean"), user("other"), user("otherDep"))
            .edge("userBean", "springBean")
            .edge("other", "otherDep")
            .build();

    @Test
    void missingRequestKeepsGraph() {
        assertThat(factory.newInstance(null).process(graph)).isSameAs(graph);
    }

    @Test
    void requestWithoutFiltersKeepsGraph() {
        GraphRequestDto request = new GraphRequestDto(null, null, null, null, null);

        assertThat(factory.newInstance(request).process(graph)).isSameAs(graph);
    }

    @Test
    void blankValueIsSkipped() {
        GraphRequestDto request = new GraphRequestDto("  ", "", null, null, null);

        assertThat(factory.newInstance(request).process(graph)).isSameAs(graph);
    }

    @Test
    void wrongValueIsRejectedWithListOfAllowedValues() {
        GraphRequestDto request = new GraphRequestDto(null, null, "EVERYTHING", null, null);

        assertThatThrownBy(() -> factory.newInstance(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EVERYTHING")
                .hasMessageContaining("scope")
                .hasMessageContaining("SINGLETON_ONLY")
                .hasMessageContaining("PROTOTYPE_ONLY");
    }

    @Test
    void valuesAreCaseInsensitive() {
        GraphRequestDto request = new GraphRequestDto(" user ", null, null, null, null);

        BeanGraph result = factory.newInstance(request).process(graph);

        assertThat(ids(allNodes(result))).containsExactlyInAnyOrder("userBean", "other", "otherDep");
    }

    @Test
    void detailLevelIsAppliedLast() {
        GraphRequestDto request = new GraphRequestDto("USER", null, null, null, "CONNECTED_ONLY");

        BeanGraph result = factory.newInstance(request).process(graph);

        assertThat(ids(allNodes(result))).containsExactly("other", "otherDep");
    }

    @Test
    void packageIsAppliedBeforeStereotype() {
        BeanGraph layered = graph()
                .with(bean("orderController", "com.example.web.OrderController", Stereotype.CONTROLLER),
                        bean("orderService", "com.example.core.OrderService", Stereotype.SERVICE))
                .edge("orderController", "orderService")
                .build();
        GraphRequestDto request = new GraphRequestDto(null, "com.example.core", null, "CONTROLLER", null);

        BeanGraph result = factory.newInstance(request).process(layered);

        assertThat(allNodes(result)).isEmpty();
    }
}
