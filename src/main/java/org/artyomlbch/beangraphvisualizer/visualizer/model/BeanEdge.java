package org.artyomlbch.beangraphvisualizer.visualizer.model;

import org.jspecify.annotations.Nullable;

/**
 * Ребро графа, бин source зависит от бина target.
 *
 * kind показывает, что это за связь, а injectionType показывает,
 * каким образом была внедрена зависимость, если связь является внедрением
 */
public record BeanEdge(
        BeanNode source,
        BeanNode target,
        @Nullable InjectionType injectionType,
        EdgeKind kind
) {

    public BeanEdge {
        if (kind == EdgeKind.INJECTION && injectionType == null) {
            throw new IllegalArgumentException("Injection edge must have an injection type");
        }
        if (kind != EdgeKind.INJECTION && injectionType != null) {
            throw new IllegalArgumentException(kind + " edge cannot have an injection type");
        }
    }

    public static BeanEdge injection(BeanNode source, BeanNode target, InjectionType type) {
        return new BeanEdge(source, target, type, EdgeKind.INJECTION);
    }

    public static BeanEdge declaredIn(BeanNode bean, BeanNode configuration) {
        return new BeanEdge(bean, configuration, null, EdgeKind.DECLARED_IN);
    }

    public static BeanEdge dependsOn(BeanNode bean, BeanNode dependency) {
        return new BeanEdge(bean, dependency, null, EdgeKind.DEPENDS_ON);
    }
}
