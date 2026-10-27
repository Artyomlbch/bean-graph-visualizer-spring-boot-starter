package org.artyomlbch.beangraphvisualizer.visualizer.core.serializer.impl;

import org.artyomlbch.beangraphvisualizer.visualizer.core.serializer.Serializer;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class JsonSerializer implements Serializer {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Override
    public String serialize(Object graph) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(graph);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize graph to JSON", e);
        }
    }

    @Override
    public MediaType mediaType() {
        return MediaType.APPLICATION_JSON;
    }
}
