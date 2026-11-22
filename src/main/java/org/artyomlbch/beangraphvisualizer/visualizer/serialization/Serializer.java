package org.artyomlbch.beangraphvisualizer.visualizer.serialization;

import org.springframework.http.MediaType;

public interface Serializer {
    String serialize(Object graph);
    MediaType mediaType();
}
