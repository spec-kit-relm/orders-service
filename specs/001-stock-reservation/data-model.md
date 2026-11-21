# Data Model: Stock reservation at checkout — orders slice

| Table | Change | Notes |
|-------|--------|-------|
| `orders` | add `reservation_id uuid null` | set when the order converted a hold |
| `idempotency_keys` | new: `key uuid pk`, `body_sha256 bytea`, `order_id uuid`, `state text (pending/done)`, `response jsonb`, `created_at` | purge after 24 h; replaces the in-memory map in `OrdersController` |

## Placement flow (with reservation)

```text
request(key) ─► idempotency_keys[key]?
                 ├─ done, same body  → replay stored 201
                 ├─ same key, other body → 422 idempotency-key-reused
                 └─ none / pending → orderId := existing or new
                       └─► inventory commit(reservationId, orderId)   (idempotent)
                             ├─ 200 → insert order(reservation_id) + mark done → 201
                             ├─ 409/404 → mark failed → 409 reservation-expired
                             └─ 503/timeout → keep pending → 503 (retry-safe)
```

Personal data: none new (ORG-5).
