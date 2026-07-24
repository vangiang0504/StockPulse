package com.training.starter.messaging;

public interface EmailDeliveryTracker {

    boolean wasSent(String eventId);

    void markSent(String eventId);
}
