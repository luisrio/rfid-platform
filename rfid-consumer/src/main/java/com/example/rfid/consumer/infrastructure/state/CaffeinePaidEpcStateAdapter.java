package com.example.rfid.consumer.infrastructure.state;

import com.example.rfid.consumer.application.port.out.PaidEpcStatePort;
import com.example.rfid.consumer.infrastructure.config.RfidProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CaffeinePaidEpcStateAdapter implements PaidEpcStatePort {

    private final Cache<String, Boolean> paid;

    public CaffeinePaidEpcStateAdapter(RfidProperties properties) {
        long ttlMinutes = properties.getFilter().getPaidTtlMinutes();
        this.paid = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(ttlMinutes))
                .maximumSize(1_000_000)
                .build();
    }

    @Override
    public void markPaid(String epc) {
        if (epc == null || epc.isBlank()) {
            return;
        }
        paid.put(epc.trim(), Boolean.TRUE);
    }

    @Override
    public boolean isPaid(String epc) {
        if (epc == null || epc.isBlank()) {
            return false;
        }
        Boolean value = paid.getIfPresent(epc.trim());
        return value != null && value;
    }
}
