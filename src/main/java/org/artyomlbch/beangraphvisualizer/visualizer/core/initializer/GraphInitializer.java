package org.artyomlbch.beangraphvisualizer.visualizer.core.initializer;

import org.artyomlbch.beangraphvisualizer.visualizer.core.factory.GraphFactory;
import org.artyomlbch.beangraphvisualizer.visualizer.core.repository.GraphRepository;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

public class GraphInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private final GraphFactory graphFactory;
    private final GraphRepository graphRepository;

    public GraphInitializer(GraphFactory graphFactory, GraphRepository graphRepository) {
        this.graphFactory = graphFactory;
        this.graphRepository = graphRepository;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() == null) {
            graphRepository.write(graphFactory.newInstance());
        }
    }
}
