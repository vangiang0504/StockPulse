package com.training.starter.messaging;

import com.training.starter.config.StockRabbitTopology;
import com.training.starter.messaging.event.StockLowEvent;
import com.training.starter.messaging.event.StockMovementCompletedEvent;
import com.training.starter.repository.StockLevelRepository;
import com.training.starter.repository.projection.StockThresholdProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockUpdateConsumer {

    private final StockLevelRepository stockLevelRepository;
    private final StockEventPublisher stockEventPublisher;

    @RabbitListener(queues = StockRabbitTopology.STOCK_UPDATE_QUEUE)
    @Transactional(readOnly = true)
    public void handleMovementCompleted(StockMovementCompletedEvent event) {
        log.debug(
                "Evaluating committed stock for movement event {}",
                event.eventId());

        List<StockThresholdProjection> levels =
                stockLevelRepository.findAffectedWithThresholds(
                        event.warehouseIds(), event.productIds());
        validateAllAffectedLevelsExist(event, levels);

        for (StockThresholdProjection level : levels) {
            if (level.getQuantity() <= level.getReorderPoint()) {
                stockEventPublisher.publishStockLow(
                        StockLowEvent.create(
                                event,
                                level.getProductId(),
                                level.getWarehouseId(),
                                level.getQuantity(),
                                level.getReorderPoint()));
            }
        }
    }

    private void validateAllAffectedLevelsExist(
            StockMovementCompletedEvent event,
            List<StockThresholdProjection> levels) {
        Set<StockKey> actual = new HashSet<>();
        for (StockThresholdProjection level : levels) {
            actual.add(new StockKey(
                    level.getWarehouseId(), level.getProductId()));
        }

        for (Long warehouseId : event.warehouseIds()) {
            for (Long productId : event.productIds()) {
                if (!actual.contains(new StockKey(warehouseId, productId))) {
                    throw new IllegalStateException(
                            "Committed stock level is missing for warehouse id %d and product id %d"
                                    .formatted(warehouseId, productId));
                }
            }
        }
    }

    private record StockKey(Long warehouseId, Long productId) {
    }
}
