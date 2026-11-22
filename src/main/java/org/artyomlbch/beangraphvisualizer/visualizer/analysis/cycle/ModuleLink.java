package org.artyomlbch.beangraphvisualizer.visualizer.analysis.cycle;

import org.artyomlbch.beangraphvisualizer.visualizer.graph.model.BeanEdge;

import java.util.List;

// Зависимость модуля from от модуля to
// сколько рёбер её образуют и какие именно
public record ModuleLink(String from, String to, int count, List<BeanEdge> edges) {
}
