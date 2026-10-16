package org.artyomlbch.beangraphvisualizer.visualizer.model;

public record BeanEdge(
        BeanNode source,
        BeanNode target,
        InjectionType injectionType,
        EdgeKind kind
) {

    public BeanEdge(BeanNode source, BeanNode target, InjectionType injectionType) {
        this(source, target, injectionType, EdgeKind.INJECTION);
    }
}
