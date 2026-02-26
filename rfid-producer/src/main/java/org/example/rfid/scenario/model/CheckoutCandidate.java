package org.example.rfid.scenario.model;

import java.util.List;

public final class CheckoutCandidate {
    public final String transactionId;
    public final List<String> epcs;

    public CheckoutCandidate(String transactionId, List<String> epcs) {
        this.transactionId = transactionId;
        this.epcs = epcs;
    }
}
