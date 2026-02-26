package org.example.rfid.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArgsParserTest {

    @Test
    void containsHelpDetectsHelpFlag() {
        assertTrue(ArgsParser.containsHelp(new String[]{"--help"}));
        assertTrue(ArgsParser.containsHelp(new String[]{"--broker", "tcp://localhost:1883", "--help"}));
    }

    @Test
    void parseUsesDefaultsWhenNoArgsProvided() {
        ProducerConfig config = ArgsParser.parse(new String[]{});

        assertEquals(Profile.MEDIUM, config.profile());
        assertEquals("tcp://localhost:1883", config.brokerUrl());
        assertEquals(1, config.qos());
        assertEquals("tienda/lecturas/caja/CAJA-01", config.cashierTopic());
        assertEquals("tienda/lecturas/probador/PROBADOR-01", config.fittingTopic());
        assertEquals("tienda/lecturas/salida/GATE-01", config.gateTopic());
        assertEquals("tienda/pagos/confirmados", config.paymentTopic());
        assertEquals("CAJA-01", config.cashierReader());
        assertEquals("PROBADOR-01", config.fittingReader());
        assertEquals("GATE-01", config.gateReader());
        assertEquals(Duration.ofMillis(Profile.MEDIUM.cashierPeriodMs()), config.cashierPeriod());
    }

    @Test
    void parseAppliesExplicitOverrides() {
        ProducerConfig config = ArgsParser.parse(new String[]{
                "--profile", "low",
                "--broker", "tcp://broker:1883",
                "--qos", "2",
                "--cashier-topic", "c/topic",
                "--fitting-topic", "f/topic",
                "--gate-topic", "g/topic",
                "--payment-topic", "p/topic",
                "--cashier-reader", "C1",
                "--fitting-reader", "F1",
                "--gate-reader", "G1",
                "--cashier-basket-size", "10",
                "--new-product-rate", "0.40",
                "--fitting-visit-rate", "0.50",
                "--post-cashier-fitting-rate", "0.20",
                "--skip-cashier-rate", "0.10",
                "--payment-transaction-rate", "0.95",
                "--payment-item-rate", "0.90",
                "--unpaid-exit-rate", "0.08",
                "--max-active-products", "90",
                "--scenario-runner-thread-count", "5",
                "--payment-max-transactions-per-tick", "6",
                "--cashier-period-ms", "1200",
                "--fitting-period-ms", "800",
                "--gate-period-ms", "700",
                "--payment-period-ms", "600"
        });

        assertEquals(Profile.LOW, config.profile());
        assertEquals("tcp://broker:1883", config.brokerUrl());
        assertEquals(2, config.qos());
        assertEquals("c/topic", config.cashierTopic());
        assertEquals("f/topic", config.fittingTopic());
        assertEquals("g/topic", config.gateTopic());
        assertEquals("p/topic", config.paymentTopic());
        assertEquals("C1", config.cashierReader());
        assertEquals("F1", config.fittingReader());
        assertEquals("G1", config.gateReader());
        assertEquals(10, config.cashierBasketSize());
        assertEquals(0.40, config.newProductRate());
        assertEquals(0.50, config.fittingVisitRate());
        assertEquals(0.20, config.postCashierFittingRate());
        assertEquals(0.10, config.skipCashierRate());
        assertEquals(0.95, config.paymentTransactionRate());
        assertEquals(0.90, config.paymentItemRate());
        assertEquals(0.08, config.unpaidExitRate());
        assertEquals(90, config.maxActiveProducts());
        assertEquals(5, config.scenarioRunnerThreadCount());
        assertEquals(6, config.paymentMaxTransactionsPerTick());
        assertEquals(Duration.ofMillis(1200), config.cashierPeriod());
        assertEquals(Duration.ofMillis(800), config.fittingPeriod());
        assertEquals(Duration.ofMillis(700), config.gatePeriod());
        assertEquals(Duration.ofMillis(600), config.paymentPeriod());
    }

    @Test
    void parseRejectsUnknownOption() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ArgsParser.parse(new String[]{"--not-valid", "x"})
        );

        assertTrue(exception.getMessage().contains("Unknown option"));
    }

    @Test
    void parseRejectsDuplicateOption() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ArgsParser.parse(new String[]{"--qos", "1", "--qos", "2"})
        );

        assertTrue(exception.getMessage().contains("Duplicate option"));
    }

    @Test
    void parseRejectsMissingOptionValue() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ArgsParser.parse(new String[]{"--broker"})
        );

        assertTrue(exception.getMessage().contains("Missing value for --broker"));
    }

    @Test
    void parseRejectsOutOfRangeAndInvalidNumberValues() {
        IllegalArgumentException outOfRange = assertThrows(
                IllegalArgumentException.class,
                () -> ArgsParser.parse(new String[]{"--qos", "3"})
        );
        assertTrue(outOfRange.getMessage().contains("--qos must be between 0 and 2"));

        IllegalArgumentException invalidNumber = assertThrows(
                IllegalArgumentException.class,
                () -> ArgsParser.parse(new String[]{"--cashier-period-ms", "abc"})
        );
        assertTrue(invalidNumber.getMessage().contains("Invalid value for --cashier-period-ms"));
    }

    @Test
    void parseRejectsUnexpectedPositionalArguments() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> ArgsParser.parse(new String[]{"oops"})
        );

        assertTrue(exception.getMessage().contains("Unexpected argument"));
    }
}
