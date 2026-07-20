package com.training.starter.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "training.exchange";
    public static final String QUEUE_NAME = "training.queue";
    public static final String ROUTING_KEY = "training.routing.key";

    @Bean
    public TopicExchange trainingExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue trainingQueue() {
        return QueueBuilder.durable(QUEUE_NAME).build();
    }

    @Bean
    public Binding trainingBinding(Queue trainingQueue, TopicExchange trainingExchange) {
        return BindingBuilder.bind(trainingQueue).to(trainingExchange).with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
