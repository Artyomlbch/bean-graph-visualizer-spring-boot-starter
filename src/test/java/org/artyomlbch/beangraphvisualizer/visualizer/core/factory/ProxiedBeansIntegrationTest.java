package org.artyomlbch.beangraphvisualizer.visualizer.core.factory;

import org.artyomlbch.beangraphvisualizer.testapp.proxy.AppConfig;
import org.artyomlbch.beangraphvisualizer.testapp.proxy.AsyncNotifier;
import org.artyomlbch.beangraphvisualizer.testapp.proxy.AsyncWorker;
import org.artyomlbch.beangraphvisualizer.testapp.proxy.ProxyTestApplication;
import org.artyomlbch.beangraphvisualizer.visualizer.core.repository.GraphRepository;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.model.InjectionType;
import org.artyomlbch.beangraphvisualizer.visualizer.model.filter.Stereotype;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.artyomlbch.beangraphvisualizer.visualizer.support.GraphLookup.edge;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.GraphLookup.node;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверка графа бинов, которые были заменены на прокси
 * Проверка стереотипов, заданных через вложенные аннотации
 */
@SpringBootTest(classes = ProxyTestApplication.class, properties = "ioc-visualizer.enabled=true")
class ProxiedBeansIntegrationTest {

    @Autowired
    private GraphRepository graphRepository;

    @Autowired
    private ApplicationContext context;

    private BeanGraph graph() {
        return graphRepository.getGraph();
    }

    /**
     * Проверка, что бины действительно были заменены на прокси
     */
    @Test
    void fixtureBeansAreReallyProxied() {
        assertThat(AopUtils.isAopProxy(context.getBean("asyncWorker"))).isTrue();
        assertThat(AopUtils.isAopProxy(context.getBean("asyncNotifier"))).isTrue();
        assertThat(context.getType("appConfig").getName()).contains("$$");
    }

    // Проверка, что в ноде хранится исходное имя класса, а не имя созданного прокси
    @Test
    void proxiedBeansHaveTheirOwnClassName() {
        assertThat(node(graph(), "asyncWorker").fullClassName()).isEqualTo(AsyncWorker.class.getName());
        assertThat(node(graph(), "asyncNotifier").fullClassName()).isEqualTo(AsyncNotifier.class.getName());
        assertThat(node(graph(), "appConfig").fullClassName()).isEqualTo(AppConfig.class.getName());
    }

    @Test
    void proxiedBeansAreUserBeans() {
        assertThat(node(graph(), "asyncWorker").isSystem()).isFalse();
        assertThat(node(graph(), "asyncNotifier").isSystem()).isFalse();
    }

    @Test
    void proxiedBeansKeepTheirStereotype() {
        assertThat(node(graph(), "asyncWorker").stereotype()).isEqualTo(Stereotype.SERVICE);
        assertThat(node(graph(), "asyncNotifier").stereotype()).isEqualTo(Stereotype.SERVICE);
        assertThat(node(graph(), "appConfig").stereotype()).isEqualTo(Stereotype.CONFIGURATION);
    }

    @Test
    void fieldInjectionIsDetectedThroughProxy() {
        assertThat(edge(graph(), "asyncWorker", "auditLog").injectionType())
                .isEqualTo(InjectionType.FIELD);
    }

    /** @SpringBootApplication содержит @Configuration внутри себя. */
    @Test
    void springBootApplicationIsConfiguration() {
        assertThat(node(graph(), "proxyTestApplication").stereotype())
                .isEqualTo(Stereotype.CONFIGURATION);
    }

    /** @UseCase содержит @Service внутри себя. */
    @Test
    void customStereotypeAnnotationIsDetected() {
        assertThat(node(graph(), "placeOrderUseCase").stereotype())
                .isEqualTo(Stereotype.SERVICE);
    }
}
