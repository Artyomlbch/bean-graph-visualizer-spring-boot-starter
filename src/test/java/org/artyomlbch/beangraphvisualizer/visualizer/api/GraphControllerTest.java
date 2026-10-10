package org.artyomlbch.beangraphvisualizer.visualizer.api;

import org.artyomlbch.beangraphvisualizer.testapp.layered.LayeredTestApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = LayeredTestApplication.class, properties = "ioc-visualizer.enabled=true")
class GraphControllerTest {

    private static final String GRAPH_URL = "/api/ioc-visualizer/graph";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void returnsGraphAsJsonByDefault() throws Exception {
        mvc.perform(post(GRAPH_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes").isArray())
                .andExpect(jsonPath("$.soloNodes").isArray())
                .andExpect(jsonPath("$.edges").isArray());
    }

    @Test
    void jsonContainsFieldsUsedByUi() throws Exception {
        mvc.perform(post(GRAPH_URL))
                .andExpect(jsonPath("$.nodes[0].id").exists())
                .andExpect(jsonPath("$.nodes[0].fullClassName").exists())
                .andExpect(jsonPath("$.nodes[0].scope").exists())
                .andExpect(jsonPath("$.nodes[0].isSystem").exists())
                .andExpect(jsonPath("$.nodes[0].stereotype").exists())
                .andExpect(jsonPath("$.edges[0].source.id").exists())
                .andExpect(jsonPath("$.edges[0].target.id").exists())
                .andExpect(jsonPath("$.edges[0].injectionType").exists());
    }

    @Test
    void returnsXmlWhenRequested() throws Exception {
        mvc.perform(post(GRAPH_URL).param("format", "xml"))
                .andExpect(status().isOk())
                .andExpect(content().string(startsWith("<")));
    }

    @Test
    void typeFilterKeepsOnlyUserBeans() throws Exception {
        mvc.perform(post(GRAPH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"filters": [{"type": "TYPE", "value": "USER"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes[*].id", hasItem("orderController")))
                .andExpect(jsonPath("$.nodes[*].isSystem", everyItem(is(false))))
                .andExpect(jsonPath("$.soloNodes[*].isSystem", everyItem(is(false))));
    }

    @Test
    void doesNotAllowRequestsFromOtherSites() throws Exception {
        mvc.perform(post(GRAPH_URL).header("Origin", "https://some.example"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}