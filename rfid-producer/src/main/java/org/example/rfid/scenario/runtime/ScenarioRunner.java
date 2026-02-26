package org.example.rfid.scenario.runtime;

import org.example.rfid.model.RfidEvent;
import org.example.rfid.mqtt.MqttPublisher;
import org.example.rfid.scenario.contracts.Scenario;
import org.example.rfid.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class ScenarioRunner implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ScenarioRunner.class);

    private final ScheduledExecutorService scheduler;
    private final MqttPublisher publisher;

    public ScenarioRunner(MqttPublisher publisher, int threadCount) {
        if (threadCount <= 0) {
            throw new IllegalArgumentException("threadCount must be > 0");
        }

        AtomicInteger seq = new AtomicInteger(1);
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("scenario-runner-" + seq.getAndIncrement());
            thread.setDaemon(false);
            return thread;
        };

        this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
        this.scheduler = Executors.newScheduledThreadPool(threadCount, threadFactory);
    }

    ScenarioRunner(MqttPublisher publisher, ScheduledExecutorService scheduler) {
        this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler must not be null");
    }

    public void runAtFixedRate(Scenario scenario, String topic, Duration period) {
        Objects.requireNonNull(scenario, "scenario must not be null");
        runAtFixedRate(
                scenario.name(),
                topic,
                period,
                () -> {
                    List<RfidEvent> events = scenario.nextEvents();
                    if (events == null || events.isEmpty()) {
                        return List.of();
                    }

                    return events.stream()
                            .map(Json::toUtf8Bytes)
                            .toList();
                }
        );
    }

    public void runAtFixedRate(String scenarioName, String topic, Duration period, Supplier<List<byte[]>> payloadSupplier) {
        Objects.requireNonNull(scenarioName, "scenarioName must not be null");
        Objects.requireNonNull(topic, "topic must not be null");
        Objects.requireNonNull(period, "period must not be null");
        Objects.requireNonNull(payloadSupplier, "payloadSupplier must not be null");

        long periodMs = period.toMillis();
        if (periodMs <= 0) {
            throw new IllegalArgumentException("period must be > 0");
        }

        log.info("Starting scenario={} topic={} periodMs={}", scenarioName, topic, periodMs);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<byte[]> payloads = payloadSupplier.get();
                if (payloads == null || payloads.isEmpty()) {
                    return;
                }
                for (byte[] payload : payloads) {
                    publisher.publish(topic, payload);
                }
            } catch (Exception e) {
                log.warn("Scenario tick failed scenario={} error={}", scenarioName, e.toString());
            }
        }, 0, periodMs, TimeUnit.MILLISECONDS);
    }

    public void runAtFixedRate(String scenarioName, Duration period, Runnable tick) {
        Objects.requireNonNull(scenarioName, "scenarioName must not be null");
        Objects.requireNonNull(period, "period must not be null");
        Objects.requireNonNull(tick, "tick must not be null");

        long periodMs = period.toMillis();
        if (periodMs <= 0) {
            throw new IllegalArgumentException("period must be > 0");
        }

        log.info("Starting scenario={} periodMs={}", scenarioName, periodMs);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                tick.run();
            } catch (Exception e) {
                log.warn("Scenario tick failed scenario={} error={}", scenarioName, e.toString());
            }
        }, 0, periodMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Scenario scheduler did not terminate in time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
