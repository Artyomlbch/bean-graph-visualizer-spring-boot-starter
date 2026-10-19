package org.artyomlbch.beangraphvisualizer.visualizer.core.serializer.impl;

import org.artyomlbch.beangraphvisualizer.visualizer.core.serializer.Serializer;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;
import tools.jackson.dataformat.xml.XmlMapper;

public class XmlSerializer implements Serializer {

    private final XmlMapper xmlMapper = XmlMapper.builder().build();

    @Override
    public String serialize(BeanGraph graph) {
        return xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(graph);
    }
}

