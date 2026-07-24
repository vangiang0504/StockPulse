package com.training.starter.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisEmailDeliveryTrackerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void wasSent_usesEventSpecificRedisKey() {
        // Given
        when(redisTemplate.hasKey("stock:email:sent:event-123"))
                .thenReturn(true);
        RedisEmailDeliveryTracker tracker =
                new RedisEmailDeliveryTracker(redisTemplate);

        // When
        boolean result = tracker.wasSent("event-123");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void markSent_storesThirtyDayDeliveryMarker() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        RedisEmailDeliveryTracker tracker =
                new RedisEmailDeliveryTracker(redisTemplate);

        // When
        tracker.markSent("event-123");

        // Then
        verify(valueOperations).set(
                "stock:email:sent:event-123",
                "sent",
                Duration.ofDays(30));
    }
}
