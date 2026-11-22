package org.artyomlbch.beangraphvisualizer.visualizer.graph.store;

import org.artyomlbch.beangraphvisualizer.visualizer.graph.build.GraphFactory;
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
