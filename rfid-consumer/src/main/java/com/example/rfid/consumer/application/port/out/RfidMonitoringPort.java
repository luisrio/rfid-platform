package com.example.rfid.consumer.application.port.out;

public interface RfidMonitoringPort {

    void onMessageReceived(String topic);

    void onInvalidPayload(String topic);

    void onWeakSignal(String topic);

    void onDuplicate(String topic);

    void onPaymentMessageReceived(String topic);

    void onPaymentDuplicateTransaction(String topic, String transactionId);

    void onPaymentEpcsMarkedPaid(String topic, int count);

    void onExitChecked(String topic, boolean paid);

    void onAlertPublished(String topic, String epc, String readerId);

    void onFittingRoomSeen(String topic, String epc);

    void onIgnoredTopic(String topic);
}
