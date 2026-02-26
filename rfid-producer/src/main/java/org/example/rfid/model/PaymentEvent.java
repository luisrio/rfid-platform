package org.example.rfid.model;

import java.util.List;

public record PaymentEvent(
        String transactionId,
        String cashierReaderId,
        long timestamp,
        List<String> paidEpcs
) {
}
