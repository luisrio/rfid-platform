package org.example.rfid.model;

public record RfidEvent(
        String epc,
        String readerId,
        int rssi,
        long timestamp
) {
}
