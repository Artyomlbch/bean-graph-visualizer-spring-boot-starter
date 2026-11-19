package org.artyomlbch.beangraphvisualizer.visualizer.algorithm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public final class StronglyConnectedComponents<T> {

    // вершина -> вершины, в которые из нее ведут ребра
    private final Map<T, ? extends Collection<T>> graph;

    private final Map<T, Integer> index = new HashMap<>();
    private final Map<T, Integer> lowLink = new HashMap<>();

    private final Deque<T> stack = new ArrayDeque<>();
    private final Set<T> onStack = new HashSet<>();

    private final List<Set<T>> components = new ArrayList<>();
    private int counter = 0;

    private StronglyConnectedComponents(Map<T, ? extends Collection<T>> graph) {
        this.graph = graph;
    }

    public static <T> List<Set<T>> find(Map<T, ? extends Collection<T>> graph) {
        StronglyConnectedComponents<T> search = new StronglyConnectedComponents<>(graph);
        for (T vertex : graph.keySet()) {
            if (!search.index.containsKey(vertex)) {
                search.visit(vertex);
            }
        }
        return search.components;
    }

    private void visit(T vertex) {
        index.put(vertex, counter);
        lowLink.put(vertex, counter);

        counter++;

        stack.push(vertex);
        onStack.add(vertex);

        Collection<T> next = graph.get(vertex);
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

        if (lowLink.get(vertex).equals(index.get(vertex))) {
            Set<T> component = new LinkedHashSet<>();

            T member;
            do {
                member = stack.pop();
                onStack.remove(member);
                component.add(member);
            } while (!member.equals(vertex));

            components.add(component);
        }
    }
}
