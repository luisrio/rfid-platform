package org.example.rfid.scenario.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.rfid.model.RfidEvent;
import org.example.rfid.mqtt.MqttPublisher;
import org.example.rfid.scenario.contracts.Scenario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScenarioRunnerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private MqttPublisher publisher;

    @Mock
    private ScheduledExecutorService scheduler;

    @Test
    void constructorsRejectInvalidArguments() {
        assertEquals(
                "threadCount must be > 0",
                assertThrows(IllegalArgumentException.class, () -> new ScenarioRunner(publisher, 0)).getMessage()
        );
        assertEquals(
                "publisher must not be null",
                assertThrows(NullPointerException.class, () -> new ScenarioRunner(null, scheduler)).getMessage()
        );
        assertEquals(
                "scheduler must not be null",
                assertThrows(NullPointerException.class, () -> new ScenarioRunner(publisher, (ScheduledExecutorService) null)).getMessage()
        );
    }

    @Test
    void runAtFixedRateScenarioPublishesSerializedEvents() throws Exception {
        ScenarioRunner runner = new ScenarioRunner(publisher, scheduler);
        Scenario scenario = new Scenario() {
            @Override
            public String name() {
                return "cashier-burst";
            }

            @Override
            public List<RfidEvent> nextEvents() {
                return List.of(
                        new RfidEvent("EPC-1", "CAJA-01", -50, 1000L),
                        new RfidEvent("EPC-2", "CAJA-01", -55, 1001L)
                );
            }
        };

        runner.runAtFixedRate(scenario, "topic/cashier", Duration.ofMillis(100));

        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).scheduleAtFixedRate(taskCaptor.capture(), eq(0L), eq(100L), eq(TimeUnit.MILLISECONDS));

        taskCaptor.getValue().run();

        ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(publisher, times(2)).publish(eq("topic/cashier"), payloadCaptor.capture());

        List<byte[]> payloads = payloadCaptor.getAllValues();
        Map<?, ?> first = MAPPER.readValue(payloads.get(0), Map.class);
        Map<?, ?> second = MAPPER.readValue(payloads.get(1), Map.class);

        assertEquals("EPC-1", first.get("epc"));
        assertEquals("CAJA-01", first.get("readerId"));
        assertEquals(-50, ((Number) first.get("rssi")).intValue());
        assertEquals(1000, ((Number) first.get("timestamp")).intValue());

        assertEquals("EPC-2", second.get("epc"));
        assertEquals("CAJA-01", second.get("readerId"));
        assertEquals(-55, ((Number) second.get("rssi")).intValue());
        assertEquals(1001, ((Number) second.get("timestamp")).intValue());
    }

    @Test
    void runAtFixedRateScenarioSkipsPublishWhenNoEvents() {
        ScenarioRunner runner = new ScenarioRunner(publisher, scheduler);
        Scenario scenario = new Scenario() {
            @Override
            public String name() {
                return "fitting-room";
            }

            @Override
            public List<RfidEvent> nextEvents() {
                return List.of();
            }
        };

        runner.runAtFixedRate(scenario, "topic/fitting", Duration.ofMillis(200));

        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).scheduleAtFixedRate(taskCaptor.capture(), eq(0L), eq(200L), eq(TimeUnit.MILLISECONDS));

        taskCaptor.getValue().run();

        verifyNoInteractions(publisher);
    }

    @Test
    void runAtFixedRatePayloadSupplierCatchesException() {
        ScenarioRunner runner = new ScenarioRunner(publisher, scheduler);

        runner.runAtFixedRate(
                "exit-gate",
                "topic/exit",
                Duration.ofMillis(150),
                () -> {
                    throw new IllegalStateException("boom");
                }
        );

        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).scheduleAtFixedRate(taskCaptor.capture(), eq(0L), eq(150L), eq(TimeUnit.MILLISECONDS));

        taskCaptor.getValue().run();

        verifyNoInteractions(publisher);
    }

    @Test
    void runAtFixedRateTickDelegatesAndCatchesException() {
        ScenarioRunner runner = new ScenarioRunner(publisher, scheduler);
        AtomicInteger tickCalls = new AtomicInteger(0);

        runner.runAtFixedRate("payment", Duration.ofMillis(220), () -> {
            tickCalls.incrementAndGet();
            throw new RuntimeException("tick failed");
        });

        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).scheduleAtFixedRate(taskCaptor.capture(), eq(0L), eq(220L), eq(TimeUnit.MILLISECONDS));

        taskCaptor.getValue().run();

        assertEquals(1, tickCalls.get());
        verifyNoInteractions(publisher);
    }

    @Test
    void closeShutsDownAndWaitsForTermination() throws Exception {
        ScenarioRunner runner = new ScenarioRunner(publisher, scheduler);
        when(scheduler.awaitTermination(5, TimeUnit.SECONDS)).thenReturn(true);

        runner.close();

        verify(scheduler, times(1)).shutdownNow();
        verify(scheduler, times(1)).awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void closeRestoresInterruptFlagWhenAwaitTerminationIsInterrupted() throws Exception {
        ScenarioRunner runner = new ScenarioRunner(publisher, scheduler);
        when(scheduler.awaitTermination(5, TimeUnit.SECONDS)).thenThrow(new InterruptedException("interrupted"));

        runner.close();

        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted();
    }
}
