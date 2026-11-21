# Quickstart: verify RELM-2 in orders-service

```bash
mvn -q test                                  # unit + ArchUnit + MockMvc
mvn -q verify -Pintegration                  # Testcontainers PostgreSQL + WireMock inventory (added by T003)
```

| Scenario | How | Expect |
|----------|-----|--------|
| RELM-2/US2/AS1 | `POST /v1/orders` with `reservationId`, inventory stub → 200 | 201; stub received exactly one commit with `orderId` |
| RELM-2/US2/AS2 | same request twice, same `Idempotency-Key` | same order ID twice; one commit call (or two identical idempotent calls) |
| RELM-2/US2/AS3 | inventory stub → 409 `reservation-expired` | 409 `https://errors.relm.example/orders/reservation-expired`; no row in `orders` |
| ORD/RELM-2/US2/AS2 | stub → 503 then 200, retry with same key | first 503, retry 201; one order |
