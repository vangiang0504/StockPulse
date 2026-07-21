package com.training.starter.service;

import com.training.starter.dto.request.CreateWarehouseRequest;
import com.training.starter.dto.request.UpdateWarehouseRequest;
import com.training.starter.dto.response.WarehouseResponse;
import com.training.starter.entity.Warehouse;
import com.training.starter.exception.DuplicateResourceException;
import com.training.starter.exception.ResourceNotFoundException;
import com.training.starter.mapper.WarehouseMapper;
import com.training.starter.repository.WarehouseRepository;
import com.training.starter.service.impl.WarehouseServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private WarehouseMapper warehouseMapper;

    @InjectMocks
    private WarehouseServiceImpl warehouseService;

    @Test
    void create_validRequest_returnsWarehouseResponse() {
        // Given
        var request = new CreateWarehouseRequest("Main Warehouse", "WH-001", "123 Test Street");
        var entity = buildWarehouse(1L, "Main Warehouse", "WH-001");
        var response = buildWarehouseResponse(1L, "Main Warehouse", "WH-001");

        when(warehouseRepository.existsByCode("WH-001")).thenReturn(false);
        when(warehouseMapper.toEntity(request)).thenReturn(entity);
        when(warehouseRepository.save(any(Warehouse.class))).thenReturn(entity);
        when(warehouseMapper.toResponse(entity)).thenReturn(response);

        // When
        var result = warehouseService.create(request);

        // Then
        assertThat(result.code()).isEqualTo("WH-001");
        assertThat(result.name()).isEqualTo("Main Warehouse");
        verify(warehouseRepository).save(any(Warehouse.class));
    }

    @Test
    void create_duplicateCode_throwsDuplicateResourceException() {
        // Given
        var request = new CreateWarehouseRequest("Duplicate Warehouse", "WH-DUP", null);
        when(warehouseRepository.existsByCode("WH-DUP")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> warehouseService.create(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(warehouseRepository, never()).save(any());
    }

    @Test
    void getById_found_returnsWarehouseResponse() {
        // Given
        var entity = buildWarehouse(1L, "Main Warehouse", "WH-001");
        var response = buildWarehouseResponse(1L, "Main Warehouse", "WH-001");

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(warehouseMapper.toResponse(entity)).thenReturn(response);

        // When
        var result = warehouseService.getById(1L);

        // Then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.code()).isEqualTo("WH-001");
    }

    @Test
    void getById_notFound_throwsResourceNotFoundException() {
        // Given
        when(warehouseRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> warehouseService.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_validRequest_updatesAndReturns() {
        // Given
        var entity = buildWarehouse(1L, "Old Name", "WH-001");
        var request = new UpdateWarehouseRequest("New Name", null, null);
        var response = buildWarehouseResponse(1L, "New Name", "WH-001");

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(warehouseRepository.save(entity)).thenReturn(entity);
        when(warehouseMapper.toResponse(entity)).thenReturn(response);

        // When
        var result = warehouseService.update(1L, request);

        // Then
        assertThat(result.name()).isEqualTo("New Name");
        verify(warehouseMapper).updateEntity(entity, request);
        verify(warehouseRepository).save(entity);
    }

    @Test
    void update_notFound_throwsResourceNotFoundException() {
        // Given
        var request = new UpdateWarehouseRequest("New Name", null, null);
        when(warehouseRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> warehouseService.update(999L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(warehouseRepository, never()).save(any());
    }

    @Test
    void update_deactivate_keepsCodeUnchanged() {
        // Given - retiring a warehouse is a soft delete via active = false
        var entity = buildWarehouse(1L, "Main Warehouse", "WH-001");
        var request = new UpdateWarehouseRequest(null, null, false);
        var response = new WarehouseResponse(1L, "Main Warehouse", "WH-001", "123 Test Street",
                false, LocalDateTime.now());

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(warehouseRepository.save(entity)).thenReturn(entity);
        when(warehouseMapper.toResponse(entity)).thenReturn(response);

        // When
        var result = warehouseService.update(1L, request);

        // Then
        assertThat(result.active()).isFalse();
        assertThat(result.code()).isEqualTo("WH-001");
        verify(warehouseMapper).updateEntity(entity, request);
    }

    @Test
    void getAll_returnsPageOfWarehouses() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        var entity = buildWarehouse(1L, "Main Warehouse", "WH-001");
        var response = buildWarehouseResponse(1L, "Main Warehouse", "WH-001");
        Page<Warehouse> page = new PageImpl<>(List.of(entity), pageable, 1);

        when(warehouseRepository.findAll(pageable)).thenReturn(page);
        when(warehouseMapper.toResponse(entity)).thenReturn(response);

        // When
        var result = warehouseService.getAll(pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).code()).isEqualTo("WH-001");
    }

    private Warehouse buildWarehouse(Long id, String name, String code) {
        Warehouse warehouse = Warehouse.builder()
                .name(name)
                .code(code)
                .address("123 Test Street")
                .active(true)
                .build();
        warehouse.setId(id);
        return warehouse;
    }

    private WarehouseResponse buildWarehouseResponse(Long id, String name, String code) {
        return new WarehouseResponse(id, name, code, "123 Test Street", true, LocalDateTime.now());
    }
}
