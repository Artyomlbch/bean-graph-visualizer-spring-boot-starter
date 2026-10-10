package org.artyomlbch.beangraphvisualizer.visualizer;


import org.artyomlbch.beangraphvisualizer.visualizer.api.GraphController;
import org.artyomlbch.beangraphvisualizer.visualizer.core.repository.GraphRepository;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;

public class IocVisualizerAutoConfigurationTest {

    private final WebApplicationContextRunner webRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(IocVisualizerAutoConfiguration.class));

    @Test
    void isDisableByDefault() {
        webRunner.run(context -> assertThat(context)
                .doesNotHaveBean(GraphController.class)
        );
    }

    @Test
    void isEnabledByProperty() {
        webRunner.withPropertyValues("ioc-visualizer.enabled=true")
                .run(context -> assertThat(context)
                        .hasSingleBean(GraphController.class)
                );
    }

    @Test
    void isNotActiveInNonWebApplication() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(IocVisualizerAutoConfigurationTest.class))
                .withPropertyValues("ioc-visualizer.enabled=true")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(GraphController.class)
                );
    }

    @Test
    void doesNotConflictWithUserBeanNames() {
        webRunner.withPropertyValues("ioc-visualizer.enabled=true")
                .withBean("graphService", String.class, () -> "user bean")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("graphService");
                    assertThat(context).hasSingleBean(GraphController.class);
                });
    }

    @Test
    void userCanReplaceGraphRepository() {
        GraphRepository custom = new GraphRepository() {
            private BeanGraph graph;

            @Override
            public void write(BeanGraph graph) {
                this.graph = graph;
            }

            @Override
            public BeanGraph getGraph() {
                return graph;
            }
        };

        webRunner.withPropertyValues("ioc-visualizer.enabled=true")
                .withBean(GraphRepository.class, () -> custom)
                .run(context -> assertThat(context.getBean(GraphRepository.class)).isSameAs(custom));
    }
}
