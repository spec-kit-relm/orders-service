<!--
Sync Impact Report
- Version change: template → 1.0.0 (initial ratification)
- Part A: RELM:ORG-BASELINE v1.0.0 (synced, unchanged)
- Part B: RELM:TEAM-BACKEND v1.0.0 (synced, unchanged)
- Part C: added ORD-1, ORD-2, ORD-3, ORD-4, ORD-5
- Templates: provided by presets relm-org + relm-backend (runtime resolution, no file edits)
- Deferred TODOs: none
-->
# orders-service Constitution

> **How this constitution is layered (Relm SDD).**
> Part A is the organization baseline and Part B is the team overlay. Both are synced from
> `spec-kit-relm/speckit-platform` and MUST NOT be edited in this repository. Part C holds this
> repository's own amendments: they may add or tighten rules, never relax Parts A or B.
> Every Spec Kit command reads this file; `/speckit.analyze` treats any MUST violation as CRITICAL.

<!-- BEGIN RELM:ORG-BASELINE v1.0.0 | source: spec-kit-relm/speckit-platform/presets/relm-org/templates/constitution-template.md | DO NOT EDIT OUTSIDE speckit-platform -->
## Part A — Relm Organization Baseline (MUST)

These principles apply to every Relm repository. They are deliberately few and every one of
them names the check that enforces it. Lower layers (team overlay, repository amendments) may
**add or tighten** rules, never relax or contradict them.

### ORG-1 Spec before code
Any change that alters user-visible behavior, a public contract, or persisted data MUST trace to
a Spec Kit feature (`Feature: RELM-<n>`) or to a declared lightweight lane (`lane:bugfix`,
`lane:chore`) on the pull request.
*Check: `sdd-gates / pr-check`.*

### ORG-2 Specs describe WHAT and WHY, never HOW
`spec.md` contains no frameworks, languages, or storage choices. Every user story has
Given/When/Then acceptance scenarios and measurable success criteria. No
`[NEEDS CLARIFICATION]` marker may remain once a spec's status is `Approved` or later.
*Check: `sdd-gates / spec-lint`, `/speckit.analyze`.*

### ORG-3 Contract-first integration
Every cross-repository interaction (HTTP, events, shared schemas) is defined in a versioned
contract (OpenAPI 3.1 or AsyncAPI 3) with exactly one owning (provider) repository. Consumers pin a
version and treat vendored copies as read-only. Breaking changes require a MAJOR version and the
expand → migrate → contract sequence.
*Check: `contract-check` workflow, consumer review on contract PRs.*

### ORG-4 Tests prove the spec
Each acceptance scenario maps to at least one automated test that carries its scenario ID
(e.g. `RELM-1/US1/AS2`). Every interface touched by a feature has provider verification and
consumer contract tests, including negative cases. Tests land in the same pull request as the
behavior they verify.
*Check: CI test jobs, `/speckit.converge`, QA review.*

### ORG-5 Secure and private by default
No secrets in code, specs, prompts, or logs. Every endpoint enforces authentication and
authorization. Personal data is classified in `data-model.md` and never logged or sent to analytics.
*Check: secret scanning, CodeQL, dependency review, PR review.*

### ORG-6 Observable by design
Services emit structured logs carrying a trace ID and RED metrics (rate, errors, duration) per
endpoint or consumer. Every feature states how each success criterion is measured in production.
*Check: plan review (`Observability` section), dashboards linked before release.*

### ORG-7 Safe, reversible delivery
New behavior ships behind a feature flag, is deployed in the dependency order defined in the
product feature's `impact.md`, and has a rollback path written in `plan.md`.
*Check: plan review, release-readiness gate.*

### ORG-8 Simplicity, justified complexity
No new service, datastore, message broker, or major dependency without an accepted ADR. Any
deviation from these principles is recorded in the plan's Complexity Tracking table.
*Check: `/speckit.analyze`, architecture review.*

### ORG-9 Humans own AI output
A named human owns every pull request and reviews AI-generated code to the same standard as
hand-written code. `/speckit.analyze` reports **0 CRITICAL** findings before `/speckit.implement`,
implementation runs are scoped to one phase at a time, and `/speckit.converge` reports converged
before merge.
*Check: PR template, CODEOWNERS, `sdd-gates / pr-check`.*

### Organization governance
- **Precedence**: Org baseline (Part A) > team overlay (Part B) > repository amendments (Part C).
  If an agent finds a conflict between layers it MUST stop and report it, not choose.
- **Amendments**: only by pull request to `spec-kit-relm/speckit-platform` (`governance/`), reviewed
  by the SDD Guild plus one approver per discipline. Versioned with semantic versioning
  (MAJOR = principle removed/redefined, MINOR = principle added/expanded, PATCH = wording).
- **Propagation**: the propagation bot opens a sync PR in every repository. Repositories MUST be on
  the same MAJOR and at most one MINOR behind; `sdd-gates / constitution-check` fails otherwise.
