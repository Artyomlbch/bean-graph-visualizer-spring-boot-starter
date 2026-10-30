package org.artyomlbch.beangraphvisualizer.visualizer.core.analysis;

import java.util.*;

// поиск циклов в графе при помощи
// алгоритма Тарьяна
// T - тип вершины
public final class CycleDetector<T> {

    // граф, вершина - коллекция вершин, в которые из нее ведут ребра
    private final Map<T, ? extends Collection<T>> graph;

    private final Map<T, Integer> index = new HashMap<>();
    private final Map<T, Integer> lowLink = new HashMap<>();

    private final Deque<T> stack = new ArrayDeque<>();
    private final Set<T> onStack = new HashSet<>();

    private final List<Set<T>> cycles = new ArrayList<>();
    private int counter = 0;

    private CycleDetector(Map<T, ? extends Collection<T>> graph) {
        this.graph = graph;
    }

    // возвращает компоненты сильной связности из >=2 вершин
    public static <T> List<Set<T>> findCycles(Map<T, ? extends Collection<T>> graph) {
        CycleDetector<T> detector = new CycleDetector<>(graph);
        for (T vertex : graph.keySet()) {
            if (!detector.index.containsKey(vertex)) {
                detector.visit(vertex);
            }
        }
        return detector.cycles;
    }

    private void visit(T vertex) {
        index.put(vertex, counter);
        lowLink.put(vertex, counter);

        counter++;

        stack.push(vertex);
        onStack.add(vertex);

        Collection<T> next =  graph.get(vertex);
        if (next != null) {
            for (T target : next) {
                if (!index.containsKey(target)) {
                    visit(target);
                    lowLink.put(vertex, Math.min(lowLink.get(vertex), lowLink.get(target)));
                } else if (onStack.contains(target)) {
                    lowLink.put(vertex, Math.min(lowLink.get(vertex), index.get(target)));
                }
            }
        }

        // определение начала цикла и сбор нод цикла
        if (lowLink.get(vertex).equals(index.get(vertex))) {
            Set<T> component = new LinkedHashSet<>();

            T member;
            do {
                member = stack.pop();
                onStack.remove(member);
                component.add(member);
            } while (!member.equals(vertex));

            if (component.size() > 1) {
                cycles.add(component);
            }
        }
    }
}
