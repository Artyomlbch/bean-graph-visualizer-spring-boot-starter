package org.artyomlbch.beangraphvisualizer.visualizer.model;

/**
 * тип ребра бин -> зависимость
 * INJECTION - стандартное внедрение (бин получает другой и использует его)
 * DECLARED_IN - бин создан @Bean-методом этой конфигурации (@Configuration -> бин)
 * DEPENDS_ON - порядок создания задан через @DependsOn (создается после того, что указано
 * внутри @DependsOn)
*/
public enum EdgeKind {
    INJECTION,
    DECLARED_IN,
    DEPENDS_ON
}
