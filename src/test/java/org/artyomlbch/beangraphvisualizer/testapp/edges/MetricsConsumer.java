package org.artyomlbch.beangraphvisualizer.testapp.edges;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// Метод с @Autowired
@Component
public class MetricsConsumer {

    private MetricsRegistry registry;

    @Autowired
    void configure(MetricsRegistry registry) {
        this.registry = registry;
    }
}

@Component
class MetricsRegistry {
}
