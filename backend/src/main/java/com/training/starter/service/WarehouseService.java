package com.training.starter.service;

import com.training.starter.dto.request.CreateWarehouseRequest;
import com.training.starter.dto.request.UpdateWarehouseRequest;
import com.training.starter.dto.response.WarehouseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WarehouseService {

    Page<WarehouseResponse> getAll(Pageable pageable);

    WarehouseResponse getById(Long id);

    WarehouseResponse create(CreateWarehouseRequest request);

    WarehouseResponse update(Long id, UpdateWarehouseRequest request);
}
