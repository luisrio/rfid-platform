package com.example.rfid.consumer.domain.model;

public record ReaderId(String value) {

    public ReaderId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ReaderId must not be blank");
        }
    }
}
