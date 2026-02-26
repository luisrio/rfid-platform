package com.example.rfid.consumer.infrastructure.mqtt;

import com.example.rfid.consumer.application.port.in.ProcessRfidEventUseCase;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class MqttInboundAdapter {

    private final ProcessRfidEventUseCase useCase;

    public MqttInboundAdapter(ProcessRfidEventUseCase useCase) {
        this.useCase = useCase;
    }

    @ServiceActivator(inputChannel = "mqttIn")
    public void onMessage(Message<?> message) {
        Object headerTopic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC);
        String topic = headerTopic != null ? headerTopic.toString() : null;

        Object rawPayload = message.getPayload();
        String payload;
        if (rawPayload instanceof byte[] bytes) {
            payload = new String(bytes, StandardCharsets.UTF_8);
        } else {
            payload = String.valueOf(rawPayload);
        }
        useCase.process(topic, payload);
    }
}
