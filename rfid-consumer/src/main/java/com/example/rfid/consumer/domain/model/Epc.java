package com.example.rfid.consumer.domain.model;

public record Epc(String value) {

    public Epc {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("EPC must not be blank");
        }
    }
}
