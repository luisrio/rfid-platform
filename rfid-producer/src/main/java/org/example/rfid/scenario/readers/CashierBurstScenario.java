package org.example.rfid.scenario.readers;

import org.example.rfid.model.RfidEvent;
import org.example.rfid.scenario.contracts.Scenario;
import org.example.rfid.scenario.coordinator.ProductJourneyCoordinator;
import org.example.rfid.util.Time;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class CashierBurstScenario implements Scenario {

    private final String readerId;
    private final int maxDistinctEpcsPerTick;
    private final ProductJourneyCoordinator coordinator;

    public CashierBurstScenario(String readerId, int maxDistinctEpcsPerTick, ProductJourneyCoordinator coordinator) {
        this.readerId = requireNonBlank(readerId, "readerId must not be blank");
        if (maxDistinctEpcsPerTick <= 0) {
            throw new IllegalArgumentException("maxDistinctEpcsPerTick must be > 0");
        }
        this.maxDistinctEpcsPerTick = maxDistinctEpcsPerTick;
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator must not be null");
    }

    @Override
    public String name() {
        return "cashier-burst";
    }

    @Override
    public List<RfidEvent> nextEvents() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int distinctTarget = 1 + random.nextInt(maxDistinctEpcsPerTick);
        List<String> basketEpcs = coordinator.nextForCashier(distinctTarget);
        if (basketEpcs.isEmpty()) {
            return List.of();
        }

        int burstSize = 6 + random.nextInt(13); // 6..18 reads/tick
        List<RfidEvent> events = new ArrayList<>(burstSize);
        long now = Time.nowMillis();

        for (int i = 0; i < burstSize; i++) {
            String epc = basketEpcs.get(random.nextInt(basketEpcs.size()));
            int rssi = -45 - random.nextInt(15);
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
