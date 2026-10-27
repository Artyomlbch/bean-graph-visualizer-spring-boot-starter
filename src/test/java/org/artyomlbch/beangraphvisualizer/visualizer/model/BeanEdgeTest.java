package org.artyomlbch.beangraphvisualizer.visualizer.model;

import org.junit.jupiter.api.Test;

import static org.artyomlbch.beangraphvisualizer.visualizer.support.TestGraphs.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class BeanEdgeTest {

    private final BeanNode service = user("service");
    private final BeanNode config = user("config");

    @Test
    void injectionKeepsDeclaredType() {
        BeanEdge edge = BeanEdge.injection(service, config, InjectionType.FIELD, "com.example.app.Config");

        assertThat(edge.declaredType()).isEqualTo("com.example.app.Config");
    }

    @Test
    void injectionWithoutDeclaredTypeIsAllowed() {
        assertThat(BeanEdge.injection(service, config, InjectionType.UNKNOWN).declaredType()).isNull();
    }

    // У служебных связей нет точки внедрения, значит, нет и типа из кода
    @Test
    void serviceEdgeCannotHaveDeclaredType() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new BeanEdge(service, config, null, EdgeKind.DECLARED_IN, "com.example.app.Config"));
    }
}
