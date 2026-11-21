package com.relm.orders.api;

import com.relm.orders.domain.Money;
import com.relm.orders.domain.Order;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * HTTP adapter for orders-api 1.2.0 (in-memory store for the baseline).
 * RELM-2 (orders-api 1.3.1) adds reservationId + inventory commit — see specs/002-stock-reservation.
 */
@RestController
@RequestMapping("/v1/orders")
class OrdersController {

    static final String PROBLEM_BASE = "https://errors.relm.example/orders/";

    private final Map<UUID, Order> orders = new ConcurrentHashMap<>();
    private final Map<UUID, Replay> idempotency = new ConcurrentHashMap<>();
    private final Clock clock = Clock.systemUTC();

    record LineRequest(@NotNull String sku, @Min(1) int quantity, @NotNull @Valid MoneyDto unitPrice) {}
    record MoneyDto(@Min(0) long amountMinor, @NotNull String currency) {}
    record PlaceOrderRequest(@NotNull UUID cartId, UUID reservationId, @NotEmpty List<@Valid LineRequest> lines) {}
    private record Replay(PlaceOrderRequest request, Order order) {}

    @PostMapping
    ResponseEntity<?> place(@RequestHeader("Idempotency-Key") UUID key, @Valid @RequestBody PlaceOrderRequest req) {
        Replay prior = idempotency.get(key);
        if (prior != null) {
            if (!prior.request().equals(req)) {
                return problem(HttpStatus.UNPROCESSABLE_ENTITY, "idempotency-key-reused",
                        "Idempotency-Key was used with a different request");
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(prior.order()); // BE-2 replay
        }
        List<Order.Line> lines = req.lines().stream()
                .map(l -> new Order.Line(l.sku(), l.quantity(), new Money(l.unitPrice().amountMinor(), l.unitPrice().currency())))
                .toList();
        Order order = Order.place(req.cartId(), req.reservationId(), lines, clock.instant());
        Replay winner = idempotency.computeIfAbsent(key, k -> new Replay(req, order));
        orders.putIfAbsent(winner.order().id(), winner.order());
        return ResponseEntity.status(HttpStatus.CREATED).body(winner.order());
    }

    @GetMapping("/{orderId}")
    ResponseEntity<?> get(@PathVariable UUID orderId) {
        Order o = orders.get(orderId);
        return o == null ? problem(HttpStatus.NOT_FOUND, "not-found", "Order not found") : ResponseEntity.ok(o);
    }

    static ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String title) {
        ProblemDetail p = ProblemDetail.forStatus(status);
        p.setType(URI.create(PROBLEM_BASE + code));
        p.setTitle(title);
        return ResponseEntity.status(status).body(p);
    }
}
