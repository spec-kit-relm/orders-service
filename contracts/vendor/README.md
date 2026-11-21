# Vendored contracts (READ-ONLY)

Pinned copies of contracts owned by other repositories (ORG-3). Never edit these files: change
the provider's contract, release a version, then re-vendor. `contracts / contract-check` fails if a
file here changes without this README being updated.

| File | Provider | Version | Source tag | Source commit | Vendored on | Locked by |
|------|----------|---------|------------|---------------|-------------|-----------|
| `inventory-api.yaml` | spec-kit-relm/inventory-service | 1.1.0 | `inventory-api-v1.1.0` | (tag commit) | 2026-11-19 | RELM-1 `contracts.lock.md` |

## Sync procedure

```bash
gh api "repos/spec-kit-relm/inventory-service/contents/contracts/inventory-api.yaml?ref=inventory-api-v1.1.0" \
  -H "Accept: application/vnd.github.raw" > contracts/vendor/inventory-api.yaml
# update the table above (version, tag, commit, date), then regenerate the client:
mvn -q generate-sources -Pinventory-client   # profile added by RELM-1 T002
```
