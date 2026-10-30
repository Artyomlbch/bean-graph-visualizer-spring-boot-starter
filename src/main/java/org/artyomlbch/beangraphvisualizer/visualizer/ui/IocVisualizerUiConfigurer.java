package org.artyomlbch.beangraphvisualizer.visualizer.ui;

import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// отдает веб-интерфецс
// файлы лежат в META-INF/ioc-visualizer/
public class IocVisualizerUiConfigurer implements WebMvcConfigurer {

    static final String UI_PATH = "/ioc-visualizer";

    private static final String UI_LOCATION = "classpath:/META-INF/ioc-visualizer/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(UI_PATH + "/**")
                .addResourceLocations(UI_LOCATION)
                .setCacheControl(CacheControl.noCache());
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController(UI_PATH, UI_PATH + "/index.html");
        registry.addRedirectViewController(UI_PATH + "/", UI_PATH + "/index.html");
    }
}
