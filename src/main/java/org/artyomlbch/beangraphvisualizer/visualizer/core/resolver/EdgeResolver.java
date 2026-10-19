package org.artyomlbch.beangraphvisualizer.visualizer.core.resolver;

import org.artyomlbch.beangraphvisualizer.visualizer.core.repository.BeanMetadataRepository;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanEdge;
import org.artyomlbch.beangraphvisualizer.visualizer.model.BeanNode;
import org.artyomlbch.beangraphvisualizer.visualizer.model.EdgeKind;
import org.artyomlbch.beangraphvisualizer.visualizer.model.InjectionType;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Определяет, что означает ребро «бин → зависимость».
 *
 * Spring записывает только сам факт зависимости, но не то, как она возникла.
 * Поэтому ответ восстанавливается по определению бина и по его классу:
 * <ol>
 *     <li>зависимость — конфигурация, в которой объявлен @Bean-метод бина → DECLARED_IN;</li>
 *     <li>среди точек внедрения бина (конструктор, @Bean-метод, методы и поля
 *     с @Autowired) есть подходящая → INJECTION с её способом;</li>
 *     <li>зависимость указана в @DependsOn → DEPENDS_ON;</li>
 *     <li>иначе → INJECTION со способом UNKNOWN.</li>
 * </ol>
 */
public class EdgeResolver {

    // насколько тип точки внедрения подходит зависимости
    // идут от худшего совпадения к лучшему
    enum Match {
        // не подходит
        NONE,
        // зависимость является элементом коллекции, массива или Optional
        ELEMENT,
        // точка внедрения является интерфейсом или родителем зависимости
        ASSIGNABLE,
        // полностью совпадает с классом зависимости
        EXACT
    }

    // аннотации, явно указывающие внедрение зависимости
    private static final List<String> INJECTION_ANNOTATIONS = List.of(
            Autowired.class.getName(),
            "jakarta.inject.Inject",
            "jakarta.annotation.Resource"
    );

    private final BeanMetadataRepository repository;

    public EdgeResolver(BeanMetadataRepository repository) {
        this.repository = repository;
    }

    public BeanEdge createEdge(BeanNode bean, BeanNode dependency) {
        BeanDefinition definition = repository.getBeanDefinition(bean.id());

        // является ли зависимость бина его фабрикой, если да,
        // то создается ребро типа DECLARED_IN
        if (dependency.id().equals(definition.getFactoryBeanName())) {
            return BeanEdge.declaredIn(bean, dependency);
        }

        InjectionType injectionType = findInjectionPointSafely(bean.id(), definition, dependency.id());
        if (injectionType != InjectionType.UNKNOWN) {
            return BeanEdge.injection(bean, dependency, injectionType);
        }

        String[] dependsOn = definition.getDependsOn();
        if (dependsOn != null && Arrays.asList(dependsOn).contains(dependency.id())) {
            return BeanEdge.dependsOn(bean, dependency);
        }

        return BeanEdge.injection(bean, dependency, InjectionType.UNKNOWN);
    }

    /**
     * рефлексия может выбросить ошибку, если в сигнатурах методов класса упоминается класс
     * из библиотеки, которой нет в приложении.
     * getDeclaredMethods() бросает NoClassDefFoundError
     * в этом случае просто возвращается неопределенный тип внедрения
     */
    private InjectionType findInjectionPointSafely(String beanName, BeanDefinition definition, String dependencyName) {
        try {
            return findInjectionPoint(beanName, definition, dependencyName);
        } catch (RuntimeException | LinkageError e) {
            return InjectionType.UNKNOWN;
        }
    }

