package org.example.rfid.config;

import java.util.Locale;

public enum Profile {
    LOW(1400, 700, 900, 450, 3, 0.35, 0.70, 0.08, 0.04, 0.96, 0.98, 0.02, 30, 2, 2),
    MEDIUM(900, 350, 450, 220, 4, 0.60, 0.70, 0.15, 0.08, 0.90, 0.94, 0.07, 45, 3, 3),
    HIGH(500, 180, 220, 120, 7, 0.90, 0.70, 0.25, 0.14, 0.82, 0.88, 0.15, 70, 4, 5);

    private final int cashierPeriodMs;
    private final int fittingPeriodMs;
    private final int gatePeriodMs;
    private final int paymentPeriodMs;
    private final int cashierBasketSize;
    private final double newProductRate;
    private final double fittingVisitRate;
    private final double postCashierFittingRate;
    private final double skipCashierRate;
    private final double paymentTransactionRate;
    private final double paymentItemRate;
    private final double unpaidExitRate;
    private final int maxActiveProducts;
    private final int scenarioRunnerThreadCount;
    private final int paymentMaxTransactionsPerTick;

    Profile(
            int cashierPeriodMs,
            int fittingPeriodMs,
            int gatePeriodMs,
            int paymentPeriodMs,
            int cashierBasketSize,
            double newProductRate,
            double fittingVisitRate,
            double postCashierFittingRate,
            double skipCashierRate,
            double paymentTransactionRate,
            double paymentItemRate,
            double unpaidExitRate,
            int maxActiveProducts,
            int scenarioRunnerThreadCount,
            int paymentMaxTransactionsPerTick
    ) {
        this.cashierPeriodMs = cashierPeriodMs;
        this.fittingPeriodMs = fittingPeriodMs;
        this.gatePeriodMs = gatePeriodMs;
        this.paymentPeriodMs = paymentPeriodMs;
        this.cashierBasketSize = cashierBasketSize;
        this.newProductRate = newProductRate;
        this.fittingVisitRate = fittingVisitRate;
        this.postCashierFittingRate = postCashierFittingRate;
        this.skipCashierRate = skipCashierRate;
        this.paymentTransactionRate = paymentTransactionRate;
        this.paymentItemRate = paymentItemRate;
        this.unpaidExitRate = unpaidExitRate;
        this.maxActiveProducts = maxActiveProducts;
        this.scenarioRunnerThreadCount = scenarioRunnerThreadCount;
        this.paymentMaxTransactionsPerTick = paymentMaxTransactionsPerTick;
    }

    public int cashierPeriodMs() {
        return cashierPeriodMs;
    }

    public int fittingPeriodMs() {
        return fittingPeriodMs;
    }

    public int gatePeriodMs() {
        return gatePeriodMs;
    }

    public int paymentPeriodMs() {
        return paymentPeriodMs;
    }

    public int cashierBasketSize() {
        return cashierBasketSize;
    }

    public double newProductRate() {
        return newProductRate;
    }

    public double fittingVisitRate() {
        return fittingVisitRate;
    }

    public double postCashierFittingRate() {
        return postCashierFittingRate;
    }

    public double skipCashierRate() {
        return skipCashierRate;
    }

    public double paymentTransactionRate() {
        return paymentTransactionRate;
    }

    public double paymentItemRate() {
        return paymentItemRate;
    }

    public double unpaidExitRate() {
        return unpaidExitRate;
    }

    public int maxActiveProducts() {
        return maxActiveProducts;
    }

    public int scenarioRunnerThreadCount() {
        return scenarioRunnerThreadCount;
    }

    public int paymentMaxTransactionsPerTick() {
        return paymentMaxTransactionsPerTick;
    }

    public static Profile fromArg(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return MEDIUM;
        }

        return switch (rawValue.toLowerCase(Locale.ROOT)) {
            case "low" -> LOW;
            case "medium" -> MEDIUM;
            case "high" -> HIGH;
            default -> throw new IllegalArgumentException("--profile must be one of: low, medium, high. Got: " + rawValue);
        };
    }
}
