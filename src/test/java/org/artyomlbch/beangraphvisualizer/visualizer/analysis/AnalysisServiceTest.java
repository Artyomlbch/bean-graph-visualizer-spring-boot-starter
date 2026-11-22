package org.artyomlbch.beangraphvisualizer.visualizer.analysis;

import org.artyomlbch.beangraphvisualizer.visualizer.analysis.cycle.BeanCycle;
import org.artyomlbch.beangraphvisualizer.visualizer.analysis.cycle.BeanCycleFinder;
import org.artyomlbch.beangraphvisualizer.visualizer.analysis.cycle.CycleReport;
import org.artyomlbch.beangraphvisualizer.visualizer.analysis.cycle.ModuleCycleFinder;
import org.artyomlbch.beangraphvisualizer.visualizer.graph.build.ApplicationPackages;
import org.artyomlbch.beangraphvisualizer.visualizer.graph.model.BeanGraph;
import org.artyomlbch.beangraphvisualizer.visualizer.graph.store.InMemoryGraphRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.artyomlbch.beangraphvisualizer.visualizer.support.TestGraphs.graph;
import static org.artyomlbch.beangraphvisualizer.visualizer.support.TestGraphs.user;
import static org.assertj.core.api.Assertions.assertThat;

class AnalysisServiceTest {

    // считает, сколько раз запускался поиск циклов
    private static final class CountingBeanCycleFinder extends BeanCycleFinder {
        int calls;

        @Override
        public List<BeanCycle> find(BeanGraph graph) {
            calls++;
            return super.find(graph);
        }
    }

    private static AnalysisService service(InMemoryGraphRepository repository, BeanCycleFinder finder) {
        return new AnalysisService(repository, finder, new ModuleCycleFinder(new ApplicationPackages(List.of())));
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
        CountingBeanCycleFinder finder = new CountingBeanCycleFinder();
        AnalysisService service = service(repository, finder);

        CycleReport first = service.findCycles();
        CycleReport second = service.findCycles();

        assertThat(finder.calls).isEqualTo(1);
        assertThat(second).isSameAs(first);
        assertThat(first.beanCycles()).hasSize(1);
    }

    @Test
    void analysesAgainWhenTheGraphIsReplaced() {
        InMemoryGraphRepository repository = new InMemoryGraphRepository();
        repository.write(cyclicGraph());
        CountingBeanCycleFinder finder = new CountingBeanCycleFinder();
        AnalysisService service = service(repository, finder);
        service.findCycles();

        repository.write(graph().with(user("a"), user("b")).edge("a", "b").build());
        CycleReport report = service.findCycles();

        assertThat(finder.calls).isEqualTo(2);
        assertThat(report.beanCycles()).isEmpty();
    }
}
