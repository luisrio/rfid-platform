package com.example.rfid.consumer.infrastructure.mqtt;

import com.example.rfid.consumer.application.port.out.AlertPublisherPort;
import com.example.rfid.consumer.infrastructure.config.RfidProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class MqttAlertPublisherAdapter implements AlertPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(MqttAlertPublisherAdapter.class);

    private final MessageChannel mqttOut;
    private final ObjectMapper objectMapper;
    private final RfidProperties properties;

    public MqttAlertPublisherAdapter(
            MessageChannel mqttOut,
            ObjectMapper objectMapper,
            RfidProperties properties
    ) {
        this.mqttOut = mqttOut;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public void publishUnpaidExitAlert(String epc, String readerId, int rssi, long sourceTimestamp, String sourceTopic) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("type", "SECURITY_ALERT");
            payload.put("reason", "UNPAID_EPC_AT_EXIT");
            payload.put("epc", epc);
            payload.put("readerId", readerId);
            payload.put("rssi", rssi);
            payload.put("sourceTimestamp", sourceTimestamp);
            payload.put("sourceTopic", sourceTopic);
            payload.put("publishedAt", Instant.now().toString());

            String json = objectMapper.writeValueAsString(payload);
            boolean sent = mqttOut.send(
                    MessageBuilder.withPayload(json)
                            .setHeader(MqttHeaders.TOPIC, properties.getTopics().getAlerts())
                            .build()
            );
            if (!sent) {
                log.warn("MQTT alert was not accepted by channel. epc={} topic={}", epc, properties.getTopics().getAlerts());
            }
        } catch (Exception e) {
            log.warn("Failed to publish alert for epc={} error={}", epc, e.toString());
        }
    }
}
