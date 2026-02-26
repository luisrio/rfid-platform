package com.example.rfid.consumer.infrastructure.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RfidPropertiesTest {

    @Test
    void defaults_matchExpectedValues() {
        RfidProperties properties = new RfidProperties();

        assertEquals("tcp://localhost:1883", properties.getMqtt().getUrl());
        assertEquals("rfid-consumer-01", properties.getMqtt().getClientId());
        assertEquals("tienda/#", properties.getMqtt().getTopic());
        assertNotNull(properties.getMqtt().getTopics());
        assertTrue(properties.getMqtt().getTopics().isEmpty());
        assertEquals(1, properties.getMqtt().getQos());

        assertEquals(-70, properties.getFilter().getMinRssi());
        assertEquals(1000L, properties.getFilter().getDedupWindowMs());
        assertEquals(30L, properties.getFilter().getPaidTtlMinutes());

        assertEquals("tienda/lecturas/caja/", properties.getTopics().getCashierPrefix());
        assertEquals("tienda/lecturas/salida/", properties.getTopics().getExitPrefix());
        assertEquals("tienda/lecturas/probador/", properties.getTopics().getFittingPrefix());
        assertEquals("tienda/pagos/confirmados", properties.getTopics().getPaymentPrefix());
        assertEquals("tienda/alertas/seguridad", properties.getTopics().getAlerts());

        assertEquals(200L, properties.getMonitoring().getLogEveryMessages());
        assertEquals(1440L, properties.getMonitoring().getFittingSeenTtlMinutes());
    }

    @Test
    void setters_overrideDefaults() {
        RfidProperties properties = new RfidProperties();

        properties.getMqtt().setUrl("tcp://broker:1883");
        properties.getMqtt().setClientId("custom-client");
        properties.getMqtt().setTopic("custom/topic/#");
        properties.getMqtt().setQos(0);
        properties.getFilter().setMinRssi(-60);
        properties.getFilter().setDedupWindowMs(2500L);
        properties.getFilter().setPaidTtlMinutes(45L);
        properties.getTopics().setCashierPrefix("a/");
        properties.getTopics().setExitPrefix("b/");
        properties.getTopics().setFittingPrefix("c/");
        properties.getTopics().setPaymentPrefix("d");
        properties.getTopics().setAlerts("e");
        properties.getMonitoring().setLogEveryMessages(10L);
        properties.getMonitoring().setFittingSeenTtlMinutes(60L);

        assertEquals("tcp://broker:1883", properties.getMqtt().getUrl());
        assertEquals("custom-client", properties.getMqtt().getClientId());
        assertEquals("custom/topic/#", properties.getMqtt().getTopic());
        assertEquals(0, properties.getMqtt().getQos());
        assertEquals(-60, properties.getFilter().getMinRssi());
        assertEquals(2500L, properties.getFilter().getDedupWindowMs());
        assertEquals(45L, properties.getFilter().getPaidTtlMinutes());
        assertEquals("a/", properties.getTopics().getCashierPrefix());
        assertEquals("b/", properties.getTopics().getExitPrefix());
        assertEquals("c/", properties.getTopics().getFittingPrefix());
        assertEquals("d", properties.getTopics().getPaymentPrefix());
        assertEquals("e", properties.getTopics().getAlerts());
        assertEquals(10L, properties.getMonitoring().getLogEveryMessages());
        assertEquals(60L, properties.getMonitoring().getFittingSeenTtlMinutes());
    }
}
