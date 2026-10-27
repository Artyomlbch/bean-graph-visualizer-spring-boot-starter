package org.artyomlbch.beangraphvisualizer.visualizer.support;

import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanEdge;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanNode;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanScope;
import org.artyomlbch.beangraphvisualizer.visualizer.model.InjectionType;
import org.artyomlbch.beangraphvisualizer.visualizer.model.filter.Stereotype;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Построение графа вручную для тестов (билдер)
 * пример:
 * BeanGraph graph = graph()
 *          .with(user("service"), system("dataSource"))
 *          .edge("service", "dataSource")
 *          .build();
 */
public final class TestGraphs {

    private final Map<String, BeanNode> nodes = new LinkedHashMap<>();
    private final List<BeanEdge> edges = new ArrayList<>();

    private TestGraphs() {}

    public static TestGraphs graph() {
        return new TestGraphs();
    }

    public TestGraphs with(BeanNode... beans) {
        for (BeanNode bean : beans) {
            nodes.put(bean.id(), bean);
        }
        return this;
    }

    public TestGraphs edge(String from, String to) {
        edges.add(BeanEdge.injection(declared(from), declared(to), InjectionType.CONSTRUCTOR));
        return this;
    }

    public TestGraphs injection(String from, String to, InjectionType type, String declaredType) {
        edges.add(BeanEdge.injection(declared(from), declared(to), type, declaredType));
        return this;
    }

    public TestGraphs declaredIn(String bean, String configuration) {
        edges.add(BeanEdge.declaredIn(declared(bean), declared(configuration)));
        return this;
    }

    public TestGraphs dependsOn(String bean, String dependency) {
        edges.add(BeanEdge.dependsOn(declared(bean), declared(dependency)));
        return this;
    }

    public BeanGraph build() {
        return BeanGraph.of(nodes.values(), edges);
    }

    private BeanNode declared(String id) {
        BeanNode node = nodes.get(id);
        if (node == null) {
            throw new IllegalArgumentException("Node " + id + " is not declared in the test graph");
        }
        return node;
    }


    // Пользовательский singleton бин с заданным классом и стереотипом
    public static BeanNode bean(String id, String className, Stereotype stereotype) {
        return new BeanNode(id, className, BeanScope.SINGLETON, false, stereotype);
    }

    // Пользовательский singleton бин без стереотипа
    public static BeanNode user(String id) {
        return bean(id, "com.example.app." + id, Stereotype.UNKNOWN);
    }

    // Пользовательский prototype бин
    public static BeanNode prototype(String id, String className) {
        return new BeanNode(id, className, BeanScope.PROTOTYPE, false, Stereotype.UNKNOWN);
    }

    // Системный бин springframework
    public static BeanNode system(String id) {
        return new BeanNode(id, "org.springframework." + id, BeanScope.SINGLETON, true, Stereotype.UNKNOWN);
    }
}
