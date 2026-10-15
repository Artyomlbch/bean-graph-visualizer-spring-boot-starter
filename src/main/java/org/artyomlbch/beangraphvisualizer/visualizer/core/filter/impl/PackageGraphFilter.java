package org.artyomlbch.beangraphvisualizer.visualizer.core.filter.impl;

import org.artyomlbch.beangraphvisualizer.visualizer.core.filter.Filter;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;

/**
 * Оставляет бины, перфикс класса которого совпадает с заданным
 */
public class PackageGraphFilter implements Filter {

    private final String packageName;

    public PackageGraphFilter(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public BeanGraph apply(BeanGraph originalGraph) {
        return originalGraph.subgraph(node -> node.fullClassName().startsWith(packageName));
    }
}