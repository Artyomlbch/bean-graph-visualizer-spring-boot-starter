package org.artyomlbch.beangraphvisualizer.visualizer.core.filter.impl;

import org.artyomlbch.beangraphvisualizer.visualizer.core.filter.Filter;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanNode;
import org.artyomlbch.beangraphvisualizer.visualizer.model.filter.Stereotype;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Оставляет бины заданного стереотипа и их прямые зависимости
 * Пример: для CONTROLLER остаются CONTROLLER и SERVICE, которые он использует
 * Если две зависимости бины тоже связаны, это ребро остается
 * */
public class BeanStereotypeGraphFilter implements Filter {

    private final Stereotype stereotype;

    public BeanStereotypeGraphFilter(Stereotype stereotype) {
        this.stereotype = stereotype;
    }

    @Override
    public BeanGraph apply(BeanGraph originalGraph) {
        Set<String> primaryIds = Stream.concat(
                originalGraph.getNodes().stream(),
                originalGraph.getSoloNodes().stream()
        ).filter(node -> node.stereotype() == stereotype).map(BeanNode::id).collect(Collectors.toSet());

        Set<String> dependencyIds = originalGraph.getEdges().stream()
                .filter(edge -> primaryIds.contains(edge.source().id()))
                .map(edge -> edge.target().id())
                .collect(Collectors.toSet());

        return originalGraph.subgraph(node ->
                primaryIds.contains(node.id()) || dependencyIds.contains(node.id()));
    }
}
