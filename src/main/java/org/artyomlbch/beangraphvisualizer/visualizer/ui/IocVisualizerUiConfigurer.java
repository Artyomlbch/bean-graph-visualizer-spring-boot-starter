package org.artyomlbch.beangraphvisualizer.visualizer.ui;

import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Раздаёт веб-интерфейс стартера.
 * <p>
 * Файлы лежат в {@code META-INF/ioc-visualizer/}, а не в {@code META-INF/resources/},
 * поэтому Spring Boot не раздаёт их сам. Их раздаёт только этот класс, а он
 * регистрируется в автоконфигурации, то есть только при {@code ioc-visualizer.enabled=true}.
 */
// раздает веб-интерфейс
// файлы лежат в META-INF/ioc-visualizer/
public class IocVisualizerUiConfigurer implements WebMvcConfigurer {

    static final String UI_PATH = "/ioc-visualizer";

    private static final String UI_LOCATION = "classpath:/META-INF/ioc-visualizer/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(UI_PATH + "/**")
                .addResourceLocations(UI_LOCATION);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Spring Boot сам открывает index.html только в корне сайта.
        // Начиная со Spring 6 "/ioc-visualizer" и "/ioc-visualizer/" — разные адреса,
        // поэтому переадресация нужна для обоих.
        registry.addRedirectViewController(UI_PATH, UI_PATH + "/index.html");
        registry.addRedirectViewController(UI_PATH + "/", UI_PATH + "/index.html");
    }
}
