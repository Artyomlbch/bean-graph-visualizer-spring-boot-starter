package org.artyomlbch.beangraphvisualizer.visualizer.model.analysis;

import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanEdge;

import java.util.List;

// цикл между бинами, имена бинов по алфавиту и ребра между ними
// может быть несколько переплетенных бинов, тк это компонента силььной связности
public record BeanCycle(List<String> beans, List<BeanEdge> edges) {
}
