---
applyTo: "contracts/**"
---
- `contracts/orders-api.yaml` is AUTHORITATIVE (owned here, ORD-3). Any change bumps `info.version`
  and goes in a separate contract PR reviewed by web-storefront and e2e-tests owners.
- `contracts/vendor/**` is READ-ONLY (ORD-5). Never edit; re-vendor from the provider's tag and
  update `contracts/vendor/README.md`.
