package com.training.starter.service.impl;

import com.training.starter.dto.response.StockLevelResponse;
import com.training.starter.service.StockCacheKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import org.springframework.transaction.support.TransactionSynchronization;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisStockCacheServiceTest {

    @Mock
    private RedisTemplate<String, StockLevelResponse> redisTemplate;

    @Mock
    private ValueOperations<String, StockLevelResponse> valueOperations;

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void get_cachedResponse_usesRequiredStockKey() {
        // Given
        StockLevelResponse response = response();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("stock:22:11")).thenReturn(response);
        RedisStockCacheService cacheService =
                new RedisStockCacheService(redisTemplate);

        // When
        var result = cacheService.get(22L, 11L);

        // Then
        assertThat(result).containsSame(response);
        verify(valueOperations).get("stock:22:11");
    }

    @Test
    void put_response_usesFiveMinuteTtl() {
        // Given
        StockLevelResponse response = response();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        RedisStockCacheService cacheService =
                new RedisStockCacheService(redisTemplate);

        // When
        cacheService.put(22L, 11L, response);

        // Then
        verify(valueOperations).set(
                "stock:22:11",
                response,
                RedisStockCacheService.STOCK_TTL);
        assertThat(RedisStockCacheService.STOCK_TTL.toMinutes()).isEqualTo(5);
    }

    @Test
    void evictAfterCommit_activeTransaction_waitsUntilCommit() {
        // Given
        RedisStockCacheService cacheService =
                new RedisStockCacheService(redisTemplate);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        // When
        cacheService.evictAfterCommit(Set.of(
                new StockCacheKey(2L, 11L),
                new StockCacheKey(1L, 11L)));

        // Then
        verify(redisTemplate, never()).delete(
                org.mockito.ArgumentMatchers.<List<String>>any());

        // When
        TransactionSynchronizationUtils.triggerAfterCommit();

        // Then
        verify(redisTemplate).delete(
                List.of("stock:1:11", "stock:2:11"));
    }

    @Test
    void evictAfterCommit_rolledBackTransactionKeepsCache() {
        // Given
        RedisStockCacheService cacheService =
                new RedisStockCacheService(redisTemplate);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        // When
        cacheService.evictAfterCommit(
                Set.of(new StockCacheKey(22L, 11L)));
        TransactionSynchronizationUtils.triggerAfterCompletion(
                TransactionSynchronization.STATUS_ROLLED_BACK);

        // Then
        verify(redisTemplate, never()).delete(
                org.mockito.ArgumentMatchers.<List<String>>any());
    }

    private StockLevelResponse response() {
        return new StockLevelResponse(
                7L,
                11L,
                "SKU-011",
                "Product",
                22L,
                "WH-022",
                "Warehouse",
                40,
                7,
                33,
                20,
                3L,
                LocalDateTime.of(2026, 7, 24, 10, 0));
    }
}
