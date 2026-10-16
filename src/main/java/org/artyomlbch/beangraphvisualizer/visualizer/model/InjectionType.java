package org.artyomlbch.beangraphvisualizer.visualizer.model;

public enum InjectionType {
    CONSTRUCTOR,
    FIELD,
    SETTER,

    // параметр @Bean-метода
    FACTORY_METHOD,
    UNKNOWN
}
