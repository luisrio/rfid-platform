package org.example.rfid.scenario.model;

import java.util.List;

public record StageReadResult(
        List<String> selectedEpcs,
        List<String> toPaymentPending,
        List<String> toRemove
) {
}
