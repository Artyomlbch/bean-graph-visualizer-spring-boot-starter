package org.artyomlbch.beangraphvisualizer.visualizer.service;

import org.artyomlbch.beangraphvisualizer.visualizer.core.serializer.Serializer;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SerializerService {

    private final Map<String, Serializer> serializers = new HashMap<>();

    public SerializerService(List<Serializer> serializerList) {
        for (Serializer serializer : serializerList) {
            String className = serializer.getClass().getSimpleName();

            String formatKey = className.toLowerCase().replace("serializer", "");

            this.serializers.put(formatKey, serializer);
        }
    }

    public String serialize(BeanGraph beanGraph, String format) {
        Serializer serializer = serializers.get(format.toLowerCase());
        if (serializer == null) {
            throw new IllegalArgumentException("Unsupported format: " + format);
        }
        return serializer.serialize(beanGraph);
    }
}
