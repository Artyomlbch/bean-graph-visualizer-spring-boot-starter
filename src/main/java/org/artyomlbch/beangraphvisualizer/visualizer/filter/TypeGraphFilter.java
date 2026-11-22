package org.artyomlbch.beangraphvisualizer.visualizer.filter;

import org.artyomlbch.beangraphvisualizer.visualizer.graph.model.BeanGraph;

/**
 * Оставляет либо системные, либо пользовательские бины
 */
public class TypeGraphFilter implements Filter {

    private final GraphType graphType;

    public TypeGraphFilter(GraphType graphType) {
        this.graphType = graphType;
    }

    @Override
    public BeanGraph apply(BeanGraph originalGraph) {
        boolean system = graphType == GraphType.SYSTEM;
        return originalGraph.subgraph(node -> node.isSystem() == system);
    }
}
