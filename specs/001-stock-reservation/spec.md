# Feature Specification: Stock reservation at checkout — orders slice

**Feature Branch**: `001-stock-reservation`

**Created**: 2026-11-19

**Status**: Approved

**Input**: User description: "RELM-1 stock-reservation: orders-service slice from the handoff brief — when an order is placed with a hold, convert the hold as part of placing the order; never create an order whose hold expired; repeated submissions create exactly one order."

## Clarifications

### Session 2026-11-19

- Q: Is the hold mandatory for every order? → A: No. Orders without a hold keep today's behavior so the storefront can roll out behind its flag (impact.md rollout step 2).
- Q: What if converting the hold fails temporarily? → A: The order is not created; the shopper may retry, and the retry must not create a second order or double-convert the hold.
- Q: What does the shopper see when the hold expired? → A: A specific "hold expired" outcome the storefront can show with a "start again" option.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Place an order that converts its hold (Priority: P1)

The storefront places an order with the cart's hold; the order is confirmed only if the hold is
converted.

**Why this priority**: Central US2/AS1 — the hold only matters if ordering uses it.

**Independent Test**: Place an order with an active hold (inventory stubbed from the pinned contract) and verify the order is confirmed and the hold conversion was requested exactly once.

**Acceptance Scenarios**:

1. **Given** an active hold for the cart, **When** the order is placed with it, **Then** the hold is converted for this order and the order is confirmed. *[RELM-1/US2/AS1]*
2. **Given** no hold in the request, **When** the order is placed, **Then** it behaves exactly as before this feature.

---

### User Story 2 - Repeated submissions create one order (Priority: P1)

The same order submitted more than once results in a single order.

**Why this priority**: Central US2/AS2 — duplicate orders are a top support complaint.

**Independent Test**: Submit the same request twice with the same idempotency key, concurrently and sequentially; verify one order and one conversion.

**Acceptance Scenarios**:

1. **Given** an order request with a hold, **When** it is submitted twice within seconds, **Then** exactly one order exists and both responses carry the same order. *[RELM-1/US2/AS2]*
2. **Given** a first attempt that failed because inventory was temporarily unavailable, **When** it is retried, **Then** at most one order is created and the hold is converted at most once.

---

### User Story 3 - Reject orders whose hold expired (Priority: P1)

Orders whose hold expired or was replaced are rejected with a specific outcome.

**Why this priority**: Central US2/AS3 — prevents overselling (SC-001).

**Independent Test**: Place an order whose hold conversion is rejected as expired; verify no order is stored and the specific outcome is returned.

**Acceptance Scenarios**:

1. **Given** an expired hold, **When** the order is placed, **Then** no order is created and the response says the hold expired. *[RELM-1/US2/AS3]*

---

### Edge Cases

- Inventory does not answer within the time budget: no order is created; the response says "try again"; a retry with the same key is safe.
- The conversion succeeds but storing the order fails: the retry with the same key finds the converted hold (conversion is idempotent per order) and completes the order.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: When a hold is given, the order MUST be confirmed only after the hold is converted for that order.
- **FR-002**: Orders without a hold MUST behave as before this feature.
- **FR-003**: The same request with the same idempotency key MUST yield the same single order.
- **FR-004**: An expired or replaced hold MUST result in no order and a specific "hold expired" outcome.
- **FR-005**: Temporary inventory failures MUST result in no order and a retry-safe "try again" outcome.

### Key Entities *(include if feature involves data)*

- **Order**: gains an optional reference to the hold it converted.
- **Idempotency record**: request fingerprint and resulting order per idempotency key.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Zero orders confirmed with an unconverted hold (central SC-001).
- **SC-002**: Zero duplicate orders from repeated submissions in the 30 days after rollout.
- **SC-003**: 99 % of order placements with a hold complete within 400 ms at the service boundary.

## Assumptions

- inventory-service is deployed with inventory-api 1.1.0 before this slice (rollout step 1).
- Payment authorization is out of scope for this slice (unchanged).

---

## Relm Traceability *(mandatory — ORG-1, PRD-1, QA-2)*

**Feature ID**: RELM-1
**Central spec**: [product-specs/specs/001-stock-reservation/spec.md](https://github.com/spec-kit-relm/product-specs/blob/main/specs/001-stock-reservation/spec.md)
**Repository role**: provider+consumer
**Contracts provided**: orders-api@1.3.0
**Contracts consumed**: inventory-api@1.1.0 (pinned in `contracts/vendor/`)
**Product stories covered**: RELM-1 US2 (AS1, AS2, AS3)

**Status lifecycle**: Draft → In Review → Approved → Implemented → Released

### Scenario IDs
Central scenario IDs in brackets. Repository-only scenarios (US1 AS2, US2 AS2) use
`ORD/RELM-1/US<k>/AS<m>` in test titles.
