package com.example.rfid.consumer.infrastructure.state;

import com.example.rfid.consumer.infrastructure.config.RfidProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaffeineDeduplicationAdapterTest {

    @Test
    void markProcessed_setsDuplicateForSameKey() {
        CaffeineDeduplicationAdapter adapter = new CaffeineDeduplicationAdapter(defaultProperties());
        String key = "GATE-01|EPC-4843A4B3CF6E";

        assertFalse(adapter.isDuplicate(key));

        adapter.markProcessed(key);

        assertTrue(adapter.isDuplicate(key));
    }

    @Test
    void duplicateCheck_isKeyScoped() {
        CaffeineDeduplicationAdapter adapter = new CaffeineDeduplicationAdapter(defaultProperties());
        String processedKey = "GATE-01|EPC-4843A4B3CF6E";
        String otherKey = "GATE-01|EPC-13B4368C666B";

        adapter.markProcessed(processedKey);

        assertTrue(adapter.isDuplicate(processedKey));
        assertFalse(adapter.isDuplicate(otherKey));
    }

    private static RfidProperties defaultProperties() {
        RfidProperties properties = new RfidProperties();
        properties.getFilter().setDedupWindowMs(1000);
        return properties;
    }
}
