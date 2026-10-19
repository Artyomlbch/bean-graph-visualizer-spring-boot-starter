package org.artyomlbch.beangraphvisualizer.visualizer.model;

/**
 * способ внедрения зависимости (имеет смысл только для EdgeKind.INJECTION)
 * у служебных ребер null
 *
 * CONSTRUCTOR - параметр конструктора
 * FIELD - поле @Autowired, @Inject, @Resource
 * SETTER - параметр метода с @Autowired (TODO: name resolving)
 * FACTORY_METHOD - параметр @Bean-метода
 * UNKNOWN - способ внедрения не удалось определить
 */
public enum InjectionType {
    CONSTRUCTOR,
    FIELD,
    SETTER,
    FACTORY_METHOD,
    UNKNOWN
}
