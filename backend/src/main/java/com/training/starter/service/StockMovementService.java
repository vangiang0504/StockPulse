package com.training.starter.service;

import com.training.starter.dto.request.CreateExportRequest;
import com.training.starter.dto.request.CreateImportRequest;
import com.training.starter.dto.request.CreateTransferRequest;
import com.training.starter.dto.response.MovementResponse;
import com.training.starter.enums.MovementStatus;
import com.training.starter.enums.MovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Creation, inspection, and lifecycle operations for stock movements.
 */
public interface StockMovementService {

    MovementResponse createImport(CreateImportRequest request);

    MovementResponse createExport(CreateExportRequest request);

    MovementResponse createTransfer(CreateTransferRequest request);

    MovementResponse getById(Long id);

    Page<MovementResponse> getAll(
            MovementType type,
            MovementStatus status,
            Long warehouseId,
            Pageable pageable);

    MovementResponse approve(Long id);

    MovementResponse completeMovement(Long id);
}
