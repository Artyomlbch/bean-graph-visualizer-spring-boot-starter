package org.artyomlbch.beangraphvisualizer.visualizer.graph.model;


public record BeanNode(
        String id,
        String fullClassName,
        BeanScope scope,
        Boolean isSystem,
        Stereotype stereotype
) {
}
