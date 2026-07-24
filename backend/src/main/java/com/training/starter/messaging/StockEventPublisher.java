package com.training.starter.messaging;

import com.training.starter.config.StockRabbitTopology;
import com.training.starter.messaging.event.StockEvent;
import com.training.starter.messaging.event.StockLowEvent;
import com.training.starter.messaging.event.StockMovementCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishMovementCompleted(StockMovementCompletedEvent event) {
        publishAfterCommit(
                StockRabbitTopology.movementCompletedRoutingKey(event.movementType()),
                event);
    }

    public void publishStockLow(StockLowEvent event) {
        publishAfterCommit(StockRabbitTopology.LOW_STOCK_ROUTING_KEY, event);
    }

    private void publishAfterCommit(String routingKey, StockEvent event) {
        Runnable publishAction = () -> publish(routingKey, event);
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            publishAction.run();
                        }
                    });
            return;
        }

        publishAction.run();
    }

    private void publish(String routingKey, StockEvent event) {
        String eventType = event.getClass().getSimpleName();
        rabbitTemplate.convertAndSend(
                StockRabbitTopology.STOCK_EXCHANGE,
                routingKey,
                event,
                message -> withEventMetadata(message, event, eventType));
        log.info(
                "Published {} {} for movement {} using routing key {}",
                eventType,
                event.eventId(),
                event.movementId(),
                routingKey);
    }

    private Message withEventMetadata(
            Message message, StockEvent event, String eventType) {
        message.getMessageProperties().setMessageId(event.eventId());
        message.getMessageProperties().setCorrelationId(event.eventId());
        message.getMessageProperties().setType(eventType);
        message.getMessageProperties().setHeader(
                "schemaVersion", event.schemaVersion());
        return message;
    }
}
