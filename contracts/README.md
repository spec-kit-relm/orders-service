# orders-service contracts

| File | Role | Version | Tag / source |
|------|------|---------|--------------|
| `orders-api.yaml` | **AUTHORITATIVE** (we provide it) | 1.3.0 | `orders-api-v1.3.0` |
| `vendor/inventory-api.yaml` | read-only pinned copy (we consume it) | 1.1.0 | see `vendor/README.md` |

## Changelog — orders-api
- **1.3.0** (RELM-1): optional `reservationId` on `POST /v1/orders`; 409 `reservation-expired`.
- **1.2.0**: `Idempotency-Key` required on `POST /v1/orders`.
- **1.1.0**: `GET /v1/orders/{orderId}`.

Releasing a version: see `inventory-service/contracts/README.md` — same procedure
(contract PR → consumers review → tag `orders-api-v<version>` → tech lead updates the registry).
