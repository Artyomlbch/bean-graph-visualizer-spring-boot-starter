package org.artyomlbch.beangraphvisualizer.testapp.layered;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Зависит от Environment. Spring регистрирует environment как готовый объект,
 * без BeanDefinition, поэтому узла Environment в графе не будет
 */
@Component
public class EnvironmentReader {

    private final Environment environment;

    public EnvironmentReader(Environment environment) {
        this.environment = environment;
    }
}
