package org.example.rfid.scenario.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.rfid.model.PaymentEvent;
import org.example.rfid.mqtt.MqttPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqttPaymentEventPublisherTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private MqttPublisher publisher;

    @Test
    void constructorValidatesInputs() {
        assertEquals(
                "topic must not be blank",
                assertThrows(IllegalArgumentException.class, () -> new MqttPaymentEventPublisher("   ", publisher, 1000)).getMessage()
        );

        assertEquals(
                "publisher must not be null",
                assertThrows(NullPointerException.class, () -> new MqttPaymentEventPublisher("topic", null, 1000)).getMessage()
        );

        assertEquals(
                "publishConfirmationTimeoutMs must be > 0",
                assertThrows(IllegalArgumentException.class, () -> new MqttPaymentEventPublisher("topic", publisher, 0)).getMessage()
        );
    }

    @Test
    void publishDelegatesToMqttPublisherWithSerializedEvent() throws Exception {
        MqttPaymentEventPublisher eventPublisher = new MqttPaymentEventPublisher("payments/topic", publisher, 2500);
        PaymentEvent event = new PaymentEvent("tx-1", "CAJA-01", 123456L, List.of("EPC-1", "EPC-2"));

        when(publisher.publishAndWait(eq("payments/topic"), any(byte[].class), eq(2500L))).thenReturn(true);

        boolean result = eventPublisher.publish(event);

        assertTrue(result);

        ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(publisher).publishAndWait(eq("payments/topic"), payloadCaptor.capture(), eq(2500L));

        Map<?, ?> payload = MAPPER.readValue(payloadCaptor.getValue(), Map.class);
        assertEquals("tx-1", payload.get("transactionId"));
        assertEquals("CAJA-01", payload.get("cashierReaderId"));
        assertEquals(123456, ((Number) payload.get("timestamp")).intValue());
        assertEquals(List.of("EPC-1", "EPC-2"), payload.get("paidEpcs"));
    }

    @Test
    void publishRejectsNullEvent() {
        MqttPaymentEventPublisher eventPublisher = new MqttPaymentEventPublisher("payments/topic", publisher, 2500);

        assertEquals(
                "event must not be null",
                assertThrows(NullPointerException.class, () -> eventPublisher.publish(null)).getMessage()
        );
    }
}