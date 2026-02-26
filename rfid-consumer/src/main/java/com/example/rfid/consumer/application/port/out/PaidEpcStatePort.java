package com.example.rfid.consumer.application.port.out;

public interface PaidEpcStatePort {
    void markPaid(String epc);

    boolean isPaid(String epc);
}
