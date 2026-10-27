package org.artyomlbch.beangraphvisualizer.visualizer.api;

import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.model.filter.GraphRequestDto;
import org.artyomlbch.beangraphvisualizer.visualizer.service.GraphService;
import org.artyomlbch.beangraphvisualizer.visualizer.service.SerializerService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class GraphController {

    private final SerializerService serializerService;
    private final GraphService graphService;

    public GraphController(SerializerService serializerService, GraphService graphService) {
        this.serializerService = serializerService;
        this.graphService = graphService;
    }

    @GetMapping("/api/ioc-visualizer/graph")
    public ResponseEntity<String> getGraph(
            @RequestParam(defaultValue = "json") String format,
            @ModelAttribute GraphRequestDto request
    ) {
        MediaType mediaType = serializerService.mediaType(format);
        BeanGraph graph = graphService.getGraph(request);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(serializerService.serialize(graph, format));
    }
}

