package com.example.rfid.consumer.infrastructure.mapper;

import com.example.rfid.consumer.domain.model.RfidEvent;
import com.example.rfid.consumer.support.TestPayloadFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RfidEventJsonMapperTest {

    private RfidEventJsonMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new RfidEventJsonMapper(new ObjectMapper());
    }

    @Test
    void tryParse_parsesValidPayload_andTrimsTextFields() {
        String payload = TestPayloadFactory.rfidEvent(" EPC-13B4368C666B ", " GATE-01 ", -40, 1771698938831L);

        Optional<RfidEvent> result = mapper.tryParse(payload);

        assertTrue(result.isPresent());
        assertEquals("EPC-13B4368C666B", result.get().epc().value());
        assertEquals("GATE-01", result.get().readerId().value());
        assertEquals(-40, result.get().rssi().value());
        assertEquals(1771698938831L, result.get().timestamp());
    }

    @Test
    void tryParse_returnsEmpty_whenEpcIsNull() {
        String payload = TestPayloadFactory.rfidEvent(null, "GATE-01", -40, 1771698938831L);
        assertTrue(mapper.tryParse(payload).isEmpty());
    }

    @Test
    void tryParse_returnsEmpty_whenReaderIdIsBlank() {
        String payload = TestPayloadFactory.rfidEvent("EPC-13B4368C666B", "   ", -40, 1771698938831L);
        assertTrue(mapper.tryParse(payload).isEmpty());
    }

    @Test
    void tryParse_returnsEmpty_whenTimestampIsNotPositive() {
        String payload = TestPayloadFactory.rfidEvent("EPC-13B4368C666B", "GATE-01", -40, 0L);
        assertTrue(mapper.tryParse(payload).isEmpty());
    }

    @Test
    void tryParse_returnsEmpty_whenJsonIsMalformed() {
        assertTrue(mapper.tryParse(TestPayloadFactory.invalidJson()).isEmpty());
    }
}
