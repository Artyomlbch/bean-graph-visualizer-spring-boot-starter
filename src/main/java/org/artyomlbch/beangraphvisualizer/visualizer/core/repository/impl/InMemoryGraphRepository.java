package org.artyomlbch.beangraphvisualizer.visualizer.core.repository.impl;

import org.artyomlbch.beangraphvisualizer.visualizer.core.repository.GraphRepository;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;

public class InMemoryGraphRepository implements GraphRepository {

    private volatile BeanGraph cachedGraph;

    @Override
    public void write(BeanGraph graph) {
        this.cachedGraph = graph;
    }

    @Override
    public BeanGraph getGraph() {
        if (this.cachedGraph == null) {
            throw new IllegalStateException("BeanGraph is not initialized yet!");
        }
        return this.cachedGraph;
    }

}
