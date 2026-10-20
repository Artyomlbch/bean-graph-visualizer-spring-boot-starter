package org.artyomlbch.beangraphvisualizer.visualizer.model.filter;

public record GraphRequestDto(String type, String packagePrefix, String scope,
                              String stereotype, String detailLevel) {}
