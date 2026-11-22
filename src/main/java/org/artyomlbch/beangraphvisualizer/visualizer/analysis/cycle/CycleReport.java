package org.artyomlbch.beangraphvisualizer.visualizer.analysis.cycle;

import java.util.List;

// результат поисках циклов, первыми идут самые большие циклы
public record CycleReport(List<BeanCycle> beanCycles, List<ModuleCycle> moduleCycles) {
}

