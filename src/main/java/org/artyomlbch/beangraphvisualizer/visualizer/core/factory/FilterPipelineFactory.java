package org.artyomlbch.beangraphvisualizer.visualizer.core.factory;

import org.artyomlbch.beangraphvisualizer.visualizer.core.filter.Filter;
import org.artyomlbch.beangraphvisualizer.visualizer.core.filter.impl.*;
import org.artyomlbch.beangraphvisualizer.visualizer.core.filter.pipeline.FilterPipeline;
import org.artyomlbch.beangraphvisualizer.visualizer.model.filter.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.springframework.util.StringUtils.hasText;

public class FilterPipelineFactory {

    public FilterPipeline newInstance(GraphRequestDto request) {
        if (request == null) {
            return new FilterPipeline(List.of());
        }

        List<Filter> filters = new ArrayList<>();

        if (hasText(request.type())) {
            filters.add(new TypeGraphFilter(parse(GraphType.class, "type", request.type())));
        }
        if (hasText(request.packagePrefix())) {
            filters.add(new PackageGraphFilter(request.packagePrefix().trim()));
        }
        if (hasText(request.scope())) {
            filters.add(new BeanScopeGraphFilter(parse(ScopeType.class, "scope", request.scope())));
        }
        if (hasText(request.stereotype())) {
            filters.add(new BeanStereotypeGraphFilter(parse(Stereotype.class, "stereotype", request.stereotype())));
        }
        if (hasText(request.detailLevel())) {
            filters.add(new DetailLevelGraphFilter(parse(DetailLevel.class, "detailLevel", request.detailLevel())));
        }

        return new FilterPipeline(filters);
    }

    private static <E extends Enum<E>> E parse(Class<E> enumType, String parameter, String value) {
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Wrong value '" + value + "' for parameter " + parameter
                            + ". Allowed values: " + Arrays.toString(enumType.getEnumConstants())
            );
        }
    }
}
