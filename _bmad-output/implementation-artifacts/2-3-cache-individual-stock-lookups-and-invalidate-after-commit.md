# Story 2.3: Cache individual stock lookups and invalidate after commit

Status: done

## Requirements

- REQ-STP-B-206
- REQ-STP-T-202

## Acceptance Criteria

- [x] Cache `StockLevelResponse` under `stock:{warehouseId}:{productId}`.
- [x] Use a five-minute TTL.
- [x] A cache hit avoids repository/database access.
- [x] Missing stock is not cached.
- [x] Successful movement completion invalidates all affected keys after commit.
- [x] A rolled-back completion does not invalidate prior cache entries.
- [x] Unit tests cover cache hit/miss and completion invalidation.

## Implementation Notes

- Reuse the existing typed `RedisTemplate<String, Object>`.
- Keep PostgreSQL and the movement transaction authoritative.
- Register eviction with transaction synchronization so it runs only after commit.

## Verification

- Cache/movement focused suite: 40 tests passing.
- Non-Docker backend regression suite: 116 tests passing.
- Tests cover first-query population, second-query cache hit, five-minute TTL,
  IMPORT/TRANSFER invalidation keys, post-commit eviction, and rollback preservation.
