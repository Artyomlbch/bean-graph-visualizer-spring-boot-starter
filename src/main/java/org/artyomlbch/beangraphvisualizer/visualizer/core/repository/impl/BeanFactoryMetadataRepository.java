package org.artyomlbch.beangraphvisualizer.visualizer.core.repository.impl;

import org.artyomlbch.beangraphvisualizer.visualizer.core.repository.BeanMetadataRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
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

    // merged bean definition - это схлопнутое определение бина,
    // в котором объединены настройки родительски и дочерних определений
    // возвращает фабричный метод, которым создавался бин (обычно @Bean-метод)
    @Override
    public Method getFactoryMethod(String beanName) {
        if (!factory.containsBeanDefinition(beanName)) {
            return null;
        }

        BeanDefinition merged = factory.getMergedBeanDefinition(beanName);
        return merged instanceof RootBeanDefinition root ? root.getResolvedFactoryMethod() : null;
    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.factory = (ConfigurableListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
    }
}