    /**
     * перебирает все точки внедрения бина и выбирает ту, которая подходит лучше всего
     * при совпадении побеждает точка, найденная раньше в порядке
     * конструктор или @Bean-метод -> @Autowired-методы -> поля
     */
    private InjectionType findInjectionPoint(String beanName, BeanDefinition definition, String dependencyName) {
        Class<?> beanClass = repository.getBeanClass(beanName);
        Class<?> dependencyClass = repository.getBeanClass(dependencyName);
        if (beanClass == null || dependencyClass == null) {
            return InjectionType.UNKNOWN;
        }

        BestMatch best = new BestMatch();

        if (definition.getFactoryMethodName() != null) {
            // бин создан в @Bean-методе, зависимости передаются в параметры метода
            Method factoryMethod = repository.getFactoryMethod(beanName);
            if (factoryMethod != null) {
                // перебор параметров и подбор наилучшего совпадения по типу
                for (int i = 0; i < factoryMethod.getParameterCount(); i++) {
                    best.offer(
                            match(ResolvableType
                                    .forMethodParameter(factoryMethod, i), dependencyClass),
                            InjectionType.FACTORY_METHOD
                    );
                }
            }
        } else {
            // перебираем конструкторы
            for (Constructor<?> constructor : injectionConstructors(beanClass)) {
                for (int i = 0; i < constructor.getParameterCount(); i++) {
                    best.offer(
                            match(ResolvableType
                                    .forConstructorParameter(constructor, i, beanClass), dependencyClass),
                            InjectionType.CONSTRUCTOR);
                }
            }
        }

        // сканирование методов
        // doWithMethods и doWithFields обходят и родительские классы
        ReflectionUtils.doWithMethods(beanClass,
                method -> {
                    for (int i = 0; i < method.getParameterCount(); i++) {
                        best.offer(match(ResolvableType.forMethodParameter(method, i, beanClass), dependencyClass),
                                InjectionType.SETTER);
                    }
                },
                method -> !method.isBridge() && !Modifier.isStatic(method.getModifiers()) && isInjectionPoint(method));

        // сканирование полей
        ReflectionUtils.doWithFields(beanClass,
                field -> best.offer(match(ResolvableType.forField(field, beanClass), dependencyClass),
                        InjectionType.FIELD),
                field -> !Modifier.isStatic(field.getModifiers()) && isInjectionPoint(field));

        return best.type;
    }

    // конструкторы, через которые могли быть внедрены зависимости
    // Приоритеты
    // 1. Помеченные @Autowired или @Inject
    // 2. Если помеченных нет, то единственный доступный конструктор
    // 3. Если помеченных нет, но конструкторов больше одного:
    // -  если есть конструктор по умолчанию (без параметров), то возвращается пустой список
    // -  если все конструкторы с параметрами, то возвращаются все эти конструкторы
    static List<Constructor<?>> injectionConstructors(Class<?> beanClass) {
        Constructor<?>[] constructors = beanClass.getDeclaredConstructors();

        List<Constructor<?>> annotated = Arrays.stream(constructors)
                .filter(EdgeResolver::isInjectionPoint)
                .toList();
        if (!annotated.isEmpty()) {
            return annotated;
        }
        if (constructors.length == 1) {
            return List.of(constructors[0]);
        }
        boolean hasNoArgConstructor = Arrays.stream(constructors).anyMatch(c -> c.getParameterCount() == 0);
        return hasNoArgConstructor ? List.of() : List.of(constructors);
    }

    private static boolean isInjectionPoint(AnnotatedElement element) {
        MergedAnnotations annotations = MergedAnnotations.from(element);
        return INJECTION_ANNOTATIONS.stream().anyMatch(annotations::isPresent);
    }

    // насколько тип точки внедрения подходит к классу зависимости
    static Match match(ResolvableType declaredType, Class<?> dependencyClass) {
        Class<?> declared = declaredType.resolve();
        if (declared == null) {
            return Match.NONE;
        }
        if (declared == dependencyClass) {
            return Match.EXACT;
        }
        if (declared.isAssignableFrom(dependencyClass)) {
            return Match.ASSIGNABLE;
        }

        // внутри дженерика либо компонент массива
        ResolvableType elementType = elementType(declaredType);
        Class<?> element = (elementType != null) ? elementType.resolve() : null;
        if (element != null && element.isAssignableFrom(dependencyClass)) {
            return Match.ELEMENT;
        }
        return Match.NONE;
    }

    // тип элемента, если точка внедрения является коллекцией, Optional, Map или ObjectProvider
    private static ResolvableType elementType(ResolvableType type) {
        if (type.isArray()) {
            return type.getComponentType();
        }
        Class<?> raw = type.resolve();
        if (raw == null) {
            return null;
        }
        if (Collection.class.isAssignableFrom(raw)) {
            return type.asCollection().getGeneric(0);
        }
        if (Map.class.isAssignableFrom(raw)) {
            return type.asMap().getGeneric(1);
        }
        if (raw == Optional.class || ObjectFactory.class.isAssignableFrom(raw)) {
            return type.getGeneric(0);
        }
        return null;
    }

    // лучшее совпадение, найденное на данный момент
    private static final class BestMatch {

        private Match match = Match.NONE;
        private InjectionType type = InjectionType.UNKNOWN;

        // точка внедрения запоминается, если она лучше предыдущей (выше по перечислению в Match)
        void offer(Match candidate, InjectionType candidateType) {
            if (candidate.compareTo(match) > 0) {
                match = candidate;
                type = candidateType;
            }
        }
    }
}
