package org.artyomlbch.beangraphvisualizer.visualizer.core.analysis;

import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public class ModuleResolver {

    private final List<String> applicationPackages;

    public ModuleResolver(List<String> applicationPackages) {
        // если один основной пакет вложен в другой, сначала проверяется более длинный
        this.applicationPackages = applicationPackages.stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
    }

    public @Nullable String moduleOf(String className) {
        for (String base : applicationPackages) {
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
