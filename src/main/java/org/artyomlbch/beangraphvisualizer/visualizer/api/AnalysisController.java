package org.artyomlbch.beangraphvisualizer.visualizer.api;

import org.artyomlbch.beangraphvisualizer.visualizer.service.AnalysisService;
import org.artyomlbch.beangraphvisualizer.visualizer.service.SerializerService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalysisController {

    private final AnalysisService analysisService;
    private final SerializerService serializerService;

    public AnalysisController(AnalysisService analysisService, SerializerService serializerService) {
        this.analysisService = analysisService;
        this.serializerService = serializerService;
    }

    @GetMapping("/api/ioc-visualizer/analysis/cycles")
    public ResponseEntity<String> getCycles(@RequestParam(defaultValue = "json") String format) {
        MediaType mediaType = serializerService.mediaType(format);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(serializerService.serialize(analysisService.findCycles(), format));
    }
}
