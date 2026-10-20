package org.artyomlbch.beangraphvisualizer.visualizer;

import org.artyomlbch.beangraphvisualizer.testapp.layered.LayeredTestApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest(classes = LayeredTestApplication.class, properties = "ioc-visualizer.enabled=true")
class UiResourcesTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void visNetworkIsServedLocally() throws Exception {
        mvc.perform(get("/ioc-visualizer/vis-network.min.js"))
                .andExpect(status().isOk());
    }

    @Test
    void indexDoesNotLoadScriptsFromCdn() throws Exception {
        mvc.perform(get("/ioc-visualizer/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("unpkg.com"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/ioc-visualizer", "/ioc-visualizer/"})
    void shortUrlRedirectsToIndex(String url) throws Exception {
        mvc.perform(get(url))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/ioc-visualizer/index.html"));
    }

    @Test
    void appScriptCallsApiByRelativeUrl() throws Exception {
        mvc.perform(get("/ioc-visualizer/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("'../api/ioc-visualizer/graph'")));
    }

    @Test
    void uiFilesAreRevalidatedByBrowser() throws Exception {
        mvc.perform(get("/ioc-visualizer/app.js"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-cache"));
    }
}