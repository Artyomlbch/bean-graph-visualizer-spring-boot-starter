package org.artyomlbch.beangraphvisualizer.visualizer.serialization;

import org.artyomlbch.beangraphvisualizer.visualizer.graph.model.BeanGraph;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SerializerServiceTest {

    private static final BeanGraph EMPTY_GRAPH = BeanGraph.of(List.of(), List.of());

    @Test
    void formatIsCaseInsensitive() {
        SerializerService service = new SerializerService(List.of(new JsonSerializer()));

        assertThat(service.serialize(EMPTY_GRAPH, "JSON")).startsWith("{");
    }

    @Test
    void mediaTypeMatchesFormat() {
        SerializerService service = new SerializerService(List.of(new JsonSerializer(), new XmlSerializer()));

        assertThat(service.mediaType("JSON")).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(service.mediaType("xml").isCompatibleWith(MediaType.APPLICATION_XML)).isTrue();
    }

    @Test
    void unknownFormatErrorListsAvailableFormatsInOrder() {
        SerializerService service = new SerializerService(List.of(new XmlSerializer(), new JsonSerializer()));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.serialize(EMPTY_GRAPH, "yaml"))
                .withMessage("Unsupported format: yaml. Available formats: [json, xml]");
    }

    @Test
    void withoutXmlModuleOnlyJsonIsAvailable() {
        SerializerService service = new SerializerService(List.of(new JsonSerializer()));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.serialize(EMPTY_GRAPH, "xml"))
                .withMessage("Unsupported format: xml. Available formats: [json]");
    }
}