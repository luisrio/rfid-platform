package org.example.rfid.config;

import java.time.Duration;
import java.util.Objects;

public record ProducerConfig(
        Profile profile,
        String brokerUrl,
        int qos,
        String cashierTopic,
        String fittingTopic,
        String gateTopic,
        String paymentTopic,
        String cashierReader,
        String fittingReader,
        String gateReader,
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
        int paymentMaxTransactionsPerTick,
        Duration cashierPeriod,
        Duration fittingPeriod,
        Duration gatePeriod,
        Duration paymentPeriod
) {
    public ProducerConfig {
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(brokerUrl, "brokerUrl must not be null");
        Objects.requireNonNull(cashierTopic, "cashierTopic must not be null");
        Objects.requireNonNull(fittingTopic, "fittingTopic must not be null");
        Objects.requireNonNull(gateTopic, "gateTopic must not be null");
        Objects.requireNonNull(paymentTopic, "paymentTopic must not be null");
        Objects.requireNonNull(cashierReader, "cashierReader must not be null");
        Objects.requireNonNull(fittingReader, "fittingReader must not be null");
        Objects.requireNonNull(gateReader, "gateReader must not be null");
        Objects.requireNonNull(cashierPeriod, "cashierPeriod must not be null");
        Objects.requireNonNull(fittingPeriod, "fittingPeriod must not be null");
        Objects.requireNonNull(gatePeriod, "gatePeriod must not be null");
        Objects.requireNonNull(paymentPeriod, "paymentPeriod must not be null");
        requireRate(newProductRate, "newProductRate");
        requireRate(fittingVisitRate, "fittingVisitRate");
        requireRate(postCashierFittingRate, "postCashierFittingRate");
        requireRate(skipCashierRate, "skipCashierRate");
        requireRate(paymentTransactionRate, "paymentTransactionRate");
        requireRate(paymentItemRate, "paymentItemRate");
        requireRate(unpaidExitRate, "unpaidExitRate");
        if (cashierBasketSize < 1) {
            throw new IllegalArgumentException("cashierBasketSize must be > 0");
        }
        if (maxActiveProducts < 1) {
            throw new IllegalArgumentException("maxActiveProducts must be > 0");
        }
        if (scenarioRunnerThreadCount < 1) {
            throw new IllegalArgumentException("scenarioRunnerThreadCount must be > 0");
        }
        if (paymentMaxTransactionsPerTick < 1) {
            throw new IllegalArgumentException("paymentMaxTransactionsPerTick must be > 0");
        }
    }

    private static void requireRate(double value, String name) {
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(name + " must be between 0.0 and 1.0, got " + value);
        }
    }
}
