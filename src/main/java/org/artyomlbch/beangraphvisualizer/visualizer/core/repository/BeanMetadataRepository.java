package org.artyomlbch.beangraphvisualizer.visualizer.core.repository;

import org.springframework.beans.factory.config.BeanDefinition;

import java.lang.reflect.Method;

public interface BeanMetadataRepository {
    String[] getBeanDefinitionNames();
    String[] getDependenciesForBean(String beanName);
    Class<?> getBeanClass(String beanName);
    BeanDefinition getBeanDefinition(String beanName);

    // возвращает @Bean-метод, которым был создан бин, или null,
    // если бин не был создан через @Bean-метод
    Method getFactoryMethod(String beanName);
}
