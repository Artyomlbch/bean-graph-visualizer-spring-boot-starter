package org.artyomlbch.beangraphvisualizer.visualizer.core.filter.impl;

import org.artyomlbch.beangraphvisualizer.visualizer.core.filter.Filter;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.model.filter.GraphType;

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
