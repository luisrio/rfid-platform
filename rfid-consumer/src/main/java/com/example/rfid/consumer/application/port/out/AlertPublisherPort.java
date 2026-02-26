package com.example.rfid.consumer.application.port.out;

public interface AlertPublisherPort {
    void publishUnpaidExitAlert(String epc, String readerId, int rssi, long sourceTimestamp, String sourceTopic);
}
