package org.example.rfid.scenario.payment;

import java.util.Objects;

public final class PaymentScenario {

    private final PaymentProcessor paymentProcessor;
    private final int maxTransactionsPerTick;

    public PaymentScenario(PaymentProcessor paymentProcessor, int maxTransactionsPerTick) {
        this.paymentProcessor = Objects.requireNonNull(paymentProcessor, "paymentProcessor must not be null");
        if (maxTransactionsPerTick <= 0) {
            throw new IllegalArgumentException("maxTransactionsPerTick must be > 0");
        }
        this.maxTransactionsPerTick = maxTransactionsPerTick;
    }

    public String name() {
        return "payment";
    }

    public void tick() {
        paymentProcessor.tick(maxTransactionsPerTick);
    }
}
