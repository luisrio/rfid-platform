package com.example.rfid.consumer.application.port.in;

public interface ProcessRfidEventUseCase {
    void process(String topic, String jsonPayload);
}
