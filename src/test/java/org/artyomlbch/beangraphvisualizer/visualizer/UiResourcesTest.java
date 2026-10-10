package org.artyomlbch.beangraphvisualizer.visualizer;

import org.artyomlbch.beangraphvisualizer.testapp.layered.LayeredTestApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


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
}