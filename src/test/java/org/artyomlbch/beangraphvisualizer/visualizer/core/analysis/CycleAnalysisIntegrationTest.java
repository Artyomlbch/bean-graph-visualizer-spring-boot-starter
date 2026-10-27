package org.artyomlbch.beangraphvisualizer.visualizer.core.analysis;

import org.artyomlbch.beangraphvisualizer.testapp.cycles.CyclesTestApplication;
import org.artyomlbch.beangraphvisualizer.visualizer.model.analysis.BeanCycle;
import org.artyomlbch.beangraphvisualizer.visualizer.service.AnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CyclesTestApplication.class, properties = {
        "ioc-visualizer.enabled=true",
        // без этого приложение с циклом бинов не запустится
        "spring.main.allow-circular-references=true"
})
class CycleAnalysisIntegrationTest {

    private static final String CYCLES_URL = "/api/ioc-visualizer/analysis/cycles";

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void fieldCycleBetweenBeansIsFound() {
        assertThat(analysisService.findCycles().beanCycles())
                .extracting(BeanCycle::beans)
                .contains(List.of("stockReservations", "stockService"));
    }

    @Test
    void cyclesAreAvailableOverHttp() throws Exception {
        mvc.perform(get(CYCLES_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.beanCycles[*].beans", hasItem(List.of("stockReservations", "stockService"))));
    }

    @Test
    void unknownFormatIsBadRequest() throws Exception {
        mvc.perform(get(CYCLES_URL).param("format", "yaml"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Available formats")));
    }
}
