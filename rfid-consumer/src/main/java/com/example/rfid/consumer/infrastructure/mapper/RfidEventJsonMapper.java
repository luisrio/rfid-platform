package com.example.rfid.consumer.infrastructure.mapper;

import com.example.rfid.consumer.domain.model.Epc;
import com.example.rfid.consumer.domain.model.ReaderId;
import com.example.rfid.consumer.domain.model.RfidEvent;
import com.example.rfid.consumer.domain.model.Rssi;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RfidEventJsonMapper {

    private static final Logger log = LoggerFactory.getLogger(RfidEventJsonMapper.class);

    private final ObjectMapper objectMapper;

    public RfidEventJsonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<RfidEvent> tryParse(String json) {
        try {
            RawRfidEvent raw = objectMapper.readValue(json, RawRfidEvent.class);

            if (raw.epc == null || raw.epc.isBlank() || raw.readerId == null || raw.readerId.isBlank()) {
                return Optional.empty();
            }
            if (raw.timestamp <= 0) {
                return Optional.empty();
            }

            Epc epc = new Epc(raw.epc.trim());
            ReaderId readerId = new ReaderId(raw.readerId.trim());
            Rssi rssi = new Rssi(raw.rssi);

            return Optional.of(new RfidEvent(epc, readerId, rssi, raw.timestamp));
        } catch (Exception e) {
            log.debug("Invalid RFID JSON payload: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static record RawRfidEvent(
        String epc,
        String readerId,
        int rssi,
        long timestamp
    ){};
}
