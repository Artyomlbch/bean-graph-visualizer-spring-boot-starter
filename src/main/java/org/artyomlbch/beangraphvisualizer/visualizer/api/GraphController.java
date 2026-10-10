package org.artyomlbch.beangraphvisualizer.visualizer.api;

import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.model.filter.GraphRequestDto;
import org.artyomlbch.beangraphvisualizer.visualizer.service.GraphService;
import org.artyomlbch.beangraphvisualizer.visualizer.service.SerializerService;
import org.springframework.web.bind.annotation.*;

@RestController
public class GraphController {

    private final SerializerService serializerService;
    private final GraphService graphService;

    public GraphController(SerializerService serializerService, GraphService graphService) {
        this.serializerService = serializerService;
        this.graphService = graphService;
    }

    @PostMapping(value = "/api/ioc-visualizer/graph")
    public String getGraph(
            @RequestParam(defaultValue = "json") String format,
            @RequestBody(required = false) GraphRequestDto requestDto
    ) {
        BeanGraph graph = graphService.getGraph(requestDto);
        return serializerService.serialize(graph, format);
    }
}
