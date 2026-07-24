package com.training.starter.messaging.event;

public interface StockEvent {

    String schemaVersion();

    String eventId();

    Long movementId();
}
