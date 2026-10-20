package org.artyomlbch.beangraphvisualizer.visualizer.core.serializer;

import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanGraph;
import org.springframework.http.MediaType;

public interface Serializer {
    String serialize(BeanGraph graph);
    MediaType mediaType();
}
