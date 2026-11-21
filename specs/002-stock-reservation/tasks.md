---
description: "Task list for RELM-2 stock reservation — orders-service slice"
---

# Tasks: Stock reservation at checkout — orders slice

**Input**: Design documents from `/specs/002-stock-reservation/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts (orders-api 1.3.1 provided, inventory-api 1.1.1 consumed — both locked)

**Tests**: Mandatory at Relm (ORG-4). Test tasks precede implementation tasks in every story phase.

**Organization**: Tasks grouped by user story; each phase fits one `/speckit.implement` run.

## Format: `[ID] [P?] [Story] Description`

## Phase 1: Setup (Shared Infrastructure)

- [ ] T001 Add dependencies to `pom.xml`: spring-boot-starter-data-jdbc, flyway-core, flyway-database-postgresql, resilience4j-spring-boot3, testcontainers (postgresql, junit-jupiter), wiremock-standalone, swagger-request-validator-mockmvc
- [ ] T002 [P] Maven profile `inventory-client`: openapi-generator from `contracts/vendor/inventory-api.yaml` into `target/generated-sources` (ORD-5)
- [ ] T003 [P] Maven profile `integration` (failsafe) and `docker-compose.yml` with PostgreSQL 16

---

## Phase 2: Foundational (Blocking Prerequisites)

- [ ] T004 Flyway `V202611200900__order_reservation.sql` and `V202611200901__idempotency_keys.sql` (ORD-2, BE-3)
- [ ] T005 [P] Extend `ArchitectureTest`: `application` must not depend on `adapters`/`api`; no `double`/`float` fields in `domain` (ORD-1, ORD-4)
- [ ] T006 [P] Ports in `application/`: `InventoryPort` (commit → Committed | Expired | Unavailable), `OrderRepository`, `IdempotencyStore`
- [ ] T007 Provider verification harness `OrdersContractTest`: validate MockMvc requests/responses against `contracts/orders-api.yaml` (ORD-3)
- [ ] T008 Move idempotency from the in-memory map in `api/OrdersController` to `IdempotencyStore` (JDBC) — existing `OrdersApiTest` must stay green
- [ ] T009 Inventory adapter `adapters/inventory/InventoryAdapter.java`: generated client, 300 ms total timeout, Resilience4j retry 3× on 503/timeout, problem → port result mapping (research Decision 2)

**Checkpoint**: persistence-backed idempotency and inventory port ready

---

## Phase 3: User Story 1 - Place an order that converts its hold (Priority: P1) 🎯 MVP

### Tests for User Story 1 ⚠️

- [ ] T010 [P] [US1] Use-case test `RELM-2/US2/AS1 order with active hold commits then confirms` in `PlaceOrderUseCaseTest`
- [ ] T011 [P] [US1] Use-case test `ORD/RELM-2/US1/AS2 order without hold behaves as 1.2.0`
- [ ] T012 [P] [US1] Consumer contract test `InventoryClientContractTest` — commit 200 against WireMock stub validated by the vendored contract
- [ ] T013 [US1] Provider verification: `POST /v1/orders` with `reservationId` returns 201 matching orders-api 1.3.1

### Implementation for User Story 1

- [ ] T014 [US1] `PlaceOrderUseCase` with commit-then-persist flow (research Decision 1)
- [ ] T015 [US1] Accept optional `reservationId` in `OrdersController` and `Order`; persist `reservation_id`
- [ ] T016 [US1] Metrics `orders_placed_total{reservation}`, `inventory_commit_duration_seconds` (ORG-6)

---

## Phase 4: User Story 2 - Repeated submissions create one order (Priority: P1)

### Tests for User Story 2 ⚠️

- [ ] T017 [P] [US2] Integration test `RELM-2/US2/AS2 double submit yields one order` (sequential and 2 concurrent requests, Testcontainers)
- [ ] T018 [P] [US2] Integration test `ORD/RELM-2/US2/AS2 retry after inventory 503 creates one order and reuses orderId`
- [ ] T019 [P] [US2] Consumer contract test: inventory commit replay returns same body; timeout path

### Implementation for User Story 2

- [ ] T020 [US2] `pending` → `done` idempotency states with order ID reserved before the commit (data-model flow)
- [ ] T021 [US2] Concurrency guard: `INSERT … ON CONFLICT DO NOTHING` on `idempotency_keys`, losers wait/replay

---

## Phase 5: User Story 3 - Reject orders whose hold expired (Priority: P1)

### Tests for User Story 3 ⚠️

- [ ] T022 [P] [US3] Use-case test `RELM-2/US2/AS3 expired hold returns reservation-expired and stores no order`
- [ ] T023 [P] [US3] Consumer contract tests: inventory 409 `reservation-expired`, 404, 503 after retries
- [ ] T024 [US3] Provider verification: 409 and 503 problem documents match orders-api 1.3.1

### Implementation for User Story 3

- [ ] T025 [US3] `ProblemMapper` for `reservation-expired` (409) and `temporarily-unavailable` (503 + Retry-After)

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T026 [P] Dashboard "Orders — RELM-2" + alerts in `ops/dashboards/` (ORG-6)
- [ ] T027 [P] Reconciliation job query: orders with `reservation_id` vs. inventory committed events (SC-001)
- [ ] T028 Update `AGENTS.md` Layout and `README.md`

---

## Phase R: Relm Verification & Handoff *(mandatory, always the last phase — ORG-3, ORG-4, ORG-9, PRD-6)*

- [ ] TR01 Provider verification and consumer contract tests pass against the locked contract versions, including negative cases
- [ ] TR02 Every acceptance scenario `RELM-2/US<k>/AS<m>` in scope has an automated test whose title starts with that ID
- [ ] TR03 Feature flag defaults to off; rollback steps from plan.md verified in a non-production environment
- [ ] TR04 Run `/speckit.converge`; resolve every appended convergence task until it reports **Converged**
- [ ] TR05 If shipped behavior or contract versions differ from the central spec, open a PR in `product-specs` updating `spec.md` / `contracts.lock.md`
- [ ] TR06 Open the pull request with the Relm PR template (Feature ID, contracts, analyze + converge evidence, scenario coverage)

---

## Dependencies & Execution Order

- Phase 1 → Phase 2 → US1 → US2 → US3 → Phase 6 → Phase R. US2 and US3 both extend US1's use case.
- Inventory is stubbed from the vendored contract, so this repo does not wait for inventory's implementation; staging integration needs inventory Phase 4 (commit) deployed.

## Implementation Strategy

- One `/speckit.implement` run per phase. Suggested PRs: Phases 1–2, Phase 3, Phases 4–5, Phase 6 + R.
