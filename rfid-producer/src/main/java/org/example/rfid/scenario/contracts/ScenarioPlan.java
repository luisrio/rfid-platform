package org.example.rfid.scenario.contracts;

import java.time.Duration;
import java.util.Objects;

public record ScenarioPlan(
        Scenario scenario,
        String topic,
        Duration period
) {
    public ScenarioPlan {
        Objects.requireNonNull(scenario, "scenario must not be null");
        Objects.requireNonNull(topic, "topic must not be null");
        Objects.requireNonNull(period, "period must not be null");
    }
}
