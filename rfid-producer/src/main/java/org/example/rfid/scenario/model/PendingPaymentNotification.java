package org.example.rfid.scenario.model;

import java.util.List;

public record PendingPaymentNotification(
        String transactionId,
        List<String> paidEpcs
) {
}
