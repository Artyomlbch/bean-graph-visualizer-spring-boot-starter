package org.artyomlbch.beangraphvisualizer.visualizer;

import org.artyomlbch.beangraphvisualizer.visualizer.api.AnalysisController;
import org.artyomlbch.beangraphvisualizer.visualizer.api.ApiExceptionHandler;
import org.artyomlbch.beangraphvisualizer.visualizer.api.GraphController;
import org.artyomlbch.beangraphvisualizer.visualizer.core.analysis.CycleAnalyzer;
import org.artyomlbch.beangraphvisualizer.visualizer.core.analysis.ModuleResolver;
import org.artyomlbch.beangraphvisualizer.visualizer.core.factory.FilterPipelineFactory;
import org.artyomlbch.beangraphvisualizer.visualizer.core.factory.GraphFactory;
import org.artyomlbch.beangraphvisualizer.visualizer.core.initializer.GraphInitializer;
import org.artyomlbch.beangraphvisualizer.visualizer.core.repository.BeanMetadataRepository;
import org.artyomlbch.beangraphvisualizer.visualizer.core.repository.GraphRepository;
import org.artyomlbch.beangraphvisualizer.visualizer.core.repository.impl.BeanFactoryMetadataRepository;
import org.artyomlbch.beangraphvisualizer.visualizer.core.repository.impl.InMemoryGraphRepository;
import org.artyomlbch.beangraphvisualizer.visualizer.core.serializer.Serializer;
import org.artyomlbch.beangraphvisualizer.visualizer.core.serializer.impl.JsonSerializer;
import org.artyomlbch.beangraphvisualizer.visualizer.core.serializer.impl.XmlSerializer;
import org.artyomlbch.beangraphvisualizer.visualizer.service.AnalysisService;
import org.artyomlbch.beangraphvisualizer.visualizer.service.GraphService;
import org.artyomlbch.beangraphvisualizer.visualizer.service.SerializerService;
import org.artyomlbch.beangraphvisualizer.visualizer.ui.IocVisualizerUiConfigurer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
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
    GraphFactory iocVisualizerGraphFactory(BeanMetadataRepository metadataRepository,
                                           ApplicationContext context) {
        return new GraphFactory(metadataRepository, context);
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
    CycleAnalyzer iocVisualizerCycleAnalyzer(ApplicationContext context) {
        // модули — подпакеты пакетов приложения, тех же, по которым GraphFactory
        // отличает бины приложения от системных
        List<String> applicationPackages = AutoConfigurationPackages.has(context)
                ? AutoConfigurationPackages.get(context)
                : List.of();
        return new CycleAnalyzer(new ModuleResolver(applicationPackages));
    }

    @Bean
    AnalysisService iocVisualizerAnalysisService(GraphRepository graphRepository,
                                                 CycleAnalyzer cycleAnalyzer) {
        return new AnalysisService(graphRepository, cycleAnalyzer);
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
