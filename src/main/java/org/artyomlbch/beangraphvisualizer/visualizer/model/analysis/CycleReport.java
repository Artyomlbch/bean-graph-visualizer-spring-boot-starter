package org.artyomlbch.beangraphvisualizer.visualizer.model.analysis;

import java.util.List;

// результат поисках циклов, первыми идут самые большие циклы
public record CycleReport(List<BeanCycle> beanCycles, List<ModuleCycle> moduleCycles) {
}

