package org.artyomlbch.beangraphvisualizer.visualizer.core.filter.impl;

import org.artyomlbch.beangraphvisualizer.visualizer.core.filter.Filter;
import org.artyomlbch.beangraphvisualizer.visualizer.model.*;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Оставляет бины, пакет которых совпадает с заданным
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