package com.training.starter.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisEmailDeliveryTracker implements EmailDeliveryTracker {

    private static final String KEY_PREFIX = "stock:email:sent:";
    private static final Duration DELIVERY_TTL = Duration.ofDays(30);

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean wasSent(String eventId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + eventId));
    }

    @Override
    public void markSent(String eventId) {
        redisTemplate.opsForValue().set(
                KEY_PREFIX + eventId, "sent", DELIVERY_TTL);
    }
}
