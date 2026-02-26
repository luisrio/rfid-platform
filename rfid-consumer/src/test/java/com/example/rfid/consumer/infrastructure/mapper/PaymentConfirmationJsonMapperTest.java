package com.example.rfid.consumer.infrastructure.mapper;

import com.example.rfid.consumer.support.TestPayloadFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentConfirmationJsonMapperTest {

    private PaymentConfirmationJsonMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PaymentConfirmationJsonMapper(new ObjectMapper());
    }

    @Test
    void tryParse_parsesValidPayload_andNormalizesValues() {
        String payload = TestPayloadFactory.paymentConfirmation(
                " TX-5BC1984EC46BAC01 ",
                "CAJA-01",
                1771698938441L,
                Arrays.asList(" EPC-4843A4B3CF6E ", "", "   ", null, "EPC-13B4368C666B")
        );

        Optional<PaymentConfirmationJsonMapper.PaymentConfirmationEvent> result = mapper.tryParse(payload);

        assertTrue(result.isPresent());
        assertEquals("TX-5BC1984EC46BAC01", result.get().transactionId());
        assertEquals(List.of("EPC-4843A4B3CF6E", "EPC-13B4368C666B"), result.get().paidEpcs());
    }

    @Test
    void tryParse_returnsEmpty_whenTransactionIdIsNull() {
        String payload = TestPayloadFactory.paymentConfirmation(
                null,
                "CAJA-01",
                1771698938441L,
                List.of("EPC-4843A4B3CF6E")
        );

        assertTrue(mapper.tryParse(payload).isEmpty());
    }

    @Test
    void tryParse_returnsEmpty_whenTransactionIdIsBlank() {
        String payload = TestPayloadFactory.paymentConfirmation(
                "   ",
                "CAJA-01",
                1771698938441L,
                List.of("EPC-4843A4B3CF6E")
        );

        assertTrue(mapper.tryParse(payload).isEmpty());
    }

    @Test
    void tryParse_returnsEmpty_whenPaidEpcsIsNull() {
        String payload = TestPayloadFactory.paymentConfirmation(
                "TX-5BC1984EC46BAC01",
                "CAJA-01",
                1771698938441L,
                null
        );

        assertTrue(mapper.tryParse(payload).isEmpty());
    }

    @Test
    void tryParse_returnsEmpty_whenJsonIsMalformed() {
        assertTrue(mapper.tryParse(TestPayloadFactory.invalidJson()).isEmpty());
    }
}
