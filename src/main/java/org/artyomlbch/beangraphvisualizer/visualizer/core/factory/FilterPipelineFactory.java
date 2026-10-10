package org.artyomlbch.beangraphvisualizer.visualizer.core.factory;

import org.artyomlbch.beangraphvisualizer.visualizer.core.filter.Filter;
import org.artyomlbch.beangraphvisualizer.visualizer.core.filter.impl.*;
import org.artyomlbch.beangraphvisualizer.visualizer.core.filter.pipeline.FilterPipeline;
import org.artyomlbch.beangraphvisualizer.visualizer.model.filter.*;

import java.util.ArrayList;
import java.util.List;

public class FilterPipelineFactory {

    public FilterPipeline newInstance(GraphRequestDto requestDto) {
        List<Filter> filters = new ArrayList<>();

        if (requestDto == null || requestDto.getFilters().isEmpty()) {
            return new FilterPipeline(filters);
        }

        for (FilterDto  dto : requestDto.getFilters()) {
            if (dto.value() == null || dto.value().isBlank()) continue;

            try {
                switch (dto.type()) {
                    case PACKAGE -> filters.add(new PackageGraphFilter(dto.value()));
                    case TYPE -> filters.add(new TypeGraphFilter(GraphType.valueOf(dto.value())));
                    case DETAIL_LEVEL -> filters.add(new DetailLevelGraphFilter(DetailLevel.valueOf(dto.value())));
                    case SCOPE -> filters.add(new BeanScopeGraphFilter(ScopeType.valueOf(dto.value())));
                    case STEREOTYPE -> filters.add(new BeanStereotypeGraphFilter(Stereotype.valueOf(dto.value())));
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Wrong value '" + dto.value() + "' for filter " + dto.type()
                );
            }
        }

        return new FilterPipeline(filters);
    }

}
