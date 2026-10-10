package org.artyomlbch.beangraphvisualizer.visualizer.core.factory;

import org.artyomlbch.beangraphvisualizer.visualizer.core.repository.BeanMetadataRepository;
import org.artyomlbch.beangraphvisualizer.visualizer.model.*;
import org.artyomlbch.beangraphvisualizer.visualizer.model.filter.Stereotype;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.ApplicationContext;

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
        BeanGraph graph = new BeanGraph();
        String[] beanNames = beanRepository.getBeanDefinitionNames();

        Map<String, BeanNode> nodeCache = new HashMap<>();
        for (String name : beanNames) {
            nodeCache.put(name, createNode(name, beanRepository));
        }

        for (String beanName : beanNames) {
            BeanNode currentNode = nodeCache.get(beanName);
            String[] dependencies = beanRepository.getDependenciesForBean(beanName);

            if (dependencies.length == 0) {
                graph.addSoloNode(currentNode);
            } else {
                graph.addNode(currentNode);

                Class<?> beanClass = beanRepository.getBeanClass(beanName);
                for (String depName : dependencies) {
                    if (beanName.equals(depName)) continue;

                    BeanNode depNode = nodeCache.get(depName);
                    if (depNode == null) continue;

                    Class<?> depClass = beanRepository.getBeanClass(depName);
                    InjectionType type = determineInjectionType(beanClass, depClass, beanRepository);
                    graph.addEdge(new BeanEdge(currentNode, depNode, type));
                }
            }
        }
        return graph;
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
            if (beanClass.isAnnotationPresent(org.springframework.web.bind.annotation.RestController.class) ||
                    beanClass.isAnnotationPresent(org.springframework.stereotype.Controller.class)) {
                stereotype = Stereotype.CONTROLLER;
            } else if (beanClass.isAnnotationPresent(org.springframework.stereotype.Service.class)) {
                stereotype = Stereotype.SERVICE;
            } else if (beanClass.isAnnotationPresent(org.springframework.stereotype.Repository.class)) {
                stereotype = Stereotype.REPOSITORY;
            } else if (beanClass.isAnnotationPresent(org.springframework.context.annotation.Configuration.class)) {
                stereotype = Stereotype.CONFIGURATION;
            }
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

    private boolean isSystemBean(String className, int role) {
        if (className == null || "Unknown".equals(className)) return true;
        if (role == BeanDefinition.ROLE_INFRASTRUCTURE || role == BeanDefinition.ROLE_SUPPORT) return true;

        boolean isUserBean = userPackages.stream().anyMatch(className::startsWith);

        return !isUserBean;
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
