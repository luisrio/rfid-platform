package org.example.rfid.scenario.contracts;

import org.example.rfid.model.PaymentEvent;

public interface PaymentEventPublisher {

    boolean publish(PaymentEvent event);
}
