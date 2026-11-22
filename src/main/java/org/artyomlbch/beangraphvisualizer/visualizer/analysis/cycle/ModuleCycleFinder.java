package org.artyomlbch.beangraphvisualizer.visualizer.analysis.cycle;

import org.artyomlbch.beangraphvisualizer.visualizer.algorithm.StronglyConnectedComponents;
import org.artyomlbch.beangraphvisualizer.visualizer.graph.build.ApplicationPackages;
import org.artyomlbch.beangraphvisualizer.visualizer.graph.model.BeanEdge;
import org.artyomlbch.beangraphvisualizer.visualizer.graph.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.graph.model.BeanNode;
import org.artyomlbch.beangraphvisualizer.visualizer.graph.model.EdgeKind;
import org.artyomlbch.beangraphvisualizer.visualizer.graph.model.InjectionType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

// Модуль это первый подпакет пакета приложения (ApplicationPackages).
// Модуль A зависит от модуля B, если код класса из A получает бин, тип которого объявлен в B.
// Считаются только INJECTION, DECLARED_IN и DEPENDS_ON зависимостями кода не являются
public class ModuleCycleFinder {

    private static final Comparator<ModuleCycle> LARGEST_FIRST =
            Comparator.comparingInt((ModuleCycle cycle) -> cycle.modules().size()).reversed()
                    .thenComparing(cycle -> cycle.modules().getFirst());

    private static final Comparator<ModuleLink> WEAKEST_LINK_FIRST =
            Comparator.comparingInt(ModuleLink::count)
                    .thenComparing(ModuleLink::from)
                    .thenComparing(ModuleLink::to);

    private final ApplicationPackages packages;

    public ModuleCycleFinder(ApplicationPackages packages) {
        this.packages = packages;
    }

    // первыми идут самые большие циклы, связи внутри цикла идут от самой слабой
    public List<ModuleCycle> find(BeanGraph graph) {
        Map<String, BeanNode> declaringConfiguration = new HashMap<>();
        for (BeanEdge edge : graph.getEdges()) {
            if (edge.kind() == EdgeKind.DECLARED_IN) {
                declaringConfiguration.put(edge.source().id(), edge.target());
            }
        }

        // модуль -> модуль -> ребра, из которых получилась зависимость
        Map<String, Map<String, List<BeanEdge>>> links = new TreeMap<>();
        for (BeanEdge edge : graph.getEdges()) {
            if (edge.kind() != EdgeKind.INJECTION) {
                continue;
            }

            String from = packages.moduleOf(codeOwner(edge, declaringConfiguration).fullClassName());
            String to = packages.moduleOf(edge.declaredType() != null
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
        for (Set<String> cycleModules : StronglyConnectedComponents.find(dependencies)) {
            // одиночный модуль циклом не считается
            if (cycleModules.size() < 2) {
                continue;
            }
            List<ModuleLink> inside = new ArrayList<>();
            links.forEach((from, targets) -> targets.forEach((to, edges) -> {
                if (cycleModules.contains(from) && cycleModules.contains(to)) {
                    inside.add(new ModuleLink(from, to, edges.size(), List.copyOf(edges)));
                }
            }));
            inside.sort(WEAKEST_LINK_FIRST);
            cycles.add(new ModuleCycle(cycleModules.stream().sorted().toList(), inside));
        }
        cycles.sort(LARGEST_FIRST);
        return cycles;
    }

    // класс, в коде которого написана зависимость. Для параметра @Bean-метода
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
