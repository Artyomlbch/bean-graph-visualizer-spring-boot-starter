package org.artyomlbch.beangraphvisualizer.visualizer.core.resolver;

import org.artyomlbch.beangraphvisualizer.visualizer.core.resolver.EdgeResolver.Match;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.artyomlbch.beangraphvisualizer.visualizer.core.resolver.EdgeResolver.injectionConstructors;
import static org.assertj.core.api.Assertions.assertThat;

// Юнит-тесты правил, по которым edge resolver сравнивает типы
class EdgeResolverTest {

    interface Exporter {
    }

    static class PdfExporter implements Exporter {
    }

    /** Поля разных типов, каждое сравнивается с классом PdfExporter. */
    @SuppressWarnings("unused")
    static class InjectionPoints {
        PdfExporter exact;
        Exporter byInterface;
        List<Exporter> list;
        List<? extends Exporter> wildcardList;
        Exporter[] array;
        Optional<PdfExporter> optional;
        Map<String, Exporter> map;
        String unrelated;
    }

    private static Match matchOf(String fieldName) throws NoSuchFieldException {
        ResolvableType fieldType = ResolvableType.forField(InjectionPoints.class.getDeclaredField(fieldName));
        return EdgeResolver.match(fieldType, PdfExporter.class);
    }

    // Сравнение типов

    @Test
    void sameClassIsExact() throws Exception {
        assertThat(matchOf("exact")).isEqualTo(Match.EXACT);
    }

    @Test
    void interfaceIsAssignable() throws Exception {
        assertThat(matchOf("byInterface")).isEqualTo(Match.ASSIGNABLE);
    }

    @Test
    void listOfInterfaceIsElement() throws Exception {
        assertThat(matchOf("list")).isEqualTo(Match.ELEMENT);
    }

    @Test
    void wildcardListIsElement() throws Exception {
        assertThat(matchOf("wildcardList")).isEqualTo(Match.ELEMENT);
    }

    @Test
    void arrayIsElement() throws Exception {
        assertThat(matchOf("array")).isEqualTo(Match.ELEMENT);
    }

    @Test
    void optionalIsElement() throws Exception {
        assertThat(matchOf("optional")).isEqualTo(Match.ELEMENT);
    }

    /** В Map<String, X> Spring кладёт бины как значения, а ключи — их имена. */
    @Test
    void mapValueIsElement() throws Exception {
        assertThat(matchOf("map")).isEqualTo(Match.ELEMENT);
    }

    @Test
    void unrelatedTypeDoesNotMatch() throws Exception {
        assertThat(matchOf("unrelated")).isEqualTo(Match.NONE);
    }

    // Выбор конструктора

    static class SingleConstructor {
        SingleConstructor(PdfExporter exporter) {
        }
    }

    static class AnnotatedConstructor {
        AnnotatedConstructor() {
        }

        @Autowired
        AnnotatedConstructor(PdfExporter exporter) {
        }
    }

    static class SeveralConstructorsWithoutAnnotation {
        SeveralConstructorsWithoutAnnotation() {
        }

        SeveralConstructorsWithoutAnnotation(PdfExporter exporter) {
        }
    }

    @Test
    void singleConstructorIsUsed() {
        assertThat(injectionConstructors(SingleConstructor.class)).hasSize(1);
    }

    @Test
    void annotatedConstructorIsUsed() {
        assertThat(injectionConstructors(AnnotatedConstructor.class))
                .singleElement()
                .extracting(Constructor::getParameterCount)
                .isEqualTo(1);
    }

    /** Spring возьмёт конструктор без параметров, через конструктор ничего не внедряется. */
    @Test
    void noArgConstructorIsUsedWhenNoneIsAnnotated() {
        assertThat(injectionConstructors(SeveralConstructorsWithoutAnnotation.class)).isEmpty();
    }
}
