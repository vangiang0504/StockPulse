package com.training.starter.messaging;

import com.training.starter.enums.MovementType;
import com.training.starter.enums.StockStatus;
import com.training.starter.messaging.event.StockLowEvent;
import com.training.starter.messaging.event.StockMovementCompletedEvent;
import com.training.starter.repository.StockLevelRepository;
import com.training.starter.repository.projection.StockThresholdProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockUpdateConsumerTest {

    @Mock
    private StockLevelRepository stockLevelRepository;

    @Mock
    private StockEventPublisher stockEventPublisher;

    @Test
    void handleMovementCompleted_quantityAboveReorderPoint_doesNotPublish() {
        // Given
        StockMovementCompletedEvent event = event(
                List.of(11L), List.of(3L));
        when(stockLevelRepository.findAffectedWithThresholds(
                        List.of(3L), List.of(11L)))
                .thenReturn(List.of(level(11L, 3L, 21, 20)));
        StockUpdateConsumer consumer =
                new StockUpdateConsumer(stockLevelRepository, stockEventPublisher);

        // When
        consumer.handleMovementCompleted(event);

        // Then
        verify(stockEventPublisher, never()).publishStockLow(any());
    }

    @Test
    void handleMovementCompleted_quantityEqualsReorderPoint_publishesLowEvent() {
        // Given
        StockMovementCompletedEvent event = event(
                List.of(11L), List.of(3L));
        when(stockLevelRepository.findAffectedWithThresholds(
                        List.of(3L), List.of(11L)))
                .thenReturn(List.of(level(11L, 3L, 20, 20)));
        StockUpdateConsumer consumer =
                new StockUpdateConsumer(stockLevelRepository, stockEventPublisher);

        // When
        consumer.handleMovementCompleted(event);

        // Then
        ArgumentCaptor<StockLowEvent> captor =
                ArgumentCaptor.forClass(StockLowEvent.class);
        verify(stockEventPublisher).publishStockLow(captor.capture());
        StockLowEvent lowEvent = captor.getValue();
        assertThat(lowEvent.sourceEventId()).isEqualTo(event.eventId());
        assertThat(lowEvent.movementId()).isEqualTo(31L);
        assertThat(lowEvent.productId()).isEqualTo(11L);
        assertThat(lowEvent.warehouseId()).isEqualTo(3L);
        assertThat(lowEvent.currentQuantity()).isEqualTo(20);
        assertThat(lowEvent.reorderPoint()).isEqualTo(20);
        assertThat(lowEvent.stockStatus()).isEqualTo(StockStatus.LOW_STOCK);
    }

    @Test
    void handleMovementCompleted_zeroQuantity_publishesOutOfStockEvent() {
        // Given
        StockMovementCompletedEvent event = event(
                List.of(11L), List.of(3L));
        when(stockLevelRepository.findAffectedWithThresholds(
                        List.of(3L), List.of(11L)))
                .thenReturn(List.of(level(11L, 3L, 0, 20)));
        StockUpdateConsumer consumer =
                new StockUpdateConsumer(stockLevelRepository, stockEventPublisher);

        // When
        consumer.handleMovementCompleted(event);

        // Then
        ArgumentCaptor<StockLowEvent> captor =
                ArgumentCaptor.forClass(StockLowEvent.class);
        verify(stockEventPublisher).publishStockLow(captor.capture());
        assertThat(captor.getValue().stockStatus())
                .isEqualTo(StockStatus.OUT_OF_STOCK);
    }

    @Test
    void handleMovementCompleted_transferChecksEveryAffectedWarehouseAndProduct() {
        // Given
        StockMovementCompletedEvent event = event(
                List.of(11L, 12L), List.of(3L, 4L));
        when(stockLevelRepository.findAffectedWithThresholds(
                        List.of(3L, 4L), List.of(11L, 12L)))
                .thenReturn(List.of(
                        level(11L, 3L, 5, 10),
                        level(11L, 4L, 25, 10),
                        level(12L, 3L, 15, 10),
                        level(12L, 4L, 0, 10)));
        StockUpdateConsumer consumer =
                new StockUpdateConsumer(stockLevelRepository, stockEventPublisher);

        // When
        consumer.handleMovementCompleted(event);

        // Then
        ArgumentCaptor<StockLowEvent> captor =
                ArgumentCaptor.forClass(StockLowEvent.class);
        verify(stockEventPublisher, org.mockito.Mockito.times(2))
                .publishStockLow(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(
                        StockLowEvent::warehouseId,
                        StockLowEvent::productId,
                        StockLowEvent::stockStatus)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(
                                3L, 11L, StockStatus.LOW_STOCK),
                        org.assertj.core.groups.Tuple.tuple(
                                4L, 12L, StockStatus.OUT_OF_STOCK));
    }

    @Test
    void handleMovementCompleted_missingCommittedLevel_throwsWithoutPublishing() {
        // Given
        StockMovementCompletedEvent event = event(
                List.of(11L), List.of(3L, 4L));
        when(stockLevelRepository.findAffectedWithThresholds(
                        List.of(3L, 4L), List.of(11L)))
                .thenReturn(List.of(level(11L, 3L, 5, 10)));
        StockUpdateConsumer consumer =
                new StockUpdateConsumer(stockLevelRepository, stockEventPublisher);

        // When & Then
        assertThatThrownBy(() -> consumer.handleMovementCompleted(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("warehouse id 4")
                .hasMessageContaining("product id 11");
        verify(stockEventPublisher, never()).publishStockLow(any());
    }

    @Test
    void stockLowEvent_redeliveryUsesSameStableEventId() {
        // Given
        StockMovementCompletedEvent source = event(
                List.of(11L), List.of(3L));

        // When
        StockLowEvent first = StockLowEvent.create(source, 11L, 3L, 5, 20);
        StockLowEvent redelivery = StockLowEvent.create(source, 11L, 3L, 4, 20);

        // Then
        assertThat(redelivery.eventId()).isEqualTo(first.eventId());
    }

    private StockMovementCompletedEvent event(
            List<Long> productIds, List<Long> warehouseIds) {
        return new StockMovementCompletedEvent(
                "1.0",
                "87bb51d8-93d9-4cf8-a176-f29130f775ab",
                31L,
                "TRF-20260724-ABCD",
                MovementType.TRANSFER,
                productIds,
                warehouseIds,
                Instant.parse("2026-07-24T04:00:00Z"));
    }

    private StockThresholdProjection level(
            Long productId,
            Long warehouseId,
            Integer quantity,
            Integer reorderPoint) {
        return new StockThresholdProjection() {
            @Override
            public Long getProductId() {
                return productId;
            }

            @Override
            public Long getWarehouseId() {
                return warehouseId;
            }

            @Override
            public Integer getQuantity() {
                return quantity;
            }

            @Override
            public Integer getReorderPoint() {
                return reorderPoint;
            }
        };
    }
}
