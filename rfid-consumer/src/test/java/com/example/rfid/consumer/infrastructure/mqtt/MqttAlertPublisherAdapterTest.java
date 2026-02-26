package com.example.rfid.consumer.infrastructure.mqtt;

import com.example.rfid.consumer.infrastructure.config.RfidProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MqttAlertPublisherAdapterTest {

    @Test
    void publishUnpaidExitAlert_sendsExpectedJsonToConfiguredAlertTopic() throws Exception {
        MessageChannel mqttOut = Mockito.mock(MessageChannel.class);
        Mockito.when(mqttOut.send(Mockito.any())).thenReturn(true);
        ObjectMapper objectMapper = new ObjectMapper();
        RfidProperties properties = propertiesWithAlertTopic("tienda/alertas/seguridad");
        MqttAlertPublisherAdapter adapter = new MqttAlertPublisherAdapter(mqttOut, objectMapper, properties);

        adapter.publishUnpaidExitAlert("EPC-10B3C002D5FC", "GATE-01", -42, 1771698931640L, "tienda/lecturas/salida/GATE-01");

        ArgumentCaptor<Message<?>> messageCaptor = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(mqttOut).send(messageCaptor.capture());

        Message<?> sentMessage = messageCaptor.getValue();
        assertEquals("tienda/alertas/seguridad", sentMessage.getHeaders().get(MqttHeaders.TOPIC));
        assertTrue(sentMessage.getPayload() instanceof String);

        Map<String, Object> payload = objectMapper.readValue((String) sentMessage.getPayload(), new TypeReference<>() {
        });
        assertEquals("SECURITY_ALERT", payload.get("type"));
        assertEquals("UNPAID_EPC_AT_EXIT", payload.get("reason"));
        assertEquals("EPC-10B3C002D5FC", payload.get("epc"));
        assertEquals("GATE-01", payload.get("readerId"));
        assertEquals(-42, payload.get("rssi"));
        assertEquals(1771698931640L, ((Number) payload.get("sourceTimestamp")).longValue());
        assertEquals("tienda/lecturas/salida/GATE-01", payload.get("sourceTopic"));
        assertNotNull(payload.get("publishedAt"));
        Instant.parse(payload.get("publishedAt").toString());
    }

    @Test
    void publishUnpaidExitAlert_doesNotThrowWhenChannelRejectsMessage() {
        MessageChannel mqttOut = Mockito.mock(MessageChannel.class);
        Mockito.when(mqttOut.send(Mockito.any())).thenReturn(false);
        ObjectMapper objectMapper = new ObjectMapper();
        RfidProperties properties = propertiesWithAlertTopic("tienda/alertas/seguridad");
        MqttAlertPublisherAdapter adapter = new MqttAlertPublisherAdapter(mqttOut, objectMapper, properties);

        adapter.publishUnpaidExitAlert("EPC-10B3C002D5FC", "GATE-01", -42, 1771698931640L, "tienda/lecturas/salida/GATE-01");

        Mockito.verify(mqttOut).send(Mockito.any());
    }

    @Test
    void publishUnpaidExitAlert_doesNotThrowWhenSerializationFails() throws Exception {
        MessageChannel mqttOut = Mockito.mock(MessageChannel.class);
        ObjectMapper objectMapper = Mockito.mock(ObjectMapper.class);
        Mockito.when(objectMapper.writeValueAsString(Mockito.any())).thenThrow(new RuntimeException("boom"));
        RfidProperties properties = propertiesWithAlertTopic("tienda/alertas/seguridad");
        MqttAlertPublisherAdapter adapter = new MqttAlertPublisherAdapter(mqttOut, objectMapper, properties);

        adapter.publishUnpaidExitAlert("EPC-10B3C002D5FC", "GATE-01", -42, 1771698931640L, "tienda/lecturas/salida/GATE-01");

        Mockito.verify(mqttOut, Mockito.never()).send(Mockito.any());
    }

    private static RfidProperties propertiesWithAlertTopic(String alertTopic) {
        RfidProperties properties = new RfidProperties();
        properties.getTopics().setAlerts(alertTopic);
        return properties;
    }
}
