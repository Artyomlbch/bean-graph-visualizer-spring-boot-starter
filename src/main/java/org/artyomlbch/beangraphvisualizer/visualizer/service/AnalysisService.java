package org.artyomlbch.beangraphvisualizer.visualizer.service;

import org.artyomlbch.beangraphvisualizer.visualizer.core.analysis.CycleAnalyzer;
import org.artyomlbch.beangraphvisualizer.visualizer.core.repository.GraphRepository;
import org.artyomlbch.beangraphvisualizer.visualizer.model.analysis.CycleReport;

// работает по полному графу
public class AnalysisService {

    private final GraphRepository graphRepository;
    private final CycleAnalyzer cycleAnalyzer;

    public AnalysisService(GraphRepository graphRepository, CycleAnalyzer cycleAnalyzer) {
        this.graphRepository = graphRepository;
        this.cycleAnalyzer = cycleAnalyzer;
    }

    public CycleReport findCycles() {
        return cycleAnalyzer.analyze(graphRepository.getGraph());
    }
}
