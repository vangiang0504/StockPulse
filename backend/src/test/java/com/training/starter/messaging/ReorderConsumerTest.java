package com.training.starter.messaging;

import com.training.starter.entity.Product;
import com.training.starter.enums.StockStatus;
import com.training.starter.messaging.event.StockLowEvent;
import com.training.starter.repository.ProductRepository;
import com.training.starter.repository.ReorderSuggestionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReorderConsumerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ReorderSuggestionRepository reorderSuggestionRepository;

    @Test
    void handleStockLow_newConditionCreatesPendingSuggestion() {
        // Given
        StockLowEvent event = event();
        Product product = product();
        when(productRepository.findById(11L)).thenReturn(Optional.of(product));
        when(reorderSuggestionRepository.insertPendingIfAbsent(
                        11L, 3L, 50, 5, 20))
                .thenReturn(1);
        ReorderConsumer consumer =
                new ReorderConsumer(productRepository, reorderSuggestionRepository);

        // When
        consumer.handleStockLow(event);

        // Then
        verify(reorderSuggestionRepository).insertPendingIfAbsent(
                11L, 3L, 50, 5, 20);
    }

    @Test
    void handleStockLow_pendingSuggestionExistsCompletesIdempotently() {
        // Given
        StockLowEvent event = event();
        when(productRepository.findById(11L)).thenReturn(Optional.of(product()));
        when(reorderSuggestionRepository.insertPendingIfAbsent(
                        11L, 3L, 50, 5, 20))
                .thenReturn(0);
        ReorderConsumer consumer =
                new ReorderConsumer(productRepository, reorderSuggestionRepository);

        // When
        consumer.handleStockLow(event);

        // Then
        verify(reorderSuggestionRepository).insertPendingIfAbsent(
                11L, 3L, 50, 5, 20);
    }

    @Test
    void handleStockLow_missingProductThrowsWithoutInsert() {
        // Given
        StockLowEvent event = event();
        when(productRepository.findById(11L)).thenReturn(Optional.empty());
        ReorderConsumer consumer =
                new ReorderConsumer(productRepository, reorderSuggestionRepository);

        // When & Then
        assertThatThrownBy(() -> consumer.handleStockLow(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Product 11")
                .hasMessageContaining(event.eventId());
        verify(reorderSuggestionRepository, never())
                .insertPendingIfAbsent(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }

    private StockLowEvent event() {
        return new StockLowEvent(
                "1.0",
                "c986848e-068f-4c92-9d6d-4dc0a889597d",
                "87bb51d8-93d9-4cf8-a176-f29130f775ab",
                31L,
                11L,
                3L,
                5,
                20,
                StockStatus.LOW_STOCK,
                Instant.parse("2026-07-24T04:01:00Z"));
    }

    private Product product() {
        Product product = Product.builder()
                .sku("SKU-11")
                .name("Widget")
                .reorderPoint(20)
                .reorderQuantity(50)
                .build();
        product.setId(11L);
        return product;
    }
}
