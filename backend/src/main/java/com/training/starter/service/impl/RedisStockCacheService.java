package com.training.starter.service.impl;

import com.training.starter.dto.response.StockLevelResponse;
import com.training.starter.service.StockCacheKey;
import com.training.starter.service.StockCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisStockCacheService implements StockCacheService {

    static final Duration STOCK_TTL = Duration.ofMinutes(5);

    private final RedisTemplate<String, StockLevelResponse> redisTemplate;

    @Override
    public Optional<StockLevelResponse> get(Long warehouseId, Long productId) {
        String key = new StockCacheKey(warehouseId, productId).redisKey();
        try {
            StockLevelResponse cached = redisTemplate.opsForValue().get(key);
            if (cached == null) {
                return Optional.empty();
            }
            return Optional.of(cached);
        } catch (RuntimeException exception) {
            log.warn("Unable to read stock cache key {}; falling back to database", key, exception);
        }
        return Optional.empty();
    }

    @Override
    public void put(
            Long warehouseId,
            Long productId,
            StockLevelResponse response) {
        String key = new StockCacheKey(warehouseId, productId).redisKey();
        try {
            redisTemplate.opsForValue().set(key, response, STOCK_TTL);
        } catch (RuntimeException exception) {
            log.warn("Unable to populate stock cache key {}", key, exception);
        }
    }

    @Override
    public void evictAfterCommit(Collection<StockCacheKey> keys) {
        List<String> redisKeys = keys.stream()
                .map(StockCacheKey::redisKey)
                .distinct()
                .sorted()
                .toList();
        if (redisKeys.isEmpty()) {
            return;
        }

        Runnable eviction = () -> evict(redisKeys);
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            eviction.run();
                        }
                    });
            return;
        }

        log.debug("No active transaction; evicting stock cache keys immediately");
        eviction.run();
    }

    private void evict(List<String> redisKeys) {
        try {
            redisTemplate.delete(redisKeys);
        } catch (RuntimeException exception) {
            log.warn("Unable to evict stock cache keys {}", redisKeys, exception);
        }
    }
}
