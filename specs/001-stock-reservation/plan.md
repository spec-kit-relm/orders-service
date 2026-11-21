# Implementation Plan: Stock reservation at checkout — orders slice

**Branch**: `001-stock-reservation` | **Date**: 2026-11-20 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-stock-reservation/spec.md`

## Summary

`POST /v1/orders` accepts an optional `reservationId` (orders-api **1.3.0**). With a reservation,
the use case commits it in inventory-service (inventory-api **1.1.0**, `POST
/v1/reservations/{id}/commit`, idempotent per orderId) *before* persisting the order; the order ID
is generated up front so retries reuse it. Expired/replaced reservations map to 409
`reservation-expired`; inventory timeouts/503 map to 503 `temporarily-unavailable`. Idempotency
moves from memory to PostgreSQL.

## Technical Context

**Language/Version**: Java 21 (ORD-1)

**Primary Dependencies**: Spring Boot 3.4 (web, validation, actuator, data-jdbc), Flyway, Resilience4j (timeout/retry), openapi-generator (inventory client from vendored contract)

**Storage**: PostgreSQL 16 — `orders.reservation_id uuid null`; new `idempotency_keys` table

**Testing**: JUnit 5, MockMvc, ArchUnit, Testcontainers (PostgreSQL), WireMock stubs generated from `contracts/vendor/inventory-api.yaml` (consumer tests), swagger-request-validator (provider verification)

**Target Platform**: Linux container on the Relm Kubernetes platform

**Project Type**: web-service

**Performance Goals**: 100 orders/s peak

**Constraints**: p99 ≤ 400 ms for `POST /v1/orders` with reservation (inventory commit budget 150 ms + 300 ms timeout ceiling)

**Scale/Scope**: ~200 k orders/day at peak

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Rule | How this plan complies | Status |
|------|------------------------|--------|
| ORG-1/ORG-2 | RELM-1 local spec approved, technology-free | ✅ |
| ORG-3 Contract-first | orders-api 1.3.0 (additive) promoted + locked; inventory-api 1.1.0 vendored with provenance | ✅ |
| ORG-4 Tests prove the spec | Scenario-ID tests; consumer tests vs. inventory stubs; provider verification of orders-api | ✅ |
| ORG-5 | `orders:place` scope; no new personal data | ✅ |
| ORG-6 | `orders_placed_total{reservation}`, `inventory_commit_duration_seconds`, RED | ✅ |
| ORG-7 | Optional field: no flag needed here; storefront flag gates usage; rollback below | ✅ |
| ORG-8 | Resilience4j is already an approved library (ADR-0003); no new datastore | ✅ |
| ORG-9 | Phases sized for one run each | ✅ |
| BE-1 … BE-6 | problem types, Idempotency-Key persisted, additive migration, no events, budgets, pyramid | ✅ |
| ORD-1 … ORD-5 | hexagonal packages; Flyway; idempotent writes; Money minor units; generated inventory client | ✅ |

## Project Structure

### Documentation (this feature)

```text
specs/001-stock-reservation/
├── plan.md  research.md  data-model.md  quickstart.md  tasks.md
└── contracts/           # draft promoted to /contracts/orders-api.yaml (1.3.0)
```

### Source Code (repository root)

```text
src/main/java/com/relm/orders/
├── domain/              Order, Money, OrderPlacement rules (no Spring)
├── application/         PlaceOrderUseCase, ports: InventoryPort, OrderRepository, IdempotencyStore
├── adapters/inventory/  generated client + InventoryAdapter (timeouts, retries, problem mapping)
├── adapters/persistence JDBC repositories
└── api/                 OrdersController, ProblemMapper
src/main/resources/db/migration/   V202611200900__order_reservation.sql, V202611200901__idempotency_keys.sql
src/test/java/…          unit, ArchitectureTest, contract (provider + consumer), integration
```

**Structure Decision**: Single Spring Boot service, hexagonal (ORD-1). Today's baseline code lives
in `api` + `domain`; this feature introduces `application` and `adapters`.

## Complexity Tracking

> No constitution violations to justify.

---

## Relm Delivery Sections *(mandatory — ORG-3, ORG-5, ORG-6, ORG-7)*

### Contracts & Versions (ORG-3)

| Interface | Role | Contract file | Locked version | Change | Consumers / provider |
|-----------|------|---------------|----------------|--------|----------------------|
| Orders API | provides | `contracts/orders-api.yaml` | 1.3.0 | additive (`reservationId`, `reservation-expired`) | web-storefront, e2e-tests |
| Inventory API — commit | consumes | `contracts/vendor/inventory-api.yaml` | 1.1.0 | — | provider: inventory-service |

### Observability (ORG-6)

| Success criterion (from spec) | Signal (metric / log / trace) | Dashboard or alert |
|-------------------------------|-------------------------------|--------------------|
| SC-001 no unconverted orders | `orders_placed_total{reservation="present",commit="ok"}` vs. inventory `committed` events | reconciliation job alert (T030) |
| SC-002 no duplicates | `idempotency_replays_total`, duplicate-order query | "Orders — RELM-1" dashboard |
| SC-003 p99 ≤ 400 ms | `http_server_requests_seconds{uri="/v1/orders"}` | alert p99 > 400 ms 10 min |

### Feature flag, rollout & rollback (ORG-7)

- **Flag**: none here — `reservationId` is optional; the storefront flag `checkout.stock-reservation` decides whether it is sent.
- **Rollout position** (from impact.md): 2 of 4 — after inventory-service.
- **Rollback**: storefront flag off first; then redeploy previous version (requests with `reservationId` would be rejected as unknown field → only safe with flag off). Migrations are additive.

### Security & privacy (ORG-5)

- **AuthN / AuthZ**: unchanged `orders:place`; outbound service token with `inventory:commit`.
- **Personal data touched**: none new.
- **Secrets / config**: `INVENTORY_BASE_URL`, `INVENTORY_TOKEN` (secret), `INVENTORY_TIMEOUT=300ms`.

### Backend: idempotency & retries (BE-2)

| Operation | Idempotency mechanism | Retry policy | Duplicate behavior |
|-----------|----------------------|--------------|--------------------|
| POST /v1/orders | `idempotency_keys(key, body_sha256, order_id, response)`; order ID reserved before commit | storefront may retry on 503 | same 201 body; different body → 422 |
| inventory commit (outbound) | inventory `Idempotency-Key` = order ID; commit idempotent per orderId | Resilience4j: 3 attempts, 50/100/200 ms ± jitter, only on 503/timeout | inventory returns same 200 |

### Backend: data changes & migrations (BE-3)

| Change | Migration | Backward-compatible with previous release? | Contract step (later release) |
|--------|-----------|---------------------------------------------|-------------------------------|
| `orders.reservation_id uuid null` | `V202611200900__order_reservation.sql` | yes — nullable, ignored by old code | none |
| `idempotency_keys` table (replaces in-memory map) | `V202611200901__idempotency_keys.sql` | yes | none |

### Backend: events (BE-4)

| Event | Direction | AsyncAPI channel | Outbox? | Consumer de-dup key |
|-------|-----------|------------------|---------|---------------------|
| — | — | — | — | — (orders-service does not publish or consume events in RELM-1) |

### Backend: performance & resilience budgets (BE-5)

| Endpoint / consumer | p99 latency budget | Error budget | Timeouts on outbound calls | Degradation behavior |
|---------------------|--------------------|--------------|----------------------------|----------------------|
| POST /v1/orders (with reservation) | 400 ms | 0.1 % | inventory 300 ms total incl. retries | 503 `temporarily-unavailable` + Retry-After: 1; no order stored |
| POST /v1/orders (without) | 150 ms | 0.1 % | — | unchanged |
