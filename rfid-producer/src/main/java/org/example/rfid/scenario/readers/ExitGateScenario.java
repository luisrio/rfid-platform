package org.example.rfid.scenario.readers;

import org.example.rfid.model.RfidEvent;
import org.example.rfid.scenario.contracts.Scenario;
import org.example.rfid.scenario.coordinator.ProductJourneyCoordinator;
import org.example.rfid.util.Time;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class ExitGateScenario implements Scenario {

    private final String readerId;
    private final ProductJourneyCoordinator coordinator;

    public ExitGateScenario(String readerId, ProductJourneyCoordinator coordinator) {
        this.readerId = requireNonBlank(readerId, "readerId must not be blank");
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator must not be null");
    }

    @Override
    public String name() {
        return "exit-gate";
    }

    @Override
    public List<RfidEvent> nextEvents() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<String> epcsAtGate = coordinator.nextForExit(1 + random.nextInt(3));
        if (epcsAtGate.isEmpty()) {
            return List.of();
        }

        int estimatedReads = epcsAtGate.size() * 2;
        List<RfidEvent> events = new ArrayList<>(estimatedReads);
        long now = Time.nowMillis();

        for (String epc : epcsAtGate) {
            int reads = 1 + random.nextInt(4);
            for (int i = 0; i < reads; i++) {
                int rssi = computeGateRssi(random);
                events.add(new RfidEvent(epc, readerId, rssi, now));
            }
        }

        return events;
    }

    private static int computeGateRssi(ThreadLocalRandom random) {
        // Exit gate usually sees stronger readings than fitting room.
        return -68 + random.nextInt(31); // -68..-38
    }

    private static String requireNonBlank(String value, String message) {
        Objects.requireNonNull(value, message);
        if (value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
