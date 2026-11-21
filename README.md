# orders-service

**Order placement for Relm Commerce.** Java 21, Spring Boot 3.4, Maven.

Owner: `@spec-kit-relm/backend` · Spec Kit project, kind **backend** (presets `relm-org` +
`relm-backend`, extensions `git`, `github`, `relm`) · Provider of **orders-api**, consumer of
**inventory-api**

## Interfaces

| Contract | Role | Version | Notes |
|----------|------|---------|-------|
| [`contracts/orders-api.yaml`](contracts/orders-api.yaml) | provides | 1.3.0 | 1.3.0 adds optional `reservationId` and `reservation-expired` (RELM-1) |
| [`contracts/vendor/inventory-api.yaml`](contracts/vendor/inventory-api.yaml) | consumes (read-only) | 1.1.0 | provenance in [`contracts/vendor/README.md`](contracts/vendor/README.md) |

## Run it

```bash
mvn -B -q test           # unit, MockMvc, ArchUnit
mvn spring-boot:run      # :8082
```

Today's code implements orders-api **1.2.0** with an in-memory store. RELM-1 adds reservations —
tasks ready, implementation not started.

## How we work in this repository — in order

1. **Rules**: [`.specify/memory/constitution.md`](.specify/memory/constitution.md) (ORG-*, BE-*,
   ORD-1 … ORD-5) and [`AGENTS.md`](AGENTS.md).
2. **Handoff**: `/speckit.relm.handoff RELM-<n>`.
3. **Specify + clarify** your slice: `/speckit.specify RELM-<n> <short-name>: …`, `/speckit.clarify`.
4. **Plan**: `/speckit.plan`. As a *consumer*, use only versions locked in the product feature's
   `contracts.lock.md` and vendor them into `contracts/vendor/`. As a *provider*, open a contract
   PR for `orders-api` first (Gate 2).
5. **Tasks + analyze**: `/speckit.tasks`, `/speckit.analyze` → 0 CRITICAL,
   `/speckit.github.taskstoissues`, plan PR (Gate 3).
6. **Implement** one phase per run, **converge**, `/speckit.relm.pr`.
7. **Release** in `impact.md` order (orders-service is usually after inventory-service).

## Current feature

| Feature | Directory | Role | State |
|---------|-----------|------|-------|
| RELM-1 Stock reservation at checkout | [`specs/001-stock-reservation/`](specs/001-stock-reservation/) | provider + consumer (2nd in rollout) | Approved · contracts locked · [tasks](specs/001-stock-reservation/tasks.md) ready — **next: `/speckit.implement Phase 1`** |

## Gates on every PR

`sdd / sdd-gates`, `contracts / contract-check` (incl. vendored provenance), `ci / test`, one
`@spec-kit-relm/backend` approval.
