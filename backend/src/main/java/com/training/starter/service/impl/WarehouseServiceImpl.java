package com.training.starter.service.impl;

import com.training.starter.dto.request.CreateWarehouseRequest;
import com.training.starter.dto.request.UpdateWarehouseRequest;
import com.training.starter.dto.response.WarehouseResponse;
import com.training.starter.entity.Warehouse;
import com.training.starter.exception.DuplicateResourceException;
import com.training.starter.exception.ResourceNotFoundException;
import com.training.starter.mapper.WarehouseMapper;
import com.training.starter.repository.WarehouseRepository;
import com.training.starter.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseMapper warehouseMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<WarehouseResponse> getAll(Pageable pageable) {
        log.debug("Fetching all warehouses with pagination");
        return warehouseRepository.findAll(pageable).map(warehouseMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseResponse getById(Long id) {
        log.debug("Fetching warehouse by id: {}", id);
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", id));
        return warehouseMapper.toResponse(warehouse);
    }

    @Override
    @Transactional
    public WarehouseResponse create(CreateWarehouseRequest request) {
        log.debug("Creating new warehouse with code: {}", request.code());
        if (warehouseRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Warehouse", "code", request.code());
        }

        Warehouse warehouse = warehouseMapper.toEntity(request);
        warehouse = warehouseRepository.save(warehouse);
        log.info("Successfully created warehouse with id: {}", warehouse.getId());

        return warehouseMapper.toResponse(warehouse);
    }

    @Override
    @Transactional
    public WarehouseResponse update(Long id, UpdateWarehouseRequest request) {
        log.debug("Updating warehouse with id: {}", id);
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", id));

        warehouseMapper.updateEntity(warehouse, request);
        warehouse = warehouseRepository.save(warehouse);
        log.info("Successfully updated warehouse with id: {}", warehouse.getId());

        return warehouseMapper.toResponse(warehouse);
    }
}
