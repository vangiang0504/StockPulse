package com.training.starter.service;

import com.training.starter.dto.request.CreateExportRequest;
import com.training.starter.dto.request.CreateImportRequest;
import com.training.starter.dto.request.CreateTransferRequest;
import com.training.starter.dto.response.MovementResponse;

/**
 * Creation and inspection of stock movements (REQ-STP-B-202). Creating or inspecting a movement
 * never mutates {@code stock_levels}; approval, completion, and the stock mutation itself are
 * handled by later stories.
 */
public interface StockMovementService {

    MovementResponse createImport(CreateImportRequest request);

    MovementResponse createExport(CreateExportRequest request);

    MovementResponse createTransfer(CreateTransferRequest request);

    MovementResponse getById(Long id);
}
