package org.artyomlbch.beangraphvisualizer.visualizer.core.factory;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет, как GraphFactory решает, что класс лежит в пакете пользователя.
 * Тест в том же пакете, что и GraphFactory, поэтому видит метод без модификатора доступа.
 */
class UserPackagesTest {

    private static final List<String> USER_PACKAGES = List.of("com.foo");

    @Test
    void classInPackageBelongsToIt() {
        assertThat(GraphFactory.belongsToPackages("com.foo.OrderService", USER_PACKAGES)).isTrue();
    }

    @Test
    void classInSubpackageBelongsToIt() {
        assertThat(GraphFactory.belongsToPackages("com.foo.orders.OrderRepository", USER_PACKAGES)).isTrue();
    }

    /** Раньше сравнение шло без точки, и com.foobar считался частью com.foo. */
    @Test
    void packageWithSamePrefixDoesNotBelongToIt() {
        assertThat(GraphFactory.belongsToPackages("com.foobar.Util", USER_PACKAGES)).isFalse();
    }

    @Test
    void defaultPackageContainsEverything() {
        assertThat(GraphFactory.belongsToPackages("com.foobar.Util", List.of(""))).isTrue();
    }

    @Test
    void noUserPackagesMeansNothingBelongs() {
        assertThat(GraphFactory.belongsToPackages("com.foo.OrderService", List.of())).isFalse();
    }
}
