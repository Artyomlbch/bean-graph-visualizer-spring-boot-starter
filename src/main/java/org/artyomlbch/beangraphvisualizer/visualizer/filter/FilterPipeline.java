package org.artyomlbch.beangraphvisualizer.visualizer.filter;

import org.artyomlbch.beangraphvisualizer.visualizer.graph.model.BeanGraph;

import java.util.List;

public class FilterPipeline {
    private final List<Filter> filters;

    public FilterPipeline(List<Filter> filters) {
        this.filters = filters;
    }

    public BeanGraph process(BeanGraph graph) {
        BeanGraph currentGraph = graph;
        for (Filter filter : filters) {
            currentGraph = filter.apply(currentGraph);
        }
        return currentGraph;
    }
}
