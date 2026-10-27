package org.artyomlbch.beangraphvisualizer.visualizer.core.analysis;

import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanEdge;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanNode;
import org.artyomlbch.beangraphvisualizer.visualizer.model.EdgeKind;
import org.artyomlbch.beangraphvisualizer.visualizer.model.InjectionType;
import org.artyomlbch.beangraphvisualizer.visualizer.model.analysis.BeanCycle;
import org.artyomlbch.beangraphvisualizer.visualizer.model.analysis.CycleReport;
import org.artyomlbch.beangraphvisualizer.visualizer.model.analysis.ModuleCycle;
import org.artyomlbch.beangraphvisualizer.visualizer.model.analysis.ModuleLink;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

// Ищет циклы между бинами и между модулями приложения
public class CycleAnalyzer {

    private static final Comparator<BeanCycle> LARGEST_BEAN_CYCLE_FIRST =
            Comparator.comparingInt((BeanCycle cycle) -> cycle.beans().size()).reversed()
                    .thenComparing(cycle -> cycle.beans().getFirst());

    private static final Comparator<ModuleCycle> LARGEST_MODULE_CYCLE_FIRST =
            Comparator.comparingInt((ModuleCycle cycle) -> cycle.modules().size()).reversed()
                    .thenComparing(cycle -> cycle.modules().getFirst());

    private static final Comparator<ModuleLink> WEAKEST_LINK_FIRST =
            Comparator.comparingInt(ModuleLink::count)
                    .thenComparing(ModuleLink::from)
                    .thenComparing(ModuleLink::to);

    private final ModuleResolver modules;

    public CycleAnalyzer(ModuleResolver modules) {
        this.modules = modules;
    }

    public CycleReport analyze(BeanGraph graph) {
        return new CycleReport(beanCycles(graph), moduleCycles(graph));
    }

    /**
     * Учитываются все виды рёбер: внедрение, @Bean-метод в конфигурации
     * (Spring создаёт конфигурацию раньше бина) и @DependsOn.
     * Все три задают порядок создания бинов
     */
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
        cycles.sort(LARGEST_BEAN_CYCLE_FIRST);
        return cycles;
    }

    /**
     * Зависимость между модулями считается по коду: от модуля класса, в котором написана
     * зависимость, к модулю типа из точки внедрения (declaredType ребра).
     * Поэтому зависимость от интерфейса своего модуля не связывает модуль с модулем реализации.
     * Учитываются только внедрения: @DependsOn и «объявлен в конфигурации» не являются
     * зависимостью кода одного класса от другого
     */
    List<ModuleCycle> moduleCycles(BeanGraph graph) {
        Map<String, BeanNode> declaringConfiguration = new HashMap<>();
        for (BeanEdge edge : graph.getEdges()) {
            if (edge.kind() == EdgeKind.DECLARED_IN) {
                declaringConfiguration.put(edge.source().id(), edge.target());
            }
        }

        // модуль → модуль → рёбра, из которых получилась зависимость
        Map<String, Map<String, List<BeanEdge>>> links = new TreeMap<>();
        for (BeanEdge edge : graph.getEdges()) {
            if (edge.kind() != EdgeKind.INJECTION) {
                continue;
            }

            String from = modules.moduleOf(codeOwner(edge, declaringConfiguration).fullClassName());
            String to = modules.moduleOf(edge.declaredType() != null
                    ? edge.declaredType()
                    : edge.target().fullClassName());
            if (from == null || to == null || from.equals(to)) {
                continue;
            }

            links.computeIfAbsent(from, module -> new TreeMap<>())
                    .computeIfAbsent(to, module -> new ArrayList<>())
                    .add(edge);
        }

        Map<String, Set<String>> dependencies = new LinkedHashMap<>();
        links.forEach((from, targets) -> dependencies.put(from, targets.keySet()));

        List<ModuleCycle> cycles = new ArrayList<>();
        for (Set<String> cycleModules : CycleDetector.findCycles(dependencies)) {
            List<ModuleLink> inside = new ArrayList<>();
            links.forEach((from, targets) -> targets.forEach((to, edges) -> {
                if (cycleModules.contains(from) && cycleModules.contains(to)) {
                    inside.add(new ModuleLink(from, to, edges.size(), List.copyOf(edges)));
                }
            }));
            inside.sort(WEAKEST_LINK_FIRST);
            cycles.add(new ModuleCycle(cycleModules.stream().sorted().toList(), inside));
        }
        cycles.sort(LARGEST_MODULE_CYCLE_FIRST);
        return cycles;
    }

    // Класс, в коде которого написана зависимость. Для параметра @Bean-метода
    // это конфигурация, где объявлен метод, а не класс созданного бина
    private static BeanNode codeOwner(BeanEdge edge, Map<String, BeanNode> declaringConfiguration) {
        if (edge.injectionType() == InjectionType.FACTORY_METHOD) {
            BeanNode configuration = declaringConfiguration.get(edge.source().id());
            if (configuration != null) {
                return configuration;
            }
        }
        return edge.source();
    }
}
