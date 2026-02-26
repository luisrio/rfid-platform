package org.example.rfid.mqtt;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;

public final class MqttPublisher implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(MqttPublisher.class);

    private final IMqttAsyncClient client;
    private final int qos;

    public MqttPublisher(String brokerUrl, String clientIdPrefix, int qos) {
        Objects.requireNonNull(brokerUrl, "brokerUrl must not be null");
        Objects.requireNonNull(clientIdPrefix, "clientIdPrefix must not be null");
        if (qos < 0 || qos > 2) {
            throw new IllegalArgumentException("qos must be 0, 1 or 2");
        }

        this.qos = qos;

        try {
            String clientId = clientIdPrefix + "-" + UUID.randomUUID();
            this.client = new MqttAsyncClient(brokerUrl, clientId, new MemoryPersistence());
        } catch (MqttException e) {
            throw new IllegalStateException("Failed to create MQTT client", e);
        }
    }

    public void connect() {
        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(45);

            log.info("Connecting to MQTT broker...");
            client.connect(options).waitForCompletion();

            DisconnectedBufferOptions disconnectedBuffer = new DisconnectedBufferOptions();
            disconnectedBuffer.setBufferEnabled(true);
            disconnectedBuffer.setPersistBuffer(false);
            disconnectedBuffer.setDeleteOldestMessages(true);
            disconnectedBuffer.setBufferSize(1000);
            client.setBufferOpts(disconnectedBuffer);

            log.info("Connected. clientId={}", client.getClientId());
        } catch (MqttException e) {
            throw new IllegalStateException("Failed to connect to broker", e);
        }
    }

    public void publish(String topic, byte[] payload) {
        Objects.requireNonNull(topic, "topic must not be null");
        Objects.requireNonNull(payload, "payload must not be null");

        if (!client.isConnected()) {
            log.debug("MQTT client not connected yet, buffering publish topic={}", topic);
        }

        try {
            MqttMessage message = buildMessage(payload);

            client.publish(topic, message, null, new IMqttActionListener() {
                @Override
                public void onSuccess(org.eclipse.paho.client.mqttv3.IMqttToken asyncActionToken) {
                    // Intentionally quiet to avoid log flooding under bursts.
                }

                @Override
                public void onFailure(org.eclipse.paho.client.mqttv3.IMqttToken asyncActionToken, Throwable exception) {
                    log.warn("Publish failed topic={} error={}", topic, exception.toString());
                }
            });
        } catch (MqttException e) {
            log.warn("Publish threw exception topic={} error={}", topic, e.toString());
        }
    }

    public boolean publishAndWait(String topic, byte[] payload, long timeoutMs) {
        Objects.requireNonNull(topic, "topic must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("timeoutMs must be > 0");
        }

        if (!client.isConnected()) {
            log.debug("MQTT client not connected yet, buffering publish topic={}", topic);
        }

        try {
            MqttMessage message = buildMessage(payload);
            IMqttDeliveryToken token = client.publish(topic, message);
            token.waitForCompletion(timeoutMs);
            return true;
        } catch (MqttException e) {
            log.warn("Publish confirmation failed topic={} error={}", topic, e.toString());
            return false;
        }
    }

    private MqttMessage buildMessage(byte[] payload) {
        MqttMessage message = new MqttMessage(payload);
        message.setQos(qos);
        message.setRetained(false);
        return message;
    }

    @Override
    public void close() {
        try {
            if (client.isConnected()) {
                log.info("Disconnecting MQTT client...");
                client.disconnect().waitForCompletion();
            }
            client.close();
        } catch (MqttException e) {
            log.warn("Error closing MQTT client: {}", e.toString());
        }
    }
}

