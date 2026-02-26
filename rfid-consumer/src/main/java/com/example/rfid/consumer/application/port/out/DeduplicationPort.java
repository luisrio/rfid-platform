package com.example.rfid.consumer.application.port.out;

public interface DeduplicationPort {
    boolean isDuplicate(String key);
    void markProcessed(String key);
}
