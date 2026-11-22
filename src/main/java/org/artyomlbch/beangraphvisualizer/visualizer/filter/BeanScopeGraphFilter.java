package org.artyomlbch.beangraphvisualizer.visualizer.filter;

import org.artyomlbch.beangraphvisualizer.visualizer.graph.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.graph.model.BeanScope;

/**
 * Оставляет либо только singleton бины, либо prototype
 */
public class BeanScopeGraphFilter implements Filter {

    private final ScopeType scopeType;

    public BeanScopeGraphFilter(ScopeType scopeType) {
        this.scopeType = scopeType;
    }

    @Override
    public BeanGraph apply(BeanGraph originalGraph) {
        BeanScope targetScope = (scopeType == ScopeType.SINGLETON_ONLY)
                ? BeanScope.SINGLETON
                : BeanScope.PROTOTYPE;
        return originalGraph.subgraph(node -> node.scope() == targetScope);
    }
}