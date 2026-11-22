package org.artyomlbch.beangraphvisualizer.visualizer.filter;

import org.artyomlbch.beangraphvisualizer.visualizer.graph.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.graph.store.GraphRepository;

public class GraphService {

    private final GraphRepository graphRepository;
    private final FilterPipelineFactory filterPipelineFactory;

    public GraphService(GraphRepository graphRepository, FilterPipelineFactory filterPipelineFactory) {
        this.graphRepository = graphRepository;
        this.filterPipelineFactory = filterPipelineFactory;
    }

    public BeanGraph getGraph(GraphRequestDto request) {
        FilterPipeline filterPipeline = filterPipelineFactory.newInstance(request);
        return filterPipeline.process(graphRepository.getGraph());
    }
}
