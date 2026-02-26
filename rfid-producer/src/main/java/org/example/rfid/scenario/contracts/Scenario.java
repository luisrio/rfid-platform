package org.example.rfid.scenario.contracts;

import org.example.rfid.model.RfidEvent;

import java.util.List;

public interface Scenario {

    String name();

    List<RfidEvent> nextEvents();
}
