package org.artyomlbch.beangraphvisualizer.visualizer.service;

import org.artyomlbch.beangraphvisualizer.visualizer.core.factory.FilterPipelineFactory;
import org.artyomlbch.beangraphvisualizer.visualizer.core.filter.pipeline.FilterPipeline;
import org.artyomlbch.beangraphvisualizer.visualizer.core.repository.GraphRepository;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.model.filter.GraphRequestDto;

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
