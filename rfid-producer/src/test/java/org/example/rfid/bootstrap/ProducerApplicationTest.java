package org.example.rfid.bootstrap;

import org.example.rfid.config.Profile;
import org.example.rfid.mqtt.MqttPublisher;
import org.example.rfid.scenario.contracts.Scenario;
import org.example.rfid.scenario.runtime.ScenarioRunner;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mockConstruction;

class ProducerApplicationTest {

    @Test
    void runStartsWithoutBlockingWhenAwaitSignalIsInjected() {
        AtomicInteger shutdownHookRegistrations = new AtomicInteger(0);
        ProducerApplication application = new ProducerApplication(
                thread -> shutdownHookRegistrations.incrementAndGet(),
                () -> {
                }
        );

        try (MockedConstruction<MqttPublisher> publisherConstruction = mockConstruction(MqttPublisher.class);
             MockedConstruction<ScenarioRunner> runnerConstruction = mockConstruction(ScenarioRunner.class)) {

            application.run(new String[]{});

            assertEquals(1, publisherConstruction.constructed().size());
            assertEquals(1, runnerConstruction.constructed().size());
            assertEquals(1, shutdownHookRegistrations.get());

            MqttPublisher publisher = publisherConstruction.constructed().get(0);
            ScenarioRunner runner = runnerConstruction.constructed().get(0);

            verify(publisher, times(1)).connect();
            verify(runner, times(1)).runAtFixedRate(
                    argThat((Scenario scenario) -> "cashier-burst".equals(scenario.name())),
                    eq("tienda/lecturas/caja/CAJA-01"),
                    eq(Duration.ofMillis(Profile.MEDIUM.cashierPeriodMs()))
            );
            verify(runner, times(1)).runAtFixedRate(
                    argThat((Scenario scenario) -> "fitting-room".equals(scenario.name())),
                    eq("tienda/lecturas/probador/PROBADOR-01"),
                    eq(Duration.ofMillis(Profile.MEDIUM.fittingPeriodMs()))
            );
            verify(runner, times(1)).runAtFixedRate(
                    argThat((Scenario scenario) -> "exit-gate".equals(scenario.name())),
                    eq("tienda/lecturas/salida/GATE-01"),
                    eq(Duration.ofMillis(Profile.MEDIUM.gatePeriodMs()))
            );
            verify(runner, times(1)).runAtFixedRate(
                    eq("payment"),
                    eq(Duration.ofMillis(Profile.MEDIUM.paymentPeriodMs())),
                    argThat((Runnable tick) -> tick != null)
            );
            verify(runner, never()).close();
            verify(publisher, never()).close();
        }
    }

    @Test
    void runClosesPublisherWhenConnectFails() {
        ProducerApplication application = new ProducerApplication(thread -> {}, () -> {
        });

        try (MockedConstruction<MqttPublisher> publisherConstruction = mockConstruction(
                MqttPublisher.class,
                (mock, context) -> doThrow(new IllegalStateException("boom")).when(mock).connect()
        );
             MockedConstruction<ScenarioRunner> runnerConstruction = mockConstruction(ScenarioRunner.class)) {

            application.run(new String[]{});

            assertEquals(1, publisherConstruction.constructed().size());
            assertEquals(0, runnerConstruction.constructed().size());

            MqttPublisher publisher = publisherConstruction.constructed().get(0);
            verify(publisher, times(1)).connect();
            verify(publisher, times(1)).close();
        }
    }

    @Test
    void runClosesRunnerAndPublisherWhenSchedulingFailsDuringStartup() {
        ProducerApplication application = new ProducerApplication(thread -> {}, () -> {
        });

        try (MockedConstruction<MqttPublisher> publisherConstruction = mockConstruction(MqttPublisher.class);
             MockedConstruction<ScenarioRunner> runnerConstruction = mockConstruction(
                     ScenarioRunner.class,
                     (mock, context) -> doThrow(new RuntimeException("schedule failure")).when(mock)
                             .runAtFixedRate(
                                     argThat((Scenario scenario) -> "cashier-burst".equals(scenario.name())),
                                     eq("tienda/lecturas/caja/CAJA-01"),
                                     eq(Duration.ofMillis(Profile.MEDIUM.cashierPeriodMs()))
                             )
             )) {

            application.run(new String[]{});

            assertEquals(1, publisherConstruction.constructed().size());
            assertEquals(1, runnerConstruction.constructed().size());

            MqttPublisher publisher = publisherConstruction.constructed().get(0);
            ScenarioRunner runner = runnerConstruction.constructed().get(0);

            verify(runner, times(1)).close();
            verify(publisher, times(1)).close();
            verify(runner, never()).runAtFixedRate(
                    eq("payment"),
                    eq(Duration.ofMillis(Profile.MEDIUM.paymentPeriodMs())),
                    argThat((Runnable tick) -> tick != null)
            );
        }
    }
}
