package org.artyomlbch.beangraphvisualizer.visualizer.core.service;

import org.artyomlbch.beangraphvisualizer.visualizer.core.analysis.CycleAnalyzer;
import org.artyomlbch.beangraphvisualizer.visualizer.core.analysis.ModuleResolver;
import org.artyomlbch.beangraphvisualizer.visualizer.core.repository.impl.InMemoryGraphRepository;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.model.analysis.CycleReport;
import org.artyomlbch.beangraphvisualizer.visualizer.service.AnalysisService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.artyomlbch.beangraphvisualizer.visualizer.support.TestGraphs.graph;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.TestGraphs.user;
import static org.assertj.core.api.Assertions.assertThat;

class AnalysisServiceTest {

    // считает, сколько раз запускался анализ
    private static final class CountingAnalyzer extends CycleAnalyzer {
        int calls;

        CountingAnalyzer() {
            super(new ModuleResolver(List.of()));
        }

        @Override
        public CycleReport analyze(BeanGraph graph) {
            calls++;
            return super.analyze(graph);
        }
    }

    private static BeanGraph cyclicGraph() {
        return graph()
                .with(user("a"), user("b"))
                .edge("a", "b")
                .edge("b", "a")
                .build();
    }

    @Test
    void analysesTheGraphOnlyOnce() {
        InMemoryGraphRepository repository = new InMemoryGraphRepository();
        repository.write(cyclicGraph());
        CountingAnalyzer analyzer = new CountingAnalyzer();
        AnalysisService service = new AnalysisService(repository, analyzer);

        CycleReport first = service.findCycles();
        CycleReport second = service.findCycles();

        assertThat(analyzer.calls).isEqualTo(1);
        assertThat(second).isSameAs(first);
        assertThat(first.beanCycles()).hasSize(1);
    }

    @Test
    void analysesAgainWhenTheGraphIsReplaced() {
        InMemoryGraphRepository repository = new InMemoryGraphRepository();
        repository.write(cyclicGraph());
        CountingAnalyzer analyzer = new CountingAnalyzer();
        AnalysisService service = new AnalysisService(repository, analyzer);
        service.findCycles();

        repository.write(graph().with(user("a"), user("b")).edge("a", "b").build());
        CycleReport report = service.findCycles();

        assertThat(analyzer.calls).isEqualTo(2);
        assertThat(report.beanCycles()).isEmpty();
    }
}
