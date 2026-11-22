package org.artyomlbch.beangraphvisualizer.visualizer.graph.build;

import org.artyomlbch.beangraphvisualizer.visualizer.graph.model.*;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;

import java.util.*;

public class GraphFactory {

    private static final String SPRING_DATA_REPOSITORY = "org.springframework.data.repository.Repository";
    private static final String SPRING_DATA_REPOSITORY_DEFINITION = "org.springframework.data.repository.RepositoryDefinition";

    private final BeanMetadataRepository beanRepository;
    private final EdgeResolver edgeResolver;
    private final ApplicationPackages applicationPackages;

    public GraphFactory(BeanMetadataRepository beanRepository, ApplicationPackages applicationPackages) {
        this.beanRepository = beanRepository;
        this.edgeResolver = new EdgeResolver(beanRepository);
        this.applicationPackages = applicationPackages;
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
            for (String depName : dependencies) {
                if (beanName.equals(depName)) continue;

                // Зависимость может быть объектом, который Spring зарегистрировал
                // без определения бина (например, environment). Таких узлов в графе нет
                BeanNode depNode = nodeCache.get(depName);
                if (depNode == null) continue;

                edges.add(edgeResolver.createEdge(currentNode, depNode));
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

    static Stereotype detectStereotype(Class<?> beanClass) {
        if (AnnotatedElementUtils.hasAnnotation(beanClass, Controller.class)) return Stereotype.CONTROLLER;
        if (AnnotatedElementUtils.hasAnnotation(beanClass, Service.class)) return Stereotype.SERVICE;
        if (AnnotatedElementUtils.hasAnnotation(beanClass, Repository.class)) return Stereotype.REPOSITORY;
        if (AnnotatedElementUtils.hasAnnotation(beanClass, Configuration.class)) return Stereotype.CONFIGURATION;
        if (isSpringDataRepository(beanClass)) return Stereotype.REPOSITORY;
        return Stereotype.UNKNOWN;
    }

    static boolean isSpringDataRepository(Class<?> beanClass) {
        // @RepositoryDefinition на самом типе или на его родителях и интерфейсах
        if (MergedAnnotations.from(beanClass, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
                .isPresent(SPRING_DATA_REPOSITORY_DEFINITION)) return true;
        ClassLoader loader = beanClass.getClassLoader();
        // ClassUtils.isPresent возвращает true, если класс можно загрузить через переданный загрузчик(если подключен Spring Data)
        return ClassUtils.isPresent(SPRING_DATA_REPOSITORY, loader)
                && ClassUtils.resolveClassName(SPRING_DATA_REPOSITORY, loader).isAssignableFrom(beanClass);
    }

    private boolean isSystemBean(String className, int role) {
        if (className == null || "Unknown".equals(className)) return true;
        if (role == BeanDefinition.ROLE_INFRASTRUCTURE || role == BeanDefinition.ROLE_SUPPORT) return true;

        return !applicationPackages.contains(className);
    }
}
