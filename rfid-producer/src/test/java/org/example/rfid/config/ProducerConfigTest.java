package org.example.rfid.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProducerConfigTest {

    @Test
    void constructorAcceptsValidValues() {
        ProducerConfig config = validConfig(
                4,
                0.6,
                45,
                3,
                3
        );

        assertEquals(Profile.MEDIUM, config.profile());
        assertEquals(4, config.cashierBasketSize());
        assertEquals(0.6, config.newProductRate());
        assertEquals(45, config.maxActiveProducts());
        assertEquals(3, config.scenarioRunnerThreadCount());
        assertEquals(3, config.paymentMaxTransactionsPerTick());
    }

    @Test
    void constructorRejectsRateOutsideBounds() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validConfig(4, 1.1, 45, 3, 3)
        );

        assertEquals("newProductRate must be between 0.0 and 1.0, got 1.1", exception.getMessage());
    }

    @Test
    void constructorRejectsInvalidCountFields() {
        assertEquals(
                "cashierBasketSize must be > 0",
                assertThrows(IllegalArgumentException.class, () -> validConfig(0, 0.6, 45, 3, 3)).getMessage()
        );
        assertEquals(
                "maxActiveProducts must be > 0",
                assertThrows(IllegalArgumentException.class, () -> validConfig(4, 0.6, 0, 3, 3)).getMessage()
        );
        assertEquals(
                "scenarioRunnerThreadCount must be > 0",
                assertThrows(IllegalArgumentException.class, () -> validConfig(4, 0.6, 45, 0, 3)).getMessage()
        );
        assertEquals(
                "paymentMaxTransactionsPerTick must be > 0",
                assertThrows(IllegalArgumentException.class, () -> validConfig(4, 0.6, 45, 3, 0)).getMessage()
        );
    }

    @Test
    void constructorRejectsNullRequiredFields() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ProducerConfig(
                        null,
                        "tcp://localhost:1883",
                        1,
                        "cashier/topic",
                        "fitting/topic",
                        "gate/topic",
                        "payment/topic",
                        "CAJA-01",
                        "PROBADOR-01",
                        "GATE-01",
                        4,
                        0.6,
                        0.7,
                        0.15,
                        0.08,
                        0.9,
                        0.94,
                        0.07,
                        45,
                        3,
                        3,
                        Duration.ofMillis(900),
                        Duration.ofMillis(350),
                        Duration.ofMillis(450),
                        Duration.ofMillis(220)
                )
        );

        assertEquals("profile must not be null", exception.getMessage());
    }

    private static ProducerConfig validConfig(
            int cashierBasketSize,
            double newProductRate,
            int maxActiveProducts,
            int scenarioRunnerThreadCount,
            int paymentMaxTransactionsPerTick
    ) {
        return new ProducerConfig(
                Profile.MEDIUM,
                "tcp://localhost:1883",
                1,
                "cashier/topic",
                "fitting/topic",
                "gate/topic",
                "payment/topic",
                "CAJA-01",
                "PROBADOR-01",
                "GATE-01",
                cashierBasketSize,
                newProductRate,
                0.7,
                0.15,
                0.08,
                0.9,
                0.94,
                0.07,
                maxActiveProducts,
                scenarioRunnerThreadCount,
                paymentMaxTransactionsPerTick,
                Duration.ofMillis(900),
                Duration.ofMillis(350),
                Duration.ofMillis(450),
                Duration.ofMillis(220)
        );
    }
}