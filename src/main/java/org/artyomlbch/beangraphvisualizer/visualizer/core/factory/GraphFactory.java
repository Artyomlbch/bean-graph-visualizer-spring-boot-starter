package org.artyomlbch.beangraphvisualizer.visualizer.core.factory;

import org.artyomlbch.beangraphvisualizer.visualizer.core.repository.BeanMetadataRepository;
import org.artyomlbch.beangraphvisualizer.visualizer.model.*;
import org.artyomlbch.beangraphvisualizer.visualizer.model.filter.Stereotype;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.lang.reflect.*;
import java.util.*;

public class GraphFactory {

    private final BeanMetadataRepository beanRepository;
    private final List<String> userPackages;

    public GraphFactory(BeanMetadataRepository beanRepository, ApplicationContext context) {
        this.beanRepository = beanRepository;
        this.userPackages = AutoConfigurationPackages.has(context)
                ? AutoConfigurationPackages.get(context)
                : List.of();
    }

    public BeanGraph newInstance() {
        String[] beanNames = beanRepository.getBeanDefinitionNames();

        Map<String, BeanNode> nodeCache = new LinkedHashMap<>();
        for (String name : beanNames) {
            nodeCache.put(name, createNode(name, beanRepository));
        }

        List<BeanEdge> edges = new ArrayList<>();
        for (String beanName : beanNames) {
            String[] dependencies = beanRepository.getDependenciesForBean(beanName);
            if (dependencies.length == 0) continue;

            BeanNode currentNode = nodeCache.get(beanName);
            Class<?> beanClass = beanRepository.getBeanClass(beanName);
            for (String depName : dependencies) {
                if (beanName.equals(depName)) continue;

                // Зависимость может быть объектом, который Spring зарегистрировал
                // без определения бина (например, environment). Таких узлов в графе нет
                BeanNode depNode = nodeCache.get(depName);
                if (depNode == null) continue;

                Class<?> depClass = beanRepository.getBeanClass(depName);
                InjectionType type = determineInjectionType(beanClass, depClass, beanRepository);
                edges.add(new BeanEdge(currentNode, depNode, type));
            }
        }

        return BeanGraph.of(nodeCache.values(), edges);
    }

    private BeanNode createNode(String beanName, BeanMetadataRepository beanRepository) {
        Class<?> beanClass = null;
        try {
            beanClass = beanRepository.getBeanClass(beanName);
        } catch (Exception ignored) {
        }

        String className = (beanClass != null) ? beanClass.getName() : "Unknown";
        BeanScope scope = BeanScope.UNKNOWN;
        Stereotype stereotype = Stereotype.UNKNOWN;
        int role = BeanDefinition.ROLE_APPLICATION;

        if (beanClass != null) {
            stereotype = detectStereotype(beanClass);
        }

        try {
            BeanDefinition bd = beanRepository.getBeanDefinition(beanName);
            role = bd.getRole();

            if (bd.isSingleton()) scope = BeanScope.SINGLETON;
            else if (bd.isPrototype()) scope = BeanScope.PROTOTYPE;
        } catch (Exception e) {
            scope = BeanScope.SINGLETON;
        }

        boolean isSystem = isSystemBean(className, role);
        return new BeanNode(beanName, className, scope, isSystem, stereotype);
    }

    // Поиск стереотипных аннотаций с учетом вложенных аннотаций
    private static Stereotype detectStereotype(Class<?> beanClass) {
        if (AnnotatedElementUtils.hasAnnotation(beanClass, Controller.class)) return Stereotype.CONTROLLER;
        if (AnnotatedElementUtils.hasAnnotation(beanClass, Service.class)) return Stereotype.SERVICE;
        if (AnnotatedElementUtils.hasAnnotation(beanClass, Repository.class)) return Stereotype.REPOSITORY;
        if (AnnotatedElementUtils.hasAnnotation(beanClass, Configuration.class)) return Stereotype.CONFIGURATION;
        return Stereotype.UNKNOWN;
    }

    private boolean isSystemBean(String className, int role) {
        if (className == null || "Unknown".equals(className)) return true;
        if (role == BeanDefinition.ROLE_INFRASTRUCTURE || role == BeanDefinition.ROLE_SUPPORT) return true;

        return !belongsToPackages(className, userPackages);
    }

    /**
     * Определяет, лежит ла класс в одном из пакетов или подпакетах
     * Без модификатора доступа, чтобы его мог использовать тест,
     * у которого пакет тоже core.factory, сам стартер его видеть не будет
     * TODO: может, стоит убрать метод отсюда
     */
    static boolean belongsToPackages(String className, List<String> packages) {
        return packages.stream()
                .anyMatch(pkg -> pkg.isEmpty()
                        || className.startsWith(pkg + ".")
                );
    }

    private InjectionType determineInjectionType(Class<?> beanClass, Class<?> depClass, BeanMetadataRepository beanRepository) {
        try {
            if (beanClass == null || depClass == null) return InjectionType.UNKNOWN;

            for (Constructor<?> constructor : beanClass.getDeclaredConstructors()) {
                if (isMatch(constructor.getGenericParameterTypes(), depClass)) return InjectionType.CONSTRUCTOR;
            }

            for (Method method : beanClass.getDeclaredMethods()) {
                if (method.getName().startsWith("set") && method.getParameterCount() == 1) {
                    if (isMatch(method.getGenericParameterTypes(), depClass)) {
                        return InjectionType.SETTER;
                    }
                }
            }

            for (Field field : beanClass.getDeclaredFields()) {
                if (isMatch(new Type[]{field.getGenericType()}, depClass)) return InjectionType.FIELD;
            }
        } catch (Exception ignored) {}
        return InjectionType.UNKNOWN;
    }

    private boolean isMatch(Type[] types, Class<?> targetClass) {
        for (Type type : types) {
            if (type instanceof Class<?> clazz && clazz.isAssignableFrom(targetClass)) return true;
            if (type instanceof ParameterizedType pt) {
                for (Type arg : pt.getActualTypeArguments()) {
                    if (arg instanceof Class<?> clazz && clazz.isAssignableFrom(targetClass)) return true;
                }
            }
        }
        return false;
    }
}
