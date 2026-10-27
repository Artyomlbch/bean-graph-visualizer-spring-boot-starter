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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = LayeredTestApplication.class, properties = "ioc-visualizer.enabled=true")
class GraphControllerTest {

    private static final String GRAPH_URL = "/api/ioc-visualizer/graph";
    private static final String TEST_APP_PACKAGE = "org.artyomlbch.beangraphvisualizer.testapp.layered";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void returnsGraphAsJsonByDefault() throws Exception {
        mvc.perform(get(GRAPH_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.nodes").isArray())
                .andExpect(jsonPath("$.soloNodes").isArray())
                .andExpect(jsonPath("$.edges").isArray());
    }

    @Test
    void jsonContainsFieldsUsedByUi() throws Exception {
        mvc.perform(get(GRAPH_URL))
                .andExpect(jsonPath("$.nodes[0].id").exists())
                .andExpect(jsonPath("$.nodes[0].fullClassName").exists())
                .andExpect(jsonPath("$.nodes[0].scope").exists())
                .andExpect(jsonPath("$.nodes[0].isSystem").exists())
                .andExpect(jsonPath("$.nodes[0].stereotype").exists())
                .andExpect(jsonPath("$.edges[0].source.id").exists())
                .andExpect(jsonPath("$.edges[0].target.id").exists())
                .andExpect(jsonPath("$.edges[0].kind").exists())
                .andExpect(jsonPath("$.edges[?(@.kind != 'INJECTION')].injectionType", everyItem(nullValue())))
                .andExpect(jsonPath("$.edges[?(@.kind == 'INJECTION')].injectionType").isNotEmpty())
                .andExpect(jsonPath("$.edges[?(@.kind != 'INJECTION')].declaredType", everyItem(nullValue())))
                .andExpect(jsonPath("$.edges[?(@.injectionType == 'CONSTRUCTOR')].declaredType", everyItem(notNullValue())));
    }

    @Test
    void returnsXmlWhenRequested() throws Exception {
        mvc.perform(get(GRAPH_URL).param("format", "xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(startsWith("<")));
    }

    @Test
    void jsonIsLabeledAsJsonEvenForBrowser() throws Exception {
        mvc.perform(get(GRAPH_URL).accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void typeFilterKeepsOnlyUserBeans() throws Exception {
        mvc.perform(get(GRAPH_URL).param("type", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes[*].id", hasItem("orderController")))
                .andExpect(jsonPath("$.nodes[*].isSystem", everyItem(is(false))))
                .andExpect(jsonPath("$.soloNodes[*].isSystem", everyItem(is(false))));
    }

    @Test
    void doesNotAllowRequestsFromOtherSites() throws Exception {
        mvc.perform(get(GRAPH_URL).header("Origin", "https://some.example"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void hidingSoloBeansKeepsEdges() throws Exception {
        mvc.perform(get(GRAPH_URL)
                        .param("type", "USER")
                        .param("detailLevel", "CONNECTED_ONLY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.soloNodes").isEmpty())
                .andExpect(jsonPath("$.edges[*].target.id", hasItem("orderRepository")));
    }

    @Test
    void packagePrefixKeepsBeansFromPackage() throws Exception {
        mvc.perform(get(GRAPH_URL).param("packagePrefix", TEST_APP_PACKAGE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes[*].id", hasItem("orderController")))
                .andExpect(jsonPath("$.nodes[*].fullClassName", everyItem(startsWith(TEST_APP_PACKAGE))))
                .andExpect(jsonPath("$.soloNodes[*].fullClassName", everyItem(startsWith(TEST_APP_PACKAGE))));
    }

    @Test
    void wrongFilterValueIsBadRequest() throws Exception {
        mvc.perform(get(GRAPH_URL).param("scope", "EVERYTHING"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("SINGLETON_ONLY")));
    }

    // Неизвестные параметры (например, опечатка в имени) просто не учитываются
    @Test
    void unknownParameterIsIgnored() throws Exception {
        mvc.perform(get(GRAPH_URL).param("color", "RED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes").isNotEmpty());
    }

    @Test
    void postIsNotAllowed() throws Exception {
        mvc.perform(post(GRAPH_URL))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void unknownFormatIsBadRequest() throws Exception {
        mvc.perform(get(GRAPH_URL).param("format", "yaml"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("yaml")))
                .andExpect(content().string(containsString("Available formats: [json, xml]")));
    }
}