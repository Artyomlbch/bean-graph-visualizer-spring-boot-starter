package org.artyomlbch.beangraphvisualizer.visualizer.core.factory;

import org.artyomlbch.beangraphvisualizer.testapp.springdata.ItemRepository;
import org.artyomlbch.beangraphvisualizer.testapp.springdata.SpringDataTestApplication;
import org.artyomlbch.beangraphvisualizer.visualizer.core.repository.GraphRepository;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.model.InjectionType;
import org.artyomlbch.beangraphvisualizer.visualizer.model.filter.Stereotype;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;

import static org.artyomlbch.beangraphvisualizer.visualizer.support.GraphLookup.edge;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.GraphLookup.node;
import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(classes = SpringDataTestApplication.class, properties = "ioc-visualizer.enabled=true")
class SpringDataRepositoriesIntegrationTest {

    @Autowired
    private GraphRepository graphRepository;

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    private BeanGraph graph() {
        return graphRepository.getGraph();
    }

    // Проверка самой заготовки: бин репозитория действительно создан фабрикой Spring Data
    @Test
    void repositoryBeanIsCreatedByFactoryBean() {
        assertThat(beanFactory.isFactoryBean("itemRepository")).isTrue();
    }

    // В узле интерфейс репозитория, а не класс фабрики и не JDK-прокси
    @Test
    void repositoryNodeHasRepositoryInterfaceAsClass() {
        assertThat(node(graph(), "itemRepository").fullClassName()).isEqualTo(ItemRepository.class.getName());
    }

    @Test
    void repositoryWithoutAnnotationIsRepository() {
        assertThat(node(graph(), "itemRepository").stereotype()).isEqualTo(Stereotype.REPOSITORY);
    }

    @Test
    void repositoryIsUserBean() {
        assertThat(node(graph(), "itemRepository").isSystem()).isFalse();
    }

    @Test
    void serviceGetsRepositoryThroughConstructor() {
        assertThat(edge(graph(), "itemService", "itemRepository").injectionType())
                .isEqualTo(InjectionType.CONSTRUCTOR);
    }
}