package org.artyomlbch.beangraphvisualizer.visualizer.service;

import org.artyomlbch.beangraphvisualizer.visualizer.core.analysis.CycleAnalyzer;
import org.artyomlbch.beangraphvisualizer.visualizer.core.repository.GraphRepository;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.model.analysis.CycleReport;

// работает по полному графу, фильтры на результат не влияют
//
// граф строится один раз при старте приложения и дальше не меняется, поэтому циклы
// считаются при первом запросе и запоминаются. Отчёт привязан к экземпляру графа:
// если в репозиторий когда-нибудь запишут новый граф, отчёт посчитается заново
public class AnalysisService {

    // граф и посчитанный по нему отчет хранятся вместе, чтобы их нельзя было прочитать отдельно
    private record CachedReport(BeanGraph graph, CycleReport report) {
    }

    private final GraphRepository graphRepository;
    private final CycleAnalyzer cycleAnalyzer;
    private volatile CachedReport cached;

    public AnalysisService(GraphRepository graphRepository, CycleAnalyzer cycleAnalyzer) {
        this.graphRepository = graphRepository;
        this.cycleAnalyzer = cycleAnalyzer;
    }

    public CycleReport findCycles() {
        BeanGraph graph = graphRepository.getGraph();
        CachedReport current = cached;
        // сравнение по ссылке: нужен именно тот же экземпляр графа, а не равный ему
        if (current == null || current.graph() != graph) {
            // два одновременных первых запроса могут посчитать отчёт дважды, это безопасно:
            // анализ не меняет граф и даёт одинаковый результат, останется любой из двух
            current = new CachedReport(graph, cycleAnalyzer.analyze(graph));
            cached = current;
        }
        return current.report();
    }
}
