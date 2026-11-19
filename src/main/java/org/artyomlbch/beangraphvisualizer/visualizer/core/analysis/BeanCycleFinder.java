package org.artyomlbch.beangraphvisualizer.visualizer.core.analysis;

import org.artyomlbch.beangraphvisualizer.visualizer.algorithm.StronglyConnectedComponents;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanEdge;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.model.analysis.BeanCycle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class BeanCycleFinder {

    private static final Comparator<BeanCycle> LARGEST_FIRST =
            Comparator.comparingInt((BeanCycle cycle) -> cycle.beans().size()).reversed()
                    .thenComparing(cycle -> cycle.beans().getFirst());

    // первыми идут самые большие циклы
    public List<BeanCycle> find(BeanGraph graph) {
        Map<String, Set<String>> dependencies = new LinkedHashMap<>();
        for (BeanEdge edge : graph.getEdges()) {
            dependencies.computeIfAbsent(edge.source().id(), id -> new LinkedHashSet<>())
                    .add(edge.target().id());
        }

        List<BeanCycle> cycles = new ArrayList<>();
        for (Set<String> beans : StronglyConnectedComponents.find(dependencies)) {
            // одиночный бин циклом не считается
            if (beans.size() < 2) {
                continue;
            }
            List<BeanEdge> inside = graph.getEdges().stream()
                    .filter(edge -> beans.contains(edge.source().id()) && beans.contains(edge.target().id()))
                    .toList();
            cycles.add(new BeanCycle(beans.stream().sorted().toList(), inside));
        }
        cycles.sort(LARGEST_FIRST);
        return cycles;
    }
}
