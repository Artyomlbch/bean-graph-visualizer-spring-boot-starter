package org.artyomlbch.beangraphvisualizer.visualizer.analysis;

import org.artyomlbch.beangraphvisualizer.visualizer.analysis.cycle.BeanCycleFinder;
import org.artyomlbch.beangraphvisualizer.visualizer.analysis.cycle.CycleReport;
import org.artyomlbch.beangraphvisualizer.visualizer.analysis.cycle.ModuleCycleFinder;
import org.artyomlbch.beangraphvisualizer.visualizer.graph.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.graph.store.GraphRepository;

// работает по полному графу, фильтры на результат не влияют
//
// граф строится один раз при старте приложения и дальше не меняется, поэтому циклы
// считаются при первом запросе и запоминаются
public class AnalysisService {

    // граф и посчитанный по нему отчет хранятся вместе, чтобы их нельзя было прочитать отдельно
    private record CachedReport(BeanGraph graph, CycleReport report) {
    }

    private final GraphRepository graphRepository;
    private final BeanCycleFinder beanCycleFinder;
    private final ModuleCycleFinder moduleCycleFinder;
    private volatile CachedReport cached;

    public AnalysisService(GraphRepository graphRepository, BeanCycleFinder beanCycleFinder,
                           ModuleCycleFinder moduleCycleFinder) {
        this.graphRepository = graphRepository;
        this.beanCycleFinder = beanCycleFinder;
        this.moduleCycleFinder = moduleCycleFinder;
    }

    public CycleReport findCycles() {
        BeanGraph graph = graphRepository.getGraph();
        CachedReport current = cached;
        if (current == null || current.graph() != graph) {
            current = new CachedReport(graph, analyze(graph));
            cached = current;
        }
        return current.report();
    }

    private CycleReport analyze(BeanGraph graph) {
        return new CycleReport(beanCycleFinder.find(graph), moduleCycleFinder.find(graph));
    }
}
