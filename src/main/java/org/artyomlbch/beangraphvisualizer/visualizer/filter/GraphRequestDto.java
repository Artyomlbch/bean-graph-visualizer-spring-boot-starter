package org.artyomlbch.beangraphvisualizer.visualizer.filter;

public record GraphRequestDto(String type, String packagePrefix, String scope,
                              String stereotype, String detailLevel) {}
