# Research: Stock reservation at checkout — orders slice

## Decision 1 — Order of operations: commit first, then persist
- **Decision**: Generate the order ID, store the idempotency record as `pending`, commit the reservation in inventory (idempotent per orderId), then persist the order and mark the record `done` — all keyed by the same order ID.
- **Rationale**: If anything fails after the commit, a retry with the same Idempotency-Key reuses the same order ID; inventory returns the same committed result; no double deduction and no orphan order (spec Edge Cases).
- **Alternatives rejected**: persist-then-commit with compensation (needs a saga and an "unconfirmed order" state shoppers could see); distributed transaction (not available, ORG-8).

## Decision 2 — Problem mapping from inventory
| Inventory response | Orders response |
|--------------------|-----------------|
| 200 committed | continue → 201 |
| 409 `reservation-expired` | 409 `reservation-expired`, no order |
| 404 | 409 `reservation-expired` (treat unknown as unusable; logged as warning) |
| 503 / timeout after retries | 503 `temporarily-unavailable`, Retry-After: 1, no order |

## Decision 3 — Generated inventory client
- **Decision**: openapi-generator (`java`, `native` HTTP client) from `contracts/vendor/inventory-api.yaml` in a Maven profile; generated sources are not committed.
- **Rationale**: ORD-5; regeneration is a one-liner when the vendored version changes.

## Decision 4 — Consumer contract tests
- **Decision**: WireMock stubs derived from the vendored OpenAPI examples + schema validation of every stubbed response; negative cases 409, 404, 503, timeout.
- **Rationale**: ORG-4 — consumer side; provider verification of inventory lives in inventory-service.
