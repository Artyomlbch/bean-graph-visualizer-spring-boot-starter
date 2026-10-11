package org.artyomlbch.beangraphvisualizer.visualizer.core.filter.impl;

import org.artyomlbch.beangraphvisualizer.visualizer.core.filter.Filter;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanNode;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanScope;
import org.artyomlbch.beangraphvisualizer.visualizer.model.filter.ScopeType;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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