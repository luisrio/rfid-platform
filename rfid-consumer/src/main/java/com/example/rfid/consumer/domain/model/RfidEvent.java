package com.example.rfid.consumer.domain.model;

public record RfidEvent(Epc epc, ReaderId readerId, Rssi rssi, long timestamp) {
}
