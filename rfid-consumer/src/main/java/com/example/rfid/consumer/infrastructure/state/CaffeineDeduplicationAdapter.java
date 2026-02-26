package com.example.rfid.consumer.infrastructure.state;

import com.example.rfid.consumer.application.port.out.DeduplicationPort;
import com.example.rfid.consumer.infrastructure.config.RfidProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CaffeineDeduplicationAdapter implements DeduplicationPort {

    private final Cache<String, Boolean> cache;

    public CaffeineDeduplicationAdapter(RfidProperties properties) {
        long windowMs = properties.getFilter().getDedupWindowMs();
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMillis(windowMs))
                .maximumSize(200_000)
                .build();
    }

    @Override
    public boolean isDuplicate(String key) {
        return cache.getIfPresent(key) != null;
    }

    @Override
    public void markProcessed(String key) {
        cache.put(key, Boolean.TRUE);
    }
}
