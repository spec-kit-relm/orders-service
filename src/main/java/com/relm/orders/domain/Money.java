package com.relm.orders.domain;

import java.util.Objects;

/** Money as integer minor units + ISO 4217 currency (ORD-4). No floating point, ever. */
public record Money(long amountMinor, String currency) {
    public Money {
        if (amountMinor < 0) throw new IllegalArgumentException("amountMinor must be >= 0");
        Objects.requireNonNull(currency, "currency");
        if (!currency.matches("[A-Z]{3}")) throw new IllegalArgumentException("currency must be ISO 4217");
    }

    public Money times(int quantity) {
        return new Money(Math.multiplyExact(amountMinor, quantity), currency);
    }

    public Money plus(Money other) {
        if (!currency.equals(other.currency)) throw new IllegalArgumentException("currency mismatch");
        return new Money(Math.addExact(amountMinor, other.amountMinor), currency);
    }
}
