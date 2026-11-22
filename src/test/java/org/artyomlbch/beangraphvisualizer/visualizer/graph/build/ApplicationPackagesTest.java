package org.artyomlbch.beangraphvisualizer.visualizer.graph.build;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationPackagesTest {

    private final ApplicationPackages app = new ApplicationPackages(List.of("com.example.app"));

    // contains: лежит ли класс в приложении

    @Test
    void classInPackageIsContained() {
        assertThat(app.contains("com.example.app.Application")).isTrue();
    }

    @Test
    void classInSubpackageIsContained() {
        assertThat(app.contains("com.example.app.orders.OrderRepository")).isTrue();
    }

    // раньше сравнение шло без точки, и com.example.application считался частью com.example.app
    @Test
    void packageWithSamePrefixIsNotContained() {
        assertThat(app.contains("com.example.application.Util")).isFalse();
    }

    @Test
    void defaultPackageContainsEverything() {
        assertThat(new ApplicationPackages(List.of("")).contains("com.foobar.Util")).isTrue();
    }

    @Test
    void withoutPackagesNothingIsContained() {
        assertThat(new ApplicationPackages(List.of()).contains("com.example.app.Application")).isFalse();
    }

    // moduleOf: к какому модулю относится класс

    @Test
    void classInSubpackageBelongsToModule() {
        assertThat(app.moduleOf("com.example.app.orders.OrderService")).isEqualTo("com.example.app.orders");
    }

    @Test
    void deeperSubpackageBelongsToSameModule() {
        assertThat(app.moduleOf("com.example.app.orders.internal.OrderRepository"))
                .isEqualTo("com.example.app.orders");
    }

    @Test
    void nestedClassBelongsToModuleOfOuterClass() {
        assertThat(app.moduleOf("com.example.app.orders.OrderService$Item")).isEqualTo("com.example.app.orders");
    }

    @Test
    void classInBasePackageHasNoModule() {
        assertThat(app.moduleOf("com.example.app.Application")).isNull();
    }

    // com.example.application только начинается с тех же букв, это другой пакет
    @Test
    void classOutsideApplicationHasNoModule() {
        assertThat(app.moduleOf("org.springframework.web.client.RestTemplate")).isNull();
        assertThat(app.moduleOf("com.example.application.orders.OrderService")).isNull();
    }

    @Test
    void longestBasePackageWins() {
        ApplicationPackages nested = new ApplicationPackages(List.of("com.example", "com.example.app"));

        assertThat(nested.moduleOf("com.example.app.orders.OrderService")).isEqualTo("com.example.app.orders");
        assertThat(nested.moduleOf("com.example.app.Application")).isNull();
    }

    @Test
    void applicationInDefaultPackage() {
        ApplicationPackages defaultPackage = new ApplicationPackages(List.of(""));

        assertThat(defaultPackage.moduleOf("orders.OrderService")).isEqualTo("orders");
        assertThat(defaultPackage.moduleOf("Application")).isNull();
    }
}
