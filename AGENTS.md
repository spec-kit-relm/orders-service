# AGENTS.md — orders-service

Java/Spring Boot service that places and reads orders for Relm Commerce. Owned by
`@spec-kit-relm/backend`. **Provider** of `contracts/orders-api.yaml`; **consumer** of
inventory-api through the read-only copy in `contracts/vendor/`.
Rules that MUST hold are in `.specify/memory/constitution.md` (ORG-*, BE-*, ORD-1 … ORD-5).

## Commands
| Purpose | Command |
|---------|---------|
| Build + unit/architecture tests | `mvn -B -q test` |
| Integration tests (Testcontainers) | `mvn -B -q verify -Pintegration` *(added by RELM-1 T003)* |
| Generate inventory client | `mvn -q generate-sources -Pinventory-client` *(added by RELM-1 T002)* |
| Contract lint | `npx @redocly/cli@1 lint --config ../speckit-platform/contracts/redocly.yaml contracts/orders-api.yaml` |
| Run locally | `mvn spring-boot:run` (port 8082) |

Run `mvn -B -q test` before marking any task `[X]`.

## Layout
```text
src/main/java/com/relm/orders/
  domain/        records + rules, no Spring/Jakarta imports (ORD-1, ORD-4)
  application/   use cases + ports                       (introduced by RELM-1)
  adapters/      inventory client, persistence           (introduced by RELM-1)
  api/           HTTP controllers, problem mapping
src/main/resources/db/migration/   Flyway (ORD-2)
contracts/orders-api.yaml          AUTHORITATIVE
contracts/vendor/                  READ-ONLY pinned copies + provenance README
specs/NNN-*/                       Spec Kit features
```

## Spec Kit in this repository
- Kind: `backend` — presets `relm-org` + `relm-backend`; extensions `git`, `github`, `relm`.
- `/speckit.specify RELM-<n> …` → `specs/<NNN>-<short-name>/`, branch `<NNN>-<short-name>`.
- Start a cross-repo slice with `/speckit.relm.handoff RELM-<n>` (reads `../product-specs`).
- One phase per `/speckit.implement` run; then `/speckit.converge` and `/speckit.relm.pr`.
- Current feature: **RELM-1** `specs/001-stock-reservation/` — tasks ready, start with Phase 1.

## Conventions
- Controllers are thin: map HTTP ⇄ use case; no business rules in `api/`.
- Problems: `ProblemDetail` with `type` = `https://errors.relm.example/orders/<code>`.
- Tests: JUnit 5 `@DisplayName` starts with the scenario ID when verifying one,
  e.g. `@DisplayName("RELM-1/US2/AS2 double submit yields one order")`.
- Money: `domain.Money` (long minor units + currency). Never `double`.
- Outbound calls: always through a port + adapter with explicit timeout (ORD-5, BE-5).

## Do not
- Edit `contracts/vendor/**` or generated sources.
- Put Spring annotations in `domain/`.
- Retry non-idempotent calls.
- Edit Parts A/B of the constitution.

## Gotchas
- The baseline keeps idempotency in memory (`OrdersController`); RELM-1 T008 moves it to PostgreSQL —
  keep `OrdersApiTest` green while doing it.
- Inventory's commit `Idempotency-Key` is the **order ID**, not the shopper's key.
