package com.training.starter.mapper;

import com.training.starter.dto.response.StockSummaryResponse;
import com.training.starter.repository.projection.StockSummaryProjection;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StockSummaryMapper {

    StockSummaryResponse toResponse(StockSummaryProjection projection);
}
