package org.example.rfid.scenario.payment;

import org.example.rfid.model.PaymentEvent;
import org.example.rfid.mqtt.MqttPublisher;
import org.example.rfid.scenario.contracts.PaymentEventPublisher;
import org.example.rfid.util.Json;

import java.util.Objects;

public final class MqttPaymentEventPublisher implements PaymentEventPublisher {

    private final String topic;
    private final MqttPublisher publisher;
    private final long publishConfirmationTimeoutMs;

    public MqttPaymentEventPublisher(String topic, MqttPublisher publisher, long publishConfirmationTimeoutMs) {
        this.topic = requireNonBlank(topic, "topic must not be blank");
        this.publisher = Objects.requireNonNull(publisher, "publisher must not be null");
        if (publishConfirmationTimeoutMs <= 0) {
            throw new IllegalArgumentException("publishConfirmationTimeoutMs must be > 0");
        }
        this.publishConfirmationTimeoutMs = publishConfirmationTimeoutMs;
    }

    @Override
    public boolean publish(PaymentEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        return publisher.publishAndWait(topic, Json.toUtf8Bytes(event), publishConfirmationTimeoutMs);
    }

    private static String requireNonBlank(String value, String message) {
        Objects.requireNonNull(value, message);
        if (value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
