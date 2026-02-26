package com.example.rfid.consumer.infrastructure.state;

import com.example.rfid.consumer.infrastructure.config.RfidProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaffeinePaidEpcStateAdapterTest {

    @Test
    void markPaid_andIsPaid_workForKnownEpc() {
        CaffeinePaidEpcStateAdapter adapter = new CaffeinePaidEpcStateAdapter(defaultProperties());

        adapter.markPaid("EPC-4843A4B3CF6E");

        assertTrue(adapter.isPaid("EPC-4843A4B3CF6E"));
    }

    @Test
    void markPaid_trimsInput_andIsPaidUsesTrimmedLookup() {
        CaffeinePaidEpcStateAdapter adapter = new CaffeinePaidEpcStateAdapter(defaultProperties());

        adapter.markPaid("  EPC-13B4368C666B  ");

        assertTrue(adapter.isPaid("EPC-13B4368C666B"));
    }

    @Test
    void markPaid_ignoresNullAndBlank() {
        CaffeinePaidEpcStateAdapter adapter = new CaffeinePaidEpcStateAdapter(defaultProperties());

        adapter.markPaid(null);
        adapter.markPaid("   ");

        assertFalse(adapter.isPaid(null));
        assertFalse(adapter.isPaid(" "));
        assertFalse(adapter.isPaid("EPC-UNKNOWN"));
    }

    @Test
    void isPaid_returnsFalseForUnknownEpc() {
        CaffeinePaidEpcStateAdapter adapter = new CaffeinePaidEpcStateAdapter(defaultProperties());

        assertFalse(adapter.isPaid("EPC-NOT-MARKED"));
    }

    private static RfidProperties defaultProperties() {
        RfidProperties properties = new RfidProperties();
        properties.getFilter().setPaidTtlMinutes(30);
        return properties;
    }
}
