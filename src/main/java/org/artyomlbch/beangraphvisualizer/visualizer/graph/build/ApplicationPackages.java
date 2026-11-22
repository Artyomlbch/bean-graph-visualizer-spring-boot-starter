package org.artyomlbch.beangraphvisualizer.visualizer.graph.build;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;

import java.util.Comparator;
import java.util.List;

// Пакеты приложения
// пакет класса с @SpringBootApplication и другие пакеты из @AutoConfigurationPackage
// говорит, лежит ли класс в приложении и к какому модулю он относится.
// Модуль - это первый подпакет пакета приложени, для com.example.app.orders.internal.OrderRepository
// это com.example.app.orders
public final class ApplicationPackages {

    private final List<String> packages;

    // если один пакет приложения вложен в другой, сначала проверяется более длинный
    private final List<String> longestFirstPackages;

    public ApplicationPackages(List<String> packages) {
        this.packages = List.copyOf(packages);
        this.longestFirstPackages = this.packages.stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
    }

    // Пакеты, которые Spring Boot запомнил при старте. В контексте без Spring Boot их нет,
    // и тогда ни один класс не считается классом приложения
    public static ApplicationPackages of(BeanFactory beanFactory) {
        return new ApplicationPackages(AutoConfigurationPackages.has(beanFactory)
                ? AutoConfigurationPackages.get(beanFactory)
                : List.of());
    }

    public List<String> packages() {
        return packages;
    }

    // Лежит ли класс в одном из пакетов приложения или в их подпакетах.
    // Пустой пакет (приложение без package) содержит все классы
    public boolean contains(String className) {
        return packages.stream()
                .anyMatch(pkg -> pkg.isEmpty() || className.startsWith(pkg + "."));
    }

    // Модуль класса или null, если класс лежит вне приложения или прямо в пакете приложения
    public @Nullable String moduleOf(String className) {
        for (String base : longestFirstPackages) {
            String prefix = base.isEmpty() ? "" : base + ".";
            if (!className.startsWith(prefix)) {
                continue;
            }

            String rest = className.substring(prefix.length());
            int dot = rest.indexOf('.');
            // если точки нет, то класс лежит прямо в основном пакете
            return (dot < 0) ? null : prefix + rest.substring(0, dot);
        }
        return null;
    }
}
