package com.example.rfid.consumer.infrastructure.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class PaymentConfirmationJsonMapper {

    private static final Logger log = LoggerFactory.getLogger(PaymentConfirmationJsonMapper.class);

    private final ObjectMapper objectMapper;

    public PaymentConfirmationJsonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<PaymentConfirmationEvent> tryParse(String json) {
        try {
            RawPaymentConfirmation raw = objectMapper.readValue(json, RawPaymentConfirmation.class);

            if (raw.transactionId == null || raw.transactionId.isBlank() || raw.paidEpcs == null) {
                return Optional.empty();
            }

            List<String> cleanedEpcs = raw.paidEpcs.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .toList();

            return Optional.of(new PaymentConfirmationEvent(raw.transactionId.trim(), cleanedEpcs));
        } catch (Exception e) {
            log.debug("Invalid payment JSON payload: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public record PaymentConfirmationEvent(String transactionId, List<String> paidEpcs) {
    }

    private static record RawPaymentConfirmation(
        String transactionId,
        String cashierReaderId,
        long timestamp,
        List<String> paidEpcs){}
}
