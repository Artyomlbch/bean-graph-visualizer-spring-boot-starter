package org.artyomlbch.beangraphvisualizer.visualizer;

import org.artyomlbch.beangraphvisualizer.visualizer.analysis.AnalysisService;
import org.artyomlbch.beangraphvisualizer.visualizer.analysis.cycle.BeanCycleFinder;
import org.artyomlbch.beangraphvisualizer.visualizer.analysis.cycle.ModuleCycleFinder;
import org.artyomlbch.beangraphvisualizer.visualizer.filter.FilterPipelineFactory;
import org.artyomlbch.beangraphvisualizer.visualizer.filter.GraphService;
import org.artyomlbch.beangraphvisualizer.visualizer.graph.build.ApplicationPackages;
import org.artyomlbch.beangraphvisualizer.visualizer.graph.build.BeanFactoryMetadataRepository;
import org.artyomlbch.beangraphvisualizer.visualizer.graph.build.BeanMetadataRepository;
import org.artyomlbch.beangraphvisualizer.visualizer.graph.build.GraphFactory;
import org.artyomlbch.beangraphvisualizer.visualizer.graph.store.GraphInitializer;
import org.artyomlbch.beangraphvisualizer.visualizer.graph.store.GraphRepository;
import org.artyomlbch.beangraphvisualizer.visualizer.graph.store.InMemoryGraphRepository;
import org.artyomlbch.beangraphvisualizer.visualizer.serialization.JsonSerializer;
import org.artyomlbch.beangraphvisualizer.visualizer.serialization.Serializer;
import org.artyomlbch.beangraphvisualizer.visualizer.serialization.SerializerService;
import org.artyomlbch.beangraphvisualizer.visualizer.serialization.XmlSerializer;
import org.artyomlbch.beangraphvisualizer.visualizer.web.AnalysisController;
import org.artyomlbch.beangraphvisualizer.visualizer.web.ApiExceptionHandler;
import org.artyomlbch.beangraphvisualizer.visualizer.web.GraphController;
import org.artyomlbch.beangraphvisualizer.visualizer.web.IocVisualizerUiConfigurer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "ioc-visualizer", name = "enabled", havingValue = "true")
public class IocVisualizerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    BeanMetadataRepository iocVisualizerMetadataRepository() {
        return new BeanFactoryMetadataRepository();
    }

    @Bean
    @ConditionalOnMissingBean
    GraphRepository iocVisualizerGraphRepository() {
        return new InMemoryGraphRepository();
    }

    @Bean
    ApplicationPackages iocVisualizerApplicationPackages(ApplicationContext context) {
        return ApplicationPackages.of(context);
    }

    @Bean
    GraphFactory iocVisualizerGraphFactory(BeanMetadataRepository metadataRepository,
                                           ApplicationPackages applicationPackages) {
        return new GraphFactory(metadataRepository, applicationPackages);
    }

    @Bean
    GraphInitializer iocVisualizerGraphInitializer(GraphFactory graphFactory,
                                                   GraphRepository graphRepository) {
        return new GraphInitializer(graphFactory, graphRepository);
    }

    @Bean
    FilterPipelineFactory iocVisualizerFilterPipelineFactory() {
        return new FilterPipelineFactory();
    }

    @Bean
    GraphService iocVisualizerGraphService(GraphRepository graphRepository,
                                           FilterPipelineFactory filterPipelineFactory) {
        return new GraphService(graphRepository, filterPipelineFactory);
    }

    @Bean
    JsonSerializer iocVisualizerJsonSerializer() {
        return new JsonSerializer();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "tools.jackson.dataformat.xml.XmlMapper")
    static class XmlSerializerConfiguration {
        @Bean
        XmlSerializer iocVisualizerXmlSerializer() {
            return new XmlSerializer();
        }
    }

    @Bean
    SerializerService iocVisualizerSerializerService(List<Serializer> serializers) {
        return new SerializerService(serializers);
    }

    @Bean
    GraphController iocVisualizerGraphController(SerializerService serializerService,
                                                 GraphService graphService) {
        return new GraphController(serializerService, graphService);
    }

    @Bean
    BeanCycleFinder iocVisualizerBeanCycleFinder() {
        return new BeanCycleFinder();
    }

    @Bean
    ModuleCycleFinder iocVisualizerModuleCycleFinder(ApplicationPackages applicationPackages) {
        return new ModuleCycleFinder(applicationPackages);
    }

    @Bean
    AnalysisService iocVisualizerAnalysisService(GraphRepository graphRepository,
                                                 BeanCycleFinder beanCycleFinder,
                                                 ModuleCycleFinder moduleCycleFinder) {
        return new AnalysisService(graphRepository, beanCycleFinder, moduleCycleFinder);
    }

    @Bean
    AnalysisController iocVisualizerAnalysisController(AnalysisService analysisService,
                                                       SerializerService serializerService) {
        return new AnalysisController(analysisService, serializerService);
    }

    @Bean
    ApiExceptionHandler iocVisualizerApiExceptionHandler() {
        return new ApiExceptionHandler();
    }

    @Bean
    IocVisualizerUiConfigurer iocVisualizerUiConfigurer() {
        return new IocVisualizerUiConfigurer();
    }
}
