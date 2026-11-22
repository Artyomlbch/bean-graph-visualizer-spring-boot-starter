package org.artyomlbch.beangraphvisualizer.visualizer.filter;

import org.artyomlbch.beangraphvisualizer.visualizer.graph.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.graph.model.BeanNode;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Оставляет либо только связанные бины, либо только одинокие
 * FilterPipelineFactory ставит его  последним,
 * так как результат зависит от того, какие бины оставили другие фильтры
 */
public class DetailLevelGraphFilter implements Filter {

    private final DetailLevel detailLevel;

    public DetailLevelGraphFilter(DetailLevel detailLevel) {
        this.detailLevel = detailLevel;
    }

    @Override
    public BeanGraph apply(BeanGraph originalGraph) {
        Set<String> soloIds = originalGraph.getSoloNodes().stream()
                .map(BeanNode::id)
                .collect(Collectors.toSet());

        return switch (detailLevel) {
            case CONNECTED_ONLY -> originalGraph.subgraph(node -> !soloIds.contains(node.id()));
            case SOLO_ONLY -> originalGraph.subgraph(node -> soloIds.contains(node.id()));
        };
    }
}
