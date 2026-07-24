package com.training.starter.messaging;

import com.training.starter.config.AlertEmailProperties;
import com.training.starter.config.StockRabbitTopology;
import com.training.starter.entity.Product;
import com.training.starter.entity.Warehouse;
import com.training.starter.messaging.event.StockLowEvent;
import com.training.starter.repository.ProductRepository;
import com.training.starter.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailConsumer {

    private final JavaMailSender mailSender;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final EmailDeliveryTracker deliveryTracker;
    private final AlertEmailProperties emailProperties;

    @RabbitListener(queues = StockRabbitTopology.EMAIL_ALERT_QUEUE)
    public void handleStockLow(StockLowEvent event) {
        if (deliveryTracker.wasSent(event.eventId())) {
            log.debug(
                    "Skipping already delivered low-stock email for event {}",
                    event.eventId());
            return;
        }

        Product product = productRepository.findById(event.productId())
                .orElseThrow(() -> missingReference(
                        "Product", event.productId(), event.eventId()));
        Warehouse warehouse = warehouseRepository.findById(event.warehouseId())
                .orElseThrow(() -> missingReference(
                        "Warehouse", event.warehouseId(), event.eventId()));
        List<String> recipients = requiredRecipients();

        SimpleMailMessage message = new SimpleMailMessage();
        if (emailProperties.from() != null
                && !emailProperties.from().isBlank()) {
            message.setFrom(emailProperties.from().trim());
        }
        message.setTo(recipients.toArray(String[]::new));
        message.setSubject(
                "[StockPulse] %s: %s at %s"
                        .formatted(
                                event.stockStatus(),
                                product.getSku(),
                                warehouse.getCode()));
        message.setText(buildBody(event, product, warehouse));

        mailSender.send(message);
        deliveryTracker.markSent(event.eventId());
        log.info(
                "Sent low-stock email for event {} to {} recipient(s)",
                event.eventId(),
                recipients.size());
    }

    private String buildBody(
            StockLowEvent event,
            Product product,
            Warehouse warehouse) {
        return """
                StockPulse inventory alert

                Severity: %s
                Product: %s - %s
                Warehouse: %s - %s
                Current stock: %d
                Reorder point: %d
                Suggested reorder quantity: %d
                Movement ID: %d
                Event ID: %s
                """
                .formatted(
                        event.stockStatus(),
                        product.getSku(),
                        product.getName(),
                        warehouse.getCode(),
                        warehouse.getName(),
                        event.currentQuantity(),
                        event.reorderPoint(),
                        product.getReorderQuantity(),
                        event.movementId(),
                        event.eventId());
    }

    private List<String> requiredRecipients() {
        List<String> recipients = emailProperties.recipientList();
        if (recipients.isEmpty()) {
            throw new IllegalStateException(
                    "ALERT_EMAIL_RECIPIENTS must contain at least one email address");
        }
        return recipients;
    }

    private IllegalStateException missingReference(
            String resource, Long id, String eventId) {
        return new IllegalStateException(
                "%s %d from low-stock event %s no longer exists"
                        .formatted(resource, id, eventId));
    }
}
