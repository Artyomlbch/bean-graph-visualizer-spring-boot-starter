package org.artyomlbch.beangraphvisualizer.visualizer.core.factory;

import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.model.filter.FilterDto;
import org.artyomlbch.beangraphvisualizer.visualizer.model.filter.FilterName;
import org.artyomlbch.beangraphvisualizer.visualizer.model.filter.GraphRequestDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.artyomlbch.beangraphvisualizer.visualizer.support.GraphLookup.allNodes;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.GraphLookup.ids;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.TestGraphs.graph;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.TestGraphs.system;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.TestGraphs.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilterPipelineFactoryTest {

    private final FilterPipelineFactory factory = new FilterPipelineFactory();

    // userBean связан только с системным бином
    // other и otherdep между собой
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
        assertThat(factory.newInstance(new GraphRequestDto()).process(graph)).isSameAs(graph);
    }

    @Test
    void blankValueIsSkipped() {
        GraphRequestDto request = request(new FilterDto("  ", FilterName.TYPE));

        assertThat(factory.newInstance(request).process(graph)).isSameAs(graph);
    }

    @Test
    void filterWithoutTypeIsRejected() {
        GraphRequestDto request = request(new FilterDto("USER", null));

        assertThatThrownBy(() -> factory.newInstance(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type is required");
    }

    @Test
    void wrongValueIsRejectedWithListOfAllowedValues() {
        GraphRequestDto request = request(new FilterDto("EVERYTHING", FilterName.SCOPE));

        assertThatThrownBy(() -> factory.newInstance(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EVERYTHING")
                .hasMessageContaining("SINGLETON_ONLY")
                .hasMessageContaining("PROTOTYPE_ONLY");
    }

    /**
     * В запросе «скрыть одинокие» стоит раньше «только пользовательские».
     * После удаления системных бинов userBean остаётся без связей,
     * поэтому «скрыть одинокие» должно сработать уже после этого.
     */
    @Test
    void detailLevelIsAppliedLast() {
        GraphRequestDto request = request(
                new FilterDto("CONNECTED_ONLY", FilterName.DETAIL_LEVEL),
                new FilterDto("USER", FilterName.TYPE));

        BeanGraph result = factory.newInstance(request).process(graph);

        assertThat(ids(allNodes(result))).containsExactly("other", "otherDep");
    }

    private static GraphRequestDto request(FilterDto... filters) {
        return new GraphRequestDto(List.of(filters));
    }
}
