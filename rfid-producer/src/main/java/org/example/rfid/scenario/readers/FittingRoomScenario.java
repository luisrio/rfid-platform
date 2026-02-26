package org.example.rfid.scenario.readers;

import org.example.rfid.model.RfidEvent;
import org.example.rfid.scenario.contracts.Scenario;
import org.example.rfid.scenario.coordinator.ProductJourneyCoordinator;
import org.example.rfid.util.Time;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class FittingRoomScenario implements Scenario {

    private final String readerId;
    private final ProductJourneyCoordinator coordinator;

    public FittingRoomScenario(String readerId, ProductJourneyCoordinator coordinator) {
        this.readerId = requireNonBlank(readerId, "readerId must not be blank");
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator must not be null");
    }

    @Override
    public String name() {
        return "fitting-room";
    }

    @Override
    public List<RfidEvent> nextEvents() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<String> currentEpcs = coordinator.nextForFitting(1 + random.nextInt(3));
        if (currentEpcs.isEmpty()) {
            return List.of();
        }
        int readsThisTick = 1 + random.nextInt(Math.min(3, currentEpcs.size()));

        List<RfidEvent> events = new ArrayList<>(readsThisTick);
        long now = Time.nowMillis();

        for (int i = 0; i < readsThisTick; i++) {
            String epc = currentEpcs.get(random.nextInt(currentEpcs.size()));
            int rssi = -60 - random.nextInt(18);
            events.add(new RfidEvent(epc, readerId, rssi, now));
        }

        return events;
    }

    private static String requireNonBlank(String value, String message) {
        Objects.requireNonNull(value, message);
        if (value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
