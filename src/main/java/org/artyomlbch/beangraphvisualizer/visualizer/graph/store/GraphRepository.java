package org.artyomlbch.beangraphvisualizer.visualizer.graph.store;

import org.artyomlbch.beangraphvisualizer.visualizer.graph.model.BeanGraph;

public interface GraphRepository {
    void write(BeanGraph graph);
    BeanGraph getGraph();
}
