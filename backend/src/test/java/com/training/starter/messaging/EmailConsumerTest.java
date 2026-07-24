package com.training.starter.messaging;

import com.training.starter.config.AlertEmailProperties;
import com.training.starter.entity.Product;
import com.training.starter.entity.Warehouse;
import com.training.starter.enums.StockStatus;
import com.training.starter.messaging.event.StockLowEvent;
import com.training.starter.repository.ProductRepository;
import com.training.starter.repository.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailConsumerTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private EmailDeliveryTracker deliveryTracker;

    @Test
    void handleStockLow_newEventSendsConfiguredEmailAndMarksDelivered() {
        // Given
        StockLowEvent event = event();
        stubReferences(event);
        EmailConsumer consumer = consumer(
                new AlertEmailProperties(
                        "alerts@stockpulse.test",
                        "staff@example.com, manager@example.com"));

        // When
        consumer.handleStockLow(event);

        // Then
        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        InOrder order = inOrder(mailSender, deliveryTracker);
        order.verify(mailSender).send(messageCaptor.capture());
        order.verify(deliveryTracker).markSent(event.eventId());

        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getFrom()).isEqualTo("alerts@stockpulse.test");
        assertThat(message.getTo())
                .containsExactly("staff@example.com", "manager@example.com");
        assertThat(message.getSubject())
                .contains("LOW_STOCK", "SKU-11", "WH-3");
        assertThat(message.getText())
                .contains(
                        "Product: SKU-11 - Widget",
                        "Warehouse: WH-3 - Main warehouse",
                        "Current stock: 5",
                        "Reorder point: 20",
                        "Suggested reorder quantity: 50",
                        "Severity: LOW_STOCK",
                        "Event ID: " + event.eventId());
    }

    @Test
    void handleStockLow_alreadyDeliveredSkipsDatabaseAndSmtp() {
        // Given
        StockLowEvent event = event();
        when(deliveryTracker.wasSent(event.eventId())).thenReturn(true);
        EmailConsumer consumer = consumer(
                new AlertEmailProperties(null, "staff@example.com"));

        // When
        consumer.handleStockLow(event);

        // Then
        verify(productRepository, never()).findById(
                org.mockito.ArgumentMatchers.any());
        verify(warehouseRepository, never()).findById(
                org.mockito.ArgumentMatchers.any());
        verify(mailSender, never()).send(
                org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
        verify(deliveryTracker, never()).markSent(
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void handleStockLow_smtpFailureDoesNotMarkDelivered() {
        // Given
        StockLowEvent event = event();
        stubReferences(event);
        doThrow(new MailSendException("SMTP unavailable"))
                .when(mailSender)
                .send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
        EmailConsumer consumer = consumer(
                new AlertEmailProperties(null, "staff@example.com"));

        // When & Then
        assertThatThrownBy(() -> consumer.handleStockLow(event))
                .isInstanceOf(MailSendException.class)
                .hasMessageContaining("SMTP unavailable");
        verify(deliveryTracker, never()).markSent(event.eventId());
    }

    @Test
    void handleStockLow_missingRecipientsFailsBeforeSmtp() {
        // Given
        StockLowEvent event = event();
        stubReferences(event);
        EmailConsumer consumer =
                consumer(new AlertEmailProperties(null, " , "));

        // When & Then
        assertThatThrownBy(() -> consumer.handleStockLow(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ALERT_EMAIL_RECIPIENTS");
        verify(mailSender, never()).send(
                org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
        verify(deliveryTracker, never()).markSent(event.eventId());
    }

    private EmailConsumer consumer(AlertEmailProperties properties) {
        return new EmailConsumer(
                mailSender,
                productRepository,
                warehouseRepository,
                deliveryTracker,
                properties);
    }

    private void stubReferences(StockLowEvent event) {
        when(productRepository.findById(event.productId()))
                .thenReturn(Optional.of(product()));
        when(warehouseRepository.findById(event.warehouseId()))
                .thenReturn(Optional.of(warehouse()));
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

    private Warehouse warehouse() {
        Warehouse warehouse = Warehouse.builder()
                .code("WH-3")
                .name("Main warehouse")
                .build();
        warehouse.setId(3L);
        return warehouse;
    }
}
