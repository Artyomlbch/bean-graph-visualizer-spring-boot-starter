package org.artyomlbch.beangraphvisualizer.visualizer.core.analysis;

import org.artyomlbch.beangraphvisualizer.testapp.cycles.CyclesTestApplication;
import org.artyomlbch.beangraphvisualizer.visualizer.model.analysis.BeanCycle;
import org.artyomlbch.beangraphvisualizer.visualizer.model.analysis.CycleReport;
import org.artyomlbch.beangraphvisualizer.visualizer.model.analysis.ModuleCycle;
import org.artyomlbch.beangraphvisualizer.visualizer.model.analysis.ModuleLink;
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
import static org.assertj.core.api.Assertions.tuple;
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
    private static final String MODULES = "org.artyomlbch.beangraphvisualizer.testapp.cycles.";

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

    /**
     * orders ⇄ billing — цикл модулей без цикла бинов.
     * shipping в цикл не входит: зависит от своего интерфейса, а не от реализации из orders.
     * inventory тоже: его цикл бинов внутри одного модуля
     */
    @Test
    void ordersAndBillingFormTheOnlyModuleCycle() {
        CycleReport report = analysisService.findCycles();

        List<ModuleCycle> cycles = report.moduleCycles();
        assertThat(cycles)
                .extracting(ModuleCycle::modules)
                .containsExactly(List.of(MODULES + "billing", MODULES + "orders"));
        assertThat(cycles.getFirst().links())
                .extracting(ModuleLink::from, ModuleLink::to, ModuleLink::count)
                .containsExactly(
                        tuple(MODULES + "billing", MODULES + "orders", 1),
                        tuple(MODULES + "orders", MODULES + "billing", 2));
        assertThat(report.beanCycles())
                .allSatisfy(cycle -> assertThat(cycle.beans()).doesNotContain("orderService", "invoiceService"));
    }

    @Test
    void cyclesAreAvailableOverHttp() throws Exception {
        mvc.perform(get(CYCLES_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.beanCycles[*].beans", hasItem(List.of("stockReservations", "stockService"))))
                .andExpect(jsonPath("$.moduleCycles[0].links[0].count").value(1))
                .andExpect(jsonPath("$.moduleCycles[0].links[0].edges[0].source.id").value("invoiceService"));
    }

    @Test
    void unknownFormatIsBadRequest() throws Exception {
        mvc.perform(get(CYCLES_URL).param("format", "yaml"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Available formats")));
    }
}
