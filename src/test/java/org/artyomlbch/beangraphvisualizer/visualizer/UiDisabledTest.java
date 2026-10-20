package org.artyomlbch.beangraphvisualizer.visualizer;

import org.artyomlbch.beangraphvisualizer.testapp.layered.LayeredTestApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// если свойство ioc-visualizer.enabled не задано
// Spring Boot не должен отдавать интерфейс (для этого он был перенесен из META-INF/resources/ в META-INF/ioc-visualizer/)
// и api должен возвращать 404 при обращении к файлам
@SpringBootTest(classes = LayeredTestApplication.class)
class UiDisabledTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/ioc-visualizer/index.html",
            "/ioc-visualizer/app.js",
            "/ioc-visualizer/vis-network.min.js",
            "/ioc-visualizer/"
    })
    void uiIsNotServed(String url) throws Exception {
        mvc.perform(get(url))
                .andExpect(status().isNotFound());
    }
}
