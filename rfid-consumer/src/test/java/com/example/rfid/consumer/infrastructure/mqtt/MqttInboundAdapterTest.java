package com.example.rfid.consumer.infrastructure.mqtt;

import com.example.rfid.consumer.application.port.in.ProcessRfidEventUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.nio.charset.StandardCharsets;

class MqttInboundAdapterTest {

    @Test
    void onMessage_forwardsUtf8BytesPayload_andTopicHeader() {
        ProcessRfidEventUseCase useCase = Mockito.mock(ProcessRfidEventUseCase.class);
        MqttInboundAdapter adapter = new MqttInboundAdapter(useCase);
        String json = "{\"epc\":\"EPC-4843A4B3CF6E\"}";
        Message<byte[]> message = MessageBuilder.withPayload(json.getBytes(StandardCharsets.UTF_8))
                .setHeader(MqttHeaders.RECEIVED_TOPIC, "tienda/lecturas/salida/GATE-01")
                .build();

        adapter.onMessage(message);

        Mockito.verify(useCase).process("tienda/lecturas/salida/GATE-01", json);
    }

    @Test
    void onMessage_forwardsStringPayload_andNullTopicWhenHeaderMissing() {
        ProcessRfidEventUseCase useCase = Mockito.mock(ProcessRfidEventUseCase.class);
        MqttInboundAdapter adapter = new MqttInboundAdapter(useCase);
        Message<String> message = MessageBuilder.withPayload("payload-text").build();

        adapter.onMessage(message);

        Mockito.verify(useCase).process(null, "payload-text");
    }

    @Test
    void onMessage_usesStringValueForNonBytePayload() {
        ProcessRfidEventUseCase useCase = Mockito.mock(ProcessRfidEventUseCase.class);
        MqttInboundAdapter adapter = new MqttInboundAdapter(useCase);
        Message<Integer> message = MessageBuilder.withPayload(12345)
                .setHeader(MqttHeaders.RECEIVED_TOPIC, "tienda/lecturas/caja/CAJA-01")
                .build();

        adapter.onMessage(message);

        Mockito.verify(useCase).process("tienda/lecturas/caja/CAJA-01", "12345");
    }
}
