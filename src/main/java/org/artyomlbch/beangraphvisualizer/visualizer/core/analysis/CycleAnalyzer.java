package org.artyomlbch.beangraphvisualizer.visualizer.core.analysis;

import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanEdge;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.model.analysis.BeanCycle;
import org.artyomlbch.beangraphvisualizer.visualizer.model.analysis.CycleReport;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// ищет циклы в графе бинов
public class CycleAnalyzer {

    private static final Comparator<BeanCycle> LARGEST_FIRST =
            Comparator.comparingInt((BeanCycle cycle) -> cycle.beans().size()).reversed()
                    .thenComparing(cycle -> cycle.beans().getFirst());

    public CycleReport analyze(BeanGraph graph) {
        return new CycleReport(beanCycles(graph));
    }

    // учитываются все виды ребер
    List<BeanCycle> beanCycles(BeanGraph graph) {
        Map<String, Set<String>> dependencies = new LinkedHashMap<>();
        for (BeanEdge edge : graph.getEdges()) {
            dependencies.computeIfAbsent(edge.source().id(), id -> new LinkedHashSet<>())
                    .add(edge.target().id());
        }

        List<BeanCycle> cycles = new ArrayList<>();
        for (Set<String> beans : CycleDetector.findCycles(dependencies)) {
            List<BeanEdge> inside = graph.getEdges().stream()
                    .filter(edge -> beans.contains(edge.source().id()) && beans.contains(edge.target().id()))
                    .toList();
            cycles.add(new BeanCycle(beans.stream().sorted().toList(), inside));
        }
        cycles.sort(LARGEST_FIRST);
        return cycles;
    }
}
