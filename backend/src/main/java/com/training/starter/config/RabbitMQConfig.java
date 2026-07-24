package com.training.starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@Configuration
@EnableConfigurationProperties(AlertEmailProperties.class)
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "training.exchange";
    public static final String QUEUE_NAME = "training.queue";
    public static final String ROUTING_KEY = "training.routing.key";

    @Bean
    public TopicExchange trainingExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue trainingQueue() {
        return QueueBuilder.durable(QUEUE_NAME).build();
    }

    @Bean
    public Binding trainingBinding(
            @Qualifier("trainingQueue") Queue trainingQueue,
            @Qualifier("trainingExchange") TopicExchange trainingExchange) {
        return BindingBuilder.bind(trainingQueue).to(trainingExchange).with(ROUTING_KEY);
    }

    @Bean
    public TopicExchange stockExchange() {
        return new TopicExchange(StockRabbitTopology.STOCK_EXCHANGE, true, false);
    }

    @Bean
    public Queue stockUpdateQueue() {
        return QueueBuilder.durable(StockRabbitTopology.STOCK_UPDATE_QUEUE).build();
    }

    @Bean
    public Queue reorderSuggestionQueue() {
        return QueueBuilder.durable(StockRabbitTopology.REORDER_SUGGESTION_QUEUE).build();
    }

    @Bean
    public Queue emailAlertQueue() {
        return QueueBuilder.durable(StockRabbitTopology.EMAIL_ALERT_QUEUE).build();
    }

    @Bean
    public Queue auditQueue() {
        return QueueBuilder.durable(StockRabbitTopology.AUDIT_QUEUE).build();
    }

    @Bean
    public Binding stockUpdateBinding(
            @Qualifier("stockUpdateQueue") Queue queue,
            @Qualifier("stockExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(StockRabbitTopology.MOVEMENT_COMPLETED_PATTERN);
    }

    @Bean
    public Binding reorderSuggestionBinding(
            @Qualifier("reorderSuggestionQueue") Queue queue,
            @Qualifier("stockExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(StockRabbitTopology.LOW_STOCK_ROUTING_KEY);
    }

    @Bean
    public Binding emailAlertBinding(
            @Qualifier("emailAlertQueue") Queue queue,
            @Qualifier("stockExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(StockRabbitTopology.EMAIL_ALERT_PATTERN);
    }

    @Bean
    public Binding auditBinding(
            @Qualifier("auditQueue") Queue queue,
            @Qualifier("stockExchange") TopicExchange exchange) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(StockRabbitTopology.AUDIT_PATTERN);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
