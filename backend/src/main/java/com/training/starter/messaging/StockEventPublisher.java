package com.training.starter.messaging;

import com.training.starter.config.StockRabbitTopology;
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

    private static final String EVENT_TYPE = "StockMovementCompletedEvent";

    private final RabbitTemplate rabbitTemplate;

    public void publishMovementCompleted(StockMovementCompletedEvent event) {
        Runnable publishAction = () -> publish(
                StockRabbitTopology.movementCompletedRoutingKey(event.movementType()),
                event);

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

    private void publish(String routingKey, StockMovementCompletedEvent event) {
        rabbitTemplate.convertAndSend(
                StockRabbitTopology.STOCK_EXCHANGE,
                routingKey,
                event,
                message -> withEventMetadata(message, event));
        log.info(
                "Published {} {} for movement {} using routing key {}",
                EVENT_TYPE,
                event.eventId(),
                event.movementId(),
                routingKey);
    }

    private Message withEventMetadata(
            Message message, StockMovementCompletedEvent event) {
        message.getMessageProperties().setMessageId(event.eventId());
        message.getMessageProperties().setCorrelationId(event.eventId());
        message.getMessageProperties().setType(EVENT_TYPE);
        message.getMessageProperties().setHeader(
                "schemaVersion", event.schemaVersion());
        return message;
    }
}
