package org.artyomlbch.beangraphvisualizer.visualizer.core.serializer.impl;

import org.artyomlbch.beangraphvisualizer.visualizer.core.serializer.Serializer;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;
import org.springframework.http.MediaType;
import tools.jackson.dataformat.xml.XmlMapper;

import java.nio.charset.StandardCharsets;

public class XmlSerializer implements Serializer {

    private final XmlMapper xmlMapper = XmlMapper.builder().build();

    @Override
    public String serialize(BeanGraph graph) {
        return xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(graph);
    }

    @Override
    public MediaType mediaType() {
        return new MediaType(MediaType.APPLICATION_XML, StandardCharsets.UTF_8);
    }
}

