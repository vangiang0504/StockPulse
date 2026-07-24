package com.training.starter.messaging;

import com.training.starter.config.StockRabbitTopology;
import com.training.starter.entity.Product;
import com.training.starter.messaging.event.StockLowEvent;
import com.training.starter.repository.ProductRepository;
import com.training.starter.repository.ReorderSuggestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReorderConsumer {

    private final ProductRepository productRepository;
    private final ReorderSuggestionRepository reorderSuggestionRepository;

    @RabbitListener(queues = StockRabbitTopology.REORDER_SUGGESTION_QUEUE)
    @Transactional
    public void handleStockLow(StockLowEvent event) {
        Product product = productRepository.findById(event.productId())
                .orElseThrow(() -> new IllegalStateException(
                        "Product %d from low-stock event %s no longer exists"
                                .formatted(event.productId(), event.eventId())));

        int inserted = reorderSuggestionRepository.insertPendingIfAbsent(
                event.productId(),
                event.warehouseId(),
                product.getReorderQuantity(),
                event.currentQuantity(),
                event.reorderPoint());

        if (inserted == 0) {
            log.debug(
                    "Pending reorder suggestion already exists for product {} in warehouse {}",
                    event.productId(),
                    event.warehouseId());
            return;
        }

        log.info(
                "Created reorder suggestion for product {} in warehouse {} from event {}",
                event.productId(),
                event.warehouseId(),
                event.eventId());
    }
}
