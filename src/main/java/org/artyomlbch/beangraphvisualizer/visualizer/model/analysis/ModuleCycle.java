package org.artyomlbch.beangraphvisualizer.visualizer.model.analysis;

import java.util.List;


// Цикл между модулями
// модули по алфавиту и связи между ними внутри цикла
// cвязи идут от самой слабой (меньше всего рёбер) к самой сильной
// (обычно, самую слабую связь проще всего разорвать)
public record ModuleCycle(List<String> modules, List<ModuleLink> links) {
}
