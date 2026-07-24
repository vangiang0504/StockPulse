package com.training.starter.messaging;

import com.training.starter.config.StockRabbitTopology;
import com.training.starter.enums.MovementType;
import com.training.starter.messaging.event.StockMovementCompletedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StockEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void publishMovementCompleted_withoutTransaction_publishesWithStableMetadata() {
        // Given
        StockEventPublisher publisher = new StockEventPublisher(rabbitTemplate);
        StockMovementCompletedEvent event = event();

        // When
        publisher.publishMovementCompleted(event);

        // Then
        ArgumentCaptor<MessagePostProcessor> processorCaptor =
                ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(
                eq(StockRabbitTopology.STOCK_EXCHANGE),
                eq("stock.import.completed"),
                same(event),
                processorCaptor.capture());

        Message message = processorCaptor.getValue().postProcessMessage(
                new Message(new byte[0], new MessageProperties()));
        assertThat(message.getMessageProperties().getMessageId())
                .isEqualTo(event.eventId());
        assertThat(message.getMessageProperties().getCorrelationId())
                .isEqualTo(event.eventId());
        assertThat(message.getMessageProperties().getType())
                .isEqualTo("StockMovementCompletedEvent");
        Object schemaVersion =
                message.getMessageProperties().getHeader("schemaVersion");
        assertThat(schemaVersion)
                .isEqualTo("1.0");
    }

    @Test
    void publishMovementCompleted_activeTransaction_waitsUntilCommit() {
        // Given
        StockEventPublisher publisher = new StockEventPublisher(rabbitTemplate);
        StockMovementCompletedEvent event = event();
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        // When
        publisher.publishMovementCompleted(event);

        // Then
        verify(rabbitTemplate, never()).convertAndSend(
                eq(StockRabbitTopology.STOCK_EXCHANGE),
                eq("stock.import.completed"),
                same(event),
                org.mockito.ArgumentMatchers.any(MessagePostProcessor.class));

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        verify(rabbitTemplate).convertAndSend(
                eq(StockRabbitTopology.STOCK_EXCHANGE),
                eq("stock.import.completed"),
                same(event),
                org.mockito.ArgumentMatchers.any(MessagePostProcessor.class));
    }

    @Test
    void publishMovementCompleted_rolledBackTransactionDoesNotPublish() {
        // Given
        StockEventPublisher publisher = new StockEventPublisher(rabbitTemplate);
        StockMovementCompletedEvent event = event();
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        // When
        publisher.publishMovementCompleted(event);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(
                        TransactionSynchronization.STATUS_ROLLED_BACK));

        // Then
        verify(rabbitTemplate, never()).convertAndSend(
                eq(StockRabbitTopology.STOCK_EXCHANGE),
                eq("stock.import.completed"),
                same(event),
                org.mockito.ArgumentMatchers.any(MessagePostProcessor.class));
    }

    private StockMovementCompletedEvent event() {
        return new StockMovementCompletedEvent(
                "1.0",
                "87bb51d8-93d9-4cf8-a176-f29130f775ab",
                31L,
                "IMP-20260724-ABCD",
                MovementType.IMPORT,
                List.of(11L),
                List.of(3L),
                Instant.parse("2026-07-24T04:00:00Z"));
    }
}