- **Exceptions**: listed in Part C → *Exceptions* with an ADR link, an owner, and an expiry date.
<!-- END RELM:ORG-BASELINE -->



<!-- BEGIN RELM:TEAM-BACKEND v1.0.0 | source: spec-kit-relm/speckit-platform/presets/relm-backend/templates/constitution-template.md | owner: @spec-kit-relm/backend | DO NOT EDIT OUTSIDE speckit-platform -->
## Part B — Backend Team Overlay (MUST)

Applies to every backend service owned by `@spec-kit-relm/backend`. Stack-specific rules
(language, framework, layout) belong in Part C of each repository.

### BE-1 Consistent HTTP APIs
Resources are plural nouns, errors use RFC 9457 `application/problem+json`, list endpoints use
cursor pagination, and every response carries `X-Request-Id`. The provider's OpenAPI file is the
source of truth; handlers are validated against it in tests.
*Check: contract lint (Redocly ruleset `relm-api`), provider verification tests.*

### BE-2 Idempotent writes
Every non-safe operation (`POST`, `PATCH`, `DELETE`, event handlers) is idempotent: HTTP writes
accept an `Idempotency-Key` header, event consumers de-duplicate by event ID. Retries are only
allowed on idempotent operations, with bounded exponential backoff and jitter.
*Check: contract tests replay every write twice; plan review.*

### BE-3 Backward-compatible data changes
Schema migrations are forward-only and backward-compatible with the previous release
(expand → migrate → contract). Destructive steps ship in a later release than the code that
stops using the column/table.
*Check: migration lint job, plan `Rollback` section.*

### BE-4 Reliable events
Events are published through a transactional outbox, carry `eventId`, `occurredAt`,
`schemaVersion`, and `traceId`, and are documented in the provider's AsyncAPI contract.
Consumers tolerate duplicates and out-of-order delivery.
*Check: AsyncAPI lint, consumer tests with duplicate/out-of-order fixtures.*

### BE-5 Resilience budgets
Every outbound call has an explicit timeout; plans state p99 latency and error-rate budgets per
endpoint; dependencies degrade gracefully (fallback or fast failure, never unbounded waits).
*Check: plan review (`Performance & Resilience` section), load test for P1 stories.*

### BE-6 Test pyramid
Domain logic has unit tests (≥ 80 % line coverage on the domain layer), adapters have
integration tests against real dependencies in containers, and every provided interface has
provider verification. Mocks of our own services are generated from pinned contracts only.
*Check: CI coverage gate, `/speckit.converge`.*
<!-- END RELM:TEAM-BACKEND -->

## Part C — Repository Amendments (MUST)

### ORD-1 Java 21 + Spring Boot 3.4, hexagonal layout
Java 21, Spring Boot 3.4, Maven. Packages: `domain` (records and rules, no Spring/Jakarta
imports), `application` (use cases, ports), `adapters` (inbound HTTP, outbound inventory client,
persistence), `api` (HTTP controllers). The domain never depends on adapters or frameworks.
*Check: `ArchitectureTest` (ArchUnit) in the unit test suite.*

### ORD-2 Schema changes through Flyway only
All schema changes are Flyway migrations `src/main/resources/db/migration/V<yyyymmddHHmm>__<what>.sql`,
forward-only and compatible with the previous release (tightens BE-3).
*Check: Flyway migration test with Testcontainers; PR review.*

### ORD-3 Provider of orders-api; every write is idempotent
This repository owns `contracts/orders-api.yaml`. `POST /v1/orders` requires `Idempotency-Key`
and persists the key with the order in one transaction, so a replay returns the same order
(tightens BE-2). Controllers are verified against the OpenAPI document in tests.
*Check: `OrdersApiTest`, provider verification `OrdersContractTest`, `contracts / contract-check`.*

### ORD-4 Money is never a floating-point number
Amounts are `long` minor units plus ISO 4217 currency (`domain.Money`). `double`, `float` and
`BigDecimal` arithmetic on amounts are forbidden outside formatting code.
*Check: ArchUnit rule on `domain`, PR review.*

### ORD-5 Inventory is consumed only through the pinned contract
Calls to inventory-service use a client generated from `contracts/vendor/inventory-api.yaml`
(read-only, version recorded in `contracts/vendor/README.md`), with a 300 ms timeout and at most
3 retries with jittered backoff on 503/timeouts — only for idempotent requests (tightens BE-5).
*Check: consumer contract tests `InventoryClientContractTest`, `contracts / contract-check` (vendor provenance).*

### Exceptions
None. <!-- Format: | Rule | Exception | ADR | Owner | Expires | -->

## Repository governance
- Part C is amended by pull request in this repository, approved by the CODEOWNERS of
  `.specify/memory/`, on a dedicated `governance/*` branch (never inside a feature branch).
- Parts A and B change only through the automated governance sync pull request.

**Version**: 1.0.0 | **Ratified**: 2026-11-21 | **Last Amended**: 2026-11-21
**Governance blocks**: RELM:ORG-BASELINE v1.0.0 · RELM:TEAM-BACKEND v1.0.0
