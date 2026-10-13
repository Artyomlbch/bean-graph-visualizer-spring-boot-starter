package org.artyomlbch.beangraphvisualizer.visualizer.core.factory;

import org.artyomlbch.beangraphvisualizer.visualizer.core.filter.Filter;
import org.artyomlbch.beangraphvisualizer.visualizer.core.filter.impl.*;
import org.artyomlbch.beangraphvisualizer.visualizer.core.filter.pipeline.FilterPipeline;
import org.artyomlbch.beangraphvisualizer.visualizer.model.filter.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Фабрика конвейера фильтров, собирает фильтрыв соответствии с запросом
 * Некорректный запрос броасет IllegalArgumentException, контроллер возвращает 400.
 */
public class FilterPipelineFactory {

    public FilterPipeline newInstance(GraphRequestDto requestDto) {
        if (requestDto == null || requestDto.getFilters() == null) {
            return new FilterPipeline(List.of());
        }

        List<Filter> filters = new ArrayList<>();

        // Одинокий бин или нет, зависит от того, какие бины остались после
        // предыдущих фильтров, поэтому DETAIL_LEVEL всегда применяется последним
        List<Filter> detailLevelFilters = new ArrayList<>();

        for (FilterDto dto : requestDto.getFilters()) {
            if (dto == null || dto.type() == null) {
                throw new IllegalArgumentException("Filter type is required");
            }
            if (dto.value() == null || dto.value().isBlank()) continue;

            switch (dto.type()) {
                case PACKAGE -> filters.add(new PackageGraphFilter(dto.value().trim()));
                case TYPE -> filters.add(new TypeGraphFilter(parse(GraphType.class, dto)));
                case SCOPE -> filters.add(new BeanScopeGraphFilter(parse(ScopeType.class, dto)));
                case STEREOTYPE -> filters.add(new BeanStereotypeGraphFilter(parse(Stereotype.class, dto)));
                case DETAIL_LEVEL -> detailLevelFilters.add(new DetailLevelGraphFilter(parse(DetailLevel.class, dto)));
            }
        }

        filters.addAll(detailLevelFilters);
        return new FilterPipeline(filters);
    }

    // Возвращает элемент enum, который соответствует записи value в dto
    private static <E extends Enum<E>> E parse(Class<E> enumType, FilterDto dto) {
        try {
            return Enum.valueOf(enumType, dto.value().trim());
        }  catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Wrong value '" + dto.value() +"' for filter" + dto.type()
                    + ". Allowed values: " + Arrays.toString(enumType.getEnumConstants())
            );
        }
    }
}
