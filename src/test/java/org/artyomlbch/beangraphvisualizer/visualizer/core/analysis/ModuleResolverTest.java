package org.artyomlbch.beangraphvisualizer.visualizer.core.analysis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleResolverTest {

    private final ModuleResolver resolver = new ModuleResolver(List.of("com.example.app"));

    @Test
    void classInSubpackageBelongsToModule() {
        assertThat(resolver.moduleOf("com.example.app.orders.OrderService")).isEqualTo("com.example.app.orders");
    }

    @Test
    void deeperSubpackageBelongsToSameModule() {
        assertThat(resolver.moduleOf("com.example.app.orders.internal.OrderRepository"))
                .isEqualTo("com.example.app.orders");
    }

    @Test
    void nestedClassBelongsToModuleOfOuterClass() {
        assertThat(resolver.moduleOf("com.example.app.orders.OrderService$Item")).isEqualTo("com.example.app.orders");
    }

    @Test
    void classInBasePackageHasNoModule() {
        assertThat(resolver.moduleOf("com.example.app.Application")).isNull();
    }

    // com.example.application только начинается с тех же букв, это другой пакет
    @Test
    void classOutsideApplicationHasNoModule() {
        assertThat(resolver.moduleOf("org.springframework.web.client.RestTemplate")).isNull();
        assertThat(resolver.moduleOf("com.example.application.orders.OrderService")).isNull();
    }

    @Test
    void longestBasePackageWins() {
        ModuleResolver nested = new ModuleResolver(List.of("com.example", "com.example.app"));

        assertThat(nested.moduleOf("com.example.app.orders.OrderService")).isEqualTo("com.example.app.orders");
        assertThat(nested.moduleOf("com.example.app.Application")).isNull();
    }

    @Test
    void applicationInDefaultPackage() {
        ModuleResolver defaultPackage = new ModuleResolver(List.of(""));

        assertThat(defaultPackage.moduleOf("orders.OrderService")).isEqualTo("orders");
        assertThat(defaultPackage.moduleOf("Application")).isNull();
    }
}
