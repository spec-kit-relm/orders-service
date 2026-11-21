package com.relm.orders.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A confirmed order. Domain types have no framework imports (ORD-1). */
public record Order(UUID id, UUID cartId, UUID reservationId, List<Line> lines, Money total, Instant createdAt) {

    public record Line(String sku, int quantity, Money unitPrice) {}

    public static Order place(UUID cartId, UUID reservationId, List<Line> lines, Instant now) {
        if (lines == null || lines.isEmpty()) throw new IllegalArgumentException("order needs at least one line");
        Money total = lines.stream()
                .map(l -> l.unitPrice().times(l.quantity()))
                .reduce(Money::plus)
                .orElseThrow();
        return new Order(UUID.randomUUID(), cartId, reservationId, List.copyOf(lines), total, now);
    }
}
