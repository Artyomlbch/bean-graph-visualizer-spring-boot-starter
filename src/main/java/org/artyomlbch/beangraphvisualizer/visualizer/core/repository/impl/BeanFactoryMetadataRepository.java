package org.artyomlbch.beangraphvisualizer.visualizer.core.repository.impl;

import org.artyomlbch.beangraphvisualizer.visualizer.core.repository.BeanMetadataRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Proxy;

public class BeanFactoryMetadataRepository implements BeanMetadataRepository, ApplicationContextAware {

    private ConfigurableListableBeanFactory factory;

    @Override
    public String[] getBeanDefinitionNames() {
        return factory.getBeanDefinitionNames();
    }

    @Override
    public String[] getDependenciesForBean(String beanName) {
        return factory.getDependenciesForBean(beanName);
    }

    @Override
    public Class<?> getBeanClass(String beanName) {
        Class<?> type = factory.getType(beanName);
        if (type == null) {
            return null;
        }
        // проверка на JDK-прокси
        if (Proxy.isProxyClass(type)) {
            Class<?> declaredType = declaredType(beanName);
            return declaredType != null ? declaredType : type;
        }
        // проверяет имя класса на наличие $$ и возвращает родителя, либо сам класс, если $$
        // отсутствует
        return ClassUtils.getUserClass(type);
    }

    private Class<?> declaredType(String beanName) {
        if (!factory.containsBeanDefinition(beanName)) {
            return null;
        }
        return factory.getMergedBeanDefinition(beanName).getResolvableType().resolve();
    }

    @Override
    public BeanDefinition getBeanDefinition(String beanName) {
        return factory.getBeanDefinition(beanName);
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.factory = (ConfigurableListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
    }
}
