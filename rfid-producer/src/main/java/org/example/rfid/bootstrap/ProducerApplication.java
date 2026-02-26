package org.example.rfid.bootstrap;

import org.example.rfid.config.ArgsParser;
import org.example.rfid.config.HelpPrinter;
import org.example.rfid.config.ProducerConfig;
import org.example.rfid.mqtt.MqttPublisher;
import org.example.rfid.scenario.contracts.ScenarioPlan;
import org.example.rfid.scenario.coordinator.ProductJourneyCoordinator;
import org.example.rfid.scenario.payment.MqttPaymentEventPublisher;
import org.example.rfid.scenario.payment.PaymentProcessor;
import org.example.rfid.scenario.payment.PaymentScenario;
import org.example.rfid.scenario.readers.CashierBurstScenario;
import org.example.rfid.scenario.readers.ExitGateScenario;
import org.example.rfid.scenario.readers.FittingRoomScenario;
import org.example.rfid.scenario.runtime.ScenarioRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class ProducerApplication {

    private static final Logger log = LoggerFactory.getLogger(ProducerApplication.class);
    private static final long PAYMENT_PUBLISH_CONFIRMATION_TIMEOUT_MS = 3_000L;
    private final Consumer<Thread> shutdownHookRegistrar;
    private final Runnable awaitSignal;

    public ProducerApplication() {
        this(Runtime.getRuntime()::addShutdownHook, ProducerApplication::sleepForever);
    }

    ProducerApplication(Consumer<Thread> shutdownHookRegistrar, Runnable awaitSignal) {
        this.shutdownHookRegistrar = Objects.requireNonNull(shutdownHookRegistrar, "shutdownHookRegistrar must not be null");
        this.awaitSignal = Objects.requireNonNull(awaitSignal, "awaitSignal must not be null");
    }

    public void run(String[] args) {
        if (ArgsParser.containsHelp(args)) {
            HelpPrinter.print();
            return;
        }

        ProducerConfig config;
        try {
            config = ArgsParser.parse(args);
        } catch (IllegalArgumentException e) {
            log.error("Invalid arguments: {}", e.getMessage());
            HelpPrinter.print();
            return;
        }

        MqttPublisher publisher = null;
        ScenarioRunner runner = null;
        boolean startupComplete = false;
        try {
            publisher = new MqttPublisher(config.brokerUrl(), "rfid-producer", config.qos());
            publisher.connect();

            runner = new ScenarioRunner(publisher, config.scenarioRunnerThreadCount());
            ProductJourneyCoordinator coordinator = new ProductJourneyCoordinator(
                    config.newProductRate(),
                    config.fittingVisitRate(),
                    config.postCashierFittingRate(),
                    config.skipCashierRate(),
                    config.paymentTransactionRate(),
                    config.paymentItemRate(),
                    config.unpaidExitRate(),
                    config.maxActiveProducts()
            );

            List<ScenarioPlan> plans = List.of(
                    new ScenarioPlan(
                            new CashierBurstScenario(config.cashierReader(), config.cashierBasketSize(), coordinator),
                            config.cashierTopic(),
                            config.cashierPeriod()
                    ),
                    new ScenarioPlan(
                            new FittingRoomScenario(config.fittingReader(), coordinator),
                            config.fittingTopic(),
                            config.fittingPeriod()
                    ),
                    new ScenarioPlan(
                            new ExitGateScenario(config.gateReader(), coordinator),
                            config.gateTopic(),
                            config.gatePeriod()
                    )
            );

            for (ScenarioPlan plan : plans) {
                runner.runAtFixedRate(plan.scenario(), plan.topic(), plan.period());
            }

            PaymentProcessor paymentProcessor = new PaymentProcessor(
                    coordinator,
                    new MqttPaymentEventPublisher(
                            config.paymentTopic(),
                            publisher,
                            PAYMENT_PUBLISH_CONFIRMATION_TIMEOUT_MS
                    ),
                    config.cashierReader()
            );
            PaymentScenario paymentScenario = new PaymentScenario(
                    paymentProcessor,
                    config.paymentMaxTransactionsPerTick()
            );
            runner.runAtFixedRate(paymentScenario.name(), config.paymentPeriod(), paymentScenario::tick);

            ScenarioRunner runnerToClose = runner;
            MqttPublisher publisherToClose = publisher;
            shutdownHookRegistrar.accept(new Thread(() -> {
                log.info("Shutting down...");
                closeResources(runnerToClose, publisherToClose);
            }));

            log.info("RFID Producer running.");
            log.info("Profile: {}", config.profile().name().toLowerCase());
            log.info("Broker: {}", config.brokerUrl());
            log.info("QoS: {}", config.qos());
            log.info("Scenario runner threads: {}", config.scenarioRunnerThreadCount());
            log.info("Payment max transactions per tick: {}", config.paymentMaxTransactionsPerTick());
            log.info(
                    "Journey ratios: newProductRate={} fittingVisitRate={} postCashierFittingRate={} skipCashierRate={} paymentTransactionRate={} paymentItemRate={} unpaidExitRate={} maxActiveProducts={}",
                    config.newProductRate(),
                    config.fittingVisitRate(),
                    config.postCashierFittingRate(),
                    config.skipCashierRate(),
                    config.paymentTransactionRate(),
                    config.paymentItemRate(),
                    config.unpaidExitRate(),
                    config.maxActiveProducts()
            );
            log.info("Topics:");
            log.info(" - {}", config.cashierTopic());
            log.info(" - {}", config.fittingTopic());
            log.info(" - {}", config.gateTopic());
            log.info(" - {}", config.paymentTopic());

            startupComplete = true;
            awaitSignal.run();
        } catch (Exception e) {
            log.error("Failed to start RFID Producer", e);
        } finally {
            if (!startupComplete) {
                closeResources(runner, publisher);
            }
        }
    }

    private static void sleepForever() {
        try {
            while (true) {
                Thread.sleep(60_000L);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void closeResources(ScenarioRunner runner, MqttPublisher publisher) {
        if (runner != null) {
            try {
                runner.close();
            } catch (Exception e) {
                log.warn("Error closing scenario runner: {}", e.toString());
            }
        }

        if (publisher != null) {
            try {
                publisher.close();
            } catch (Exception e) {
                log.warn("Error closing MQTT publisher: {}", e.toString());
            }
        }
    }
}
