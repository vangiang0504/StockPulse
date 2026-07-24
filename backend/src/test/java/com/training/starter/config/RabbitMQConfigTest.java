package com.training.starter.config;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.training.starter.enums.MovementType;
import com.training.starter.enums.StockStatus;
import com.training.starter.messaging.event.StockLowEvent;
import com.training.starter.messaging.event.StockMovementCompletedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.MessageConverter;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMQConfigTest {

    private final RabbitMQConfig config = new RabbitMQConfig();

    @Test
    void stockTopology_usesRequiredDurableQueuesAndExactBindings() {
        // Given
        TopicExchange exchange = config.stockExchange();
        Queue stockUpdateQueue = config.stockUpdateQueue();
        Queue reorderQueue = config.reorderSuggestionQueue();
        Queue emailQueue = config.emailAlertQueue();
        Queue auditQueue = config.auditQueue();

        // When
        Binding stockUpdateBinding =
                config.stockUpdateBinding(stockUpdateQueue, exchange);
        Binding reorderBinding =
                config.reorderSuggestionBinding(reorderQueue, exchange);
        Binding emailBinding =
                config.emailAlertBinding(emailQueue, exchange);
        Binding auditBinding =
                config.auditBinding(auditQueue, exchange);

        // Then
        assertThat(exchange.getName()).isEqualTo("stock.exchange");
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
        assertQueueAndBinding(
                stockUpdateQueue,
                stockUpdateBinding,
                "stock.update.queue",
                "stock.*.completed");
        assertQueueAndBinding(
                reorderQueue,
                reorderBinding,
                "reorder.suggestion.queue",
                "stock.alert.low");
        assertQueueAndBinding(
                emailQueue,
                emailBinding,
                "email.alert.queue",
                "stock.alert.#");
        assertQueueAndBinding(
                auditQueue,
                auditBinding,
                "audit.queue",
                "stock.#");
    }

    @Test
    void jsonMessageConverter_roundTripsVersionedMovementCompletedEvent() {
        // Given
        MessageConverter converter = config.jsonMessageConverter(
                JsonMapper.builder().findAndAddModules().build());
        StockMovementCompletedEvent event = new StockMovementCompletedEvent(
                "1.0",
                "87bb51d8-93d9-4cf8-a176-f29130f775ab",
                31L,
                "IMP-20260724-ABCD",
                MovementType.IMPORT,
                List.of(11L, 12L),
                List.of(3L),
                Instant.parse("2026-07-24T04:00:00Z"));

        // When
        Message message = converter.toMessage(event, new MessageProperties());
        Object roundTrip = converter.fromMessage(message);

        // Then
        assertThat(roundTrip).isEqualTo(event);
    }

    @Test
    void movementRoutingKey_usesMovementTypeSegment() {
        assertThat(StockRabbitTopology.movementCompletedRoutingKey(MovementType.IMPORT))
                .isEqualTo("stock.import.completed");
        assertThat(StockRabbitTopology.movementCompletedRoutingKey(MovementType.EXPORT))
                .isEqualTo("stock.export.completed");
        assertThat(StockRabbitTopology.movementCompletedRoutingKey(MovementType.TRANSFER))
                .isEqualTo("stock.transfer.completed");
    }

    @Test
    void jsonMessageConverter_roundTripsVersionedStockLowEvent() {
        // Given
        MessageConverter converter = config.jsonMessageConverter(
                JsonMapper.builder().findAndAddModules().build());
        StockLowEvent event = new StockLowEvent(
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

        // When
        Message message = converter.toMessage(event, new MessageProperties());
        Object roundTrip = converter.fromMessage(message);

        // Then
        assertThat(roundTrip).isEqualTo(event);
    }

    private void assertQueueAndBinding(
            Queue queue,
            Binding binding,
            String expectedQueue,
            String expectedRoutingKey) {
        assertThat(queue.getName()).isEqualTo(expectedQueue);
        assertThat(queue.isDurable()).isTrue();
        assertThat(queue.isAutoDelete()).isFalse();
        assertThat(binding.getExchange()).isEqualTo("stock.exchange");
        assertThat(binding.getDestination()).isEqualTo(expectedQueue);
        assertThat(binding.getRoutingKey()).isEqualTo(expectedRoutingKey);
    }
}
