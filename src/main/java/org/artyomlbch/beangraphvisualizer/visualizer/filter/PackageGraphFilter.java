package org.artyomlbch.beangraphvisualizer.visualizer.filter;

import org.artyomlbch.beangraphvisualizer.visualizer.graph.model.BeanGraph;

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