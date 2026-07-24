package com.training.starter.config;

import com.training.starter.enums.MovementType;

import java.util.Locale;

public final class StockRabbitTopology {

    public static final String STOCK_EXCHANGE = "stock.exchange";

    public static final String STOCK_UPDATE_QUEUE = "stock.update.queue";
    public static final String REORDER_SUGGESTION_QUEUE = "reorder.suggestion.queue";
    public static final String EMAIL_ALERT_QUEUE = "email.alert.queue";
    public static final String AUDIT_QUEUE = "audit.queue";

    public static final String MOVEMENT_COMPLETED_PATTERN = "stock.*.completed";
    public static final String LOW_STOCK_ROUTING_KEY = "stock.alert.low";
    public static final String EMAIL_ALERT_PATTERN = "stock.alert.#";
    public static final String AUDIT_PATTERN = "stock.#";

    private StockRabbitTopology() {
    }

    public static String movementCompletedRoutingKey(MovementType movementType) {
        return "stock.%s.completed"
                .formatted(movementType.name().toLowerCase(Locale.ROOT));
    }
}
