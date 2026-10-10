package org.artyomlbch.beangraphvisualizer.visualizer.core.serializer.impl;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.artyomlbch.beangraphvisualizer.visualizer.core.serializer.Serializer;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;

public class XmlSerializer implements Serializer {

    private final XmlMapper xmlMapper;

    public XmlSerializer() {
        this.xmlMapper = new XmlMapper();
    }

    @Override
    public String serialize(BeanGraph graph) {
        try {
            return xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(graph);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize graph to XML", e);
        }
    }
}

